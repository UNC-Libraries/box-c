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
package edu.unc.lib.dl.ingest.sip;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.unc.lib.dl.agents.Agent;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.FileUtils;
import edu.unc.lib.dl.util.ZipFileUtil;

public class METSPackageSIP implements SubmissionInformationPackage {
	private static final Log log = LogFactory.getLog(METSPackageSIP.class);
	public static final String metsLocation = "METS.xml";
	public static final String metsLocation2 = "mets.xml";
	private boolean discardDataFilesOnDestroy = true;
	private Agent owner = null;
	private File metsFile = null;
	private File batchPrepDir = null;
	private File sipDataSubDir = null;
	private PID containerPID = null;
	private boolean allowIndexing = true;
	private PreIngestEventLogger preIngestEvents = new PreIngestEventLogger();

	public boolean isAllowIndexing() {
		return allowIndexing;
	}

	public void setAllowIndexing(boolean allowIndexing) {
		this.allowIndexing = allowIndexing;
	}

	public PreIngestEventLogger getPreIngestEventLogger() {
		return this.preIngestEvents;
	}

	public METSPackageSIP(PID containerPID, File sip, Agent owner, boolean isZIP) throws IOException {
		this.batchPrepDir = FileUtils.createTempDirectory("ingest-prep");
		this.sipDataSubDir = new File(this.batchPrepDir, "data");
		this.sipDataSubDir.mkdir();
		if (isZIP) {
			ZipFileUtil.unzipToDir(sip, this.sipDataSubDir);
			metsFile = new File(sipDataSubDir, metsLocation);
			if (!metsFile.exists()) {
				metsFile = new File(sipDataSubDir, metsLocation2);
			}
		} else { // NOT A ZIP, JUST METS FILE
			this.metsFile = new File(this.sipDataSubDir, "mets.xml");
			FileUtils.renameOrMoveTo(sip, metsFile);
		}
		if (!metsFile.exists()) {
			throw new IOException("METS file " + metsFile.getPath() + " not found.");
		}
		this.containerPID = containerPID;
		this.owner = owner;
	}

	public File getMetsFile() {
		return metsFile;
	}

	public File getSIPDataDir() {
		return sipDataSubDir;
	}

	public List<File> getDataFiles() {
		List<File> result = null;
		if (this.sipDataSubDir != null) {
			result = FileUtils.getFilesInDir(this.sipDataSubDir);
			File mets = null;
			for (File f : result) {
				if (this.getMetsFile().equals(f)) {
					mets = f;
				}
			}
			if (mets != null) {
				result.remove(mets);
			}
		} else {
			result = new ArrayList<File>();
		}
		return result;
	}

	public PID getContainerPID() {
		return containerPID;
	}

	public Agent getOwner() {
		return owner;
	}

	public File getFileForLocator(String url) throws IOException {
		if (getSIPDataDir() != null) {
			return FileUtils.getFileForUrl(url, getSIPDataDir());
		} else {
			throw new IOException("There are no zipped files associated with this METS");
		}
	}

	public void setDiscardDataFilesOnDestroy(boolean discard) {
		this.discardDataFilesOnDestroy = discard;
	}

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		//this.destroy();
	}

	public void delete() {
		log.debug("delete called");
		// cleanup *any* remaining files
		if (this.batchPrepDir != null && batchPrepDir.exists()) {
			FileUtils.deleteDir(batchPrepDir);
		}
	}

	public File getBatchPrepDir() {
		return batchPrepDir;
	}

}
