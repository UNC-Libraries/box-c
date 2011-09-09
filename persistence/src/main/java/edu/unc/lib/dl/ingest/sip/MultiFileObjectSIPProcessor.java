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
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.ingest.IngestException;
import edu.unc.lib.dl.ingest.aip.AIPException;
import edu.unc.lib.dl.ingest.aip.AIPImpl;
import edu.unc.lib.dl.ingest.aip.ArchivalInformationPackage;
import edu.unc.lib.dl.ingest.aip.RDFAwareAIPImpl;
import edu.unc.lib.dl.ingest.sip.MultiFileObjectSIP.Datastream;
import edu.unc.lib.dl.util.Checksum;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.util.JRDFGraphUtil;
import edu.unc.lib.dl.util.PathUtil;
import edu.unc.lib.dl.util.PremisEventLogger;
import edu.unc.lib.dl.util.TripleStoreQueryService;
import edu.unc.lib.dl.xml.FOXMLJDOMUtil;
import edu.unc.lib.dl.xml.ModsXmlHelper;

public class MultiFileObjectSIPProcessor implements SIPProcessor {
    private static final Log log = LogFactory.getLog(MultiFileObjectSIPProcessor.class);

    private edu.unc.lib.dl.pidgen.PIDGenerator pidGenerator = null;
    private TripleStoreQueryService tripleStoreQueryService = null;

    @Override
    public ArchivalInformationPackage createAIP(SubmissionInformationPackage in, PremisEventLogger logger)
		    throws IngestException {
	MultiFileObjectSIP sip = (MultiFileObjectSIP) in;

	HashSet<File> files = new HashSet<File>();
	for (Datastream d : sip.getDatastreams()) {
	    File f = d.getFile();
	    if (!f.exists()) {
		throw new IngestException("Data file not found.");
	    }
	    if (f.length() == 0) {
		throw new IngestException("Data file is zero length.");
	    }
	    files.add(d.getFile());
	}

	AIPImpl aip = new AIPImpl(files, logger);
	sip.setDiscardFilesOnDestroy(false);


	// CHECK FOR MISSING OR TRUNCATED SIP DATA
	if (sip.getContainerPath() == null) {
	    throw new IngestException("Please specify a container path");
	} else if (sip.getOwner() == null) {
	    throw new IngestException("Please specify a the owner agent");
	} else if (sip.getModsXML() == null || !sip.getModsXML().exists() || sip.getModsXML().length() == 0) {
	    throw new IngestException("MODS metadata file not found");
	}

	aip.setContainerContentModel(ContentModelHelper.Model.CONTAINER.name());
	PID pid = this.getPidGenerator().getNextPID();

	// place the object within a container path
	Set<PID> topPIDs = new HashSet<PID>();
	topPIDs.add(pid);
	aip.setTopPIDs(topPIDs);
	aip.setTopPIDPlacement(sip.getContainerPath(), pid, null, null);

	// create FOXML stub document
	Document foxml = FOXMLJDOMUtil.makeFOXMLDocument(pid.getPid());

	String dsidDefaultWebData = null;

	for (int dsCount = 0; dsCount < sip.getDatastreams().size(); dsCount++) {
	    Datastream d = sip.getDatastreams().get(dsCount);
	    Element locator = null;
	    String dsid = "DATA_FILE_" + dsCount;
	    // TYPE="MD5" and DIGEST="aaaaaa" on datastreamVersion
	    // verify checksum if one is present in SIP, then set it in FOXML

	    if (sip.getDefaultWebData() != null) {
		if (d.getFile().getAbsolutePath().equals(sip.getDefaultWebData().getFile().getAbsolutePath())) {
		    dsidDefaultWebData = dsid;
		}
	    }

	    if (d.getMd5checksum() != null && d.getMd5checksum().trim().length() > 0) {
		Checksum checker = new Checksum();
		try {
		    String sum = checker.getChecksum(d.getFile());
		    if (!sum.equals(d.getMd5checksum().toLowerCase())) {
			String msg = "Checksum failed for data file (SIP specified '" + d.getMd5checksum()
					+ "', but ingest got '" + sum + "'.)";
			throw new IngestException(msg);
		    }
		    String msg = "Externally supplied checksum verified for data file.";
		    aip.getEventLogger().logEvent(PremisEventLogger.Type.VALIDATION, msg, pid, "DATA_FILE_" + dsCount);
		} catch (IOException e) {
		    throw new IngestException("Checksum processor failed to find data file.");
		}
		locator = FOXMLJDOMUtil.makeLocatorDatastream(dsid, "M", "file://" + d.getFile().getAbsolutePath(), d
				.getMimeType(), "URL", d.getLabel(), true, d.getMd5checksum());
	    } else {
		locator = FOXMLJDOMUtil.makeLocatorDatastream(dsid, "M", "file://" + d.getFile().getAbsolutePath(), d
				.getMimeType(), "URL", d.getLabel(), true, null);
	    }
	    // add the data file
	    foxml.getRootElement().addContent(locator);
	}

	// parse the MODS and insert into FOXML
	String label = null;
	try {
	    Document mods = new SAXBuilder().build(sip.getModsXML());
	    if (log.isDebugEnabled()) {
		XMLOutputter out = new XMLOutputter();
		String output = out.outputString(mods.getRootElement());
		log.info("HERE:\n" + output);
	    }
	    label = ModsXmlHelper.getFormattedLabelText(mods.getRootElement());
	    Element root = mods.getRootElement();
	    root.detach();
	    FOXMLJDOMUtil.setDatastreamXmlContent(foxml, "MD_DESCRIPTIVE", "Descriptive Metadata (MODS)", root, true);
	} catch (JDOMException e) {
	    throw new IngestException("Error parsing MODS xml.", e);
	} catch (IOException e) {
	    throw new IngestException("Error reading MODS xml file.", e);
	}

	// set the label
	FOXMLJDOMUtil.setProperty(foxml, FOXMLJDOMUtil.ObjectProperty.label, label);

	// save FOXML to AIP
	aip.saveFOXMLDocument(pid, foxml);

	// move over pre-ingest events
	if (sip.getPreIngestEventLogger().hasEvents()) {
	    for (Element event : sip.getPreIngestEventLogger().getEvents(pid)) {
		aip.getEventLogger().addEvent(pid, event);
	    }
	}

	// MAKE RDF AWARE AIP
	RDFAwareAIPImpl rdfaip = null;
	try {
	    rdfaip = new RDFAwareAIPImpl(aip);
	    aip = null;
	} catch (AIPException e) {
	    throw new IngestException("Could not create RDF AIP for simplified RELS-EXT setup of agent", e);
	}

	// set owner
	JRDFGraphUtil.addFedoraPIDRelationship(rdfaip.getGraph(), pid, ContentModelHelper.Relationship.owner, sip
			.getOwner().getPID());
	// set content model
	JRDFGraphUtil.addFedoraProperty(rdfaip.getGraph(), pid, ContentModelHelper.FedoraProperty.hasModel,
			ContentModelHelper.Model.SIMPLE.getURI());

	// set slug, detecting sibling slug conflicts and incrementing
	String slug = PathUtil.makeSlug(label);
	while (this.getTripleStoreQueryService().fetchByRepositoryPath(sip.getContainerPath() + "/" + slug) != null) {
	    slug = PathUtil.incrementSlug(slug);
	}
	JRDFGraphUtil.addCDRProperty(rdfaip.getGraph(), pid, ContentModelHelper.CDRProperty.slug, slug);

	// setup the allowIndexing property
	if (sip.isAllowIndexing()) {
	    JRDFGraphUtil.addCDRProperty(rdfaip.getGraph(), pid, ContentModelHelper.CDRProperty.allowIndexing, "yes");
	} else {
	    JRDFGraphUtil.addCDRProperty(rdfaip.getGraph(), pid, ContentModelHelper.CDRProperty.allowIndexing, "no");
	}

	// set default web data to correct datastream URI if any
	if (dsidDefaultWebData != null) {
	    URI dsURI = null;
	    try {
		dsURI = new URI(pid.getURI() + "/" + dsidDefaultWebData);
		JRDFGraphUtil.addCDRProperty(rdfaip.getGraph(), pid, ContentModelHelper.CDRProperty.defaultWebData,
				dsURI);
	    } catch (URISyntaxException e) {
		throw new Error("Unexpected exception creating URI for DATA_FILE datastream.", e);
	    }
	}

	// set datastream sourceData and indexText pointers
	for (int dsCount = 0; dsCount < sip.getDatastreams().size(); dsCount++) {
	    Datastream d = sip.getDatastreams().get(dsCount);
	    String dsid = "DATA_FILE_" + dsCount;
	    URI dsURI;
	    try {
		dsURI = new URI(pid.getURI() + "/" + dsid);
	    } catch (URISyntaxException e) {
		throw new Error("Unexpected error with URI syntax.");
	    }
	    JRDFGraphUtil.addCDRProperty(rdfaip.getGraph(), pid, ContentModelHelper.CDRProperty.sourceData, dsURI);
	    // set indexText when appropriate
	    if (d.getMimeType() != null && d.getMimeType().startsWith("text/")) {
		JRDFGraphUtil.addCDRProperty(rdfaip.getGraph(), pid, ContentModelHelper.CDRProperty.indexText, dsURI);
	    }
	}

	if (log.isDebugEnabled()) {
	    rdfaip.commitGraphChanges();
	    XMLOutputter out = new XMLOutputter();
	    String output = out.outputString(rdfaip.getFOXMLDocument(pid));
	    log.debug("HEREFOXML:\n" + output);
	}

	return rdfaip;
    }

    public edu.unc.lib.dl.pidgen.PIDGenerator getPidGenerator() {
	return pidGenerator;
    }

    public TripleStoreQueryService getTripleStoreQueryService() {
	return tripleStoreQueryService;
    }

    public void setPidGenerator(edu.unc.lib.dl.pidgen.PIDGenerator pidGenerator) {
	this.pidGenerator = pidGenerator;
    }

    public void setTripleStoreQueryService(TripleStoreQueryService tripleStoreQueryService) {
	this.tripleStoreQueryService = tripleStoreQueryService;
    }

}
