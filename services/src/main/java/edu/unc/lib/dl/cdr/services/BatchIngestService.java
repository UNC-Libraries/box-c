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
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.unc.lib.dl.cdr.services.processing.ServiceConductor;
import edu.unc.lib.dl.cdr.services.processing.ServicesThreadPoolExecutor;
import edu.unc.lib.dl.services.BatchIngestQueue;
import edu.unc.lib.dl.services.BatchIngestTask;
import edu.unc.lib.dl.services.BatchIngestTaskFactory;

/**
 * The IngestService runs a single-threaded ingest service, based on information prearranged in a file system queue. The
 * service will pick up where it left off after a server crash or interruption, including halfway through a batch.
 *
 * @author Gregory Jansen
 *
 */
public class BatchIngestService implements ServiceConductor {
	private static final Log LOG = LogFactory.getLog(BatchIngestService.class);
	private static final String identifier = "BatchIngestService";

	private boolean startOnInit = false;
	private BatchIngestQueue batchIngestQueue = null;
	private BatchIngestTaskFactory batchIngestTaskFactory = null;
	protected ServicesThreadPoolExecutor<BatchIngestTask> executor = null;
	protected int maxThreads = 1;
	private Timer pollingTimer = null;
	private int pollDirectorySeconds = 10;
	private long beforeExecuteDelay = 0;

	public void init() {
		initializeExecutor();
		if (!isStartOnInit()) {
			this.executor.pause();
		}
		// add a file system monitor to queue new ingests
		pollingTimer = new Timer();
		pollingTimer.schedule(new EnqueueTask(), 20 * 1000, pollDirectorySeconds * 1000);
	}

	class EnqueueTask extends TimerTask {
		@Override
		public void run() {
			queueNewPendingIngests();
		}
	}

	/**
	 *
	 */
	private void queueNewPendingIngests() {
		LOG.debug("Checking for new ingests...");
		Set<String> handled = new HashSet<String>();
		for (BatchIngestTask task : this.executor.getAllRunningAndQueued()) {
			handled.add(task.getBaseDir().getName());
		}
		for (File dir : this.batchIngestQueue.getReadyIngestDirectories()) {
			if (!handled.contains(dir.getName())) {
				LOG.debug("Adding new batch ingest task to the queue: "+dir.getAbsolutePath());
				BatchIngestTask newtask = this.batchIngestTaskFactory.createTask();
				newtask.setBaseDir(dir);
				this.executor.execute(newtask);
			}
		}
	}

	public void destroy() {
		this.shutdown();
		this.pollingTimer.cancel();
	}

	public void shutdown() {
		this.executor.shutdownNow();
		LOG.warn("Batch Ingest Service is shutting down now, no further batches will be processed.");
	}

	public int getMaxThreads() {
		return maxThreads;
	}

	public void setMaxThreads(int maxThreads) {
		this.maxThreads = maxThreads;
	}

	public int activeThreadsCount() {
		return executor.getActiveCount();
	}

	protected void initializeExecutor() {
		LOG.debug("Initializing services thread pool executor with " + this.maxThreads + " threads.");
		this.executor = new ServicesThreadPoolExecutor<BatchIngestTask>(this.maxThreads, "SolrUpdates");
		this.executor.setKeepAliveTime(0, TimeUnit.DAYS);
		(this.executor).setBeforeExecuteDelay(beforeExecuteDelay);
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

	/**
	 * Blocks the calling thread until the queue is completely empty and ingest task is finished.
	 */
	// public void waitUntilIdle() {
	// LOG.debug("Thread waiting for idle ingest service: " + Thread.currentThread().getName());
	// // TODO reimplement
	// }

	/**
	 * Blocks the calling thread until an ingest task is active.
	 */
	// public void waitUntilActive() {
	// LOG.debug("Thread waiting for active ingest service: " + Thread.currentThread().getName());
	// // TODO reimplement
	// }

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

	/*
	 * (non-Javadoc)
	 *
	 * @see edu.unc.lib.dl.cdr.services.processing.ServiceConductor#pause()
	 */
	@Override
	public void pause() {
		this.executor.pause();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see edu.unc.lib.dl.cdr.services.processing.ServiceConductor#resume()
	 */
	@Override
	public void resume() {
		this.executor.resume();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see edu.unc.lib.dl.cdr.services.processing.ServiceConductor#isPaused()
	 */
	@Override
	public boolean isPaused() {
		return this.executor.isPaused();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see edu.unc.lib.dl.cdr.services.processing.ServiceConductor#isEmpty()
	 */
	@Override
	public boolean isEmpty() {
		return this.executor.getQueue().isEmpty();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see edu.unc.lib.dl.cdr.services.processing.ServiceConductor#isIdle()
	 */
	@Override
	public boolean isIdle() {
		return this.executor.isPaused() || this.executor.getQueue().isEmpty();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see edu.unc.lib.dl.cdr.services.processing.ServiceConductor#shutdownNow()
	 */
	@Override
	public void shutdownNow() {
		this.executor.shutdownNow();
		LOG.warn("Batch Ingest Service is shutting down NOW, no further batches will be processed.");
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see edu.unc.lib.dl.cdr.services.processing.ServiceConductor#abort()
	 */
	@Override
	public void abort() {
		this.executor.pause();
		this.executor.shutdownNow();
		try {
			this.executor.awaitTermination(5, TimeUnit.MINUTES);
		} catch (InterruptedException ignored) {
		}
		initializeExecutor();
		pause();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see edu.unc.lib.dl.cdr.services.processing.ServiceConductor#getConductorStatus()
	 */
	@Override
	public Map<String, Object> getInfo() {
		Map<String, Object> result = new HashMap<String, Object>();
		// TODO set JSON mappings
		return result;
	}

	public BatchIngestTaskFactory getBatchIngestTaskFactory() {
		return batchIngestTaskFactory;
	}

	public void setBatchIngestTaskFactory(BatchIngestTaskFactory batchIngestTaskFactory) {
		this.batchIngestTaskFactory = batchIngestTaskFactory;
	}

	public long getBeforeExecuteDelay() {
		return beforeExecuteDelay;
	}

	public void setBeforeExecuteDelay(long beforeExecuteDelay) {
		this.beforeExecuteDelay = beforeExecuteDelay;
		if (this.executor != null) {
			this.executor.setBeforeExecuteDelay(beforeExecuteDelay);
		}
	}

}
