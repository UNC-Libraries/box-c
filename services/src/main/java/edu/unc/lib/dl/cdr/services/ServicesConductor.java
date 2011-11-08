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
package edu.unc.lib.dl.cdr.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.jdom.Document;
import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.cdr.services.exception.EnhancementException;
import edu.unc.lib.dl.cdr.services.model.FailedObjectHashMap;
import edu.unc.lib.dl.cdr.services.model.PIDMessage;
import edu.unc.lib.dl.cdr.services.model.SideEffectMessageFilterList;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.ContentModelHelper;

/**
 * Central service conductor class which stores and processes a queue of messages indicating updates to Fedora objects.
 *
 * @author Gregory Jansen, Ben Pennell
 */
public class ServicesConductor {
	private static final Logger LOG = LoggerFactory.getLogger(ServicesConductor.class);

	/**
	 * The object enhancement services, in priority order.
	 */
	private List<ObjectEnhancementService> services = new ArrayList<ObjectEnhancementService>();

	/**
	 * Maximum numbers of threads in executor pool.
	 */
	private int maxThreads = 5;

	// Queue of pids with their invoking messages
	private BlockingQueue<PIDMessage> pidQueue = null;
	// List of pids which applied to locked pids when they were first called,
	// preserved in order
	private List<PIDMessage> collisionList = null;
	// Set of locked pids, used to prevent items from being processed multiple
	// times at once
	private Set<String> lockedPids = null;
	// Set of pids which failed to process one or more services. Contents of the entry indicate which services have
	// failed
	private FailedObjectHashMap failedPids = null;
	//List of messages that should be ignored the next time they appear as they are expected resultant 
	//messages generated as a side effect of 
	private SideEffectMessageFilterList sideEffectMessageFilterList = null; 

	private ServicesThreadPoolExecutor executor = null;

	private boolean paused = false;

	public ServicesConductor() {
		LOG.debug("Starting up Services Conductor");
		pidQueue = new LinkedBlockingQueue<PIDMessage>();
		// Initialize as synchronized collections for thread safety
		collisionList = Collections.synchronizedList(new ArrayList<PIDMessage>());
		lockedPids = Collections.synchronizedSet(new HashSet<String>());
		failedPids = new FailedObjectHashMap();
	}

	/**
	 * Initializes this bean after properties are set.
	 */
	public void init() {
		// start up the thread pool
		this.executor = new ServicesThreadPoolExecutor(this.maxThreads);
		this.executor.setKeepAliveTime(0, TimeUnit.DAYS);
	}

	/**
	 * Deconstructor method, stops the thread pool.
	 */
	public void destroy() {
		this.shutdown();
	}

	public void unlockPid(String pid) {
		this.lockedPids.remove(pid);
	}

	public void add(String pid) {
		add(new PIDMessage(pid, null));
	}

	public void add(PID pid) {
		add(new PIDMessage(pid, null));
	}

	public void add(String pid, Document msg) {
		add(new PIDMessage(pid, msg));
	}

	public void add(PID pid, Document msg) {
		add(new PIDMessage(pid, msg));
	}

	/**
	 * Queues a pid to force the rerunning of a particular service as if it were stale.
	 * @param pid
	 * @param serviceName fully qualified name of service to run.  ie,
	 * 	edu.unc.lib.dl.cdr.services.techmd.TechnicalMetadataEnhancementService
	 */
	public void add(String pid, String serviceName){
		add(new PIDMessage(pid, null, serviceName));
	}

	public void add(PID pid, String serviceName){
		add(new PIDMessage(pid, null, serviceName));
	}


	public void add(PID pid, Document msg, String serviceName) {
		add(new PIDMessage(pid, msg, serviceName));
	}

	/**
	 * Offers a pid and its message to the queue of messages to be processed. Will start up processing threads if none
	 * are running or available.
	 *
	 * @param pid
	 * @param msg
	 */
	public void add(PIDMessage pidMsg) {
		synchronized (pidQueue) {
			if (executor.isTerminating() || executor.isShutdown() || executor.isTerminated() || !messageApplicable(pidMsg)) {
				LOG.debug("Ignoring message for pid " + pidMsg.getPIDString());
				return;
			}
			pidQueue.offer(pidMsg);
			startProcessing();
		}
	}

	public String getServiceNames(){
		StringBuilder sb = new StringBuilder();
		for (ObjectEnhancementService s: this.services){
			sb.append(s.getClass().getName()).append('\n');
		}
		return sb.toString();
	}

	public boolean messageApplicable(PIDMessage msg) {
		if (msg.getMessage() == null)
			return true;
		String datastream = msg.getDatastream();
		
		return !this.sideEffectMessageFilterList.contains(msg) && 
			!(ContentModelHelper.Datastream.AUDIT.getName().equals(datastream)
				|| ContentModelHelper.Datastream.MD_CONTENTS.getName().equals(datastream)
				|| ContentModelHelper.Datastream.MD_EVENTS.getName().equals(datastream)
				|| ContentModelHelper.Datastream.MD_TECHNICAL.getName().equals(datastream)
				|| ContentModelHelper.Datastream.THUMB_LARGE.getName().equals(datastream)
				|| ContentModelHelper.Datastream.THUMB_SMALL.getName().equals(datastream)
				|| ContentModelHelper.Datastream.IMAGE_JP2000.getName().equals(datastream));
	}
	
	public void addSideEffect(String pid, String action, String datastream, String relation){
		LOG.debug("Adding side effect " + pid + " " + action + " " + datastream + " " + relation);
		this.sideEffectMessageFilterList.add(new PIDMessage(pid, action, datastream, relation));
	}

	/**
	 * Kick starts a thread for processing messages if needed
	 */
	public void startProcessing() {
		executor.submit(new PerformServicesRunnable());
	}

	/**
	 * Shuts down the thread pool
	 */
	public void shutdown() {
		executor.shutdown();
		LOG.warn("ServiceConductor is shutting down, no further objects will be received");
	}

	public int getMaxThreads() {
		return maxThreads;
	}

	public void setMaxThreads(int maxThreads) {
		this.maxThreads = maxThreads;
	}

	public List<ObjectEnhancementService> getServices() {
		return services;
	}

	public void setServices(List<ObjectEnhancementService> services) {
		this.services = services;
	}

	public void setServices(ArrayList<ObjectEnhancementService> services) {
		this.services = services;
	}

	public BlockingQueue<PIDMessage> getPidQueue() {
		return pidQueue;
	}

	public void setPidQueue(BlockingQueue<PIDMessage> pidQueue) {
		this.pidQueue = pidQueue;
	}

	public SideEffectMessageFilterList getSideEffectMessageFilterList() {
		return sideEffectMessageFilterList;
	}

	public void setSideEffectMessageFilterList(SideEffectMessageFilterList sideEffectMessageFilterList) {
		this.sideEffectMessageFilterList = sideEffectMessageFilterList;
	}

	public Set<String> getLockedPids() {
		return lockedPids;
	}

	public void setLockedPids(Set<String> lockedPids) {
		this.lockedPids = lockedPids;
	}

	public List<PIDMessage> getCollisionList() {
		return collisionList;
	}

	public void setCollisionList(List<PIDMessage> collisionList) {
		this.collisionList = collisionList;
	}

	public FailedObjectHashMap getFailedPids() {
		return failedPids;
	}

	public void setFailedPids(FailedObjectHashMap failedPids) {
		this.failedPids = failedPids;
	}
	
	public void repopulateFailedPids(String dump){
		this.failedPids.repopulate(dump);
	}

	public void setMaxThreadPoolSize(int threadPoolSize){
		this.executor.setCorePoolSize(threadPoolSize);
		this.executor.setMaximumPoolSize(threadPoolSize);
	}

	public void resetMaxThreadPoolSize(){
		this.executor.setCorePoolSize(this.maxThreads);
		this.executor.setMaximumPoolSize(this.maxThreads);
	}

	public boolean isPaused() {
		return paused;
	}

	public void pause(){
		this.paused = true;
		this.executor.pause();
	}

	public void resume(){
		this.paused = false;
		this.executor.resume();
	}

	public void servicesStatus() {
		LOG.info("Services Conductor Status:");
		LOG.info("---------------------------------------");
		LOG.info("PID Queue: " + this.pidQueue.size());
		LOG.info("Collision List: " + this.collisionList.size());
		LOG.info("Locked pids: " + this.lockedPids.size());
		LOG.info("Failed pids: " + this.failedPids.size());
	}

	public void logQueues() {
		LOG.info("Queue Statuses:");
		LOG.info("---------------------------------------");
		LOG.info("PID Queue: " + this.pidQueue);
		LOG.info("Collision List: " + this.collisionList);
		LOG.info("Locked pids: " + this.lockedPids);
		LOG.info("Side effects: " + this.sideEffectMessageFilterList);
		LOG.info("Failed pids: \n" + this.failedPids);
	}

	public synchronized void flushPids(String confirm) {
		if (confirm.equalsIgnoreCase("yes")) {
			this.pidQueue.clear();
			this.collisionList.clear();
			this.lockedPids.clear();
			this.sideEffectMessageFilterList.clear();
		}
	}

	public synchronized void clearFailedPids(String confirm) {
		if (confirm.equalsIgnoreCase("yes")) {
			this.failedPids.clear();
		}
	}

	public void reprocessFailedPids() {
		synchronized (failedPids) {
			for (String pid : failedPids.keySet()) {
				this.add(pid);
			}
			failedPids.clear();
		}
	}

	/**
	 * Runnable class which performs the actual processing of messages from the queue. Messages are read if they do not
	 * apply to a pid that is already being processed, and a list of services are tested against each message to
	 * determine if they should be run.
	 *
	 * @author bbpennel
	 *
	 */
	public class PerformServicesRunnable implements Runnable {

		public PerformServicesRunnable() {
			LOG.debug("Instantiating a new PerformServicesRunnable");
		}

		private void retryException(PIDMessage pidMessage, ObjectEnhancementService s, String message, Exception e, long retryDelay){
			LOG.warn(message + s.getClass().getName() + " for " + pidMessage.getPIDString() + ".  Retrying after a delay.", e);
			try {
				Thread.sleep(retryDelay);
				this.applyService(pidMessage, s);
				LOG.info("Second attempt to run " + s.getClass().getName() + " for " + pidMessage.getPIDString()
						+ " was successful.");
			} catch (Exception e2) {
				LOG.error("Second attempt to run " + s.getClass().getName() + " for " + pidMessage.getPIDString()
						+ " failed.");
				failedPids.add(pidMessage.getPIDString(), s.getClass().getName());
			}
		}

		/**
		 * Retrieves the next available message. The request must not effect a pid which is currently locked. If the next
		 * pid is locked, then it is moved to the collision list, and the next message is polled until an unlocked pid is
		 * found or the list is empty. If there are any items in the collision list, they are treated as if they were at
		 * the beginning of message queue, meaning that they are examined before polling of the queue begins in order to
		 * retain operation order.
		 *
		 * @return the next available message which is not locked, or null if none if available.
		 */
		private PIDMessage getNextMessage() {
			synchronized (collisionList) {
				synchronized (pidQueue) {
					PIDMessage pid = null;

					do {
						// First read from the collision list in case there are items
						// that were blocked which need to be read
						if (collisionList != null && !collisionList.isEmpty()) {
							Iterator<PIDMessage> collisionIt = collisionList.iterator();
							while (collisionIt.hasNext()) {
								pid = collisionIt.next();
								synchronized (lockedPids) {
									if (!lockedPids.contains(pid.getPIDString())) {
										lockedPids.add(pid.getPIDString());
										collisionIt.remove();
										return pid;
									}
								}

							}
						}

						// There were no usable pids in the collision list, so read
						// the regular queue.
						pid = pidQueue.poll();
						if (pid == null) {
							return null;
						}

						synchronized (lockedPids) {
							if (lockedPids.contains(pid.getPIDString())) {
								collisionList.add(pid);
							} else {
								lockedPids.add(pid.getPIDString());
								return pid;
							}
						}
					} while (true);
				}
			}
		}

		public void applyService(PIDMessage pidMessage, ObjectEnhancementService s) throws EnhancementException {
			// Check if there were any failed services for this pid. If there were, check if the current service
			// was one of them.
			Set<String> failedServices = failedPids.get(pidMessage.getPIDString());
			if (s.isActive() &&
					((pidMessage.getServiceName() != null && pidMessage.getServiceName().equals(s.getClass().getName()))
						|| s.isApplicable(pidMessage))
					&& (failedServices == null || !failedServices.contains(s.getClass().getName()))) {
				LOG.info("Adding enhancement task: " + s.getClass().getCanonicalName() + " on " + pidMessage.getPIDString());
				Enhancement<Element> task = s.getEnhancement(pidMessage);
				// Enhancement services need to be run serially, so
				// making a direct invocation of call instead of using
				// an executor.
				task.call();
			}
		}

		@Override
		public void run() {

			if (paused) {
				do {
					try {
						Thread.sleep(5000L);
					} catch (Exception e) {
					}
				} while (paused);
			}

			LOG.debug("Starting new run of ServiceConductor thread " + this);
			PIDMessage pidMessage = null;
			do {
				pidMessage = getNextMessage();
				if (pidMessage == null) {
					// Was unable to get an unlocked PID, wait a moment before trying
					// again
					try {
						Thread.sleep(200L);
					} catch (Exception e) {
					}
				}
				// Loop until an unlocked PID is retrieved or there are no more
				// pids.
			} while (pidMessage == null && !(pidQueue.size() == 0 && collisionList.size() == 0));

			if (pidMessage != null) {
				for (ObjectEnhancementService s : services) {
					try {
						this.applyService(pidMessage, s);
					} catch (EnhancementException e){
						switch (e.getSeverity()) {
							case RECOVERABLE: 
								retryException(pidMessage, s, "A recoverable enhancement exception occurred while attempting to apply service "
										+ s.getClass().getName(), e, 20000L);
								break;
							case UNRECOVERABLE:
								LOG.error("An unrecoverable exception occurred while attempting to apply service " 
										+ s.getClass().getName() + " for " + pidMessage.getPIDString() + ".  Adding to failure list.", e);
								failedPids.add(pidMessage.getPIDString(), s.getClass().getName());
								break;
							case FATAL:
								pause();
								LOG.error("A fatal exception occurred while attempting to apply service " 
										+ s.getClass().getName() + " for " + pidMessage.getPIDString() +", halting all future services.", e);
								failedPids.add(pidMessage.getPIDString(), s.getClass().getName());
								
								break;
							default:
								LOG.error("An exception occurred while attempting to apply service "
										+ s.getClass().getName() + " for " + pidMessage.getPIDString(), e);
								failedPids.add(pidMessage.getPIDString(), s.getClass().getName());
								break;
						}
					} catch (RuntimeException e) {
						retryException(pidMessage, s, "A runtime error occurred while attempting to apply service",
								e, 60000L);
					} catch (Exception e) {
						LOG.error("An unexpected exception occurred while attempting to apply service " + s.getClass().getName(), e);
						failedPids.add(pidMessage.getPIDString(), s.getClass().getName());
					}
				}
				lockedPids.remove(pidMessage.getPIDString());
			}
		}

	}
}
