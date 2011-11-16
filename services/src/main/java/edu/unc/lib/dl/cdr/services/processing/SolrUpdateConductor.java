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

package edu.unc.lib.dl.cdr.services.processing;

import java.util.List;
import java.util.concurrent.TimeUnit;

import edu.unc.lib.dl.cdr.services.model.PIDMessage;
import edu.unc.lib.dl.cdr.services.util.JMSMessageUtil;
import edu.unc.lib.dl.data.ingest.solr.SolrUpdateAction;
import edu.unc.lib.dl.data.ingest.solr.SolrUpdateRequest;
import edu.unc.lib.dl.data.ingest.solr.SolrUpdateService;

public class SolrUpdateConductor extends SolrUpdateService implements MessageConductor {
	public static final String identifier = "SOLR_UPDATE";
	
	@Override
	protected void initializeExecutor(){
		LOG.debug("Initializing services thread pool executor with " + this.maxIngestThreads + " threads.");
		this.executor = new ServicesThreadPoolExecutor(this.maxIngestThreads);
		this.executor.setKeepAliveTime(0, TimeUnit.DAYS);
	}
	
	@Override
	public void add(PIDMessage message) {
		String namespace = message.getNamespace();
		String action = message.getAction();
		if (namespace != null && action != null){
			if (namespace.equals(SolrUpdateAction.namespace)){
				SolrUpdateAction actionEnum = SolrUpdateAction.getAction(action);
				if (actionEnum != null){
					this.offer(message.getPIDString(), actionEnum);
				}
			} else {
				if (JMSMessageUtil.FedoraActions.PURGE_OBJECT.equals(action)){
					this.offer(message.getPIDString(), SolrUpdateAction.DELETE_SOLR_TREE);
				} else if (JMSMessageUtil.CDRActions.MOVE.equals(action) || JMSMessageUtil.CDRActions.ADD.equals(action)
						|| JMSMessageUtil.CDRActions.REORDER.equals(action)){
					message.generateCDRMessageContent();
					if (JMSMessageUtil.CDRActions.MOVE.equals(action) || JMSMessageUtil.CDRActions.ADD.equals(action)){
						//Move and add are both recursive adds of all subjects, plus a nonrecursive update for reordered children.
						for (String pidString: message.getCDRMessageContent().getSubjects()){
							this.offer(new SolrUpdateRequest(pidString, SolrUpdateAction.RECURSIVE_ADD));
						}
					}
					// Reorder is a non-recursive add.
					for (String pidString: message.getCDRMessageContent().getReordered()){
						this.offer(new SolrUpdateRequest(pidString, SolrUpdateAction.ADD));
					}
				} else if (JMSMessageUtil.CDRActions.REINDEX.equals(action)){
					//Determine which kind of reindex to perform based on the mode
					message.generateCDRMessageContent();
					if (message.getCDRMessageContent().getMode().equals("inplace")){
						this.offer(new SolrUpdateRequest(message.getCDRMessageContent().getParent(), SolrUpdateAction.RECURSIVE_REINDEX));
					} else {
						this.offer(new SolrUpdateRequest(message.getCDRMessageContent().getParent(), SolrUpdateAction.CLEAN_REINDEX));
					}
				} else {
					//For all other message types, do a single record update
					this.offer(message.getPIDString());
				}
			}
		}
	}

	@Override
	public void pause() {
		((ServicesThreadPoolExecutor)this.executor).pause();
		
	}

	@Override
	public void resume() {
		((ServicesThreadPoolExecutor)this.executor).resume();
	}
	
	@Override
	public boolean isPaused(){
		return ((ServicesThreadPoolExecutor)this.executor).isPaused();
	}
	
	@Override
	public String getConductorStatus(){
		StringBuilder sb = new StringBuilder();
		sb.append("Solr Update Conductor Status:\n")
			.append("Paused: " + isPaused() + "\n")
			.append("PID Queue: " + this.pidQueue.size() + "\n")
			.append("Collision List: " + this.collisionList.size() + "\n")
			.append("Locked pids: " + this.lockedPids.size() + "\n")
			.append("Executor: " + executor.getActiveCount() + " active workers, " + executor.getQueue().size() + " queued");
		return sb.toString();
	}
	
	@Override
	public String queuesToString(){
		StringBuilder sb = new StringBuilder();
		sb.append("Solr Update Conductor Queues:\n")
			.append("PID Queue: " + this.pidQueue + "\n")
			.append("Collision List: " + this.collisionList + "\n")
			.append("Locked pids: " + this.lockedPids + "\n");
		return sb.toString();
	}

	@Override
	public int getQueueSize() {
		return this.pidQueue.size() + this.collisionList.size();
	}
	
	@Override
	public synchronized void clearQueue(){
		this.pidQueue.clear();
		this.collisionList.clear();
	}
	
	@Override
	public synchronized void clearState(){
		this.pidQueue.clear();
		this.collisionList.clear();
		this.lockedPids.clear();
		executor.getQueue().clear();
	}
	
	@Override
	public boolean isEmpty() {
		return this.pidQueue.size() == 0 && this.collisionList.size() == 0 && this.lockedPids.size() == 0;
	}
	
	@Override
	public boolean isIdle(){
		return isPaused() || this.lockedPids.size() == 0;
	}
	
	@Override
	public boolean isReady(){
		return !this.executor.isShutdown() && !this.executor.isTerminated() && !this.executor.isTerminating();
	}

	@Override
	public void shutdown() {
		this.executor.shutdown();
		LOG.warn("Solr Update conductor is shutting down, no further objects will be received");
	}
	
	@Override
	public void shutdownNow() {
		this.executor.shutdownNow();
		LOG.warn("Solr Update conductor is shutting down now, interrupting future processing");
	}

	@Override
	public synchronized void abort() {
		this.lockedPids.clear();
		//Perform hard shutdown and wait for it to finish
		List<Runnable> runnables = this.executor.shutdownNow();
		while (this.executor.isTerminating() && !this.executor.isShutdown());
		//restart and pause the executor
		initializeExecutor();
		pause();
		//Pass the old runnables on to the new executor.
		if (runnables != null){
			for (Runnable runnable: runnables){
				this.executor.submit(runnable);
			}
		}
	}

	@Override
	public void restart() {
		if (this.executor == null || this.executor.isShutdown() || this.executor.isTerminated())
			initializeExecutor();
	}
	
	@Override
	public String getIdentifier() {
		return identifier;
	}

	public ServicesThreadPoolExecutor getThreadPoolExecutor(){
		return (ServicesThreadPoolExecutor)this.executor;
	}
}
