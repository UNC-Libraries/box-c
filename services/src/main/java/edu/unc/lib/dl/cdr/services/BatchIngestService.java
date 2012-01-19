/**
 * Copyright 2008 The University of North Carolina at Chapel Hill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.unc.lib.dl.cdr.services;

import java.io.File;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.unc.lib.dl.services.BatchIngestQueue;
import edu.unc.lib.dl.services.BatchIngestTask;
import edu.unc.lib.dl.util.FileUtils;

/**
 * The IngestService runs a single-threaded ingest service, based on information prearranged in a file system queue.
 * The service will pick up where it left off after a server crash or interruption, including halfway through a batch.
 *
 * @author Gregory Jansen
 *
 */
public abstract class BatchIngestService {
	private static final Log LOG = LogFactory.getLog(BatchIngestService.class);

	private boolean startOnInit = false;

	private ReentrantLock pauseLock = new ReentrantLock();

	private Thread serviceThread = null;
	private Thread taskThread = null;
	private BatchIngestQueue batchIngestQueue = null;
	private BatchIngestTask batchIngestTask = null;

	public BatchIngestService() {
	}

	public void init() {
		if (isStartOnInit()) {
			start();
		}
	}

	public void start() {
		this.pauseLock.lock();
		try {
			LOG.info("Starting Batch Ingest Service...");
			serviceThread = new Thread(newServiceRunner(), "BatchIngestThread");
			serviceThread.start();
		} finally {
			pauseLock.unlock();
		}
	}

	private Runnable newServiceRunner() {
		return new Runnable() {

			@Override
			public void run() {
				LOG.debug("Batch Ingest Service thread started.");
				while (true) {
					try {
						if (taskThread == null || !taskThread.isAlive()) {
							// clean up last batch
							if (batchIngestTask != null) {
								if (batchIngestTask.isFailed()) {
										batchIngestQueue.fail(batchIngestTask.getBaseDir());
								} else {
									FileUtils.deleteDir(batchIngestTask.getBaseDir());
								}
								batchIngestTask = null;
							}
							taskThread = null;

							File nextDir = batchIngestQueue.peek();
							// start next batch
							if (nextDir != null) {
								LOG.info("Creating task for " + nextDir);
								batchIngestTask = createTask();
								batchIngestTask.init(nextDir);
								taskThread = new Thread(batchIngestTask, nextDir.getName());
								taskThread.start();
							} else {
								// queue empty
							}
							notifyWaitingThreads();
						} else {
							Thread.sleep(10 * 1000);
						}
					} catch (InterruptedException e) {
						LOG.debug("Queue thread interrupted.", e);
					}
					if (Thread.interrupted()) {
						return;
					}
				}
			}
		};
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see edu.unc.lib.dl.services.BatchIngestServiceInterface#pauseQueue()
	 */
	public void pause() {
		this.pauseLock.lock();
		try {
			LOG.info("Pausing Batch Ingest Service..");
			if (this.serviceThread != null && this.serviceThread.isAlive()) {
				this.serviceThread.interrupt();
			}
			if (this.batchIngestTask != null) {
				this.batchIngestTask.stop();
			}
			if (this.taskThread != null && this.taskThread.isAlive()) {
				try {
					LOG.info("Waiting up to 60 seconds for batch ingest task to stop gracefully..");
					this.taskThread.join(60 * 1000);
				} catch (InterruptedException e) {
					LOG.info("Interrupting batch ingest thread, after giving it time to stop.");
					this.taskThread.interrupt();
				}
			}
		} finally {
			pauseLock.unlock();
		}
	}

	private synchronized void notifyWaitingThreads() {
		synchronized (this) {
			notifyAll();
		}
	}

	/**
	 * Blocks the calling thread until the queue is completely empty and ingest task is finished.
	 */
	public void waitUntilIdle() {
		LOG.debug("Thread waiting for idle ingest service: " + Thread.currentThread().getName());
		synchronized (this) {
			while (this.batchIngestTask != null) {
				try {
					this.wait();
				} catch (InterruptedException e) {
				}
			}
		}
	}

	/**
	 * Blocks the calling thread until an ingest task is active.
	 */
	public void waitUntilActive() {
		LOG.debug("Thread waiting for active ingest service: " + Thread.currentThread().getName());
		synchronized (this) {
			while (this.batchIngestTask == null) {
				try {
					this.wait();
				} catch (InterruptedException e) {
				}
			}
		}
	}

	/**
	 * Container may override this method to return a task bean
	 *
	 * @return a BatchIngestTask bean configured for this repository
	 */
	public abstract BatchIngestTask createTask();

	public boolean isStartOnInit() {
		return startOnInit;
	}

	public void setStartOnInit(boolean startOnInit) {
		this.startOnInit = startOnInit;
	}

	public BatchIngestQueue getBatchIngestQueue() {
		return batchIngestQueue;
	}

	public void setBatchIngestQueue(BatchIngestQueue batchIngestQueue) {
		this.batchIngestQueue = batchIngestQueue;
	}

}
