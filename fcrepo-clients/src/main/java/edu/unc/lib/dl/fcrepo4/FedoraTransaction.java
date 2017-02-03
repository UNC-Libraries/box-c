/**
 * Copyright 2017 The University of North Carolina at Chapel Hill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.unc.lib.dl.fcrepo4;

import java.net.URI;

/**
 * This class is responsible for storing a transaction id to a thread-local variable
 * @author harring
 *
 */
public class FedoraTransaction implements AutoCloseable {
	
	protected static ThreadLocal<URI> txUriThread = new ThreadLocal<>(); // initial value == null
	protected static ThreadLocal<FedoraTransaction> rootTxThread = new ThreadLocal<>(); // keeps track of root tx
	// whether tx is already underway on the current thread
	private boolean isSub = true;
	private boolean isCancelled = false;
	private URI txUri;
	private Repository repo;
	
	public FedoraTransaction(URI txUri, Repository repo) {
		// if tx is root
		if (rootTxThread.get() == null) {
			rootTxThread.set(this);
			storeTxId(txUri);
			isSub = false;
		}
		this.txUri = txUri;
		this.repo = repo;
	}
	
	public static boolean hasTxId() {
		return FedoraTransaction.txUriThread.get() != null;
	}
	
	public URI getTxUri() {
		return txUri;
	}

	@Override
	public void close() throws Exception {
		if (!isSub && !isCancelled) {
			repo.commitTransaction(txUri);
			clearTxId();
			txUri = null;
		}
	}
	
	public void keepAlive(URI txUri) {
		//TODO implement clock to extend tx for ~ 1 hour?
		repo.keepTransactionAlive(txUri);
	}
	
	public void cancel() {
		if (isSub) {
			// sub tx defers to root
			FedoraTransaction.rootTxThread.get().cancel();
		} else if (!isCancelled) {
			isCancelled = true;
			clearTxId();
			FedoraTransaction.rootTxThread.set(null);
			repo.cancelTransaction(txUri);
		}
		throw new TransactionCancelledException("The transaction with id " + txUri + " was rolled back");
	}
	
	//stores txid to current thread
	private static void storeTxId(URI uri) {
		FedoraTransaction.txUriThread.set(uri);
	}
	//clears txid from current thread	
	private static void clearTxId() {
		FedoraTransaction.txUriThread.remove();
	}
	
	
}
