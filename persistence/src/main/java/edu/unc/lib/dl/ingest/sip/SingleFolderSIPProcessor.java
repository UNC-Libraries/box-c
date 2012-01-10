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

import java.io.IOException;
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
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.util.JRDFGraphUtil;
import edu.unc.lib.dl.util.PathUtil;
import edu.unc.lib.dl.xml.FOXMLJDOMUtil;
import edu.unc.lib.dl.xml.JDOMNamespaceUtil;
import edu.unc.lib.dl.xml.ModsXmlHelper;

public class SingleFolderSIPProcessor implements SIPProcessor {
	private static final Log log = LogFactory.getLog(SingleFolderSIPProcessor.class);

	private edu.unc.lib.dl.pidgen.PIDGenerator pidGenerator = null;

	@Override
	public ArchivalInformationPackage createAIP(SubmissionInformationPackage in)
			throws IngestException {
		SingleFolderSIP sip = (SingleFolderSIP) in;
		AIPImpl aip = new AIPImpl();

		// Note: container must exist.
		PID pid = this.getPidGenerator().getNextPID();

		// place the object within a container path
		Set<PID> topPIDs = new HashSet<PID>();
		topPIDs.add(pid);
		aip.setTopPIDs(topPIDs);
		aip.setContainerPlacement(sip.getContainerPID(), pid, null, null);

		// create FOXML stub document
		Document foxml = FOXMLJDOMUtil.makeFOXMLDocument(pid.getPid());

		// add the contents file
		Element structMap = new Element("structMap", JDOMNamespaceUtil.METS_NS);
		structMap.addContent(new Element("div", JDOMNamespaceUtil.METS_NS).setAttribute("TYPE", "Container"));
		Element contents = FOXMLJDOMUtil.makeXMLManagedDatastreamElement("MD_CONTENTS", "List of Contents", "MD_CONTENTS1.0", structMap, false);
		foxml.getRootElement().addContent(contents);

		// Note: either slug or label or MODS title must be set.
		// parse the MODS and insert into FOXML
		String label = sip.getLabel();
		String slug = sip.getSlug();
		if (slug != null) {
			slug = PathUtil.makeSlug(slug);
		}
		if (sip.getModsXML() != null) {
			try {
				Document mods = new SAXBuilder().build(sip.getModsXML());

				if (log.isDebugEnabled()) {
					XMLOutputter out = new XMLOutputter();
					String output = out.outputString(mods.getRootElement());
					log.info("HERE:\n" + output);
				}
				if (label == null) {
					label = ModsXmlHelper.getFormattedLabelText(mods.getRootElement());
				}
				Element root = mods.getRootElement();
				root.detach();
				FOXMLJDOMUtil.setDatastreamXmlContent(foxml, "MD_DESCRIPTIVE", "Descriptive Metadata (MODS)", root, true);
			} catch (JDOMException e) {
				throw new IngestException("Error parsing MODS xml.", e);
			} catch (IOException e) {
				throw new IngestException("Error reading MODS xml file.", e);
			}
		}

		if (label == null && slug == null) {
			throw new IngestException("Folder creation requires that one of slug, label or MODS titleInfo not be null.");
		}

		if (slug == null) {
			slug = PathUtil.makeSlug(label);
			// could increment slug here, if you wanted to autoincrement folder names
		}
		if (label == null) {
			label = slug;
		}

		// set the label
		FOXMLJDOMUtil.setProperty(foxml, FOXMLJDOMUtil.ObjectProperty.label, label);

		// save FOXML to AIP
		aip.saveFOXMLDocument(pid, foxml);

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
				ContentModelHelper.Model.CONTAINER.getURI());

		if (sip.isCollection()) {
			if (sip.getModsXML() == null) {
				throw new IngestException("Collection creation requires descriptive metadata (MODS).");
			} else {
				JRDFGraphUtil.addFedoraProperty(rdfaip.getGraph(), pid, ContentModelHelper.FedoraProperty.hasModel,
						ContentModelHelper.Model.COLLECTION.getURI());
			}
		}
		// set slug
		JRDFGraphUtil.addCDRProperty(rdfaip.getGraph(), pid, ContentModelHelper.CDRProperty.slug, slug);

		// setup the allowIndexing property
		if (sip.isAllowIndexing()) {
			JRDFGraphUtil.addCDRProperty(rdfaip.getGraph(), pid, ContentModelHelper.CDRProperty.allowIndexing, "yes");
		} else {
			JRDFGraphUtil.addCDRProperty(rdfaip.getGraph(), pid, ContentModelHelper.CDRProperty.allowIndexing, "no");
		}

		if (log.isDebugEnabled()) {
			rdfaip.commitGraphChanges();
			XMLOutputter out = new XMLOutputter();
			String output = out.outputString(rdfaip.getFOXMLDocument(pid));
			log.info("HEREFOXML:\n" + output);
		}

		return rdfaip;
	}

	public edu.unc.lib.dl.pidgen.PIDGenerator getPidGenerator() {
		return pidGenerator;
	}

	public void setPidGenerator(edu.unc.lib.dl.pidgen.PIDGenerator pidGenerator) {
		this.pidGenerator = pidGenerator;
	}

}
