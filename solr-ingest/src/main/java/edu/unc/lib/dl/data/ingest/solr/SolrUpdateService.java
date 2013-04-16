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
import java.util.LinkedList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.util.IndexingActionType;

/**
 * Service which handles ingest and update of a solr index via threaded processors which read from a queue of ordered
 * update requests.
 * 
 * @author bbpennel
 */
public class SolrUpdateService {
	protected static final Logger LOG = LoggerFactory.getLogger(SolrUpdateService.class);
	public static final String identifier = "SOLR_UPDATE";

	protected SolrUpdateRunnableFactory solrUpdateRunnableFactory;
	protected ThreadPoolExecutor executor = null;
	protected BlockingQueue<SolrUpdateRequest> pidQueue = null;
	protected List<SolrUpdateRequest> collisionList = null;
	protected List<SolrUpdateRequest> finishedMessages = null;
	protected List<SolrUpdateRequest> activeMessages = null;
	protected List<SolrUpdateRequest> failedMessages = null;
	protected Set<String> lockedPids = null;
	protected int maxThreads = 3;
	protected long recoverableDelay = 0;
	protected boolean autoCommit = true;
	protected int finishedQueueSize = 1000;
	protected boolean isPaused = false;

	protected UpdateNodeRequest root;

	public SolrUpdateService() {
		pidQueue = new LinkedBlockingQueue<SolrUpdateRequest>();
		lockedPids = Collections.synchronizedSet(new HashSet<String>());
		collisionList = Collections.synchronizedList(new ArrayList<SolrUpdateRequest>());
		activeMessages = Collections.synchronizedList(new ArrayList<SolrUpdateRequest>());
		failedMessages = Collections.synchronizedList(new ArrayList<SolrUpdateRequest>());
		finishedMessages = Collections.synchronizedList(new LimitedQueue<SolrUpdateRequest>(this.finishedQueueSize));
		
		root = new UpdateNodeWithManagedChildrenRequest(identifier + ":ROOT", null);
	}

	public void init() {
		initializeExecutor();
	}

	protected void initializeExecutor() {
		LOG.debug("Initializing thread pool executor with " + this.maxThreads + " threads.");
		this.executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(this.maxThreads);
		this.executor.setKeepAliveTime(0, TimeUnit.DAYS);
		// Populate the runnables
		for (int i = 0; i < this.maxThreads; i++) {
			executor.execute(this.solrUpdateRunnableFactory.createJob());
		}
	}

	public void destroy() {
		executor.shutdownNow();
	}

	public void shutdown() {
		executor.shutdown();
	}

	public String nextMessageID() {
		return identifier + ":" + UUID.randomUUID().toString();
	}

	public void offer(String pid, IndexingActionType action) {
		offer(new SolrUpdateRequest(pid, action, nextMessageID(), root));
	}

	public void offer(String pid) {
		offer(new SolrUpdateRequest(pid, IndexingActionType.ADD, nextMessageID(), root));
	}

	public void offer(SolrUpdateRequest ingestRequest) {
		// Set the message's status to queued
		ingestRequest.setStatus(ProcessingStatus.QUEUED);

		if (ingestRequest.getMessageID() == null) {
			ingestRequest.setMessageID(nextMessageID());
		}

		// If no parent is provided, then this is a root node
		if (ingestRequest.getParent() == null)
			ingestRequest.setParent(root);

		synchronized (pidQueue) {
			if (executor.isTerminating() || executor.isShutdown() || executor.isTerminated())
				return;
			LOG.info("Queueing: " + ingestRequest.getPid());
			pidQueue.offer(ingestRequest);
		}
	}

	public int getMaxThreads() {
		return maxThreads;
	}

	public void setMaxThreads(int maxThreads) {
		this.maxThreads = maxThreads;
	}

	public BlockingQueue<SolrUpdateRequest> getPidQueue() {
		return pidQueue;
	}

	public void setPidQueue(BlockingQueue<SolrUpdateRequest> pidQueue) {
		this.pidQueue = pidQueue;
	}

	public List<SolrUpdateRequest> getCollisionList() {
		return collisionList;
	}

	public void setCollisionList(List<SolrUpdateRequest> collisionList) {
		this.collisionList = collisionList;
	}

	public Set<String> getLockedPids() {
		return lockedPids;
	}

	public void setLockedPids(Set<String> lockedPids) {
		this.lockedPids = lockedPids;
	}

	public List<SolrUpdateRequest> getFinishedMessages() {
		return finishedMessages;
	}

	public List<SolrUpdateRequest> getActiveMessages() {
		return activeMessages;
	}

	public List<SolrUpdateRequest> getFailedMessages() {
		return failedMessages;
	}

	public UpdateNodeRequest getRoot() {
		return root;
	}

	public boolean isAutoCommit() {
		return autoCommit;
	}

	public void setAutoCommit(boolean autoCommit) {
		this.autoCommit = autoCommit;
	}

	public void setFinishedQueueSize(int finishedQueueSize) {
		this.finishedQueueSize = finishedQueueSize;
	}

	public int queueSize() {
		return pidQueue.size();
	}

	public int lockedSize() {
		return lockedPids.size();
	}

	public int collisionSize() {
		return collisionList.size();
	}

	public int activeThreadsCount() {
		return executor.getActiveCount();
	}
	
	public boolean isPaused() {
		return this.isPaused;
	}

	public void setSolrUpdateRunnableFactory(SolrUpdateRunnableFactory solrUpdateRunnableFactory) {
		this.solrUpdateRunnableFactory = solrUpdateRunnableFactory;
	}

	public String statusString() {
		StringBuilder status = new StringBuilder();
		status.append("\nPid Queue Size: ").append(pidQueue.size()).append("\nCollision List size: ")
				.append(collisionList.size()).append(collisionList.toString()).append("\nPool size: ")
				.append(executor.getPoolSize()).append("\nPool queue size: ").append(executor.getQueue().size())
				.append("\nLocked Pids: ").append(lockedPids.size()).append("(" + lockedPids + ")");
		return status.toString();
	}
	
	public class LimitedQueue<E> extends LinkedList<E> {
		private static final long serialVersionUID = 1L;
		private int limit;

	    public LimitedQueue(int limit) {
	        this.limit = limit;
	    }

	    @Override
	    public boolean add(E o) {
	        super.add(o);
	        while (size() > limit) { super.remove(); }
	        return true;
	    }
	}
}
