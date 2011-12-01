/**
 * Copyright 2011 The University of Northarolina at Chapel Hill
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
package edu.unc.lib.dl.services;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.unc.lib.dl.ingest.IngestException;
import edu.unc.lib.dl.util.FileUtils;
import edu.unc.lib.dl.util.IngestProperties;

/**
 * The IngestService runs a single-threaded ingest queue, based on archival information prearranged in the file system.
 * The service will pick up where it left off after a server crash or interruption, including halfway through a batch.
 *
 * @author Gregory Jansen
 *
 */
public abstract class BatchIngestServiceImpl implements BatchIngestService {
	private static final Log LOG = LogFactory.getLog(BatchIngestServiceImpl.class);
	private static final String FAILED_SUBDIR = "failed";
	private static final String QUEUED_SUBDIR = "queued";
	private static final String READY_FOR_INGEST = "READY";

	private String serviceDirectoryPath = null;
	private File serviceDirectory = null;
	private File failedDirectory = null;
	private File queuedDirectory = null;

	private ReentrantLock pauseLock = new ReentrantLock();

	private Thread qThread = null;
	private Thread taskThread = null;
	private BatchIngestTask batchIngestTask = null;

	public BatchIngestServiceImpl() {
	}

	public void init() {
		this.serviceDirectory = new File(serviceDirectoryPath);
		this.failedDirectory = new File(this.serviceDirectory, FAILED_SUBDIR);
		this.queuedDirectory = new File(this.serviceDirectory, QUEUED_SUBDIR);
		if (!this.serviceDirectory.exists()) {
			this.serviceDirectory.mkdir();
			this.failedDirectory.mkdir();
			this.queuedDirectory.mkdir();
		}
		startQueue();
	}

	@Override
	public void startQueue() {
		this.pauseLock.lock();
		try {
			LOG.info("Starting Batch Ingest Service...");
			qThread = new Thread(newQRunner(), "BatchIngestThread");
			qThread.start();
		} finally {
			pauseLock.unlock();
		}
	}

	private Runnable newQRunner() {
		return new Runnable() {

			@Override
			public void run() {
				LOG.debug("Queue thread started.");
				while (true) {
					try {
						if (taskThread == null || !taskThread.isAlive()) {
							// clean up last batch
							if (batchIngestTask != null) {
								if (batchIngestTask.isFailed()) {
									try {
										File failedLoc = new File(failedDirectory,
												batchIngestTask.baseDir.getName());
										LOG.info("Moving failed batch ingest to "+failedLoc);
										FileUtils.renameOrMoveTo(batchIngestTask.baseDir, failedLoc);
									} catch (IOException e) {
										throw new Error("Cannot move failed ingest", e);
									}
								} else {
									FileUtils.deleteDir(batchIngestTask.baseDir);
								}
								batchIngestTask = null;
							}
							taskThread = null;

							File nextDir = getNextBatch();
							// start next batch
							if (nextDir != null) {
								LOG.info("Creating task for "+nextDir);
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
	@Override
	public void pauseQueue() {
		this.pauseLock.lock();
		try {
			LOG.info("Pausing Batch Ingest Service..");
			if (this.qThread != null && this.qThread.isAlive()) {
				this.qThread.interrupt();
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

	/*
	 * (non-Javadoc)
	 *
	 * @see edu.unc.lib.dl.services.BatchIngestServiceInterface#queueBatch(java.io.File)
	 */
	@Override
	public void queueBatch(File prepDir) throws IngestException {
		File queuedDir = moveToQueue(prepDir);
		// rename is not atomic, so we need to add a marker file
		try {
			new File(queuedDir, READY_FOR_INGEST).createNewFile();
		} catch (IOException e) {
			throw new Error("Cannot create READY file.", e);
		}
		LOG.info("Added ingest batch: " + queuedDir.getAbsolutePath());
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see edu.unc.lib.dl.services.BatchIngestServiceInterface#ingestBatchNow(java.io.File)
	 */
	@Override
	public void ingestBatchNow(File prepDir) throws IngestException {
		//File queuedDir = moveToInstant(prepDir);
		// do not set marker file!
		LOG.info("Ingesting batch now, in parallel with queue: " + prepDir.getAbsolutePath());
		BatchIngestTask task = createTask(); // obtain from Spring prototype
		task.init(prepDir);
		task.run();
		task = null;
		FileUtils.deleteDir(prepDir);
	}

	/**
	 * @param prepDir
	 * @return the directory where the batch is queued
	 * @throws IngestException
	 * @throws IOException
	 */
	private File moveToQueue(File prepDir) throws IngestException {
		IngestProperties props = null;
		try {
			props = new IngestProperties(prepDir);
		} catch (Exception e) {
			throw new IngestException("Cannot load ingest properties.", e);
		}
		File result = new File(this.queuedDirectory, props.getSubmitter() + "-" + System.currentTimeMillis());
		if (result.exists())
			throw new Error("Queue folder conflict (Unexpected)");
		try {
			FileUtils.renameOrMoveTo(prepDir, result);
		} catch (IOException e) {
			throw new IngestException("Cannot move prep folder to queue location.", e);
		}
		return result;
	}

	/**
	 * Returns the next batch ingest directory in the queue or null if empty.
	 *
	 * @return a batch ingest directory
	 */
	private File getNextBatch() {
		File result = null;
		File[] batchDirs = this.queuedDirectory.listFiles(new FileFilter() {
			@Override
			public boolean accept(File arg0) {
				return arg0.isDirectory();
			}
		});
		Arrays.sort(batchDirs, new Comparator<File>() {
			@Override
			public int compare(File o1, File o2) {
				return String.CASE_INSENSITIVE_ORDER.compare(o1.getName(), o2.getName());
			}
		});
		if(batchDirs.length > 0) {
			result = batchDirs[0];
		}
		return result;
	}

	/**
	 * Blocks the calling thread until the queue is completely empty and ingest task is finished.
	 */
	@Override
	public void waitUntilIdle() {
		LOG.debug("Thread waiting for idle ingest service: "+Thread.currentThread().getName());
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
	@Override
	public void waitUntilActive() {
		LOG.debug("Thread waiting for active ingest service: "+Thread.currentThread().getName());
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

	public String getServiceDirectoryPath() {
		return serviceDirectoryPath;
	}

	public void setServiceDirectoryPath(String serviceDirectoryPath) {
		this.serviceDirectoryPath = serviceDirectoryPath;
	}

}
