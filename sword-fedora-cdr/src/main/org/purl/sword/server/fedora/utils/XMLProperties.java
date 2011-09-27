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
  * This class controls access to the properties file. If you need to add properties then you should
  * add the access methods to this class rathern than getting the XML Document its self. This ensures 
  * all property access is in one place and can be changed to use another technology.
  *
  */

import org.purl.sword.base.SWORDException;
import org.purl.sword.base.ServiceDocument;
import org.purl.sword.base.Workspace;
import org.purl.sword.base.Collection;

import org.purl.sword.server.fedora.baseExtensions.XMLServiceDocument;

import org.jdom.input.SAXBuilder;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.Element;
import org.jdom.xpath.XPath;

import org.apache.log4j.Logger;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import java.io.FileInputStream;
import java.io.File;
import java.io.IOException;

public class XMLProperties {
	private static final Logger LOG = Logger.getLogger(XMLProperties.class);
	protected Document _props = null;

	/**
	 * This builds the properties object and calls StartupServlet to get the Properties location
	 */
	public XMLProperties() {
		SAXBuilder tBuilder = new SAXBuilder();
		LOG.debug("Loading " + StartupServlet.getPropertiesLocation());
		try {
			_props = tBuilder.build(new FileInputStream(StartupServlet.getPropertiesLocation()));
		} catch (IOException tIOExcpt) {
			LOG.error("Couldn't open properties file " + tIOExcpt.toString());
		} catch (JDOMException tJDOMExcpt) {
			LOG.error("Properties file is invalid XML " + tJDOMExcpt.toString());
		}
	}

	/**
	 * This returns a list of file handlers from the config file. 
	 *
	 * @return List<String> list of class names (including package) for the file handler
	 * @throws SWORDException if there was a problem reading the config file
	 */
@SuppressWarnings(value={"unchecked"})
	public List<String> getFileHandlerClasses() throws SWORDException {
		List<String> tClassesList = new ArrayList<String>();
		
		try {
			for (Element tFileHandlerEl : (List<Element>)XPath.selectNodes(_props, "/properties/file_handlers/*")) {
				if (tFileHandlerEl.getAttributeValue("class") == null || tFileHandlerEl.getAttributeValue("class").trim().length() == 0) {
					throw new JDOMException("You must specify a class attribute for the node handler");
				}
				tClassesList.add(tFileHandlerEl.getAttributeValue("class"));
			}
		} catch (JDOMException tJDOMExcpt) {
			String tMessage = "Couldn't load file handlers from properties file";
			LOG.error(tMessage);
			LOG.error(tJDOMExcpt.toString());
			throw new SWORDException(tMessage, tJDOMExcpt);
		}

		return tClassesList;
	}

	/**
	 * This returns a the temp directory where deposits can be stored before ingest
	 *
	 * @return String the temp location. This should be an absolute path
	 * @throws SWORDException if there was a problem reading the config file
	 */
	public String getTempDir() throws SWORDException {
		Element tTempDir = null;
		try {
			tTempDir  = (Element)XPath.selectSingleNode(_props, "/properties/general/temp_dir");
			if (tTempDir == null) {
				throw new JDOMException("Couldn't find node temp_dir");
			}
		} catch (JDOMException tJDOMExcpt) {
			String tMessage = "JDOM Exception occured in getTempDir method due to a problem accessing the properties file";
			LOG.error(tMessage);
			LOG.error(tJDOMExcpt.toString());
			throw new SWORDException(tMessage, tJDOMExcpt);
		}

		return tTempDir.getText();
	}

	/**
	 * This returns the unique URI for a repository
	 *
	 * @return String the repository URI. 
	 * @throws SWORDException if there was a problem reading the config file
	 */
	public String getRepositoryUri() throws SWORDException {
		Element tRepositoryUri = null;
		try {
			tRepositoryUri = (Element)XPath.selectSingleNode(_props, "/properties/general/repository_uri");
			if (tRepositoryUri == null) {
				throw new JDOMException("Couldn't find node repository_uri");
			}
		} catch (JDOMException tJDOMExcpt) {
			String tMessage = "JDOM Exception occured in getRepositoryUri method due to a problem accessing the properties file";
			LOG.error(tMessage);
			LOG.error(tJDOMExcpt.toString());
			throw new SWORDException(tMessage, tJDOMExcpt);
		}

		return tRepositoryUri.getText();
	}

	/**
	 * This returns the URL to the object metadata page for a specific PID
	 *
	 * @param String the object's pid
	 * @return String the URL to the object's metadata page
	 * @throws SWORDException if there was a problem reading the config file
	 */
	public String getExternalURL(final String pPID) throws SWORDException {
		Element tExternalURL = null;
		try {
			tExternalURL = (Element)XPath.selectSingleNode(_props, "/properties/fedora/external_obj_url");
			if (tExternalURL == null) {
				throw new JDOMException("Couldn't find fedora external url");
			}
		} catch (JDOMException tJDOMExcpt) {
			String tMessage = "JDOM Exception occured in getExternalURL method due to a problem accessing the properties file";
			LOG.error(tMessage);
			LOG.error(tJDOMExcpt.toString());
			throw new SWORDException(tMessage, tJDOMExcpt);
		}

		return tExternalURL.getText().replaceAll("##PID##", pPID);
	}

	/**
	 * This returns the URL for the file that was ingested 
	 *
	 * @param String the object's pid
	 * @param String the datastream name
	 * @return String the URL to the file that was ingested
	 * @throws SWORDException if there was a problem reading the config file
	 */
	public String getExternalDSURL(final String pPID, final String pDSId) throws SWORDException {
		Element tExternalURL = null;
		try {
			tExternalURL = (Element)XPath.selectSingleNode(_props, "/properties/fedora/external_ds_url");
			if (tExternalURL == null) {
				throw new JDOMException("Couldn't find fedora external url");
			}
		} catch (JDOMException tJDOMExcpt) {
			String tMessage = "JDOM Exception occured in getExternalURL method due to a problem accessing the properties file";
			LOG.error(tMessage);
			LOG.error(tJDOMExcpt.toString());
			throw new SWORDException(tMessage, tJDOMExcpt);
		}

		return tExternalURL.getText().replaceAll("##PID##", pPID).replaceAll("##DS##", pDSId);
	}

	/**
	 * This returns the PID namespace deposits should be added to
	 *
	 * @return String the PID namespace deposits should belong to
	 * @throws SWORDException if there was a problem reading the config file
	 */
	public String getPIDNamespace() throws SWORDException {
		Element tPidNamespace = null;
		try {
			tPidNamespace  = (Element)XPath.selectSingleNode(_props, "/properties/fedora/pid_namespace");
			if (tPidNamespace == null) {
				throw new JDOMException("Couldn't find node pid_namespace");
			}
		} catch (JDOMException tJDOMExcpt) {
			String tMessage = "JDOM Exception occured in getPIDNamespace method due to a problem accessing the properties file";
			LOG.error(tMessage);
			LOG.error(tJDOMExcpt.toString());
			throw new SWORDException(tMessage, tJDOMExcpt);
		}

		return tPidNamespace.getText();
	}

	/** 
	 * This returns the Fedora URL used when accessing the web services. It is in the form:
	 * http://host:port/fedora.
	 *
	 * @return String the Fedora access URL.
	 * @throws SWORDException if there was a problem reading the config file
	 */ 
	public String getFedoraURL() throws SWORDException {
		Element tFedoraProps = _props.getRootElement().getChild("fedora");
		if (tFedoraProps == null) {
			LOG.error("Couldn't find fedora properties in properties files");
			throw new SWORDException("Invlaid properties file");
		}

		String tProtocol = "";
		if (tFedoraProps.getChild("protocol") == null) {
			LOG.error("Couldn't find protocol under fedora properties in properties files");
			throw new SWORDException("Invlaid properties file");
		} else {
			tProtocol = tFedoraProps.getChild("protocol").getText();
		}

		String tHost = "";
		if (tFedoraProps.getChild("host") == null) {
			LOG.error("Couldn't find host under fedora properties in properties files");
			throw new SWORDException("Invlaid properties file");
		} else {
			tHost = tFedoraProps.getChild("host").getText();
		}

		String tPort = null;
		if (tFedoraProps.getChild("port") == null) {
			LOG.error("Couldn't find port under fedora propertiepOnBehalfOfs in properties files");
			throw new SWORDException("Invlaid properties file");
		} else {
			tPort = tFedoraProps.getChild("port").getText();
		}

		return tProtocol + "://" + tHost + ":" + tPort + "/fedora";
	}	

	/**
	 * This returns the directory where the entry documents should be stored
	 *
	 * @return String the sub service document location. This should be an relative to the web app path
	 * @throws SWORDException if there was a problem reading the config file
	 */
	public String getEntryStoreLocation() throws SWORDException {
		Element tEntryLoc = null;
		try {
			tEntryLoc  = (Element)XPath.selectSingleNode(_props, "/properties/general/entry-location");
			if (tEntryLoc == null) {
				throw new JDOMException("Couldn't find node entry-location");
			}
		} catch (JDOMException tJDOMExcpt) {
			String tMessage = "JDOM Exception occured in getEntryLoc method due to a problem accessing the properties file";
			LOG.error(tMessage);
			LOG.error(tJDOMExcpt.toString());
			throw new SWORDException(tMessage, tJDOMExcpt);
		}

		return StartupServlet.getRealPath(tEntryLoc.getText());
	}
	/**
	 * This returns the directory where the sub service documents are stored relative to the web app directory
	 *
	 * @return String the sub service document location. This should be an relative to the web app path
	 * @throws SWORDException if there was a problem reading the config file
	 */
	public String getSubSDDir() throws SWORDException {
		Element tSubSDDir = null;
		try {
			tSubSDDir  = (Element)XPath.selectSingleNode(_props, "/properties/general/sub-service-documents");
			if (tSubSDDir == null) {
				throw new JDOMException("Couldn't find node sub-service-documents");
			}
		} catch (JDOMException tJDOMExcpt) {
			String tMessage = "JDOM Exception occured in getSubSDDir method due to a problem accessing the properties file";
			LOG.error(tMessage);
			LOG.error(tJDOMExcpt.toString());
			throw new SWORDException(tMessage, tJDOMExcpt);
		}

		return tSubSDDir.getText();
	}


	/**
	 * This is the method which retrieves the Service document from the properties file. 
	 * 
    * @param String the user that is requesting the ServiceDocument
	 * @return ServiceDocument the service document
	 * @throws SWORDException if there was a problem reading the config file
	 */
	public ServiceDocument getServiceDocument(final String pOnBehalfOf) throws SWORDException {
		Element tServiceDocumentElement = null;

		try {
			tServiceDocumentElement = (Element)XPath.selectSingleNode(_props, "/properties/service_document");
		} catch (JDOMException tJDOMExcpt) {
			String tMessage = "JDOM Exception occured on doServiceDocument method due to a problem accessing the properties file";
			LOG.error(tMessage);
			LOG.error(tJDOMExcpt.toString());
			throw new SWORDException(tMessage, tJDOMExcpt);
		}

		XMLServiceDocument tServiceDoc = new XMLServiceDocument(tServiceDocumentElement, pOnBehalfOf);
		this.addLocationToService(tServiceDoc);
		return tServiceDoc;
	}

	
	public ServiceDocument getServiceDocument(final String pOnBehalfOf, final String pLocation) throws SWORDException {
		SAXBuilder tBuilder = new SAXBuilder();
		Document tChildDocs = null; 
		try {
			tChildDocs = tBuilder.build(StartupServlet.getRealPath(new File(this.getSubSDDir(), pLocation).getPath()));
		} catch (IOException tIOExcpt) {
			String tMessage = "IO Exception occured on doServiceDocument method due to a problem accessing the properties file";
			LOG.error(tMessage);
			LOG.error(tIOExcpt.toString());
			throw new SWORDException(tMessage, tIOExcpt);
		} catch (JDOMException tJDOMExcpt) {
			String tMessage = "JDOM Exception occured on doServiceDocument method due to a problem accessing the properties file";
			LOG.error(tMessage);
			LOG.error(tJDOMExcpt.toString());
			throw new SWORDException(tMessage, tJDOMExcpt);
		}

		XMLServiceDocument tServiceDoc = new XMLServiceDocument(tChildDocs.getRootElement(), pOnBehalfOf);
		this.addLocationToService(tServiceDoc);
		return tServiceDoc;
	}

	private void addLocationToService(final XMLServiceDocument pServiceDoc) throws SWORDException {
		for (Workspace tWorkspaces: pServiceDoc.getService().getWorkspacesList()) {
			 for (Collection tCollection: tWorkspaces.getCollections()) {
				 	if (tCollection.getService() != null && tCollection.getService().trim().length() != 0) {
						tCollection.setService(this.getRepositoryUri() + "/servicedocument/" + tCollection.getService());
					}
			 }
		}
	}
	
	/**
	 * ** Use only when you don't have access to the source for XMLProperties **
	 *
	 * @return props as Document.
	 */
	public Document getProps() {
	    return _props;
	}
	
	/**
	 * Set props.
	 * ** Use only when you don't have access to the source for XMLProperties **
	 * @param props the value to set.
	 */
	public void setProps(final Document pProps) {
	     _props = pProps;
	}
}
