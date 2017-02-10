/**
 * Copyright 2017 The University of North Carolina at Chapel Hill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *		 http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.unc.lib.dl.fcrepo4;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Arrays;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.InputStreamEntity;
import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoOperationFailedException;
import org.fcrepo.client.FcrepoResponse;

import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.util.URIUtil;


/**
 * This wrapper class rewrites uris to have a transaction id (txid) in them, if one is present on the current thread
 * If no txid is present, class behavior is identical to FcrepoClient
 * @author harring
 *
 */
public class TransactionalFcrepoClient extends FcrepoClient {
	
	private static final String REST = "/rest";
	
	private static final String[] MIMETYPES = {"application/sparql-update", "text/turtle", "text/rdf+n3",
																	"application/n3", "text/n3", "application/rdf+xml",
																	"application/n-triples", "text/html", "text/plain", "application/ld+json",
																	"message/external-body"};
	
	private String fedoraBaseRest;
	
	protected TransactionalFcrepoClient(String username, String password, String host,
															Boolean throwExceptionOnFailure, String fedoraBase) {
		super(username, password, host, throwExceptionOnFailure);
		this.fedoraBaseRest = fedoraBase + REST;
	}
	
	/**
	 * Build a TransactionalFcrepoClient
	 * 
	 * @return a client builder
	 */
	public static FcrepoClientBuilder client() {
		return new TransactionalFcrepoClientBuilder();
	}
	
	@Override
	public FcrepoResponse executeRequest(URI uri, HttpRequestBase request)
			throws FcrepoOperationFailedException {
		if (hasTxId()) {
			if (needsBodyRewrite(request)) {
				rewriteRequestBodyUris(request);
			}
			URI txUri = rewriteUri(uri);
			request.setURI(txUri);
			return super.executeRequest(txUri, request);
		}
		return super.executeRequest(uri, request);
	}
	
	private URI rewriteUri(URI rescUri) {
		URI txUri = FedoraTransaction.txUriThread.get();
		String rescId = rescUri.toString();
		int txIdIndex = rescId.indexOf(REST);
		rescId = rescId.substring(txIdIndex + REST.length());
		return URI.create(edu.unc.lib.dl.util.URIUtil.join(txUri, rescId));
	}
	
	private boolean hasTxId() {
		return FedoraTransaction.hasTxId();
	}
	
	private boolean needsBodyRewrite(HttpRequestBase request) {
		org.apache.http.Header contentTypeHeader = request.getFirstHeader("Content-Type");
		String contentType = contentTypeHeader.getValue();
		List<String> mimetypes = Arrays.asList(MIMETYPES);
		// request method is POST, PUT, or PATCH AND has one of the whitelisted mimetypes
		return request.getMethod().startsWith("P") && mimetypes.contains(contentType);
	}
 	
	private HttpRequestBase rewriteRequestBodyUris(HttpRequestBase request) {
		HttpEntity requestBody = ((HttpEntityEnclosingRequestBase) request).getEntity();
		if (requestBody != null) {
			String bodyString = null;
			try (InputStream stream = requestBody.getContent()) {
				bodyString = stream.toString();
			} catch (IOException e) {
				throw new FedoraException("Could not stream content from body of request", e);
			}
			String txId = FedoraTransaction.txUriThread.get().toString();
			String replacementUri = URIUtil.join(URI.create(fedoraBaseRest), txId).toString();
			String replacementBody = StringUtils.replace(bodyString, fedoraBaseRest, replacementUri);
			InputStream replacementStream = new ByteArrayInputStream(replacementBody.getBytes());
			InputStreamEntity replacementEntity = new InputStreamEntity(replacementStream);
			((HttpEntityEnclosingRequestBase) request).setEntity(replacementEntity);
			return request;
		}
		return null;
	}
	
	public static class TransactionalFcrepoClientBuilder extends FcrepoClientBuilder {
		
		private String authUser;

		private String authPassword;

		private String authHost;

		private boolean throwExceptionOnFailure;
		
		private String fedoraBase;
		
		public TransactionalFcrepoClientBuilder() {
			// TODO Auto-generated constructor stub
		}

		/**
		 * Add basic authentication credentials to this client
		 * 
		 * @param username username for authentication
		 * @param password password for authentication
		 * @return the client builder
		 */
		@Override
		public FcrepoClientBuilder credentials(final String username, final String password) {
			this.authUser = username;
			this.authPassword = password;
			return this;
		}

		/**
		 * Add an authentication scope to this client
		 * 
		 * @param authHost authentication scope value
		 * @return this builder
		 */
		@Override
		public FcrepoClientBuilder authScope(final String authHost) {
			this.authHost = authHost;
			return this;
		}

		/**
		 * Client should throw exceptions when failures occur
		 * 
		 * @return this builder
		 */
		@Override
		public FcrepoClientBuilder throwExceptionOnFailure() {
			this.throwExceptionOnFailure = true;
			return this;
		}
		
		/**
		 * Add a fedora base + rest/ uri to this client
		 * 
		 * @param fedoraBaseRest the base uri
		 * @return this builder
		 */
		public FcrepoClientBuilder fedoraBase(final String fedoraBase) {
			this.fedoraBase = fedoraBase;
			return this;
		}

		/**
		 * Get the client
		 * 
		 * @return the client constructed by this builder
		 */
		public FcrepoClient build() {
			return new TransactionalFcrepoClient(authUser, authPassword, authHost, throwExceptionOnFailure, fedoraBase);
		}
	}
}
