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

import edu.unc.lib.dl.fedora.PID;

/**
 * This submission package is composed of a single data file, plus MODS metadata. These submissions are normally
 * converted into a single Fedora object.
 * 
 * @author count0
 * 
 */
public class SingleFileSIP extends FileSIP {
	private File modsXML = null;

	public SingleFileSIP() {
		super();
	}
	
	public SingleFileSIP(PID containerPID, File data, String mimeType, String fileLabel, String md5checksum) {
		super(containerPID, data, mimeType, fileLabel, md5checksum);
	}
	
	public File getModsXML() {
		return modsXML;
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
