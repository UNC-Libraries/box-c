package org.purl.sword.server.fedora.baseExtensions;

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
  * This extends the base file and allows it to be created from a XML config file and also
  * allows quering to find if a depositor has permission to deposit in a particular collection
  */

import org.purl.sword.base.ServiceDocument;
import org.purl.sword.base.SWORDException;
import org.purl.sword.base.Service;
import org.purl.sword.base.Workspace;
import org.purl.sword.base.Collection;

import org.purl.sword.server.fedora.FedoraServer;

import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.xpath.XPath;

import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

import org.apache.log4j.Logger;

public class XMLServiceDocument extends ServiceDocument implements ServiceDocumentQueries {
	private static final Logger LOG = Logger.getLogger(XMLServiceDocument.class);
	protected Element _serviceDocEl = null;

@SuppressWarnings(value={"unchecked"})
	public XMLServiceDocument(final Element pServiceDocEl, final String pUsername) {
		super();

		_serviceDocEl = pServiceDocEl;

		Service tService = new Service(FedoraServer.VERSION);

		tService.setNoOp(this.convertToBoolean(pServiceDocEl.getChild("noOp").getText()));
		tService.setVerbose(this.convertToBoolean(pServiceDocEl.getChild("verbose").getText()));

		Workspace tWorkspace = new Workspace();
		tWorkspace.setTitle(pServiceDocEl.getChild("workspace").getAttributeValue("title"));

		List<Element> tXMLCollections = pServiceDocEl.getChild("workspace").getChildren();

		for (Element tCollectionXML : tXMLCollections) {
			try {
				LOG.debug("Looking for collecitons for user " + pUsername + " with XPath=" + "./users/user[./text() = '" + pUsername + "']");
				if (tCollectionXML.getChild("users").getChildren().isEmpty() || XPath.selectSingleNode(tCollectionXML, "./users/user[./text() = '" + pUsername + "']") != null) {
					// Open collection or user is present so add
					tWorkspace.addCollection(new XMLCollection(tCollectionXML));
				}	
			} catch (JDOMException tExcpt) {
				LOG.error("Problems reading props file " + tExcpt);
			}
		}
		tService.addWorkspace(tWorkspace);

		super.setService(tService);
	}

	/**
	 * Return a collection from the supplied PID, Assumes there is only one workspace
	 * 
	 * returns null if there is no collection with the supplied pid 
	 *
	 * @param String the pid of the collection 
	 * 
	 * @return The ServiceDocument collection details
	 * 
	 */
	public Collection getCollection(final String pCollectionPid) {
		Iterator<Collection> tCollectionIter = super.getService().getWorkspacesList().get(0).collectionIterator();
		XMLCollection tCurCollection = null;
		while (tCollectionIter.hasNext()) {	
			tCurCollection = (XMLCollection)tCollectionIter.next();
			if (tCurCollection.getCollectionPid().equals(pCollectionPid)) {
				return tCurCollection;
			}
		}

		return null;
	}

	private boolean convertToBoolean(final String pBoolean) {
		if (pBoolean.toLowerCase().equals("true")) {
			return true;
		} else if (pBoolean.toLowerCase().equals("false")) {
			return false;
		} else {
			throw new IllegalArgumentException("Value must be true or false you supplied '" + pBoolean + "'");
		}
	}

	/**
	 * Return a collection from the supplied PID, Works of multiple collections and returns the collection
	 * as an XML element for futher processing
	 * 
	 * @throws JDOMException if there is no collection with supplied pid or if the config file isn't formated correctly
	 *
	 * @param String the pid of the collection 
	 * 
	 * @return Element the collection
	 * 
	 */
	public Element getCollectionElement(final String pCollectionPID) throws SWORDException {
		Element tCollectionEl = null;
		try {
			tCollectionEl = (Element)XPath.selectSingleNode(_serviceDocEl, "./workspace/collection[@collection_pid='" + pCollectionPID + "']");
			if (tCollectionEl == null) {
				throw new JDOMException("Couldn't find collection element with pid " + pCollectionPID);
			}
		} catch (JDOMException tJDOMExcpt) {
			String tMessage = "JDOM Exception occured in getCollectionElement method due to a problem accessing the properties file";
			LOG.error(tMessage);
			LOG.error(tJDOMExcpt.toString());
			throw new SWORDException(tMessage, tJDOMExcpt);
		}

		return tCollectionEl;
	}

	/**
	 * Checks if a depositor can submit to a collection
	 * 
	 * @throws SWORDException if there is no collection with supplied pid or if the config file isn't formated correctly
	 *
	 * @param String the username/on behalf of value of the depositor
	 * @param String the pid of the collection 
	 * 
	 * @return boolean if allowed to deposit
	 * 
	 */
	public boolean isAllowedToDeposit(final String pDepositer, final String pCollectionPID) throws SWORDException {
		// Check to see if user is allowed to deposit in collection
		try {
			Element tUsersEl = (Element)XPath.selectSingleNode(_serviceDocEl, "./workspace/collection[@collection_pid='" + pCollectionPID + "']/users");
			if (tUsersEl == null) {
				throw new SWORDException("There is a problem with the servie document as the users element for collection " + pCollectionPID + " could not be found");
			}
			Element tUser = (Element)XPath.selectSingleNode(tUsersEl, "./user[. = '" + pDepositer + "']");
			if (!tUsersEl.getChildren().isEmpty() && tUser == null) {
				return false;
			} else {
				return true;
			}	
		} catch (JDOMException tJDOMExcpt) {
			String tErrMessage = "Couldn't load props file to find out if a user has permission to deposit";
			LOG.error(tErrMessage + tJDOMExcpt.toString());
			throw new SWORDException(tErrMessage, tJDOMExcpt); 
		}
	}

	/**
	 * Checks if the file type is allowed in a particular collection
	 * 
	 * @throws SWORDException if there is no collection with supplied pid or if the config file isn't formated correctly
	 *
	 * @param String the mime type 
	 * @param String the pid of the collection 
	 * 
	 * @return boolean if allowed to deposit
	 * 
	 */
	public boolean isContentTypeAllowed(final String pContentType, final String pCollectionPID) throws SWORDException {
		// Check to see if content type is in the allowed list
		try {
			Element tAcceptEl = (Element)XPath.selectSingleNode(_serviceDocEl, "./workspace/collection[@collection_pid='" + pCollectionPID + "']/accepts/accept[. = '" + pContentType + "']");
			if (tAcceptEl == null) {
				return false;
			} else {
				return true;
			}
		} catch (JDOMException tJDOMExcpt) {
			String tErrMessage = "Problem reading properties XML tried to find content type " + pContentType + " for collection " + pCollectionPID + ": ";
			LOG.error(tErrMessage + tJDOMExcpt.toString());
			throw new SWORDException(tErrMessage, tJDOMExcpt); 
		}
	}

	public boolean isPackageTypeAllowed(final String pPackageType, final String pCollectionPID) throws SWORDException {
		if (pPackageType == null || pPackageType.trim().length() == 0) {
			return true; // haven't specified a package type
		}
		// Check to see if content type is in the allowed list
		try {
			Element tAcceptEl = (Element)XPath.selectSingleNode(_serviceDocEl, "./workspace/collection[@collection_pid='" + pCollectionPID + "']/packaging/package[. = '" + pPackageType + "']");
			if (tAcceptEl == null) {
				return false;
			} else {
				if (tAcceptEl.getAttributeValue("q") != null) {
						float tQ = Float.parseFloat(tAcceptEl.getAttributeValue("q"));
						if (tQ > 0.0) {
							return true;
						} else {
							return false;
						}
				} else {	
					return true;
				}
			}
		} catch (JDOMException tJDOMExcpt) {
			String tErrMessage = "Problem reading properties XML tried to find content type " + pPackageType + " for collection " + pCollectionPID + ": ";
			LOG.error(tErrMessage + tJDOMExcpt.toString());
			throw new SWORDException(tErrMessage, tJDOMExcpt); 
		}
	}
}
