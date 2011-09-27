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
  * Date: 18 October 2007 
  *
  * This is a simple class to find the mime type from the extension
  * of a file.
  */

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.JDOMException;
import org.jdom.xpath.XPath;

import org.apache.log4j.Logger;

import java.io.FileInputStream;
import java.io.IOException;

public class FindMimeType {
	private static final Logger LOG = Logger.getLogger(FindMimeType.class);

	protected static Document _props = null;
	protected static Document _mimeTypes = null;

	/**
	 * Get the mime type from the file extension
	 * @param String the file extension
	 * @return String the mime type
	 */ 
	public static String getMimeType(final String pExtension) {
		LOG.debug("Loading " + StartupServlet.getPropertiesLocation());
		if (_mimeTypes == null) {
			try {
				SAXBuilder tBuilder = new SAXBuilder();
				Document tProps = tBuilder.build(new FileInputStream(StartupServlet.getPropertiesLocation()));
				LOG.debug("Building props");
				String tMimeTypesFilePath = tProps.getRootElement().getChild("files").getChild("mime-type").getText();
				LOG.debug("Loading props file from " + tMimeTypesFilePath);
				_mimeTypes = tBuilder.build(new FileInputStream(StartupServlet.getRealPath(tMimeTypesFilePath)));
				LOG.debug("Built mime-types xml");
			} catch (IOException tIOExcpt) {
				LOG.error("Couldn't open properties file " + tIOExcpt.toString());
			} catch (JDOMException tJDOMExcpt) {
				LOG.error("Properties file is invalid XML " + tJDOMExcpt.toString());
			}
		}	


		try {
			LOG.debug("Running XPath //type[./extension = '" + pExtension.toLowerCase() + "']/mime-type");
			
			Element tMimeTypeNode = (Element)XPath.selectSingleNode(_mimeTypes, "//type[./extension = '" + pExtension.toLowerCase() + "']/mime-type");
			if (tMimeTypeNode == null) {
				return "application/octet-stream";
			} else {	
				LOG.debug("sending mime-type " + tMimeTypeNode.getText() + " for extension " + pExtension);
				return tMimeTypeNode.getText();
			}	
		} catch (JDOMException tJDOMExcpt) {
			LOG.error("Couldn't find extension " + tJDOMExcpt.toString());
			return "application/octet-stream";
		}
	}
}
