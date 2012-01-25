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
package edu.unc.lib.dl.services;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.AbstractQueue;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.unc.lib.dl.util.FileUtils;
import edu.unc.lib.dl.util.IngestProperties;

/**
 * @author Gregory Jansen
 *
 */
public class BatchIngestQueue extends AbstractQueue<File> {
	private static final Log LOG = LogFactory.getLog(BatchIngestQueue.class);
	private static final String FAILED_SUBDIR = "failed";
	private static final String QUEUED_SUBDIR = "queued";
	private static final String READY_FOR_INGEST = "READY";
	private String serviceDirectoryPath = null;
	private File serviceDirectory = null;
	private File failedDirectory = null;
	private File queuedDirectory = null;

	public void init() {
		this.serviceDirectory = new File(serviceDirectoryPath);
		this.failedDirectory = new File(this.serviceDirectory, FAILED_SUBDIR);
		this.queuedDirectory = new File(this.serviceDirectory, QUEUED_SUBDIR);
		if (!this.serviceDirectory.exists()) {
			this.serviceDirectory.mkdir();
			this.failedDirectory.mkdir();
			this.queuedDirectory.mkdir();
		}
	}

	public String getServiceDirectoryPath() {
		return serviceDirectoryPath;
	}

	public void setServiceDirectoryPath(String serviceDirectoryPath) {
		this.serviceDirectoryPath = serviceDirectoryPath;
	}

	@Override
	public boolean offer(File prepDir) {
		IngestProperties props = null;
		try {
			props = new IngestProperties(prepDir);
		} catch (Exception e) {
			LOG.error(e);
			return false;
		}
		File result = new File(this.queuedDirectory, System.currentTimeMillis() + "-" + props.getSubmitter());
		if (result.exists()) {
			LOG.error("queued directory name conflict: "+result.toString());
			return false;
		}
		try {
			FileUtils.renameOrMoveTo(prepDir, result);
			File readyFile = new File(result, READY_FOR_INGEST);
			readyFile.createNewFile();
		} catch (IOException e) {
			LOG.error(e);
			return false;
		}
		return true;
	}

	@Override
	public File peek() {
		try {
			return this.iterator().next();
		} catch(NoSuchElementException e) {
			return null;
		}
	}

	@Override
	public File poll() {
		throw new UnsupportedOperationException("poll() operation unsupported for a directory queue");
	}

	@Override
	public Iterator<File> iterator() {
		File[] batchDirs = getSortedDirectoryArray();
		return Arrays.asList(batchDirs).iterator();
	}

	private static class ReadyDirectoriesFilter implements FileFilter {
		@Override
		public boolean accept(File arg0) {
			if(arg0.isDirectory()) {
				File readyFile = new File(arg0, READY_FOR_INGEST);
				return readyFile.exists();
			}
			return false;
		}
	}
	private static ReadyDirectoriesFilter readyFilter = new ReadyDirectoriesFilter();
	private static class ModDateComparator implements Comparator<File> {
		@Override
		public int compare(File o1, File o2) {
			if(o1 == null || o2 == null) return 0;
			return (int)(o1.lastModified() - o2.lastModified());
		}
	}
	private static Comparator<File> earlyFilesFirst = new ModDateComparator();

	/**
	 * @return
	 */
	private File[] getSortedDirectoryArray() {
		File[] readyDirs = this.queuedDirectory.listFiles(BatchIngestQueue.readyFilter);
		Arrays.sort(readyDirs, BatchIngestQueue.earlyFilesFirst);
		return readyDirs;
	}

	@Override
	public int size() {
		return getSortedDirectoryArray().length;
	}

	/**
	 * @param baseDir
	 */
	public void fail(File baseDir) {
		File failedLoc = new File(failedDirectory, baseDir.getName());
		LOG.info("Moving failed batch ingest to " + failedLoc);
		try {
			FileUtils.renameOrMoveTo(baseDir, failedLoc);
		} catch (IOException e) {
			throw new Error(e);
		}
	}



}
