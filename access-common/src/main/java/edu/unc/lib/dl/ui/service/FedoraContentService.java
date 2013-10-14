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

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.fedora.AccessClient;
import edu.unc.lib.dl.fedora.AuthorizationException;
import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.acl.util.GroupsThreadStore;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.httpclient.HttpClientUtil;
import edu.unc.lib.dl.search.solr.model.Datastream;
import edu.unc.lib.dl.ui.exception.ClientAbortException;
import edu.unc.lib.dl.ui.exception.ResourceNotFoundException;
import edu.unc.lib.dl.ui.util.FedoraUtil;
import edu.unc.lib.dl.ui.util.FileIOUtil;
import edu.unc.lib.dl.util.ContentModelHelper.DatastreamCategory;

/**
 * Connects to and streams datastreams from Fedora.
 * 
 * @author bbpennel
 */
public class FedoraContentService {
	private static final Logger LOG = LoggerFactory.getLogger(FedoraContentService.class);

	private AccessClient accessClient;

	private FedoraUtil fedoraUtil;

	public void setAccessClient(edu.unc.lib.dl.fedora.AccessClient accessClient) {
		this.accessClient = accessClient;
	}

	public void setFedoraUtil(FedoraUtil fedoraUtil) {
		this.fedoraUtil = fedoraUtil;
	}

	public void streamData(String simplepid, Datastream datastream, String slug, HttpServletResponse response,
			boolean asAttachment) throws FedoraException, IOException {
		this.streamData(simplepid, datastream, slug, response, asAttachment, 1);
	}

	public void streamData(String simplepid, Datastream datastream, String slug, HttpServletResponse response,
			boolean asAttachment, int retryServerError) throws FedoraException, IOException {
		OutputStream outStream = response.getOutputStream();

		String dataUrl = fedoraUtil.getFedoraUrl() + "/objects/" + simplepid + "/datastreams/" + datastream.getName()
				+ "/content";

		HttpClient client = HttpClientUtil.getAuthenticatedClient(dataUrl, accessClient.getUsername(),
				accessClient.getPassword());
		client.getParams().setAuthenticationPreemptive(true);
		GetMethod method = new GetMethod(dataUrl);
		method.addRequestHeader(HttpClientUtil.FORWARDED_GROUPS_HEADER, GroupsThreadStore.getGroupString());

		try {
			client.executeMethod(method);

			if (method.getStatusCode() == HttpStatus.SC_OK) {
				if (response != null) {
					PID pid = new PID(simplepid);

					// Adjusting content related headers

					// Use the content length from Fedora it is not provided or negative, in which case use solr's
					long contentLength;
					try {
						String contentLengthString = method.getResponseHeader("content-length").getValue();
						contentLength = Long.parseLong(contentLengthString);
					} catch (Exception e) {
						// If the content length wasn't provided or wasn't a number, set it to -1
						contentLength = -1L;
					}
					if (contentLength < 0L) {
						contentLength = datastream.getFilesize();
					}
					response.setHeader("Content-Length", Long.toString(contentLength));

					// Use Fedora's content type unless it is unset or octet-stream
					String mimeType;
					try {
						mimeType = method.getResponseHeader("content-type").getValue();
						if (mimeType == null || "application/octet-stream".equals(mimeType)) {
							if ("mp3".equals(datastream.getExtension())) {
								mimeType = "audio/mpeg";
							} else {
								mimeType = datastream.getMimetype();
							}
						}
					} catch (Exception e) {
						mimeType = datastream.getMimetype();
					}
					response.setHeader("Content-Type", mimeType);

					// Setting the filename header for the response
					if (slug == null) {
						slug = pid.getPid();
					}
					// For metadata types files, append the datastream name
					if (datastream.getDatastreamCategory().equals(DatastreamCategory.METADATA)
							|| datastream.getDatastreamCategory().equals(DatastreamCategory.ADMINISTRATIVE)) {
						slug += "_" + datastream.getName();
					}
					// Add the file extension unless its already in there.
					if (datastream.getExtension() != null && datastream.getExtension().length() > 0
							&& !slug.toLowerCase().endsWith("." + datastream.getExtension())
							&& !"unknown".equals(datastream.getExtension())) {
						slug += "." + datastream.getExtension();
					}
					if (asAttachment) {
						response.setHeader("content-disposition", "attachment; filename=\"" + slug + "\"");
					} else {
						response.setHeader("content-disposition", "inline; filename=\"" + slug + "\"");
					}
				}

				// Stream the content
				FileIOUtil.stream(outStream, method);
			} else if (method.getStatusCode() == HttpStatus.SC_FORBIDDEN) {
				throw new AuthorizationException(
						"User does not have sufficient permissions to retrieve the specified object");
			} else {
				// Retry server errors
				if (method.getStatusCode() == 500 && retryServerError > 0) {
					LOG.warn("Failed to retrieve " + dataUrl + ", retrying.");
					this.streamData(simplepid, datastream, slug, response, asAttachment, retryServerError - 1);
				} else {
					throw new ResourceNotFoundException("Failure to stream fedora content due to response of: "
							+ method.getStatusLine().toString() + "\nPath was: " + dataUrl);
				}
			}
		} catch (ClientAbortException e) {
			if (LOG.isDebugEnabled())
				LOG.debug("User client aborted request to stream Fedora content for " + simplepid, e);
		} catch (HttpException e) {
			LOG.error("Error while attempting to stream Fedora content for " + simplepid, e);
		} catch (IOException e) {
			LOG.warn("Problem retrieving " + dataUrl + " for " + simplepid, e);
		} finally {
			if (method != null)
				method.releaseConnection();
		}

	}
}