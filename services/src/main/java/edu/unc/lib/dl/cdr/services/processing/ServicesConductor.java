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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.cdr.services.Enhancement;
import edu.unc.lib.dl.cdr.services.ObjectEnhancementService;
import edu.unc.lib.dl.cdr.services.exception.EnhancementException;
import edu.unc.lib.dl.cdr.services.model.FailedObjectHashMap;
import edu.unc.lib.dl.cdr.services.model.PIDMessage;
import edu.unc.lib.dl.cdr.services.util.JMSMessageUtil;

/**
 * Central service conductor class which stores and processes a queue of messages indicating updates to Fedora objects.
 *
 * @author Gregory Jansen, Ben Pennell
 */
public class ServicesConductor implements MessageConductor, ServiceConductor {
	private static final Logger LOG = LoggerFactory.getLogger(ServicesConductor.class);

	public static final String identifier = "SERVICES";

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

	private ServicesThreadPoolExecutor<PerformServicesRunnable> executor = null;

	private long recoverableDelay = 0;
	private long unexpectedExceptionDelay = 0;
	private long beforeExecuteDelay = 0;

	public ServicesConductor() {
		LOG.debug("Starting up Services Conductor");
		pidQueue = new LinkedBlockingQueue<PIDMessage>();
		// Initialize as synchronized collections for thread safety
		collisionList = Collections.synchronizedList(new ArrayList<PIDMessage>());
		lockedPids = Collections.synchronizedSet(new HashSet<String>());
	}

	public String getIdentifier(){
		return identifier;
	}

	/**
	 * Initializes this bean after properties are set.
	 */
	public void init() {
		// start up the thread pool
		initializeExecutor();
	}

	private void initializeExecutor(){
		this.executor = new ServicesThreadPoolExecutor<PerformServicesRunnable>(this.maxThreads, this.getIdentifier());
		this.executor.setKeepAliveTime(0, TimeUnit.DAYS);
		this.executor.setBeforeExecuteDelay(beforeExecuteDelay);
	}

	/**
	 * Deconstructor method, stops the thread pool.
	 */
	public void destroy() {
		this.executor.shutdownNow();
	}

	/**
	 * Offers a pid and its message to the queue of messages to be processed. Will start up processing threads if none
	 * are running or available.
	 *
	 * @param pid
	 * @param msg
	 */
	@Override
	public void add(PIDMessage pidMsg) {
		synchronized (pidQueue) {
			synchronized (executor) {
				if (executor.isTerminating() || executor.isShutdown() || executor.isTerminated()) {
					LOG.debug("Ignoring message for pid " + pidMsg.getPIDString());
					return;
				}
				boolean success = pidQueue.offer(pidMsg);
				if (!success){
					LOG.error("Failure to queue pid " + pidMsg.getPIDString());
				}
				startProcessing();
			}
		}
	}

	public int getMaxThreadPoolSize(){
		return this.executor.getMaximumPoolSize();
	}

	public void setMaxThreadPoolSize(int threadPoolSize){
		this.executor.setCorePoolSize(threadPoolSize);
		this.executor.setMaximumPoolSize(threadPoolSize);
	}

	public void resetMaxThreadPoolSize(){
		this.executor.setCorePoolSize(this.maxThreads);
		this.executor.setMaximumPoolSize(this.maxThreads);
	}

	@Override
	public boolean isPaused() {
		return this.executor.isPaused();
	}

	@Override
	public void pause(){
		this.executor.pause();
	}

	@Override
	public void resume(){
		this.executor.resume();
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
	public Map<String, Object> getInfo(){
		// TODO put values in separate keys
		Map<String, Object> result = new HashMap<String, Object>();
		StringBuilder sb = new StringBuilder();
		sb.append("Services Conductor Status:\n")
			.append("Paused: " + isPaused() + "\n")
			.append("PID Queue: " + this.pidQueue.size() + "\n")
			.append("Collision List: " + this.collisionList.size() + "\n")
			.append("Locked pids: " + this.lockedPids.size() + "\n")
			.append("Failed pids: " + this.failedPids.size() + "\n")
			.append("Executor: " + executor.getActiveCount() + " active workers, " + executor.getQueue().size() + " queued");
		result.put("message", sb.toString());
		return result;
	}

	@Override
	public String queuesToString(){
		StringBuilder sb = new StringBuilder();
		sb.append("Services Conductor Queues:\n")
			.append("PID Queue: " + this.pidQueue + "\n")
			.append("Collision List: " + this.collisionList + "\n")
			.append("Locked pids: " + this.lockedPids + "\n")
			.append("Failed pids: " + this.failedPids + "\n");
		return sb.toString();
	}

	public void logStatus() {
		LOG.info((String)getInfo().get("message"));
	}

	public void logQueues() {
		LOG.info(queuesToString());
	}

	@Override
	public synchronized void clearQueue() {
		this.pidQueue.clear();
		this.collisionList.clear();
	}

	@Override
	public void clearState() {
		this.pidQueue.clear();
		this.collisionList.clear();
		this.lockedPids.clear();
		this.failedPids.clear();
		executor.getQueue().clear();
	}

	public synchronized void clearFailedPids() {
		this.failedPids.clear();
	}

	public synchronized void unlockPid(String pid) {
		this.lockedPids.remove(pid);
	}


	public synchronized void repopulateFailedPids(String dump){
		this.failedPids.repopulate(dump);
	}

	public synchronized void removeFailedPid(String pid){
		this.failedPids.remove(pid);
	}

	public void reprocessFailedPids() {
		synchronized (failedPids) {
			for (String pid : failedPids.keySet()) {
				this.add(new PIDMessage(pid, JMSMessageUtil.servicesMessageNamespace,
						JMSMessageUtil.ServicesActions.APPLY_SERVICE_STACK.getName()));
			}
			failedPids.clear();
		}
	}

	/**
	 * Kick starts a thread for processing messages if needed
	 */
	protected void startProcessing() {
		executor.submit(new PerformServicesRunnable());
	}

	@Override
	public void shutdown() {
		this.executor.shutdown();
		this.clearQueue();
		this.lockedPids.clear();
		LOG.warn("ServiceConductor is shutting down, no further objects will be received");
	}

	@Override
	public void shutdownNow() {
		this.executor.shutdownNow();
		this.clearQueue();
		this.lockedPids.clear();
		LOG.warn("ServiceConductor is shutting down, no further objects will be received");
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

	public int getMaxThreads() {
		return maxThreads;
	}

	public ServicesThreadPoolExecutor getExecutor(){
		return this.executor;
	}

	public int getActiveThreadCount() {
		return this.executor.getActiveCount();
	}

	public void setMaxThreads(int maxThreads) {
		this.maxThreads = maxThreads;
	}

	public long getRecoverableDelay() {
		return recoverableDelay;
	}

	public void setRecoverableDelay(long recoverableDelay) {
		this.recoverableDelay = recoverableDelay;
	}

	public long getUnexpectedExceptionDelay() {
		return unexpectedExceptionDelay;
	}

	public void setUnexpectedExceptionDelay(long unexpectedExceptionDelay) {
		this.unexpectedExceptionDelay = unexpectedExceptionDelay;
	}

	public long getBeforeExecuteDelay() {
		return beforeExecuteDelay;
	}

	public void setBeforeExecuteDelay(long beforeExecuteDelay) {
		this.beforeExecuteDelay = beforeExecuteDelay;
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

	@Override
	public int getQueueSize() {
		return this.pidQueue.size() + this.collisionList.size();
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
			} catch (InterruptedException e2) {
				LOG.warn("Retry attempt for " + s.getClass().getName() + " for " + pidMessage.getPIDString()
						+ " was interrupted.");
				Thread.currentThread().interrupt();
			} catch (Exception e2) {
				LOG.error("Second attempt to run " + s.getClass().getName() + " for " + pidMessage.getPIDString()
						+ " failed.");
				failedPids.add(pidMessage.getPIDString(), s.getClass().getName());
			}
		}

		/**
		 * Retrieves the next available message. The request must not return a pid which is currently locked. If the next
		 * pid is locked, then it is moved to the collision list, and the next message is polled until an unlocked pid is
		 * found or the list is empty. If there are any items in the collision list, they are treated as if they were at
		 * the beginning of message queue, meaning that they are examined before polling of the queue begins in order to
		 * retain operation order.
		 *
		 * @return the next available message which is not locked, or null if none if available.
		 */
		private PIDMessage getNextMessage() {
			PIDMessage pid = null;

			do {
				synchronized (collisionList) {
					synchronized (pidQueue) {
						// First read from the collision list in case there are items
						// that were blocked which need to be read
						if (!collisionList.isEmpty()) {
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

						do {
							// There were no usable pids in the collision list, so read
							// the regular queue.
							pid = pidQueue.poll();

							if (pid != null){
								synchronized (lockedPids) {
									if (lockedPids.contains(pid.getPIDString())) {
										collisionList.add(pid);
									} else {
										lockedPids.add(pid.getPIDString());
										return pid;
									}
								}
							}
						} while (pid != null && !Thread.currentThread().isInterrupted());
					}
				}
				try {
					Thread.sleep(200L);
				} catch (InterruptedException e) {
					LOG.warn("Services runnable interrupted while waiting to get next message.");
					Thread.currentThread().interrupt();
				}
			} while (pid == null && !Thread.currentThread().isInterrupted()
					&& (!pidQueue.isEmpty() || !collisionList.isEmpty()));
			return null;
		}

		public void applyService(PIDMessage pidMessage, ObjectEnhancementService s) throws EnhancementException {
			// Check if there were any failed services for this pid. If there were, check if the current service
			// was one of them.

			if (LOG.isDebugEnabled())
				LOG.debug("Applying service " + s.getClass().getCanonicalName() + " to " + pidMessage.getPIDString());

			Set<String> failedServices = null;
			synchronized(failedPids){
				failedServices = failedPids.get(pidMessage.getPIDString());
				//Determine if the service should not be run
				if (!(s.isActive() &&
						((pidMessage.getServiceName() != null && pidMessage.getServiceName().equals(s.getClass().getName()))
							|| s.isApplicable(pidMessage))
						&& (failedServices == null || !failedServices.contains(s.getClass().getName())))){
					if (LOG.isDebugEnabled())
						LOG.debug("Enhancement not run: " + s.getClass().getCanonicalName() + " on " + pidMessage.getPIDString());
					return;
				}
			}

			//Generate the enhancement
			LOG.info("Adding enhancement task: " + s.getClass().getCanonicalName() + " on " + pidMessage.getPIDString());
			Enhancement<Element> task = s.getEnhancement(pidMessage);

			//Pause before applying the service if the executor is paused.
			pauseWait();
			//If the thread was interrupted, we're done
			if (Thread.currentThread().isInterrupted()){
				LOG.warn("Services thread " + Thread.currentThread().getId() + " for " + pidMessage.getPIDString()
						+ " interrupted before calling enhancement");
				return;
			}
			//Enhancement services need to be run serially per pid, so making a direct invocation of call
			task.call();
		}

		private void pauseWait(){
			while (executor.isPaused() && !Thread.currentThread().isInterrupted()){
				try {
					Thread.sleep(1000L);
				} catch (Exception e) {
					LOG.debug("Services thread " + Thread.currentThread().getId() + " interrupted while paused");
					Thread.currentThread().interrupt();
				}
			}
		}

		@Override
		public void run() {
			LOG.debug("Starting new run of ServiceConductor thread " + this);
			//Get the next message, waiting as long as necessary for one to be free
			PIDMessage pidMessage = getNextMessage();

			if (LOG.isDebugEnabled())
				LOG.debug("Received pid " + pidMessage.getPIDString() + ": " + pidMessage.getFilteredServices());

			if (pidMessage != null && pidMessage.getFilteredServices() != null) {
				try {
					//Quit before doing work if thread was interrupted
					if (Thread.currentThread().isInterrupted()){
						LOG.debug("Thread was interrupted");
						return;
					}
					for (ObjectEnhancementService s : pidMessage.getFilteredServices()) {
						try {
							if (Thread.currentThread().isInterrupted()){
								LOG.debug("Thread was interrupted");
								return;
							}
							//Apply the service
							this.applyService(pidMessage, s);
						} catch (EnhancementException e){
							switch (e.getSeverity()) {
								case RECOVERABLE:
									retryException(pidMessage, s, "A recoverable enhancement exception occurred while attempting to apply service "
											+ s.getClass().getName(), e, recoverableDelay);
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
									return;
								default:
									LOG.error("An exception occurred while attempting to apply service "
											+ s.getClass().getName() + " for " + pidMessage.getPIDString(), e);
									failedPids.add(pidMessage.getPIDString(), s.getClass().getName());
									break;
							}
						} catch (RuntimeException e) {
							retryException(pidMessage, s, "A runtime error occurred while attempting to apply service",
									e, unexpectedExceptionDelay);
						} catch (Exception e) {
							LOG.error("An unexpected exception occurred while attempting to apply service " + s.getClass().getName(), e);
							failedPids.add(pidMessage.getPIDString(), s.getClass().getName());
						}
					}
				} finally {
					lockedPids.remove(pidMessage.getPIDString());
				}
			}
		}
	}
}
