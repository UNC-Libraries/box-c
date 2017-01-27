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

import java.io.IOException;
import java.net.URI;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import edu.unc.lib.dl.fedora.FedoraException;

/**
 * This class is responsible for storing a transaction id to a thread-local variable
 * @author harring
 *
 */
public class FedoraTransaction implements AutoCloseable {
	
	private static ThreadLocal<URI> txUriThread = new ThreadLocal<>(); // initial value == null
	// is a transaction already underway on the current thread
	private boolean isChild = true;
	private URI txUri;
	private Repository repo;
	
	public FedoraTransaction(URI txUri, Repository repo) {
		if (!FedoraTransaction.hasTxId()) {
			isChild = false;
			FedoraTransaction.storeTxId(txUri);
		}
		this.txUri = txUri;
		this.repo = repo;
	}
	
	//stores txid to current thread
	private static void storeTxId(URI uri) {
		FedoraTransaction.txUriThread.set(uri);
	}
		
	private static void clearTxId() {
		FedoraTransaction.txUriThread.remove();
	}
	
	public static boolean hasTxId() {
		return FedoraTransaction.txUriThread.get() != null;
	}
	
	public URI getTxUri() {
		return txUri;
	}

	@Override
	public void close() throws Exception {
		if (!isChild) {
			FedoraTransaction.clearTxId();
			repo.commitTransaction(txUri);
		}
	}
}
