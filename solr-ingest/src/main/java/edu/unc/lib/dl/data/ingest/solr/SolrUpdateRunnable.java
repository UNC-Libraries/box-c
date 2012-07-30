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
package edu.unc.lib.dl.data.ingest.solr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.xpath.XPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadataBean;
import edu.unc.lib.dl.search.solr.model.SearchRequest;
import edu.unc.lib.dl.search.solr.model.SearchResultResponse;
import edu.unc.lib.dl.search.solr.model.SearchState;
import edu.unc.lib.dl.search.solr.model.SimpleIdRequest;
import edu.unc.lib.dl.search.solr.service.SearchStateFactory;
import edu.unc.lib.dl.search.solr.util.SearchFieldKeys;

/**
 * Thread which executes solr ingest requests, retrieving data from Fedora and uploading it to Solr in batches. Intended
 * to be run in parallel with other ingest threads reading from the same list of requests.
 * 
 * @author bbpennel
 */
public class SolrUpdateRunnable implements Runnable {
	private static final Logger LOG = LoggerFactory.getLogger(SolrUpdateRunnable.class);
	private static SolrUpdateService solrUpdateService;

	private static Namespace foxmlNS = Namespace.getNamespace("foxml", "info:fedora/fedora-system:def/foxml#");

	public static final int INGEST_PAGE_SIZE = 50;

	private static XPath relsExtXpath;
	private static XPath containerXpath;
	private static XPath containsXpath;

	private SolrUpdateRequest updateRequest = null;

	public SolrUpdateRunnable() {
		LOG.debug("Creating a new SolrIngestThread " + this);
	}

	public static void initQueries() {
		try {
			relsExtXpath = XPath
					.newInstance("/view-inputs/*[local-name() = 'digitalObject']/*[local-name() = 'datastream' and @ID='RELS-EXT']/"
							+ "*[local-name() = 'datastreamVersion']/*[local-name() = 'xmlContent']/*[local-name() = 'RDF']/"
							+ "*[local-name() = 'Description']");

			containerXpath = XPath
					.newInstance("*[local-name() = 'hasModel' and @*[local-name() = 'resource'] = 'info:fedora/cdr-model:Container']");
			containsXpath = XPath.newInstance("*[local-name() = 'contains']/@*[local-name() = 'resource']");
		} catch (JDOMException e) {
			LOG.error("Failed to initialize queries", e);
		}
	}

	public SolrUpdateRequest getUpdateRequest() {
		return updateRequest;
	}

	public void setUpdateRequest(SolrUpdateRequest updateRequest) {
		this.updateRequest = updateRequest;
	}

	public static SolrUpdateService getSolrUpdateService() {
		return solrUpdateService;
	}

	public static void setSolrUpdateService(SolrUpdateService solrUpdateService) {
		SolrUpdateRunnable.solrUpdateService = solrUpdateService;
	}

	/**
	 * Gets the ancestor path facet value for
	 * 
	 * @param updateRequest
	 * @return
	 */
	private BriefObjectMetadataBean getRootAncestorPath(SolrUpdateRequest updateRequest) {
		List<String> resultFields = new ArrayList<String>();
		resultFields.add(SearchFieldKeys.ID);
		resultFields.add(SearchFieldKeys.ANCESTOR_PATH);
		resultFields.add(SearchFieldKeys.RESOURCE_TYPE);

		SimpleIdRequest idRequest = new SimpleIdRequest(updateRequest.getTargetID(), resultFields,
				solrUpdateService.getAccessGroups());

		try {
			return solrUpdateService.getSolrSearchService().getObjectById(idRequest);
		} catch (Exception e) {
			LOG.error("Error while retrieving Solr entry for ", e);
		}
		return null;
	}

	/**
	 * Adds or updates the pid from the request. If the pid was for an object which was a container, then all of its
	 * immediate children (retrieved from pid's contains relations) generate recursive add requests for themselves.
	 * 
	 * @param updateRequest
	 * @throws Exception
	 */
	private void recursiveAdd(SolrUpdateRequest updateRequest) throws Exception {
		try {
			boolean targetAll = false;
			if (SolrUpdateService.TARGET_ALL.equals(updateRequest.getTargetID())) {
				updateRequest.setPid(solrUpdateService.getCollectionsPid().getPid());
				targetAll = true;
			}

			Document resultDoc = getObjectViewXML(updateRequest, targetAll);
			LOG.debug("Recursively adding " + updateRequest.getTargetID());

			if (resultDoc != null && resultDoc.getRootElement().getChild("digitalObject", foxmlNS) != null) {
				// Do no index the current document if it is a full index flag
				if (!targetAll) {
					LOG.debug("Adding " + resultDoc.hashCode());
					solrUpdateService.getUpdateDocTransformer().addDocument(resultDoc);
				}

				Element relsExt = (Element) relsExtXpath.selectSingleNode(resultDoc);
				if (relsExt != null && containerXpath.selectSingleNode(relsExt) != null) {
					// If the parent operation was blocking something, then the spawn actions will too.
					SolrUpdateRequest parentLinkedRequest = updateRequest.getLinkedRequest();
					@SuppressWarnings("unchecked")
					List<Attribute> containsList = (List<Attribute>) containsXpath.selectNodes(relsExt);
					for (Attribute containsResource : containsList) {
						solrUpdateService.offer(new SolrUpdateRequest(containsResource.getValue(),
								SolrUpdateAction.RECURSIVE_ADD, parentLinkedRequest, solrUpdateService.nextMessageID(),
								updateRequest));
					}
				}
			}
		} catch (Exception e) {
			throw new IndexingException("Error while performing a recursive add on " + updateRequest.getTargetID(), e);
		}
	}

	/**
	 * Removes all children of the requested pid if the children have timestamps that predate the timestamp in the
	 * request.
	 * 
	 * @param updateRequest
	 *           Must be a request of type DeleteChildrenPriorToTimestampRequest, with a timestamp indicating when the
	 *           request was generated.
	 * @throws Exception
	 */
	private void deleteChildrenPriorToTimestamp(SolrUpdateRequest updateRequest) throws Exception {
		try {
			if (!(updateRequest instanceof DeleteChildrenPriorToTimestampRequest)) {
				throw new IndexingException("Improper request issued to deleteChildrenPriorToTimestamp.  The request must be of type DeleteChildrenPriorToTimestampRequest");
			}
			DeleteChildrenPriorToTimestampRequest cleanupRequest = (DeleteChildrenPriorToTimestampRequest) updateRequest;

			// Query Solr for the full list of items that will be deleted
			SearchState searchState = SearchStateFactory.createIDSearchState();

			// If the root is not the all target then restrict the delete query to its path.
			if (!SolrUpdateService.TARGET_ALL.equals(updateRequest.getTargetID())) {
				// Get the path facet value for the starting point, since we need the hierarchy tier.
				BriefObjectMetadataBean ancestorPathBean = getRootAncestorPath(updateRequest);
				// If no ancestor path was returned, then this item is either doesn't exist or can't have children, so exit
				if (ancestorPathBean == null) {
					LOG.debug("Canceling deleteChildrenPriorToTimestamp, the root object was not found.");
					return;
				}

				HashMap<String, Object> facets = new HashMap<String, Object>();
				facets.put(SearchFieldKeys.ANCESTOR_PATH, ancestorPathBean.getPath());
				searchState.setFacets(facets);
			}

			// Get as many results as possible
			searchState.setRowsPerPage(Integer.MAX_VALUE);

			// Override default resource types to include folder as well.
			searchState.setResourceTypes(solrUpdateService.getSearchSettings().getResourceTypes());

			// Filter the results to only contain children with timestamps from before the requests timestamp.
			HashMap<String, SearchState.RangePair> rangePairs = new HashMap<String, SearchState.RangePair>();
			rangePairs
					.put(SearchFieldKeys.TIMESTAMP, new SearchState.RangePair(null, cleanupRequest.getTimestampString()));
			searchState.setRangeFields(rangePairs);

			SearchRequest searchRequest = new SearchRequest(searchState, solrUpdateService.getAccessGroups());
			SearchResultResponse orphanedChildResults = solrUpdateService.getSolrSearchService().getSearchResults(
					searchRequest);
			LOG.debug("Orphaned children found: " + orphanedChildResults.getResultList().size());

			// Queue up the children for processing, with a delete tree command for containers and a regular delete
			// otherwise
			for (BriefObjectMetadataBean child : orphanedChildResults.getResultList()) {
				solrUpdateService.offer(child.getId(), SolrUpdateAction.DELETE);
			}
		} catch (Exception e) {
			throw new IndexingException("Error encountered in deleteChildrenPriorToTimestampRequest for " + updateRequest.getTargetID(), e);
		}
	}

	/**
	 * Deletes all items that contained the request pid in their ancestor path.
	 * 
	 * @param updateRequest
	 * @throws Exception
	 */
	private void deleteSolrTree(SolrUpdateRequest updateRequest) throws Exception {
		// If the all target is being deleted, then delete everything
		if (SolrUpdateService.TARGET_ALL.equals(updateRequest.getTargetID())) {
			LOG.debug("Delete Solr Tree, targeting all object.");
			solrUpdateService.getUpdateDocTransformer().deleteQuery("*:*");
			return;
		}

		BriefObjectMetadataBean ancestorPathBean = getRootAncestorPath(updateRequest);
		if (ancestorPathBean == null) {
			LOG.debug("Root object " + updateRequest.getTargetID() + " was not found while attempting to delete tree.");
			return;
		}

		// Determine if the starting node is a container.
		if (ancestorPathBean.getResourceType().equals(solrUpdateService.getSearchSettings().getResourceTypeCollection())
				|| ancestorPathBean.getResourceType().equals(solrUpdateService.getSearchSettings().getResourceTypeFolder())) {
			// Deleting a folder or collection, so perform a full path delete.
			solrUpdateService.getUpdateDocTransformer()
					.deleteQuery(
							solrUpdateService.getSolrSearchService().getSolrSettings().getFieldName(SearchFieldKeys.ID)
									+ ":"
									+ solrUpdateService.getSolrSearchService().getSolrSettings()
											.sanitize(updateRequest.getTargetID()));

			solrUpdateService.getUpdateDocTransformer().deleteQuery(
					solrUpdateService.getSolrSearchService().getSolrSettings().getFieldName(SearchFieldKeys.ANCESTOR_PATH)
							+ ":"
							+ solrUpdateService.getSolrSearchService().getSolrSettings()
									.sanitize(ancestorPathBean.getPath().getSearchValue())
							+ solrUpdateService.getSearchSettings().getFacetSubfieldDelimiter() + "*");
		} else {
			// Targeting an individual file, just delete it.
			solrUpdateService.getUpdateDocTransformer().deleteDocument(updateRequest.getTargetID());
		}
	}

	/**
	 * Deletes all Solr records which have the PID from updateRequest in their collection path facet, and then reindexes
	 * the item and all its children from Fedora. Performed with a tree delete followed by a commit and a recursive add.
	 * 
	 * @param updateRequest
	 */
	private void cleanReindex(SolrUpdateRequest updateRequest) {
		SolrUpdateRequest deleteTreeRequest = new SolrUpdateRequest(updateRequest.getTargetID(),
				SolrUpdateAction.DELETE_SOLR_TREE, solrUpdateService.nextMessageID(), updateRequest);
		SolrUpdateRequest commitRequest = new SolrUpdateRequest(updateRequest.getTargetID(), SolrUpdateAction.COMMIT,
				solrUpdateService.nextMessageID(), updateRequest);
		SolrUpdateRequest recursiveAddRequest = new SolrUpdateRequest(updateRequest.getTargetID(),
				SolrUpdateAction.RECURSIVE_ADD, solrUpdateService.nextMessageID(), updateRequest);
		solrUpdateService.offer(deleteTreeRequest);
		solrUpdateService.offer(commitRequest);
		solrUpdateService.offer(recursiveAddRequest);
	}

	/**
	 * Reindexes the pid of request. If it was a container, than a recursive add is issued for all of its children,
	 * followed by a commit and then a request to delete all children of pid which were not updated by the recursive add,
	 * indicating that they are no longer linked to pid.
	 * 
	 * @param updateRequest
	 */
	private void recursiveReindex(SolrUpdateRequest updateRequest) {
		try {
			boolean targetAll = false;
			if (SolrUpdateService.TARGET_ALL.equals(updateRequest.getTargetID())) {
				updateRequest.setPid(solrUpdateService.getCollectionsPid().getPid());
				targetAll = true;
			}

			long startTime = System.currentTimeMillis();
			Document resultDoc = getObjectViewXML(updateRequest, targetAll);

			if (resultDoc != null && resultDoc.getRootElement().getChild("digitalObject", foxmlNS) != null) {
				// Skip indexing current item if it is the target all flag
				if (!targetAll) {
					solrUpdateService.getUpdateDocTransformer().addDocument(resultDoc);
				}

				Element relsExt = (Element) relsExtXpath.selectSingleNode(resultDoc);
				if (relsExt != null && containerXpath.selectSingleNode(relsExt) != null) {

					@SuppressWarnings("unchecked")
					List<Attribute> containsList = (List<Attribute>) containsXpath.selectNodes(relsExt);

					// Generate cleanup request before offering children to be
					// processed. Set start time to minus one so that the search is less than (instead of <=)
					CountDownUpdateRequest cleanupRequest = new DeleteChildrenPriorToTimestampRequest(
							updateRequest.getTargetID(), SolrUpdateAction.DELETE_CHILDREN_PRIOR_TO_TIMESTAMP,
							solrUpdateService.nextMessageID(), updateRequest, startTime - 1);

					CountDownUpdateRequest commitRequest = new CountDownUpdateRequest(updateRequest.getTargetID(),
							SolrUpdateAction.COMMIT, cleanupRequest, solrUpdateService.nextMessageID(), updateRequest);

					LOG.debug("CleanupRequest: " + cleanupRequest.toString());

					// Get all children, set to block the cleanup request
					for (Attribute containsResource : containsList) {
						SolrUpdateRequest childRequest = new SolrUpdateRequest(containsResource.getValue(),
								SolrUpdateAction.RECURSIVE_ADD, commitRequest, solrUpdateService.nextMessageID(), updateRequest);
						LOG.debug("Queueing for recursive reindex: " + containsResource.getValue() + "|"
								+ childRequest.getTargetID());
						solrUpdateService.offer(childRequest);
					}

					solrUpdateService.offer(commitRequest);
					// Add the cleanup request
					solrUpdateService.offer(cleanupRequest);
				}
			}
		} catch (Exception e) {
			throw new IndexingException("Error while performing a recursive add on " + updateRequest.getTargetID(), e);
		}
	}

	private Document getObjectViewXML(SolrUpdateRequest updateRequest, boolean ignoreAllowIndexing) {
		return getObjectViewXML(updateRequest, ignoreAllowIndexing, 2);
	}

	private Document getObjectViewXML(SolrUpdateRequest updateRequest, boolean ignoreAllowIndexing, int retries) {
		Document resultDoc = null;
		try {
			PID pid = updateRequest.getPid();
			if (ignoreAllowIndexing
					|| (!solrUpdateService.getFedoraDataService().getTripleStoreQueryService().isOrphaned(pid) && solrUpdateService
							.getFedoraDataService().getTripleStoreQueryService().allowIndexing(pid))) {
				LOG.debug("Preparing to retrieve object view for " + updateRequest.getTargetID());
				resultDoc = solrUpdateService.getFedoraDataService().getObjectViewXML(updateRequest.getTargetID(), true);
			}
		} catch (Exception e) {
			LOG.warn("Failed to get ObjectViewXML for " + updateRequest.getTargetID() + ".  Retrying.");
			LOG.debug("", e);
			if (retries > 1) {
				try {
					Thread.sleep(30000L);
					return this.getObjectViewXML(updateRequest, ignoreAllowIndexing, retries - 1);
				} catch (InterruptedException e2) {
					LOG.warn("Retry attempt to retrieve object view XML was interrupted", e);
					Thread.currentThread().interrupt();
				} catch (Exception e2) {
					throw new IndexingException("Failed to get ObjectViewXML for " + updateRequest.getTargetID() + " after two attempts.", e2);
				}
			}

		}
		return resultDoc;
	}

	/**
	 * Overwrites or adds a single object based on the pid of the request.
	 * 
	 * @param updateRequest
	 * @throws Exception
	 */
	private void addObject(SolrUpdateRequest updateRequest) throws Exception {
		// Retrieve object metadata from Fedora and add to update document list
		Document resultDoc = getObjectViewXML(updateRequest, false);
		if (resultDoc != null && resultDoc.getRootElement().getChild("digitalObject", foxmlNS) != null) {
			solrUpdateService.getUpdateDocTransformer().addDocument(resultDoc);
		}
	}

	/**
	 * Deletes everything in the index and issues a commit.
	 * 
	 * @param updateRequest
	 * @throws Exception
	 */
	private void clearIndex(SolrUpdateRequest updateRequest) throws Exception {
		solrUpdateService.getUpdateDocTransformer().deleteDocument("*:*");
		solrUpdateService.offer(new SolrUpdateRequest(updateRequest.getTargetID(), SolrUpdateAction.COMMIT,
				solrUpdateService.nextMessageID(), updateRequest));
	}

	/**
	 * Retrieves the next available ingest request. The request must not effect a pid which is currently locked. If the
	 * next pid is locked, then it is moved to the collision list, and the next pid is polled until an unlocked pid is
	 * found or the list is empty. If there are any items in the collision list, they are treated as if they were at the
	 * beginning of pid queue, meaning that they are examined before polling of the queue begins in order to retain
	 * operation order.
	 * 
	 * @return the next available ingest request which is not locked, or null if none if available.
	 */
	private SolrUpdateRequest nextRequest() {
		SolrUpdateRequest updateRequest = null;
		String pid = null;

		do {
			synchronized (solrUpdateService.getCollisionList()) {
				synchronized (solrUpdateService.getPidQueue()) {
					// First read from the collision list in case there are items that
					// were blocked which need to be read
					if (solrUpdateService.getCollisionList() != null && !solrUpdateService.getCollisionList().isEmpty()) {

						Iterator<SolrUpdateRequest> collisionIt = solrUpdateService.getCollisionList().iterator();
						while (collisionIt.hasNext()) {
							updateRequest = collisionIt.next();
							synchronized (solrUpdateService.getLockedPids()) {
								if (!solrUpdateService.getLockedPids().contains(updateRequest.getTargetID())
										&& !updateRequest.isBlocked()) {
									solrUpdateService.getLockedPids().add(updateRequest.getTargetID());
									collisionIt.remove();
									return updateRequest;
								}
							}
						}
					}

					do {
						// There were no usable pids in the collision list, so read the regular queue.
						updateRequest = solrUpdateService.getPidQueue().poll();
						if (updateRequest != null) {
							pid = updateRequest.getTargetID();
							synchronized (solrUpdateService.getLockedPids()) {
								if (solrUpdateService.getLockedPids().contains(pid) || updateRequest.isBlocked()) {
									solrUpdateService.getCollisionList().add(updateRequest);
									updateRequest.setStatus(ProcessingStatus.BLOCKED);
								} else {
									solrUpdateService.getLockedPids().add(pid);
									return updateRequest;
								}
							}
						}
					} while (updateRequest != null && !Thread.currentThread().isInterrupted());
				}
			}
			// There were no usable requests, so wait a moment.
			try {
				Thread.sleep(200L);
			} catch (InterruptedException e) {
				LOG.warn("Services runnable interrupted while waiting to get next message", e);
				Thread.currentThread().interrupt();
			}
		} while (updateRequest == null && !Thread.currentThread().isInterrupted()
				&& (solrUpdateService.getPidQueue().size() != 0 || solrUpdateService.getCollisionList().size() != 0));
		return null;
	}

	/**
	 * Determines if a commit to Solr is needed and performs it if so.
	 * 
	 * @param forceCommit
	 */
	private void commitSolrChanges(boolean forceCommit) {
		String addDocString = null;
		ExecutionTimer t = new ExecutionTimer();
		synchronized (solrUpdateService.getSolrSearchService()) {
			synchronized (solrUpdateService.getSolrDataAccessLayer()) {
				// Process documents into a single update document if there aren't
				// any more items to process or exceeded page size
				synchronized (solrUpdateService.getUpdateDocTransformer()) {
					// Commit if forceCommit is true and there are documents to commit
					// OR auto commit is on, and there are page size number of documents or no more requests pending
					if ((forceCommit && solrUpdateService.getUpdateDocTransformer().getDocumentCount() > 0)
							|| (solrUpdateService.autoCommit && ((solrUpdateService.getUpdateDocTransformer()
									.getDocumentCount() >= INGEST_PAGE_SIZE) || (solrUpdateService.getPidQueue().size() == 0 && solrUpdateService
									.getCollisionList().size() == 0)))) {
						addDocString = solrUpdateService.getUpdateDocTransformer().exportUpdateDocument();
					}
				}

				// If the update document is populated, then upload it to Solr
				if (addDocString != null) {
					LOG.debug("Submitting to solr: " + addDocString);
					t.start();
					try {
						solrUpdateService.getSolrDataAccessLayer().updateIndex(addDocString);
					} catch (Exception e) {
						throw new IndexingException("Failed to ingest update document to Solr", e, addDocString);
					}
					t.end();
					LOG.info("Uploaded document to Solr: " + t.duration());
				}
			}
		}
	}

	/**
	 * Performs the action indicated by the updateRequest
	 * 
	 * @param updateRequest
	 * @return Whether the action requires an immediate commit to be issued.
	 */
	private boolean performAction(SolrUpdateRequest updateRequest) {
		boolean forceCommit = false;
		try {
			switch (updateRequest.getUpdateAction()) {
				case DELETE:
					// Add a delete request to the update document
					solrUpdateService.getUpdateDocTransformer().deleteDocument(updateRequest.getTargetID());
					break;
				case ADD:
					addObject(updateRequest);
					break;
				case RECURSIVE_ADD:
					recursiveAdd(updateRequest);
					break;
				case DELETE_SOLR_TREE:
					deleteSolrTree(updateRequest);
					break;
				case CLEAN_REINDEX:
					cleanReindex(updateRequest);
					break;
				case RECURSIVE_REINDEX:
					recursiveReindex(updateRequest);
					break;
				case DELETE_CHILDREN_PRIOR_TO_TIMESTAMP:
					deleteChildrenPriorToTimestamp(updateRequest);
					break;
				case COMMIT:
					forceCommit = true;
					break;
				case CLEAR_INDEX:
					clearIndex(updateRequest);
					break;
			}
		} catch (Exception e) {
			updateRequest.setStatus(ProcessingStatus.FAILED);
			LOG.error("An error occurred while attempting perform action " + updateRequest.getAction() + " on object "
					+ updateRequest.getTargetID(), e);
		}
		return forceCommit;
	}

	@Override
	public void run() {
		try {
			String pid = null;
			boolean forceCommit = false;
			// Get the next pid and lock it
			updateRequest = nextRequest();

			if (updateRequest != null) {
				solrUpdateService.getActiveMessages().add(updateRequest);
				updateRequest.setStatus(ProcessingStatus.ACTIVE);
				try {
					// Quit before doing work if thread was interrupted
					if (Thread.currentThread().isInterrupted())
						return;
					// Get the next available pid
					LOG.debug("Obtained " + updateRequest.getTargetID() + "|" + updateRequest.getAction());
					pid = updateRequest.getTargetID();
					forceCommit = performAction(updateRequest);
				} finally {
					// If needed, perform commit before completing this request
					commitSolrChanges(forceCommit);
					// Finish request and cleanup
					updateRequest.requestCompleted();
					solrUpdateService.getActiveMessages().remove(updateRequest);
					solrUpdateService.getLockedPids().remove(pid);
					if (ProcessingStatus.FAILED.equals(updateRequest.getStatus())) {
						solrUpdateService.getFailedMessages().add(updateRequest);
					} else {
						solrUpdateService.getFinishedMessages().add(updateRequest);
					}
					LOG.debug("Processed pid " + pid);
				}
			} else {
				// Commit changes to solr if they are ready to go
				commitSolrChanges(forceCommit);
			}
		} catch (Exception e) {
			// Encountered an exception
			LOG.error("Encountered an exception while ingesting to Solr.  Finished SolrIngestThread", e);
		}
	}
}
