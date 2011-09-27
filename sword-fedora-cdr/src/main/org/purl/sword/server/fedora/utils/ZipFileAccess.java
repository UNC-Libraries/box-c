package org.purl.sword.server.fedora.utils;

/**
  * Copyright (c) 2007, Aberystwyth University
  *
  * All rights reserved.
  *
  * Redistribution and use in source and binary forms, with or without
  * modification, are permitted provided that the following conditions
  * are met:
  *
  *  - Redistributions of source code must retain the above
  *    copyright notice, this list of conditions and the
  *    following disclaimer.
  *
  *  - Redistributions in binary form must reproduce the above copyright
  *    notice, this list of conditions and the following disclaimer in
  *    the documentation and/or other materials provided with the
  *    distribution.
  *
  *  - Neither the name of the Centre for Advanced Software and
  *    Intelligent Systems (CASIS) nor the names of its
  *    contributors may be used to endorse or promote products derived
  *    from this software without specific prior written permission.
  *
  * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
  * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
  * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
  * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
  * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
  * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
  * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
  * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
  * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
  * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF
  * THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
  * SUCH DAMAGE.
  *
  * @author Glen Robson
  * @version 1.0
  * Date: 26th February 2009
  *
  * This is a utility class to similfy access to Zip files.
  *
  */

import java.util.List;
import java.util.ArrayList;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;
import java.util.Enumeration;
import java.util.Random;

import org.purl.sword.server.fedora.utils.FindMimeType;
import org.purl.sword.server.fedora.fedoraObjects.Datastream;
import org.purl.sword.server.fedora.fedoraObjects.LocalDatastream;

import org.purl.sword.base.SWORDException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import org.apache.commons.io.IOUtils;

import org.apache.log4j.Logger;

public class ZipFileAccess {
	private static final Logger LOG = Logger.getLogger(ZipFileAccess.class);
	protected String _tmpExtractDirName = "";

	/**
	 * Setup this object and tell it where it can extract the zip file to.
	 *
	 * @param String the location where the zip file can be extracted to.
	 */
	public ZipFileAccess(final String pTempDir) {
		this.setTmpDir(pTempDir);
	}

	/**
	 * Get tempLocation.
	 *
	 * @return tempLocation as String.
	 */
	public String getTmpExtractDirName() {
	    return _tmpExtractDirName;
	}
	
	/**
	 * Set tempLocation.
	 *
	 * @param tempLocation the value to set.
	 */
	public void setTmpDir(final String pTempDir) {
			Random tRand = new Random();
	     _tmpExtractDirName = pTempDir + "zip-extract-" + tRand.nextInt();
	}

	/** 
	 * This returns a list of all the files that were in the zip file as a list of datastreams.
	 *
	 * @param String the zip file
	 * @return List<Datastream> a list of datastreams
	 * @throws IOException if there was a problem extracting the Zip file or if accessing the files.
	 */
	public List<Datastream> getFiles(final String pFile) throws IOException {
		List<Datastream> tDatastreams = new ArrayList<Datastream>();
		
		new File(this.getTmpExtractDirName()).mkdir();
		ZipFile tZipFile = new ZipFile(pFile);

		Enumeration tEntries = tZipFile.entries();

		ZipEntry tEntry = null;
		File tFile = null;
		String tFileLocation = "";
		LocalDatastream tLocalDs = null;
		while(tEntries.hasMoreElements()) {
			tEntry = (ZipEntry)tEntries.nextElement();

			if(tEntry.isDirectory()) {
				//new File(ZIP_LOCATION + tEntry.getName()).mkdir();
				continue;
			}

			tFileLocation = this.getTmpExtractDirName() + System.getProperty("file.separator") + tEntry.getName();
			tFile = new File(tFileLocation);
			LOG.debug("Saving " + tEntry.getName() + " to " + tFile.getPath());
			tFile.getParentFile().mkdirs();
			IOUtils.copy(tZipFile.getInputStream(tEntry), new FileOutputStream(tFile));

			tLocalDs = new LocalDatastream(tFile.getName().split("\\.")[0], FindMimeType.getMimeType(tFile.getName().split("\\.")[1]), tFileLocation);
			tLocalDs.setLabel(tEntry.getName());
			
			tDatastreams.add(tLocalDs);
		}

		return tDatastreams;
	}	

	/**
	 * After ingest this removes all the directories that were created during the ingest. It will not 
	 * remove files that are still in the temp location so ensure you have removed any files that are under this directory
	 * otherwise a SWORDException will be thrown.
	 *
	 * @throws SWORDException if a file is present in the extract of the zip file after ingest has taken place
	 */ 
	public void removeLocalFiles() throws SWORDException {
		this.recursiveDelete(new File(this.getTmpExtractDirName()));
	}

	/**
	 * Recursive method to remove the directories created during the zip extract
	 *
	 * @throws SWORDException if a file is present in the extract of the zip file after ingest has taken place
	 */ 
	protected void recursiveDelete(final File pDir) throws SWORDException {
		File[] tFiles = pDir.listFiles();
		if (tFiles.length == 0) {
			// Empty dir so safe to delete
			pDir.delete();
			return;
		}
		for (int i = 0; i < tFiles.length; i++) {
			if (tFiles[i].isDirectory()) {
				if (tFiles[i].listFiles().length == 0) {
					// Empty dir so safe to delete
					tFiles[i].delete();
				} else {
					this.recursiveDelete(tFiles[i]);
				}
			} else {
				// Delete files rather than throw exception
				tFiles[i].delete();
		//		throw new SWORDException("Trying to delete directorys for ZipFileHandler but I came accross a file so i'm panicking and exiting" + pDir.getAbsolutePath());
			}
		}
		pDir.delete();
	}
}
