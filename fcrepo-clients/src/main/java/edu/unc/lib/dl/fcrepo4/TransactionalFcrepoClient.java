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

import org.apache.http.client.methods.HttpRequestBase;
import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoOperationFailedException;
import org.fcrepo.client.FcrepoResponse;

/**
 * This wrapper class rewrites uris to have a transaction id (txid) in them, if one is present on the current thread
 * If no txid is present, class behavior is identical to FcrepoClient
 * @author harring
 *
 */
public class TransactionalFcrepoClient extends FcrepoClient {
	
	private static final String REST = "/rest";
	
	protected TransactionalFcrepoClient(String username, String password, String host, Boolean throwExceptionOnFailure) {
		super(username, password, host, throwExceptionOnFailure);
	}
	
	@Override
	public FcrepoResponse executeRequest(URI uri, HttpRequestBase request)
            throws FcrepoOperationFailedException {
		if (hasTxId()) {
			URI txUri = rewriteUri(uri); 
			return super.executeRequest(txUri, request);
		}
		return super.executeRequest(uri, request);
    }
	
	private URI rewriteUri(URI rescUri) {
		URI txUri = FedoraTransaction.txUriThread.get();
		String rescId = rescUri.toString();
		int txIdIndex = rescId.indexOf(REST);
		rescId = rescId.substring(txIdIndex + REST.length());
		return txUri.resolve(rescId);
	}
	
	private boolean hasTxId() {
		return FedoraTransaction.hasTxId();
	}
}
