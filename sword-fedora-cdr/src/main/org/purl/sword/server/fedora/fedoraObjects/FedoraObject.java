package org.purl.sword.server.fedora.fedoraObjects;

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
  * This object encapsulates an object in Fedora and has methods to ingest its
  * self
  *
  */

import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

import org.purl.sword.server.fedora.utils.XMLProperties;

import org.purl.sword.base.SWORDException;

import org.jdom.Element;
import org.jdom.Document;
import org.jdom.Namespace; 
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;

import java.net.MalformedURLException;

import java.io.IOException;
import java.io.ByteArrayOutputStream;

import java.rmi.RemoteException;

import javax.xml.rpc.ServiceException;

import org.apache.axis.client.Stub;
import org.apache.axis.types.NonNegativeInteger;

import org.purl.sword.server.fedora.api.FedoraAPIMServiceLocator;
import org.purl.sword.server.fedora.api.FedoraAPIMService;
import org.purl.sword.server.fedora.api.FedoraAPIM;
import org.purl.sword.server.fedora.api.FedoraAPIAServiceLocator;
import org.purl.sword.server.fedora.api.FedoraAPIAService;
import org.purl.sword.server.fedora.api.FedoraAPIA;
import org.purl.sword.server.fedora.api.RepositoryInfo;

import org.apache.log4j.Logger;

public class FedoraObject {
	private static final Logger LOG = Logger.getLogger(FedoraObject.class);
	protected static Namespace FOXML = Namespace.getNamespace("foxml", "info:fedora/fedora-system:def/foxml#");
	protected String _pid = "";
	protected List<Property> _identifiers = null; 
	protected DublinCore _dc = null;
	protected Relationship _relationship = null;
	protected List<Datastream> _datastreams = null;
	protected List<Disseminator> _disseminators = null;

	protected FedoraAPIM _APIM = null;
	protected XMLProperties _props = null;
	protected String _username = null;
	protected String _password = null;
	protected String _fedoraVersion = null;

	/**
	 * Contacts the Fedora repository to retrieve the next avilable PID
	 * @param String Username to access fedora
	 * @param String Password to access fedora
	 * @throws SWORDException if conection refused to fedora repository
	 */
	public FedoraObject(final String pUsername, final String pPassword) throws SWORDException {
		_username = pUsername;
		_password = pPassword;

		_props = new XMLProperties();
	
		try {
			FedoraAPIMService tService = new FedoraAPIMServiceLocator();
			((FedoraAPIMServiceLocator)tService).setmanagementEndpointAddress(_props.getFedoraURL() + "/services/management");
			_APIM = tService.getmanagement();

			FedoraAPIAService tServiceAPIA = new FedoraAPIAServiceLocator();
			((FedoraAPIAServiceLocator)tServiceAPIA).setaccessEndpointAddress(_props.getFedoraURL() + "/services/access");
			FedoraAPIA tAPIA = tServiceAPIA.getaccess();
			((Stub)tAPIA).setUsername(pUsername);
			((Stub)tAPIA).setPassword(pPassword);
			RepositoryInfo tInfo = tAPIA.describeRepository();
			_fedoraVersion = tInfo.getRepositoryVersion().trim();
			LOG.debug("Storing fedora version " + _fedoraVersion);
		} catch (ServiceException tServiceExcpt) {
			LOG.error("Can't connect to Fedora");
			LOG.error(tServiceExcpt.toString());
		} catch (SWORDException tSwordExcpt) {
			LOG.error("Invalid fedora section of configuraiton file");
			LOG.error(tSwordExcpt.toString());
		} catch (RemoteException tRemoteExcpt) {
			LOG.error("Can't connect to Fedora to find the Version");
			LOG.error(tRemoteExcpt.toString());
			throw new SWORDException("Can't connect to Fedora to find the Version: " + tRemoteExcpt.toString());
		}

		// find next pid 
		LOG.debug("Finding next pid user=" + _username + " password=" + _password);
		try {
			((Stub)_APIM).setUsername(_username);
			((Stub)_APIM).setPassword(_password);
			String[] tPidArray = _APIM.getNextPID(new NonNegativeInteger("1"), _props.getPIDNamespace());
			
			this.setPid(tPidArray[0]);
		} catch (RemoteException tRemoteExcpt) {
			String tErrMessage = "Had problems retrieving the next pid from the repository; ";
			LOG.error(tErrMessage + tRemoteExcpt.toString());
			throw new SWORDException(tErrMessage, tRemoteExcpt);
		} catch (Exception tExcpt) {
			String tErrMessage = "Had problems retrieving the next pid from the repository; ";
			LOG.error(tErrMessage + tExcpt.toString());
			throw new SWORDException(tErrMessage, tExcpt);

		}
	}

	public String getPid() {
		return _pid;
	}

	public void setPid(final String pPID) {	
		_pid = pPID;
	}

	public List<Property> getIdentifiers() {
		return _identifiers;
	}

	public void setIdentifiers(final List<Property> pIdentifiers) {
		_identifiers = pIdentifiers;
	}

	public DublinCore getDC() {
		return _dc;
	}

	public void setDC(final DublinCore pDC) {
		_dc = pDC;
	}

	public Relationship getRelationships() {
		// Ensure relationship has the PID
		_relationship.setPid(this.getPid());
		if (!_fedoraVersion.startsWith("3")) {
			// remove all fedora:model attributes for non Fedora 3 objects or
			// else they don't ingest
			_relationship.removeModels();
		}
		return _relationship;
	}

	public void setRelationships(final Relationship pRelations) {
		_relationship = pRelations;
		// Ensure relationship has the PID
		_relationship.setPid(this.getPid());
	}

	public List<Datastream> getDatastreams() {
		return _datastreams;
	}

	public void setDatastreams(final List<Datastream> pDatastreams) {
		_datastreams = pDatastreams;
	}

	public List<Disseminator> getDisseminators() {
		return _disseminators;
	}

	public void setDisseminators(final List<Disseminator> pDisseminators) {
		_disseminators = pDisseminators;
	}

	/**
	 * Get fedoraVersion.
	 *
	 * @return fedoraVersion as String.
	 */
	public String getFedoraVersion() {
	    return _fedoraVersion;
	}
	
	/**
	 * Set fedoraVersion.
	 *
	 * @param fedoraVersion the value to set.
	 */
	public void setFedoraVersion(final String pFedoraVersion) {
	     _fedoraVersion = pFedoraVersion;
	}

	protected void uploadLocalDatastreams() throws SWORDException {
		try {
			Iterator<Datastream> tDatastreamsIter = this.getDatastreams().iterator();
			Datastream tDatastream = null;
			while (tDatastreamsIter.hasNext()) {
				tDatastream = tDatastreamsIter.next();

				if (tDatastream instanceof LocalDatastream) {
					((LocalDatastream)tDatastream).upload(_username, _password);
				}	
			}
		} catch (MalformedURLException tExcpt) {
			LOG.error("Can't access fedora for upload" + tExcpt.toString());
			throw new SWORDException("Can't access fedora for upload", tExcpt);
		} catch (IOException tIOExcpt) {
			LOG.error("Couldn't access uploaded file" + tIOExcpt.toString());
			throw new SWORDException("Couldn't access uploaded file: ", tIOExcpt);
		}
	}

	/**
	 * Ingest this object into fedora. It first calls FedoraObject.toFOXML
	 * to convert this object into FOXML and then ingests that using the Fedora
	 * APIM methods
	 * @throws SWORDException if ingest failed
	 */
	public void ingest() throws SWORDException {
		// Upload them
		this.uploadLocalDatastreams();
		
		LOG.debug("Ready to upload xml");
		ByteArrayOutputStream tByteArray = new ByteArrayOutputStream();
		XMLOutputter tOut = new XMLOutputter(Format.getPrettyFormat());
		
		// upload foxml
		Document tFOXML = this.toFOXML();
		try {
			tOut.output(tFOXML, tByteArray);
			tOut.output(tFOXML, new java.io.FileOutputStream("/tmp/test.xml"));

			((Stub)_APIM).setUsername(_username);
			((Stub)_APIM).setPassword(_password);
			String tXMLFormat = "";
			if (_fedoraVersion.startsWith("3")) { 
				tXMLFormat = "info:fedora/fedora-system:FOXML-1.1";
			} else {
				tXMLFormat = "foxml1.0";
			}

			_APIM.ingest(tByteArray.toByteArray(), tXMLFormat, "ingested by the sword program");
		} catch (RemoteException tRemoteExcpt) {
			try {
				tOut.output(tFOXML, System.out);
			} catch (IOException tIOExcpt) { }
			String tErrMessage = "Had problems adding the object to the repository; ";
			LOG.error(tErrMessage + tRemoteExcpt.toString());
			throw new SWORDException(tErrMessage, tRemoteExcpt);
		} catch (Exception tExcpt) {
			try {
				tOut.output(tFOXML, System.out);
			} catch (IOException tIOExcpt) { }
			String tErrMessage = "Had problems adding the object to the repository; ";
			LOG.error(tErrMessage + tExcpt.toString());
			throw new SWORDException(tErrMessage, tExcpt);

		}
	}

	/**
	 * Returns a externally accessible URL for a datastream
	 * @param String the datastream that the URL should point to
	 *
	 * @throws SWORDException if there was a problem reading the properties file where this value is stored
	 */ 
	public String getURLToDS(final String pDSName) throws SWORDException {
		return _props.getExternalDSURL(this.getPid(), pDSName);
	}

	/**
	 * Returns a externally accessible URL for an object
	 *
	 * @throws SWORDException if there was a problem reading the properties file where this value is stored
	 */ 
	public String getURLToObject() throws SWORDException {
		return _props.getExternalURL(this.getPid());
	}

	/**
	 * Converts this object into FOXML ready for ingestion
	 * @return Document the FOXML
	 */ 
	public Document toFOXML() {
		Document tFOXML = new Document();
		
		Element tDigitalObject = new Element("digitalObject", FOXML);
		tFOXML.setRootElement(tDigitalObject);
		tDigitalObject.setAttribute("PID", this.getPid());
		if (_fedoraVersion.startsWith("3")) {
			tDigitalObject.setAttribute("VERSION", "1.1");
		}

		tDigitalObject.addContent(this.getObjectPropsXML());
		tDigitalObject.addContent(this.addDSXML());
		if (!_fedoraVersion.startsWith("3")) {
			// Don't add disseminators to Fedora 3 as they are handled by content models
			tDigitalObject.addContent(this.addDisseminatorsXML());
		}

		return tFOXML;
	}

	protected Element getObjectPropsXML() {
		Element tObjectPropsEl = new Element("objectProperties", FOXML);

		for (Property tProp : this.getIdentifiers()) {
			if (!(_fedoraVersion.startsWith("3") && tProp.getName().equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"))) {
				LOG.debug("Adding " + tProp.getName());
				tObjectPropsEl.addContent(tProp.toFOXML(FOXML));
			}	
		}

		return tObjectPropsEl;
	}

	protected List<Element> addDSXML() {
		List<Element> tDatastreamsList = new ArrayList<Element>();
		
		tDatastreamsList.add(this.getDC().toFOXML(FOXML));
		tDatastreamsList.add(this.getRelationships().toFOXML(FOXML));
		
		for (Datastream tDSXML : this.getDatastreams()) {
			tDatastreamsList.add(tDSXML.toFOXML(FOXML));
		}
		
		return tDatastreamsList;
	}

	protected List<Element> addDisseminatorsXML() {
		List<Element> tDisseminatorsList = new ArrayList<Element>();
		
		for (Disseminator tDissXML : this.getDisseminators()) {
			tDisseminatorsList.add(tDissXML.toFOXML(FOXML));
		}
		
		return tDisseminatorsList;
	}
	

}
