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

import edu.unc.lib.dl.ingest.IngestException;
import edu.unc.lib.dl.ingest.IngestProperties;

/**
 * The IngestService runs a single-threaded ingest queue, based on archival information prearranged in the file system.
 * The service will pick up where it left off after a server crash or interruption, including halfway through a batch.
 *
 * @author Gregory Jansen
 *
 */
public abstract class BatchIngestService {
	private static final Log log = LogFactory.getLog(BatchIngestService.class);
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

	/**
	 * Adds a prepared batch ingest to the queue. The directory and all files will be renamed to a new location,
	 * managed by this service.
	 *
	 * @param prepDir
	 *           the directory for the prepared batch.
	 */
	public void addBatch(File prepDir) throws IngestException {
		IngestProperties props = new IngestProperties(prepDir);
		File target = new File(this.queueDirectory, props.getSubmitter()+"-"+System.currentTimeMillis());
		if(target.exists()) throw new Error("Queue folder conflict (Unexpected)");
		prepDir.renameTo(target);
		// rename is not atomic, so we need to add a marker file
		try {
			new File(target, READY_FOR_INGEST).createNewFile();
		} catch (IOException e) {
			throw new Error("Cannot create READY file.", e);
		}
		log.info("Added ingest batch: "+target.getAbsolutePath());
	}



	public String getServiceDirectoryPath() {
		return serviceDirectoryPath;
	}



	public void setServiceDirectoryPath(String serviceDirectoryPath) {
		this.serviceDirectoryPath = serviceDirectoryPath;
	}

}
