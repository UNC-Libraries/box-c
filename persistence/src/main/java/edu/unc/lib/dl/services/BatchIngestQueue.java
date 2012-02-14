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
import java.util.Arrays;
import java.util.Comparator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.unc.lib.dl.util.FileUtils;
import edu.unc.lib.dl.util.IngestProperties;

/**
 * @author Gregory Jansen
 *
 */
public class BatchIngestQueue {
	private static final Log LOG = LogFactory.getLog(BatchIngestQueue.class);
	public static final String FAILED_SUBDIR = "failed";
	public static final String FINISHED_SUBDIR = "finished";
	private static final String QUEUED_SUBDIR = "queued";
	private static final String READY_FILE = "READY";
	private String serviceDirectoryPath = null;
	private File serviceDirectory = null;
	private File queuedDirectory = null;
	private File failedDirectory = null;
	private File finishedDirectory = null;

	public void init() {
		this.serviceDirectory = new File(serviceDirectoryPath);
		this.queuedDirectory = new File(this.serviceDirectory, QUEUED_SUBDIR);
		this.failedDirectory = new File(this.serviceDirectory, FAILED_SUBDIR);
		this.finishedDirectory = new File(this.serviceDirectory, FINISHED_SUBDIR);
		if(!this.serviceDirectory.exists()) this.serviceDirectory.mkdir();
		if(!this.failedDirectory.exists()) this.failedDirectory.mkdir();
		if(!this.finishedDirectory.exists()) this.finishedDirectory.mkdir();
		if(!this.queuedDirectory.exists()) this.queuedDirectory.mkdir();
	}

	public String getServiceDirectoryPath() {
		return serviceDirectoryPath;
	}

	public void setServiceDirectoryPath(String serviceDirectoryPath) {
		this.serviceDirectoryPath = serviceDirectoryPath;
	}

	public boolean add(File prepDir) {
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
			File readyFile = new File(result, READY_FILE);
			readyFile.createNewFile();
		} catch (IOException e) {
			LOG.error(e);
			return false;
		}
		return true;
	}

	public File[] getFailedDirectories() {
		File[] batchDirs = this.failedDirectory.listFiles(new FileFilter() {
			@Override
			public boolean accept(File arg0) {
				return arg0.isDirectory();
			}
		});
		Arrays.sort(batchDirs, new Comparator<File>() {
			@Override
			public int compare(File o1, File o2) {
				if(o1 == null || o2 == null) return 0;
				return (int)(o1.lastModified() - o2.lastModified());
			}
		});
		return batchDirs;
	}

	/**
	 * @return
	 */
	public File[] getReadyIngestDirectories() {
		File[] batchDirs = this.queuedDirectory.listFiles(new FileFilter() {
			@Override
			public boolean accept(File arg0) {
				if(arg0.isDirectory()) {
					String[] readyFiles = arg0.list(new FilenameFilter() {
						@Override
						public boolean accept(File dir, String name) {
							return (READY_FILE.equals(name));
						}
					});
					if(readyFiles.length > 0) {
						return true;
					}
				}
				return false;
			}
		});
		Arrays.sort(batchDirs, new Comparator<File>() {
			@Override
			public int compare(File o1, File o2) {
				if(o1 == null || o2 == null) return 0;
				return (int)(o1.lastModified() - o2.lastModified());
			}
		});
		return batchDirs;
	}

	/**
	 * @param baseDir
	 */
	public void moveToFailedDir(File baseDir) {
		File failedLoc = new File(failedDirectory, baseDir.getName());
		LOG.info("Moving failed batch ingest to " + failedLoc);
		try {
			FileUtils.renameOrMoveTo(baseDir, failedLoc);
		} catch (IOException e) {
			throw new Error(e);
		}
	}

	/**
	 * @param baseDir
	 */
	public void moveToFinishedDir(File baseDir) {
		File finishedLoc = new File(finishedDirectory, baseDir.getName());
		LOG.info("Moving finished batch ingest to " + finishedLoc);
		try {

			FileUtils.renameOrMoveTo(baseDir, finishedLoc);
		} catch (IOException e) {
			throw new Error(e);
		}
	}

	/**
	 * @return
	 */
	public File[] getFinishedDirectories() {
		File[] batchDirs = this.finishedDirectory.listFiles(new FileFilter() {
			@Override
			public boolean accept(File arg0) {
				return arg0.isDirectory();
			}
		});
		Arrays.sort(batchDirs, new Comparator<File>() {
			@Override
			public int compare(File o1, File o2) {
				if(o1 == null || o2 == null) return 0;
				return (int)(o1.lastModified() - o2.lastModified());
			}
		});
		return batchDirs;
	}

}
