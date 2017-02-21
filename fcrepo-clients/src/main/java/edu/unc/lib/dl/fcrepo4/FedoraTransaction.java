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
 * NB: care must be taken to close a transaction when it is no longer needed; otherwise the old
 * txId will persist on the thread and may interfere with future transactions
 * 
 * @author harring
 *
 */
public class FedoraTransaction {
	
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
			txUriThread.set(txUri);
			isSub = false;
		}
		this.txUri = txUri;
		this.repo = repo;
	}
	
	public static boolean hasTxId() {
		return txUriThread.get() != null;
	}
	
	public static boolean isStillAlive() {
		return rootTxThread.get() != null;
	}
	
	public URI getTxUri() {
		return txUri;
	}

	public void close() {
		if (!isSub && !isCancelled) {
			repo.commitTransaction(txUri);
			txUriThread.remove();
			rootTxThread.remove();
		}
		txUri = null;
	}
	
	public void keepAlive() {
		//TODO implement clock to extend tx for ~ 1 hour?
		repo.keepTransactionAlive(txUri);
	}
	
	public void cancel(Throwable t) {
		if (isSub) {
			// sub tx defers to root
			FedoraTransaction.rootTxThread.get().cancel();
		} else if (!isCancelled) {
			isCancelled = true;
			txUriThread.remove();
			rootTxThread.remove();
			repo.cancelTransaction(txUri);
		}
		if (t instanceof TransactionCancelledException) {
			throw (TransactionCancelledException) t;
		} else {
			throw new TransactionCancelledException("The transaction with id " + txUri + " was rolled back", t);
		}
	}
	
	public void cancel() {
		if (isSub) {
			// sub tx defers to root
			FedoraTransaction.rootTxThread.get().cancel();
		} else if (!isCancelled) {
			isCancelled = true;
			txUriThread.remove();
			rootTxThread.remove();
			repo.cancelTransaction(txUri);
		}
		throw new TransactionCancelledException("The transaction with id " + txUri + " was rolled back");
	}
	
}
