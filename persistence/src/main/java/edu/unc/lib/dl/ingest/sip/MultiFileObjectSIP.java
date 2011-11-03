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
import java.util.List;

import edu.unc.lib.dl.agents.Agent;
import edu.unc.lib.dl.fedora.PID;

/**
 * This submission package is composed of a single file file, plus MODS metadata. These submissions are normally
 * converted into a single Fedora object.
 *
 * @author count0
 *
 */
public class MultiFileObjectSIP implements SubmissionInformationPackage {
	private boolean allowIndexing = true;
	private PID containerPID = null;
	private List<Datastream> datastreams = null;
	private Datastream defaultWebData = null;
	private boolean discardFilesOnDestroy = true;

	public boolean isDiscardFilesOnDestroy() {
		return discardFilesOnDestroy;
	}

	public void setDiscardFilesOnDestroy(boolean discardFilesOnDestroy) {
		this.discardFilesOnDestroy = discardFilesOnDestroy;
	}

	private PreIngestEventLogger preIngestEvents = new PreIngestEventLogger();

	/**
	 * Get a reference to the datastream with the default web representation
	 *
	 * @return
	 */
	public Datastream getDefaultWebData() {
		return defaultWebData;
	}

	/**
	 * Get a pre-ingest event logger for this SIP
	 *
	 * @return the pre-ingest event logger
	 */
	public PreIngestEventLogger getPreIngestEventLogger() {
		return this.preIngestEvents;
	}

	/**
	 * Set which datastream holds the default web representation, if any. The specified datastream object must also be in
	 * the list of datastreams for the SIP.
	 *
	 * @param defaultWebData
	 */
	public void setDefaultWebData(Datastream defaultWebData) {
		this.defaultWebData = defaultWebData;
	}

	/**
	 * Represents a datastream within a SIP
	 *
	 * @author count0
	 *
	 */
	public class Datastream {
		public Datastream(File file, String label, String md5checksum, String mimetype) {
			this.file = file;
			this.label = label;
			this.md5checksum = md5checksum;
			this.mimeType = mimetype;
		}

		public File getFile() {
			return file;
		}

		public void setFile(File file) {
			this.file = file;
		}

		private File file = null;
		private String label = null;
		private String md5checksum = null;
		private String mimeType = null;

		/**
		 * Set the label for the data file, usually the original file name.
		 *
		 * @param label
		 */
		public void setLabel(String label) {
			this.label = label;
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
		 * Get the label for this datastream.
		 *
		 * @return
		 */
		public String getLabel() {
			return label;
		}

		/**
		 * Get the user-specified MD5 checksum expected for this datastream.
		 *
		 * @return
		 */
		public String getMd5checksum() {
			return md5checksum;
		}

		/**
		 * Get the IANA MIME-Type for this data.
		 *
		 * @return
		 */
		public String getMimeType() {
			return mimeType;
		}
	}

	private File modsXML = null;
	private Agent owner = null;

	/**
	 * Get the repository path for the container for this SIP.
	 *
	 * @return
	 */
	public PID getContainerPID() {
		return containerPID;
	}

	/**
	 * Get the list of datastreams in this SIP.
	 *
	 * @return a list of datastream objects
	 */
	public List<Datastream> getDatastreams() {
		return datastreams;
	}

	/**
	 * Get the descriptive metadata file (MODS XML) for this SIP.
	 *
	 * @return a MODS XML file
	 */
	public File getModsXML() {
		return modsXML;
	}

	/**
	 * Get the agent that owns this SIP.
	 *
	 * @return
	 */
	public Agent getOwner() {
		return this.owner;
	}

	/**
	 * Find out if this SIP should be indexed.
	 *
	 * @return true if indexing is allowed
	 */
	public boolean isAllowIndexing() {
		return allowIndexing;
	}

	/**
	 * Tells the repository whether or not to index this object. (Default is yes)
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
	 * Set the datastreams.
	 *
	 * @param data
	 */
	public void setDatastreams(List<Datastream> datastreams) {
		this.datastreams = datastreams;
	}

	/**
	 * Set the MODS metadata file.
	 *
	 * @param modsXML
	 */
	public void setModsXML(File modsXML) {
		this.modsXML = modsXML;
	}

	public void destroy() {
		if (this.discardFilesOnDestroy) {
			this.modsXML.delete();
			for (Datastream d : this.datastreams) {
				d.file.delete();
			}
		}
	}

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		this.destroy();
	}

	/**
	 * Set the owner of the submitted objects.
	 *
	 * @param owner
	 */
	public void setOwner(Agent owner) {
		this.owner = owner;
	}
}
