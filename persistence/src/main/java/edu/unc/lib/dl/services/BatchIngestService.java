/**
 * Copyright 2011 The University of North Carolina at Chapel Hill
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
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.unc.lib.dl.ingest.BatchIngestTask;
import edu.unc.lib.dl.ingest.IngestException;
import edu.unc.lib.dl.ingest.IngestProperties;
import edu.unc.lib.dl.util.ZipFileUtil;

/**
 * The IngestService runs a single-threaded ingest queue, based on archival information prearranged in the file system.
 * The service will pick up where it left off after a server crash or interruption, including halfway through a batch.
 *
 * @author Gregory Jansen
 *
 */
public abstract class BatchIngestService {
	private static final Log LOG = LogFactory.getLog(BatchIngestService.class);
	private static final String PREP_SUBDIR = "batch-prep";
	private static final String QUEUE_SUBDIR = "batch-queue";
	private static final String READY_FOR_INGEST = "READY";

	private String serviceDirectoryPath = null;
	private File serviceDirectory = null;
	private File prepDirectory = null;
	private File queueDirectory = null;

	public BatchIngestService() {
	}

	public void init() {
		this.serviceDirectory = new File(serviceDirectoryPath);
		this.prepDirectory = new File(this.serviceDirectory, PREP_SUBDIR);
		this.queueDirectory = new File(this.serviceDirectory, QUEUE_SUBDIR);
		if(!this.serviceDirectory.exists()) {
			this.serviceDirectory.mkdir();
			this.prepDirectory.mkdir();
			this.queueDirectory.mkdir();
		}
	}

	public void pauseQueue() {
		// stop queuing thread
		// stop current task
	}

	public void resumeQueue() {
		// start queueing thread
	}

	/**
	 * Adds a prepared batch ingest to the queue. The directory and all files will be renamed to a new location,
	 * managed by this service.
	 *
	 * @param prepDir
	 *           the directory for the prepared batch.
	 */
	public void queueBatch(File prepDir) throws IngestException {
		File queuedDir = moveToQueue(prepDir);
		// rename is not atomic, so we need to add a marker file
		try {
			new File(queuedDir, READY_FOR_INGEST).createNewFile();
		} catch (IOException e) {
			throw new Error("Cannot create READY file.", e);
		}
		LOG.info("Added ingest batch: "+queuedDir.getAbsolutePath());
	}

	/**
	 * Begins ingesting a batch of objects immediately, in parallel with other batches in the queue.
	 * @param prepDir
	 * @throws IngestException
	 */
	public void ingestBatchNow(File prepDir) throws IngestException {
		File queuedDir = moveToQueue(prepDir);
		// do not set marker file!
		LOG.info("Ingesting batch now, in parallel with queue: "+queuedDir.getAbsolutePath());
		BatchIngestTask task = createTask(); //obtain from Spring prototype
		task.init(queuedDir);
		task.run();
		// return after ingest complete..
	}

	/**
	 * @param prepDir
	 * @return the directory where the batch is queued
	 * @throws IngestException
	 * @throws IOException
	 */
	private File moveToQueue(File prepDir) throws IngestException {
		IngestProperties props = new IngestProperties(prepDir);
		File result = new File(this.queueDirectory, props.getSubmitter()+"-"+System.currentTimeMillis());
		if(result.exists()) throw new Error("Queue folder conflict (Unexpected)");
		if(!prepDir.renameTo(result)) {
			// cannot rename, try copy and delete
			LOG.warn("Cannot rename directory "+prepDir+" to queue location "+result+", forced to perform slower copy and delete operations.");
			try {
				ZipFileUtil.copyFolder(prepDir, result);
			} catch(IOException e) {
				throw new IngestException("Cannot rename or copy batch ingest to queue.", e);
			}
			if(!prepDir.delete()) {
				LOG.warn("Cannot delete original, non-queue, location of a batch ingest.");
			}
		}
		return result;
	}

	/**
	 * Container may override this method to return a task bean
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
