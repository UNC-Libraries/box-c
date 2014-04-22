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
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.GroupsThreadStore;
import edu.unc.lib.dl.fedora.AccessClient;
import edu.unc.lib.dl.fedora.AuthorizationException;
import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.httpclient.HttpClientUtil;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadataBean;
import edu.unc.lib.dl.search.solr.model.Datastream;
import edu.unc.lib.dl.search.solr.model.SimpleIdRequest;
import edu.unc.lib.dl.search.solr.util.SearchFieldKeys;
import edu.unc.lib.dl.search.solr.util.SearchSettings;
import edu.unc.lib.dl.ui.exception.ClientAbortException;
import edu.unc.lib.dl.ui.exception.InvalidRecordRequestException;
import edu.unc.lib.dl.ui.exception.ResourceNotFoundException;
import edu.unc.lib.dl.ui.util.AccessUtil;
import edu.unc.lib.dl.ui.util.AnalyticsTrackerUtil;
import edu.unc.lib.dl.ui.util.FedoraUtil;
import edu.unc.lib.dl.ui.util.FileIOUtil;
import edu.unc.lib.dl.util.ContentModelHelper;
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

	@Autowired
	private SearchSettings searchSettings;
	@Autowired
	protected SolrQueryLayerService queryLayer;

	@Autowired
	protected AnalyticsTrackerUtil analyticsTracker;

	private final int numberOfRetries = 1;

	private static List<String> resultFields = Arrays.asList(SearchFieldKeys.ID.name(),
			SearchFieldKeys.DATASTREAM.name(), SearchFieldKeys.RELATIONS.name(), SearchFieldKeys.RESOURCE_TYPE.name(),
			SearchFieldKeys.ROLE_GROUP.name(), SearchFieldKeys.PARENT_COLLECTION.name(),
			SearchFieldKeys.ANCESTOR_PATH.name(), SearchFieldKeys.TITLE.name());

	public void streamData(String pid, String datastream, boolean download, String gaCid, HttpServletResponse response) {
		AccessGroupSet accessGroups = GroupsThreadStore.getGroups();

		// Default datastream is DATA_FILE
		if (datastream == null) {
			datastream = ContentModelHelper.Datastream.DATA_FILE.toString();
		}

		// Use solr to check if the user is allowed to view this item.
		SimpleIdRequest idRequest = new SimpleIdRequest(pid, resultFields, accessGroups);

		BriefObjectMetadataBean briefObject = queryLayer.getObjectById(idRequest);
		// If the record isn't accessible then invalid record exception.
		if (briefObject == null) {
			throw new InvalidRecordRequestException();
		}
		// Block access to thumbnails for non-containers,
		if (AccessUtil.hasListAccessOnly(accessGroups, briefObject)
				&& (searchSettings.resourceTypeFile.equals(briefObject.getResourceType()) || searchSettings.resourceTypeAggregate
						.equals(briefObject.getResourceType())))
			throw new InvalidRecordRequestException();
		// Grab out the slug if its available, to be used as the filename.
		List<String> slugRelations = briefObject.getRelation("slug");
		String slug = null;
		if (slugRelations != null && slugRelations.size() > 0)
			slug = slugRelations.get(0);

		try {
			edu.unc.lib.dl.search.solr.model.Datastream datastreamResult = briefObject.getDatastreamObject(datastream);
			if (datastreamResult == null)
				throw new ResourceNotFoundException("Datastream " + datastream + " was not found on object " + pid);

			// Track the download event if the request is for the original content
			if (datastreamResult.getDatastreamCategory() != null
					&& datastreamResult.getDatastreamCategory().equals(DatastreamCategory.ORIGINAL)) {
				analyticsTracker.trackEvent(gaCid, briefObject.getParentCollectionObject() == null ?
						"(no collection)" : briefObject.getParentCollectionObject().getDisplayValue(),
						"download", briefObject.getTitle() + "|" + pid, null);
			}

			this.streamData(pid, datastreamResult, slug, response, download, numberOfRetries);
		} catch (AuthorizationException e) {
			throw new InvalidRecordRequestException(e);
		} catch (ResourceNotFoundException e) {
			LOG.info("Resource not found while attempting to stream datastream", e);
			throw e;
		} catch (Exception e) {
			LOG.error("Failed to retrieve content for " + pid + " datastream: " + datastream, e);
			throw new ResourceNotFoundException();
		}
	}

	private void streamData(String simplepid, Datastream datastream, String slug, HttpServletResponse response,
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

	public void setAccessClient(edu.unc.lib.dl.fedora.AccessClient accessClient) {
		this.accessClient = accessClient;
	}

	public void setFedoraUtil(FedoraUtil fedoraUtil) {
		this.fedoraUtil = fedoraUtil;
	}

	public void setSearchSettings(SearchSettings searchSettings) {
		this.searchSettings = searchSettings;
	}

	public void setQueryLayer(SolrQueryLayerService queryLayer) {
		this.queryLayer = queryLayer;
	}
}