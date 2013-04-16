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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.cdr.services.Enhancement;
import edu.unc.lib.dl.cdr.services.ObjectEnhancementService;
import edu.unc.lib.dl.cdr.services.exception.EnhancementException;
import edu.unc.lib.dl.cdr.services.model.EnhancementMessage;
import edu.unc.lib.dl.cdr.services.model.FailedEnhancementMap;
import edu.unc.lib.dl.message.ActionMessage;
import edu.unc.lib.dl.util.JMSMessageUtil;

/**
 * Central service conductor class which stores and processes a queue of messages indicating updates to Fedora objects.
 * 
 * @author Gregory Jansen, Ben Pennell
 */
public class EnhancementConductor implements MessageConductor, ServiceConductor {
	private static final Logger log = LoggerFactory.getLogger(EnhancementConductor.class);

	public static final String identifier = "ENHANCEMENT";

	/**
	 * The object enhancement services, in priority order.
	 */
	private List<ObjectEnhancementService> services = new ArrayList<ObjectEnhancementService>();
	private Map<String, ObjectEnhancementService> servicesMap = new HashMap<String, ObjectEnhancementService>();

	/**
	 * Maximum numbers of threads in executor pool.
	 */
	private int maxThreads = 5;

	// Queue of pids with their invoking messages
	private BlockingQueue<EnhancementMessage> pidQueue = null;
	// List of pids which applied to locked pids when they were first called,
	// preserved in order
	private List<EnhancementMessage> collisionList = null;
	// List of messages that are currently being processed
	private List<EnhancementMessage> activeMessages = null;
	// List of messages that have recently been processed.
	private List<EnhancementMessage> finishedMessages = null;
	private int maxFinishedMessages = 300;
	private long finishedMessageTimeout = 86400000;
	
	// Set of locked pids, used to prevent items from being processed multiple
	// times at once
	private Set<String> lockedPids = null;
	// Set of pids which failed to process one or more services. Contents of the entry indicate which services have
	// failed
	private FailedEnhancementMap failedPids = null;

	private ServicesThreadPoolExecutor<PerformServicesRunnable> executor = null;

	private long recoverableDelay = 0;
	private long unexpectedExceptionDelay = 0;
	private long beforeExecuteDelay = 0;

	public EnhancementConductor() {
		log.debug("Starting up Services Conductor");
		pidQueue = new LinkedBlockingQueue<EnhancementMessage>();
		// Initialize as synchronized collections for thread safety
		collisionList = Collections.synchronizedList(new ArrayList<EnhancementMessage>());
		lockedPids = Collections.synchronizedSet(new HashSet<String>());
		activeMessages = Collections.synchronizedList(new ArrayList<EnhancementMessage>());
		finishedMessages = Collections.synchronizedList(new LimitedWindowList<EnhancementMessage>(maxFinishedMessages));
	}

	public String getIdentifier() {
		return identifier;
	}

	/**
	 * Initializes this bean after properties are set.
	 */
	public void init() {
		// start up the thread pool
		initializeExecutor();
	}

	private void initializeExecutor() {
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
	public void add(ActionMessage aMessage) {
		EnhancementMessage message = (EnhancementMessage) aMessage;
		synchronized (pidQueue) {
			synchronized (executor) {
				if (executor.isTerminating() || executor.isShutdown() || executor.isTerminated()) {
					log.debug("Ignoring message for pid " + message.getTargetID());
					return;
				}
				if (message.getMessageID() == null) {
					UUID uuid = UUID.randomUUID();
					message.setMessageID(identifier + ":" + uuid.toString());
				}
				boolean success = pidQueue.offer(message);
				if (!success) {
					log.error("Failure to queue pid " + message.getTargetID());
				}
				startProcessing();
			}
		}
	}

	public int getMaxThreadPoolSize() {
		return this.executor.getMaximumPoolSize();
	}

	public void setMaxThreadPoolSize(int threadPoolSize) {
		this.executor.setCorePoolSize(threadPoolSize);
		this.executor.setMaximumPoolSize(threadPoolSize);
	}

	public void resetMaxThreadPoolSize() {
		this.executor.setCorePoolSize(this.maxThreads);
		this.executor.setMaximumPoolSize(this.maxThreads);
	}

	@Override
	public boolean isPaused() {
		return this.executor.isPaused();
	}

	@Override
	public void pause() {
		this.executor.pause();
	}

	@Override
	public void resume() {
		this.executor.resume();
	}

	@Override
	public boolean isEmpty() {
		return this.pidQueue.size() == 0 && this.collisionList.size() == 0 && this.lockedPids.size() == 0;
	}

	@Override
	public boolean isIdle() {
		return isPaused() || this.lockedPids.size() == 0;
	}

	@Override
	public boolean isReady() {
		return !this.executor.isShutdown() && !this.executor.isTerminated() && !this.executor.isTerminating();
	}

	public Map<String, Object> getInfo() {
		// TODO put values in separate keys
		Map<String, Object> result = new HashMap<String, Object>();
		StringBuilder sb = new StringBuilder();
		sb.append("Services Conductor Status:\n")
				.append("Paused: " + isPaused() + "\n")
				.append("PID Queue: " + this.pidQueue.size() + "\n")
				.append("Collision List: " + this.collisionList.size() + "\n")
				.append("Locked pids: " + this.lockedPids.size() + "\n")
				.append("Failed pids: " + this.failedPids.size() + "\n")
				.append(
						"Executor: " + executor.getActiveCount() + " active workers, " + executor.getQueue().size()
								+ " queued");
		result.put("message", sb.toString());
		return result;
	}

	@Override
	public String queuesToString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Services Conductor Queues:\n").append("PID Queue: " + this.pidQueue + "\n")
				.append("Collision List: " + this.collisionList + "\n").append("Locked pids: " + this.lockedPids + "\n")
				.append("Failed pids: " + this.failedPids + "\n");
		return sb.toString();
	}

	public void logStatus() {
		log.info((String) getInfo().get("message"));
	}

	public void logQueues() {
		log.info(queuesToString());
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

	public synchronized void removeFailedPid(String pid) {
		this.failedPids.remove(pid);
	}

	public void reprocessFailedPids() {
		synchronized (failedPids) {
			for (String pid : failedPids.getPidToService().keySet()) {
				this.add(new EnhancementMessage(pid, JMSMessageUtil.servicesMessageNamespace,
						JMSMessageUtil.ServicesActions.APPLY_SERVICE_STACK.getName()));
			}
			failedPids.clear();
		}
	}

	/**
	 * Kick starts a thread for processing messages if needed
	 */
	protected void startProcessing() {
		executor.execute(new PerformServicesRunnable());
	}

	@Override
	public void shutdown() {
		this.executor.shutdown();
		this.clearQueue();
		this.lockedPids.clear();
		log.warn("ServiceConductor is shutting down, no further objects will be received");
	}

	@Override
	public void shutdownNow() {
		this.executor.shutdownNow();
		this.clearQueue();
		this.lockedPids.clear();
		log.warn("ServiceConductor is shutting down, no further objects will be received");
	}

	@Override
	public synchronized void abort() {
		this.lockedPids.clear();
		// Perform hard shutdown and wait for it to finish
		List<Runnable> runnables = this.executor.shutdownNow();
		while (this.executor.isTerminating() && !this.executor.isShutdown())
			;
		// restart and pause the executor
		initializeExecutor();
		pause();
		// Pass the old runnables on to the new executor.
		if (runnables != null) {
			for (Runnable runnable : runnables) {
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

	public ServicesThreadPoolExecutor<PerformServicesRunnable> getExecutor() {
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

	public synchronized void setServices(List<ObjectEnhancementService> services) {
		this.services = services;
		this.servicesMap = new HashMap<String, ObjectEnhancementService>();
		for (ObjectEnhancementService service : services) {
			this.servicesMap.put(service.getClass().getName(), service);
		}
	}

	public void setServices(ArrayList<ObjectEnhancementService> services) {
		this.services = services;
		this.servicesMap = new HashMap<String, ObjectEnhancementService>();
		for (ObjectEnhancementService service : services) {
			this.servicesMap.put(service.getClass().getName(), service);
		}
	}

	public BlockingQueue<EnhancementMessage> getPidQueue() {
		return pidQueue;
	}

	public void setPidQueue(BlockingQueue<EnhancementMessage> pidQueue) {
		this.pidQueue = pidQueue;
	}

	public Set<String> getLockedPids() {
		return lockedPids;
	}

	public void setLockedPids(Set<String> lockedPids) {
		this.lockedPids = lockedPids;
	}

	public List<EnhancementMessage> getCollisionList() {
		return collisionList;
	}

	public void setCollisionList(List<EnhancementMessage> collisionList) {
		this.collisionList = collisionList;
	}

	public FailedEnhancementMap getFailedPids() {
		return failedPids;
	}

	public void setFailedPids(FailedEnhancementMap failedPids) {
		this.failedPids = failedPids;
	}

	public List<EnhancementMessage> getActiveMessages() {
		return activeMessages;
	}

	public List<EnhancementMessage> getFinishedMessages() {
		return finishedMessages;
	}
	
	public void cleanupFinishedMessages() {
		long currentTime = System.currentTimeMillis();
		
		synchronized (finishedMessages) {
			Iterator<EnhancementMessage> iterator = finishedMessages.iterator();
			while (iterator.hasNext()) {
				EnhancementMessage message = iterator.next();
				if (currentTime - message.getTimeFinished() >= finishedMessageTimeout) {
					iterator.remove();
				}
			}
		}
	}

	public int getMaxFinishedMessages() {
		return maxFinishedMessages;
	}

	public void setMaxFinishedMessages(int maxFinishedMessages) {
		this.maxFinishedMessages = maxFinishedMessages;
	}

	public long getFinishedMessageTimeout() {
		return finishedMessageTimeout;
	}

	public void setFinishedMessageTimeout(long finishedMessageTimeout) {
		this.finishedMessageTimeout = finishedMessageTimeout;
	}

	@Override
	public int getQueueSize() {
		return this.pidQueue.size() + this.collisionList.size();
	}

	public static class LimitedWindowList<E> extends ArrayList<E> {
		private static final long serialVersionUID = 1L;
		private int maxWindowSize;
		
		public LimitedWindowList(int maxWindowSize) {
			super(maxWindowSize);
			this.maxWindowSize = maxWindowSize;
		}
		
		@Override
		public boolean addAll(Collection<? extends E> c) {
			if (this.size() + c.size() >= maxWindowSize) {
				if (c.size() >= maxWindowSize) {
					for (E e: c) {
						this.add(e);
					}
					return true;
				}
				int overage = this.size() + c.size() - maxWindowSize;
				this.removeRange(0, overage);
			}
			super.addAll(c);
			return true;
		}
		
		@Override
		public boolean add(E e) {
			// if the list is full, remove the oldest first
			if (this.size() == maxWindowSize)
				this.remove(0);
			super.add(e);
			return true;
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

		private EnhancementMessage message = null;

		public PerformServicesRunnable() {
			log.debug("Instantiating a new PerformServicesRunnable");
		}

		public EnhancementMessage getMessage() {
			return message;
		}

		private void retryException(ObjectEnhancementService s, String messagePrefix, Exception e, long retryDelay) {
			log.warn(messagePrefix + s.getClass().getName() + " for " + message.getTargetID()
					+ ".  Retrying after a delay.", e);
			try {
				Thread.sleep(retryDelay);
				this.applyService(s);
				log.info("Second attempt to run " + s.getClass().getName() + " for " + message.getTargetID()
						+ " was successful.");
			} catch (InterruptedException e2) {
				log.warn("Retry attempt for " + s.getClass().getName() + " for " + message.getTargetID()
						+ " was interrupted.");
				Thread.currentThread().interrupt();
			} catch (Exception e2) {
				log.error("Second attempt to run " + s.getClass().getName() + " for " + message.getTargetID() + " failed.");
				failedPids.add(message.getPid(), s.getClass(), message, e);
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
		private EnhancementMessage getNextMessage() {
			EnhancementMessage pid = null;

			do {
				synchronized (collisionList) {
					synchronized (pidQueue) {
						// First read from the collision list in case there are items
						// that were blocked which need to be read
						if (!collisionList.isEmpty()) {
							Iterator<EnhancementMessage> collisionIt = collisionList.iterator();
							while (collisionIt.hasNext()) {
								pid = collisionIt.next();
								synchronized (lockedPids) {
									if (!lockedPids.contains(pid.getTargetID())) {
										lockedPids.add(pid.getTargetID());
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

							if (pid != null) {
								synchronized (lockedPids) {
									if (lockedPids.contains(pid.getTargetID())) {
										collisionList.add(pid);
									} else {
										lockedPids.add(pid.getTargetID());
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
					log.warn("Services runnable interrupted while waiting to get next message.");
					Thread.currentThread().interrupt();
				}
			} while (pid == null && !Thread.currentThread().isInterrupted()
					&& (!pidQueue.isEmpty() || !collisionList.isEmpty()));
			return null;
		}

		public void applyService(ObjectEnhancementService s) throws EnhancementException {
			// Check if there were any failed services for this pid. If there were, check if the current service
			// was one of them.

			if (log.isDebugEnabled())
				log.debug("Applying service " + s.getClass().getCanonicalName() + " to " + message.getTargetID());

			Set<String> failedServices = null;
			synchronized (failedPids) {
				failedServices = failedPids.getFailedServices(message.getTargetID());
				// Determine if the service should not be run, depending on if the service is active, applicable and hasn't
				// previously failed
				if (!s.isActive() || !(s.getClass().getName().equals(message.getServiceName()) || s.isApplicable(message))
						|| (failedServices != null && failedServices.contains(s.getClass().getName()))) {
					if (log.isDebugEnabled())
						log.debug("Enhancement not run: " + s.getClass().getCanonicalName() + " on " + message.getTargetID());
					return;
				}
			}

			// Generate the enhancement
			log.info("Adding enhancement task: " + s.getClass().getCanonicalName() + " on " + message.getTargetID());
			Enhancement<Element> task = s.getEnhancement(message);

			// Pause before applying the service if the executor is paused.
			pauseWait();
			// If the thread was interrupted, we're done
			if (Thread.currentThread().isInterrupted()) {
				log.warn("Services thread " + Thread.currentThread().getId() + " for " + message.getTargetID()
						+ " interrupted before calling enhancement");
				return;
			}
			// Enhancement services need to be run serially per pid, so making a direct invocation of call
			task.call();
		}

		private void pauseWait() {
			while (executor.isPaused() && !Thread.currentThread().isInterrupted()) {
				try {
					Thread.sleep(1000L);
				} catch (Exception e) {
					log.debug("Services thread " + Thread.currentThread().getId() + " interrupted while paused");
					Thread.currentThread().interrupt();
				}
			}
		}

		@Override
		public void run() {
			log.debug("Starting new run of ServiceConductor thread " + this);
			// Get the next message, waiting as long as necessary for one to be free
			message = getNextMessage();

			if (log.isDebugEnabled())
				log.debug("Received pid " + message.getTargetID() + ": " + message.getFilteredServices());

			boolean serviceSuccess = false;
			if (message != null && message.getFilteredServices() != null) {
				try {
					// Quit before doing work if thread was interrupted
					if (Thread.currentThread().isInterrupted()) {
						log.debug("Thread was interrupted");
						return;
					}
					
					// Store message as active
					activeMessages.add(message);
					
					for (String serviceClassName : message.getFilteredServices()) {
						ObjectEnhancementService s = servicesMap.get(serviceClassName);
						try {
							if (s == null) {
								log.error("Service " + serviceClassName
										+ " is not included in the list of enabled services for this conductor, ignoring.");
							} else {
								if (Thread.currentThread().isInterrupted()) {
									log.debug("Thread was interrupted");
									return;
								}
								// Store which service is presently active on this message
								message.setActiveService(serviceClassName);
								// Apply the service
								this.applyService(s);
								serviceSuccess = true;
							}
						} catch (EnhancementException e) {
							switch (e.getSeverity()) {
								case RECOVERABLE:
									retryException(s,
											"A recoverable enhancement exception occurred while attempting to apply service "
													+ s.getClass().getName(), e, recoverableDelay);
									break;
								case UNRECOVERABLE:
									log.error("An unrecoverable exception occurred while attempting to apply service "
											+ s.getClass().getName() + " for " + message.getTargetID()
											+ ".  Adding to failure list.", e);
									failedPids.add(message.getPid(), s.getClass(), message, e);
									break;
								case FATAL:
									pause();
									log.error("A fatal exception occurred while attempting to apply service "
											+ s.getClass().getName() + " for " + message.getTargetID()
											+ ", halting all future services.", e);
									failedPids.add(message.getPid(), s.getClass(), message, e);
									return;
								default:
									log.error("An exception occurred while attempting to apply service "
											+ s.getClass().getName() + " for " + message.getTargetID(), e);
									failedPids.add(message.getPid(), s.getClass(), message, e);
									break;
							}
						} catch (RuntimeException e) {
							retryException(s, "A runtime error occurred while attempting to apply service", e,
									unexpectedExceptionDelay);
						} catch (Exception e) {
							log.error("An unexpected exception occurred while attempting to apply service "
									+ s.getClass().getName(), e);
							failedPids.add(message.getPid(), s.getClass(), message, e);
						}
					}
				} finally {
					lockedPids.remove(message.getTargetID());
					// Shift message to being finished
					activeMessages.remove(message);
					message.setTimeFinished(System.currentTimeMillis());
					// Store the message as finished if any services completed for it
					if (serviceSuccess)
						finishedMessages.add(message);
					// Unset the active service class
					message.setActiveService(null);
				}
			}
		}
	}
}
