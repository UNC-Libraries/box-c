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

import edu.unc.lib.dl.ingest.IngestException;

/**
 * @author Gregory Jansen
 *
 */
public interface BatchIngestService {

	public abstract void pauseQueue();

	public abstract void resumeQueue();

	/**
	 * Adds a prepared batch ingest to the queue. The directory and all files will be renamed to a new location, managed
	 * by this service.
	 *
	 * @param prepDir
	 *           the directory for the prepared batch.
	 */
	public abstract void queueBatch(File prepDir) throws IngestException;

	/**
	 * Begins ingesting a batch of objects immediately, in parallel with other batches in the queue.
	 *
	 * @param prepDir
	 * @throws IngestException
	 */
	public abstract void ingestBatchNow(File prepDir) throws IngestException;

}