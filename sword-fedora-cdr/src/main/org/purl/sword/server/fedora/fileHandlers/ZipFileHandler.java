package org.purl.sword.server.fedora.fileHandlers;

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
  * This class handles Zip file deposits.
  *
  */

import org.purl.sword.server.fedora.baseExtensions.DepositCollection;
import org.purl.sword.server.fedora.fedoraObjects.Datastream;
import org.purl.sword.server.fedora.fedoraObjects.LocalDatastream;
import org.purl.sword.server.fedora.utils.ZipFileAccess;

import org.purl.sword.base.SWORDException;
import org.purl.sword.base.ServiceDocument;
import org.purl.sword.base.SWORDEntry;

import java.util.List;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import java.io.InputStream;
import java.io.IOException;
import java.io.File;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;

import org.apache.commons.io.IOUtils;

public class ZipFileHandler extends DefaultFileHandler implements FileHandler {
	private static final Logger LOG = Logger.getLogger(ZipFileHandler.class);

	protected ZipFileAccess _zipFile = null;

	public ZipFileHandler() {
		super("application/zip", "");
	}
	
	/**
	 * This file handler accepts files with a mime type application/zip and a packaging which is null or empty
	 *
	 * @param String the mime type
	 * @param String packaging
	 * @return boolean if this handler can handle the current deposit
	 */
	public boolean isHandled(final String pMimeType, final String pPackaging) {
		return pMimeType.equals("application/zip") && (pPackaging == null || pPackaging.trim().length() == 0);
	}

	/**
	 *	To ensure the temp directories are deleted after ingest this method is overridden to remove
	 * the temp dirs but it calls the super.ingestDepost first
	 * 
	 * @param DepositCollection the deposit and its associated collection
	 * @param ServiceDocument the service document which this request applies to
	 * @throws SWORDException if any problems occured during ingest
	 */
	public SWORDEntry ingestDepost(final DepositCollection pDeposit, final ServiceDocument pServiceDocument) throws SWORDException {
		_zipFile = new ZipFileAccess(super.getTempDir());
		SWORDEntry tEntry = super.ingestDepost(pDeposit, pServiceDocument);
		LOG.debug("Cleaning up local zip files in " + super.getTempDir() + "zip-extract");
		// ensure the directories are deleted
		_zipFile.removeLocalFiles();

		return tEntry;
	}

	/** 
	 * This returns a list of datastreams that were contained in the Zip file
	 *
	 * @param DepositCollection the deposit
	 * @return List<Datastream> a list of the datastreams
	 * @throws IOException if there was a problem extracting the archive or accessing the files
	 * @throws SWORDException if there were any other problems
	 */
	protected List<Datastream> getDatastreams(final DepositCollection pDeposit) throws IOException, SWORDException {
		List<Datastream> tDatastreams = new ArrayList<Datastream>();
		LOG.debug("copying file");
		
		String tZipTempFileName = super.getTempDir() + "uploaded-file.tmp";
		IOUtils.copy(pDeposit.getFile(), new FileOutputStream(tZipTempFileName));
		// Add the original zip file
		Datastream tDatastream = new LocalDatastream(super.getGenericFileName(pDeposit), this.getContentType(), tZipTempFileName);
		tDatastreams.add(tDatastream);
	
		tDatastreams.addAll(_zipFile.getFiles(tZipTempFileName));

		return tDatastreams;
	}	
}
