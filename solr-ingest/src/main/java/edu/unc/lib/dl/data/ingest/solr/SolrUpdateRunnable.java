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

import java.util.Iterator;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.data.ingest.solr.action.IndexingAction;
import edu.unc.lib.dl.util.IndexingActionType;

/**
 * Thread which executes solr ingest requests, retrieving data from Fedora and uploading it to Solr in batches. Intended
 * to be run in parallel with other ingest threads reading from the same list of requests.
 * 
 * @author bbpennel
 */
public class SolrUpdateRunnable implements Runnable {
	private static final Logger LOG = LoggerFactory.getLogger(SolrUpdateRunnable.class);

	// Context
	private SolrUpdateService solrUpdateService;
	private Map<IndexingActionType, IndexingAction> solrIndexingActionMap;
	private long idleWaitTime = 1000L;
	private long nextMessageWaitTime = 0L;
	private long blockedWaitTime = 200L;

	// State information
	private SolrUpdateRequest updateRequest = null;

	public SolrUpdateRunnable() {
		LOG.debug("Creating a new SolrIngestThread " + this);
	}

	public SolrUpdateRequest getUpdateRequest() {
		return updateRequest;
	}

	public void setSolrUpdateService(SolrUpdateService solrUpdateService) {
		this.solrUpdateService = solrUpdateService;
	}

	public void setSolrIndexingActionMap(Map<IndexingActionType, IndexingAction> solrIndexingActionMap) {
		this.solrIndexingActionMap = solrIndexingActionMap;
	}

	public void setIdleWaitTime(long idleWaitTime) {
		this.idleWaitTime = idleWaitTime;
	}

	public void setNextMessageWaitTime(long nextMessageWaitTime) {
		this.nextMessageWaitTime = nextMessageWaitTime;
	}

	public void setBlockedWaitTime(long blockedWaitTime) {
		this.blockedWaitTime = blockedWaitTime;
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
	private SolrUpdateRequest nextRequest() throws InterruptedException {
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
				Thread.sleep(blockedWaitTime);
			} catch (InterruptedException e) {
				LOG.info("Services runnable interrupted while waiting to get next message", e);
				throw e;
			}
		} while (updateRequest == null && !Thread.currentThread().isInterrupted()
				&& (solrUpdateService.getPidQueue().size() != 0 || solrUpdateService.getCollisionList().size() != 0));
		return null;
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
			IndexingAction indexingAction = this.solrIndexingActionMap.get(updateRequest.getUpdateAction());
			if (indexingAction != null) {
				indexingAction.performAction(updateRequest);
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
		LOG.debug("Starting up SolrUpdateRunnable");
		do {
			try {
				while (this.solrUpdateService.isPaused() && !Thread.currentThread().isInterrupted()) {
					Thread.sleep(this.idleWaitTime);
				}
				
				String pid = null;
				// Get the next pid and lock it
				updateRequest = nextRequest();

				if (updateRequest != null) {
					solrUpdateService.getActiveMessages().add(updateRequest);
					updateRequest.setStatus(ProcessingStatus.ACTIVE);
					try {
						// Quit before doing work if thread was interrupted
						if (Thread.currentThread().isInterrupted())
							throw new InterruptedException();
						// Get the next available pid
						LOG.debug("Obtained " + updateRequest.getTargetID() + "|" + updateRequest.getAction());
						pid = updateRequest.getTargetID();
						performAction(updateRequest);
					} finally {
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
				}
				if (updateRequest == null) {
					Thread.sleep(this.idleWaitTime);
				} else {
					if (this.nextMessageWaitTime > 0L)
						Thread.sleep(this.nextMessageWaitTime);
				}
			} catch (InterruptedException e) {
				LOG.info("Solr update runnable interrupted, shutting down.");
				break;
			} catch (Exception e) {
				// Encountered an exception
				LOG.error("Encountered an exception while ingesting to Solr.  Finished SolrIngestThread", e);
			}
		} while (true);
	}
}
