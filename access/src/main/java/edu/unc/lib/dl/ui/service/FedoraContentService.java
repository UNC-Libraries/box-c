/**
 * Copyright 2008 The University of North Carolina at Chapel Hill
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
package edu.unc.lib.dl.ui.service;

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.DefaultMethodRetryHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NoHttpResponseException;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import edu.unc.lib.dl.fedora.AccessClient;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.ui.exception.ResourceNotFoundException;
import edu.unc.lib.dl.ui.util.FedoraUtil;
import edu.unc.lib.dl.ui.util.FileIOUtil;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.util.TripleStoreQueryService;

/**
 * Connects to and streams datastreams from Fedora.
 * 
 * @author bbpennel
 */
public class FedoraContentService {
	private static final Logger LOG = LoggerFactory.getLogger(FedoraContentService.class);
	private AccessClient accessClient = null;
	private TripleStoreQueryService tripleStoreQueryService = null;
	@Autowired
	private FedoraUtil fedoraUtil = null;

	public FedoraContentService() {

	}

	public edu.unc.lib.dl.fedora.AccessClient getAccessClient() {
		return accessClient;
	}

	public void setAccessClient(edu.unc.lib.dl.fedora.AccessClient accessClient) {
		this.accessClient = accessClient;
	}

	public TripleStoreQueryService getTripleStoreQueryService() {
		return tripleStoreQueryService;
	}

	public void setTripleStoreQueryService(TripleStoreQueryService tripleStoreQueryService) {
		this.tripleStoreQueryService = tripleStoreQueryService;
	}

	public FedoraUtil getFedoraUtil() {
		return fedoraUtil;
	}

	public void setFedoraUtil(FedoraUtil fedoraUtil) {
		this.fedoraUtil = fedoraUtil;
	}

	public void streamData(String simplepid, String datastream, OutputStream outStream) {
		streamData(simplepid, datastream, outStream, null, null, true);
	}

	public void streamData(String simplepid, String datastream, OutputStream outStream, HttpServletResponse response,
			String fileExtension, boolean asAttachment){
		this.streamData(simplepid, datastream, outStream, response, fileExtension, asAttachment, 1);
	}
	
	public void streamData(String simplepid, String datastream, OutputStream outStream, HttpServletResponse response,
			String fileExtension, boolean asAttachment, int retryServerError) {
		String dataUrl = fedoraUtil.getFedoraUrl();

		HttpClient client = new HttpClient();

		UsernamePasswordCredentials cred = new UsernamePasswordCredentials(accessClient.getUsername(),
				accessClient.getPassword());
		client.getState().setCredentials(new AuthScope(null, 443), cred);
		client.getState().setCredentials(new AuthScope(null, 80), cred);

		GetMethod method = new GetMethod(dataUrl + "/objects/" + simplepid + "/datastreams/" + datastream + "/content");

		try {
			method.setDoAuthentication(true);
			client.executeMethod(method);
			if (method.getStatusCode() == HttpStatus.SC_OK) {
				if (response != null) {
					PID pid = new PID(simplepid);

					String mimeType = null;
					if (fileExtension != null && fileExtension.equals("mp3")) {
						mimeType = "audio/mpeg";
					}
					if (mimeType == null && datastream.equals(ContentModelHelper.Datastream.DATA_FILE.getName())) {
						mimeType = tripleStoreQueryService.lookupSourceMimeType(pid);
					}
					if (mimeType == null) {
						response.setHeader("Content-Type", method.getResponseHeader("content-type").getValue());
					} else {
						response.setHeader("Content-Type", mimeType);
					}

					String slug = null;
					try {
						slug = tripleStoreQueryService.lookupSlug(pid);
						if (slug != null) {
							if (fileExtension != null && !slug.toLowerCase().contains("." + fileExtension.toLowerCase())) {
								slug += "." + fileExtension;
							}
						}
					} catch (Exception e) {
						LOG.error("Error while attempting to retrieve slug for " + simplepid, e);
					}

					if (asAttachment) {
						response.setHeader("content-disposition", "attachment; filename=\"" + slug + "\"");
					} else {
						response.setHeader("content-disposition", "inline; filename=\"" + slug + "\"");
					}
				}

				try {
					FileIOUtil.stream(outStream, method);
				} catch (IOException e) {
					LOG.info("Problem retrieving " + dataUrl + " for " + simplepid + ": " + e.getMessage());
				} finally {
					method.releaseConnection();
				}
			} else {
				//Retry server errors
				if (method.getStatusCode() == 500 && retryServerError > 0){
					LOG.warn("Failed to retrieve " + method.getURI().getURI() + ", retrying.");
					this.streamData(simplepid, datastream, outStream, response, fileExtension, asAttachment, retryServerError-1);
				} else {
					throw new ResourceNotFoundException("Failure to fedora content due to response of: " + method.getStatusLine().toString() + 
							"\nPath was: " + method.getURI().getURI());
				}
			}
		} catch (Exception e) {
			LOG.error("Error while attempting to stream Fedora content for " + simplepid, e);
		}
	}
}
