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
  * This is a utility class to allow easy access to a METS document
  */

import org.jdom.Element;
import org.jdom.Document;
import org.jdom.input.SAXBuilder;
import org.jdom.JDOMException;
import org.jdom.xpath.XPath;
import org.jdom.Namespace;

import org.purl.sword.server.fedora.fedoraObjects.DublinCore;
import org.purl.sword.server.fedora.fedoraObjects.Relationship;
import org.purl.sword.server.fedora.fedoraObjects.Datastream;
import org.purl.sword.server.fedora.fedoraObjects.XMLInlineDatastream;
import org.purl.sword.server.fedora.fedoraObjects.ManagedDatastream;
import org.purl.sword.server.fedora.fedoraObjects.LocalDatastream;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

import org.apache.log4j.Logger;

public class METSObject {
	private static final Logger LOG = Logger.getLogger(METSObject.class);

	protected Document _METSDoc = null;

	/** The METS namespace */
	public static Namespace METS = Namespace.getNamespace("METS", "http://www.loc.gov/METS/");
	/** The XLINK namespace */
	public static Namespace XLINK = Namespace.getNamespace("xlink", "http://www.w3.org/1999/xlink");

	/** 
	 * Build this object from a XML METS document
	 * @param Document the XML METS document
	 */ 
	public METSObject(final Document pMETSDoc) {
		_METSDoc = pMETSDoc;
	}

	/**
	 * Return the Dublin Core datastream from the METS if present otherwise return null.
	 *
	 * @return DublinCore the dublin core from the METS
	 */
	public DublinCore getDublinCore() {
		Element tDCEl = null;

		try {
			XPath tXPath = XPath.newInstance("/METS:mets/METS:dmdSec/METS:mdWrap[translate(@MDTYPE, 'ABCDEFGHIJKLMNOPQURSTVWXYZ', 'abcdefghijklmnopurstvwxyz') = 'dc']/METS:xmlData/*");
			tXPath.addNamespace(METS);
		
			tDCEl = (Element)tXPath.selectSingleNode(_METSDoc);
		} catch (JDOMException tExcpt) {
			LOG.debug("Didin't find a DC metadata in METS: " + tExcpt);
		}

		if (tDCEl == null) {
			LOG.debug("Didn't find a DC metadata in METS");
			return null;
		} else {
			return new DublinCore(new Document((Element)tDCEl.clone()));
		}
	} 

	/**
	 * Return the Relationship datastream from the METS if present otherwise return null.
	 *
	 * @return Relationships the rdf relationship data from the METS
	 */
	public Relationship getRelationships() {
		Element tRdfEl = null;

		try {
			XPath tXPath = XPath.newInstance("/METS:mets/METS:dmdSec/METS:mdWrap[translate(@OTHERMDTYPE, 'ABCDEFGHIJKLMNOPQURSTVWXYZ', 'abcdefghijklmnopurstvwxyz') = 'rdf']/METS:xmlData/*");
			tXPath.addNamespace(METS);
		
			tRdfEl = (Element)tXPath.selectSingleNode(_METSDoc);
		} catch (JDOMException tExcpt) {
			LOG.debug("Didin't find a RELS-EXT metadata in METS: " + tExcpt);
		}	

		if (tRdfEl == null) {
			return null;
		} else {
			return new Relationship(new Document((Element)tRdfEl.clone()));
		}
	}
	
	/**
	 * Return all the datastreams for the METS document including the METS document its self. This method
	 * calls getMETSDs, getMetadataDatastreams and getFileDatastreams
	 *
	 * @return List<Datastream> a list of the datastreams
	 * @throws JDOMException if there was a problem processing the METS document
	 */
	public List<Datastream> getDatastreams() throws JDOMException {
			
		List <Datastream> tDatastreamList = new ArrayList<Datastream>();

		tDatastreamList.addAll(this.getMetadataDatastreams());
		tDatastreamList.addAll(this.getFileDatastreams());
		
		// Add METS file
		tDatastreamList.add(this.getMETSDs());

		return tDatastreamList;
		
	}

	/**
	 * Returns this METS document as a datastream
	 *
	 * @return Datastream this METS document as a datastream
	 */
	public Datastream getMETSDs() {
		// Add METS file
		Datastream tMETSDs = new XMLInlineDatastream("METS", _METSDoc);
		tMETSDs.setLabel("METS as it was deposited");

		return tMETSDs;
	}

	/** 
	 * Returns the Metadata as list of datastream objects
	 *
	 * @return List<Datastream> a list of the metadata datastreams
	 * @throws JDOMException  if there was a problem processing the METS document
	 */
@SuppressWarnings(value={"unchecked"})
	public List<Datastream> getMetadataDatastreams() throws JDOMException {
		List<Datastream> tDatastreamList = new ArrayList<Datastream>();

		LOG.debug("Adding MD as streams");
		// Add metadata as seperate streams
		String tMDName = "";
		Document tNewMDDoc = null;
		for (Element tDMDSecEl : (List<Element>)_METSDoc.getRootElement().getChildren("dmdSec", METS)) {
			
			tNewMDDoc = new Document((Element)((Element)tDMDSecEl.getChild("mdWrap", METS).getChild("xmlData", METS).getChildren().get(0)).clone());
			LOG.debug("Root is " + tNewMDDoc.getRootElement().getName());

			if (tDMDSecEl.getChild("mdWrap", METS).getAttributeValue("MDTYPE").equals("OTHER")) {
				tMDName = tDMDSecEl.getChild("mdWrap", METS).getAttributeValue("OTHERMDTYPE");
			} else {
				tMDName = tDMDSecEl.getChild("mdWrap", METS).getAttributeValue("MDTYPE");
			}

			// Don't add DC or RDF as they are handled above
			if (!tMDName.equalsIgnoreCase("dc") && !tMDName.equalsIgnoreCase("rdf")) {
				tDatastreamList.add(new XMLInlineDatastream(tMDName, tNewMDDoc));
			}
		}

		return tDatastreamList;
	}

	/** 
	 * Returns the files in the METS:fileSec as a list of datastream objects
	 *
	 * @return List<Datastream> a list of the datastreams
	 * @throws JDOMException  if there was a problem processing the METS document
	 */
@SuppressWarnings(value={"unchecked"})
	public List<Datastream> getFileDatastreams() throws JDOMException {
		List<Datastream> tDatastreamList = new ArrayList<Datastream>();

		LOG.debug("Adding files");
		XPath tPath = XPath.newInstance("//METS:file");
		tPath.addNamespace(METS);

		// Add files from METS document
		String tID = "";
		String tMimeType = "";
		String tURL = "";
		Datastream tNewDs = null;
		for (Element tFileEl : (List<Element>)tPath.selectNodes(_METSDoc)) {
			tID = tFileEl.getAttributeValue("ID");
			tMimeType = tFileEl.getAttributeValue("MIMETYPE");
			tURL = tFileEl.getChild("FLocat", METS).getAttributeValue("href", XLINK);

			if (tFileEl.getChild("FLocat", METS).getAttributeValue("LOCTYPE").equals("URL")) {
				tNewDs = new ManagedDatastream(tID, tMimeType, tURL);
			} else {
				tNewDs = new LocalDatastream(tID, tMimeType, tURL);
			}	

			tNewDs.setLabel(((Element)tFileEl.getParent()).getAttributeValue("USE"));

			tDatastreamList.add(tNewDs);
		}

		return tDatastreamList;
	}
}
