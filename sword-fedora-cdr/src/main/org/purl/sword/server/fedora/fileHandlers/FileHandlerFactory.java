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
  * Date: 18 October 2007 
  *
  * This class reads in the file handlers from the config file and decides which 
  * one can handle the request. The first one it comes across that says it can
  * handle the requested is returned. If none are matched a DefaultFileHandler is 
  * used.
  */
import org.purl.sword.server.fedora.utils.XMLProperties;
import org.purl.sword.base.SWORDException;

import org.apache.log4j.Logger;

import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

public class FileHandlerFactory {
	private static final Logger LOG = Logger.getLogger(FileHandlerFactory.class);
	/**
	 * Find the file handler which can handle the mime type and packaging
	 *
	 * @param String the mime type of the deposit
	 * @param String the packaging
	 * @return FileHandler the file handler which can handle the deposit
	 * @throws SWORDException if there was a problem reading the config file or a file handler couldn't be created
	 */
	public static FileHandler getFileHandler(final String pContentType, final String pPackaging) throws SWORDException {
		LOG.debug("Looking for " + pContentType + " and packaging " + pPackaging);

		Iterator<FileHandler> tHandlerIter = FileHandlerFactory.getFileHandlers().iterator();
		FileHandler tHandler = null;
		while (tHandlerIter.hasNext()) {
			tHandler = tHandlerIter.next();

			if (tHandler.isHandled(pContentType, pPackaging)) {
				LOG.debug("Found handler " + tHandler.getClass().getName());
				return tHandler;
			}
		}

		// Nothing found so return default handler
		LOG.debug("Couldn't find a file handler so using default");
		return new DefaultFileHandler(pContentType, pPackaging);
	}

	private static List<FileHandler> getFileHandlers() throws SWORDException {
		List<FileHandler> tHandlers = new ArrayList<FileHandler>();

		XMLProperties tProps = new XMLProperties();
		Iterator<String> tHandlerClassIter = tProps.getFileHandlerClasses().iterator();
		String tClassName = "";
		while (tHandlerClassIter.hasNext()) {
			tClassName = tHandlerClassIter.next();
			
			LOG.debug("Loading " + tClassName + " as a file handler");
			
			try {
				tHandlers.add((FileHandler)Class.forName(tClassName).newInstance());
			} catch (ClassNotFoundException tClassExcpt) {
				String tMessage = "Couldn't find class " + tClassName + " in CLASSPATH";
				LOG.error(tMessage);
				throw new SWORDException(tMessage, tClassExcpt);
			} catch (InstantiationException tInstExcpt) {	
				String tMessage = "Couldn't instanciate " + tClassName + " ensure it has a default constructor and implements FileHandler interface";
				LOG.error(tMessage);
				throw new SWORDException(tMessage, tInstExcpt);
			} catch (IllegalAccessException tIllegalAccess) {
				String tMessage = "Couldn't instanciate " + tClassName + " ensure it has a default constructor and implements FileHandler interface";
				LOG.error(tMessage);
				throw new SWORDException(tMessage, tIllegalAccess);
			}
		}

		return tHandlers;
	}
}
