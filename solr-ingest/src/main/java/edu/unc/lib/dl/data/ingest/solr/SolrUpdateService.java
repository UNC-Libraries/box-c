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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import edu.unc.lib.dl.fedora.FedoraDataService;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.search.solr.service.SolrSearchService;
import edu.unc.lib.dl.search.solr.util.SearchSettings;
import edu.unc.lib.dl.security.access.AccessGroupSet;

/**
 * Service which handles ingest and update of a solr index via threaded processors
 * which read from a queue of ordered update requests.
 * @author bbpennel
 */
public class SolrUpdateService {
	private static final Logger LOG = LoggerFactory.getLogger(SolrUpdateService.class);
	private FedoraDataService fedoraDataService;
	private UpdateDocTransformer updateDocTransformer;
	private SolrDataAccessLayer solrDataAccessLayer;
	private SolrSearchService solrSearchService;
	private AccessGroupSet accessGroups;
	private String solrPath;
	@Autowired
	private SearchSettings searchSettings;
	public static final String TARGET_ALL = "fullIndex";
	private PID collectionsPid = null;

	private ThreadPoolExecutor executor = null;
	private BlockingQueue<SolrUpdateRequest> pidQueue = null;
	private List<SolrUpdateRequest> collisionList = null;
	private Set<String> lockedPids = null;
	private int maxIngestThreads = 3;
	
	public SolrUpdateService() {
		pidQueue = new LinkedBlockingQueue<SolrUpdateRequest>();
		lockedPids = Collections.synchronizedSet(new HashSet<String>());
		collisionList = Collections.synchronizedList(new ArrayList<SolrUpdateRequest>());
		updateDocTransformer = new UpdateDocTransformer();
	}

	public void init() {
		try {
			updateDocTransformer.init();
		} catch (Exception e) {
			LOG.error("Failed to initialize AddDocTransformer for SolrIngestService", e);
		}

		//Pass the runnables a reference back to the update service
		SolrUpdateRunnable.setSolrUpdateService(this);
		SolrUpdateRunnable.initQueries();
		
		collectionsPid = fedoraDataService.getTripleStoreQueryService().fetchByRepositoryPath("/Collections");
		if (collectionsPid == null){
			throw new Error("Initialization of SolrUpdateService failed.  It was unable to retrieve Collections object from repository.");
		}
		

		this.executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(this.maxIngestThreads);
		this.executor.setKeepAliveTime(0, TimeUnit.DAYS);
	}
	
	public void destroy() {
		this.shutdown();
	}

	public void shutdown() {
		executor.shutdown();
	}

	public void offer(String pid, SolrUpdateAction action) {
		offer(new SolrUpdateRequest(pid, action));
	}

	public void offer(String pid) {
		offer(new SolrUpdateRequest(pid, SolrUpdateAction.ADD));
	}

	public void offer(SolrUpdateRequest ingestRequest) {
		synchronized (pidQueue) {
			if (executor.isTerminating() || executor.isShutdown() || executor.isTerminated())
				return;
			LOG.info("Queueing: " + ingestRequest.getPid());
			pidQueue.offer(ingestRequest);
			executor.submit(new SolrUpdateRunnable());
		}
	}

	public void offer(List<String> pids) {
		synchronized (pidQueue) {
			if (executor.isTerminating() || executor.isShutdown() || executor.isTerminated())
				return;
			for (String pid : pids) {
				pidQueue.offer(new SolrUpdateRequest(pid, SolrUpdateAction.ADD));
			}
			executor.submit(new SolrUpdateRunnable());
		}
	}

	public FedoraDataService getFedoraDataService() {
		return fedoraDataService;
	}

	public void setFedoraDataService(FedoraDataService fedoraDataService) {
		this.fedoraDataService = fedoraDataService;
	}

	public UpdateDocTransformer getUpdateDocTransformer() {
		return updateDocTransformer;
	}

	public void setUpdateDocTransformer(UpdateDocTransformer updateDocTransformer) {
		this.updateDocTransformer = updateDocTransformer;
	}

	public SolrDataAccessLayer getSolrDataAccessLayer() {
		return solrDataAccessLayer;
	}

	public void setSolrDataAccessLayer(SolrDataAccessLayer solrDataAccessLayer) {
		this.solrDataAccessLayer = solrDataAccessLayer;
	}

	public SolrSearchService getSolrSearchService() {
		return solrSearchService;
	}

	public void setSolrSearchService(SolrSearchService solrSearchService) {
		this.solrSearchService = solrSearchService;
	}

	public AccessGroupSet getAccessGroups() {
		return accessGroups;
	}

	public void setAccessGroups(AccessGroupSet accessGroups) {
		this.accessGroups = accessGroups;
	}

	public String getSolrPath() {
		return solrPath;
	}

	public void setSolrPath(String solrPath) {
		this.solrPath = solrPath;
	}
	
	public int getMaxIngestThreads() {
		return maxIngestThreads;
	}

	public void setMaxIngestThreads(int maxIngestThreads) {
		this.maxIngestThreads = maxIngestThreads;
	}
	
	public SearchSettings getSearchSettings() {
		return searchSettings;
	}

	public void setSearchSettings(SearchSettings searchSettings) {
		this.searchSettings = searchSettings;
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

	public PID getCollectionsPid() {
		return collectionsPid;
	}

	public void setCollectionsPid(PID collectionsPid) {
		this.collectionsPid = collectionsPid;
	}

	public int queueSize(){
		return pidQueue.size();
	}
	
	public int lockedSize(){
		return lockedPids.size();
	}
	
	public int collisionSize(){
		return collisionList.size();
	}
	
	public int activeThreadsCount(){
		return executor.getActiveCount();
	}
	
	public String getTargetAllSelector(){
		return SolrUpdateService.TARGET_ALL;
	}
	
	public String statusString(){
		StringBuilder status = new StringBuilder();
		status.append("\nPid Queue Size: ").append(pidQueue.size())
				.append("\nCollision List size: ").append(collisionList.size()).append(collisionList.toString())
				.append("\nPool size: ").append(executor.getPoolSize())
				.append("\nPool queue size: ").append(executor.getQueue().size())
				.append("\nLocked Pids: ").append(lockedPids.size()).append("(" + lockedPids + ")");
		return status.toString();
	}
}
