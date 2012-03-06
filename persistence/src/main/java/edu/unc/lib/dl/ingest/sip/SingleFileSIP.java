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

import edu.unc.lib.dl.agents.Agent;
import edu.unc.lib.dl.fedora.PID;

/**
 * This submission package is composed of a single data file, plus MODS metadata. These submissions are normally
 * converted into a single Fedora object.
 * 
 * @author count0
 * 
 */
public class SingleFileSIP implements SubmissionInformationPackage {
	private boolean allowIndexing = true;
	private PID containerPID = null;
	private File data = null;
	private String fileLabel = null;
	private String md5checksum = null;
	private String mimeType = null;
	private File modsXML = null;
	private boolean discardFilesOnDestroy = true;

	public boolean isDiscardFilesOnDestroy() {
		return discardFilesOnDestroy;
	}

	public void destroy() {
		if (this.discardFilesOnDestroy) {
			this.data.delete();
		}
	}

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		this.destroy();
	}

	public void setDiscardFilesOnDestroy(boolean discardFilesOnDestroy) {
		this.discardFilesOnDestroy = discardFilesOnDestroy;
	}

	private PreIngestEventLogger preIngestEvents = new PreIngestEventLogger();

	public PreIngestEventLogger getPreIngestEventLogger() {
		return this.preIngestEvents;
	}

	public PID getContainerPID() {
		return containerPID;
	}

	public File getData() {
		return data;
	}

	public String getFileLabel() {
		return fileLabel;
	}

	public String getMd5checksum() {
		return md5checksum;
	}

	public String getMimeType() {
		return mimeType;
	}

	public File getModsXML() {
		return modsXML;
	}

	public boolean isAllowIndexing() {
		return allowIndexing;
	}

	/**
	 * Tells the repository whether or not to this object. (Default is yes)
	 * 
	 * @param allowIndexing
	 */
	public void setAllowIndexing(boolean allowIndexing) {
		this.allowIndexing = allowIndexing;
	}

	/**
	 * Set the path to the folder that will contain the entire submission.
	 * 
	 * @param containerPath
	 */
	public void setContainerPID(PID containerPID) {
		this.containerPID = containerPID;
	}

	/**
	 * Set the data file.
	 * 
	 * @param data
	 */
	public void setData(File data) {
		this.data = data;
	}

	/**
	 * Set the label for the data file, usually the original file name.
	 * 
	 * @param fileLabel
	 */
	public void setFileLabel(String fileLabel) {
		this.fileLabel = fileLabel;
	}

	/**
	 * Optional: set the checksum for the data file.
	 * 
	 * @param md5checksum
	 */
	public void setMd5checksum(String md5checksum) {
		this.md5checksum = md5checksum;
	}

	/**
	 * Set the IANA MIME-Type of the data file.
	 * 
	 * @param mimeType
	 */
	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}

	/**
	 * Set the MODS metadata file.
	 * 
	 * @param modsXML
	 */
	public void setModsXML(File modsXML) {
		this.modsXML = modsXML;
	}

}
