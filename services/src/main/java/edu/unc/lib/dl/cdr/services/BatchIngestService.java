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
import java.util.HashSet;
import java.util.List;
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
import edu.unc.lib.dl.util.FileUtils;

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
	private int pollDirectorySeconds = 2;
	private int sweepFinishedSeconds = 60 * 60;
	private long beforeExecuteDelay = 0;
	private long finishedPurgeSeconds = 3600 * 24 * 2;

	public void init() {
		initializeExecutor();
		if (!isStartOnInit()) {
			this.executor.pause();
		}
		// add a file system monitor to queue new ingests
		pollingTimer = new Timer();
		pollingTimer.schedule(new EnqueueTask(), pollDirectorySeconds * 1000, pollDirectorySeconds * 1000);
		pollingTimer.schedule(new SweepFinishedTask(), 60 * 1000, sweepFinishedSeconds * 1000);
	}

	class EnqueueTask extends TimerTask {
		@Override
		public void run() {
			queueNewPendingIngests();
		}
	}

	class SweepFinishedTask extends TimerTask {
		@Override
		public void run() {
			sweepFinishedIngests();
		}
	}

	/**
	 *
	 */
	private void queueNewPendingIngests() {
		Set<String> handled = new HashSet<String>();
		for (BatchIngestTask task : this.executor.getAllRunningAndQueued()) {
			handled.add(task.getBaseDir().getName());
		}
		for (File dir : this.batchIngestQueue.getReadyIngestDirectories()) {
			if (!handled.contains(dir.getName())) {
				LOG.debug("Adding new batch ingest task to the queue: "+dir.getAbsolutePath());
				BatchIngestTask newtask = this.batchIngestTaskFactory.createTask();
				newtask.setBaseDir(dir);
				newtask.init();
				this.executor.execute(newtask);
			}
		}
	}

	/**
	 *
	 */
	public void sweepFinishedIngests() {
		for(File f : this.batchIngestQueue.getFinishedDirectories()) {
			try {
				long touched = new File(f, BatchIngestTask.INGEST_LOG).lastModified();
				if(touched + finishedPurgeSeconds*1000 < System.currentTimeMillis()) {
					LOG.debug("Sweeping away finished dir: "+f.getAbsolutePath());
					FileUtils.deleteDir(f);
				}
			} catch(Exception e) {
				throw new Error("Unexpected exception while sweeping finished dirs.");
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

	/* (non-Javadoc)
	 * @see edu.unc.lib.dl.cdr.services.processing.ServiceConductor#getActiveThreadCount()
	 */
	@Override
	public int getActiveThreadCount() {
		return this.executor.getActiveCount();
	}

	public int getQueuedJobCount() {
		return this.executor.getQueue().size();
	}

	public List<BatchIngestTask> getQueuedJobs() {
		return this.executor.getQueued();
	}

	public int getFailedJobCount() {
		return this.batchIngestQueue.getFailedDirectories().length;
	}

	public int getFinishedJobCount() {
		return this.batchIngestQueue.getFinishedDirectories().length;
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
		return this.executor.isPaused() || this.executor.getAllRunningAndQueued().isEmpty();
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

	public Set<BatchIngestTask> getActiveJobs() {
		return this.executor.getRunningNow();
	}

}
