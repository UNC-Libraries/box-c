package edu.unc.lib.dl.ingest.sip;

import java.util.Map.Entry;

import javax.xml.transform.TransformerException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.ingest.IngestException;
import edu.unc.lib.dl.ingest.aip.AIPException;
import edu.unc.lib.dl.ingest.aip.AIPImpl;
import edu.unc.lib.dl.ingest.aip.ArchivalInformationPackage;
import edu.unc.lib.dl.ingest.aip.DepositRecord;
import edu.unc.lib.dl.ingest.aip.RDFAwareAIPImpl;
import edu.unc.lib.dl.util.AtomPubMetadataParserUtil;
import edu.unc.lib.dl.util.ContentModelHelper.Datastream;
import edu.unc.lib.dl.xml.FOXMLJDOMUtil;
import edu.unc.lib.dl.xml.JDOMNamespaceUtil;
import edu.unc.lib.dl.xml.ModsXmlHelper;

public class AtomPubEntrySIPProcessor extends FileSIPProcessor {
	private static final Log log = LogFactory.getLog(AtomPubEntrySIPProcessor.class);

	@Override
	public ArchivalInformationPackage createAIP(SubmissionInformationPackage genericSIP, DepositRecord record)
			throws IngestException {

		if (genericSIP == null)
			return null;
		if (!(genericSIP instanceof AtomPubEntrySIP))
			throw new IngestException("Invalid SIP, SIP must be of type " + AtomPubEntrySIP.class.getName());

		AtomPubEntrySIP sip = (AtomPubEntrySIP) genericSIP;

		PID pid = pidGenerator.getNextPID();

		AIPImpl aip = this.prepareIngestDirectory(sip, record);

		// create FOXML stub document
		Document foxml = FOXMLJDOMUtil.makeFOXMLDocument(pid.getPid());

		String label = null;
		if (sip.getMetadataStreams() != null) {
			Element modsElement = sip.getMetadataStreams().get(Datastream.MD_DESCRIPTIVE.getName());
			// If there are dcterms entries but not MODS, then transforms and use dcterms for md_descriptive
			if (modsElement == null) {
				Element atomDCTerms = sip.getMetadataStreams().get(AtomPubMetadataParserUtil.ATOM_DC_DATASTREAM);
				if (atomDCTerms != null) {
					try {
						modsElement = ModsXmlHelper.transformDCTerms2MODS(atomDCTerms).getRootElement();
						sip.getMetadataStreams().put(Datastream.MD_DESCRIPTIVE.getName(), (Element) modsElement.detach());
						sip.getMetadataStreams().remove(AtomPubMetadataParserUtil.ATOM_DC_DATASTREAM);

						label = ModsXmlHelper.getFormattedLabelText(modsElement);
					} catch (TransformerException e) {
						throw new IngestException("Failed to transform dcterms into MODS", e);
					}
				}
			} else {
				label = ModsXmlHelper.getFormattedLabelText(modsElement);
			}

			// Set the rdf:about attribute so the triples have the correct subject
			Element relsEXT = sip.getMetadataStreams().get(Datastream.RELS_EXT.getName());
			if (relsEXT != null) {
				Element descriptionElement = relsEXT.getChild("Description", JDOMNamespaceUtil.RDF_NS);
				Attribute aboutAttribute = new Attribute("about", pid.getURI(), JDOMNamespaceUtil.RDF_NS);
				descriptionElement.setAttribute(aboutAttribute);
			}

			// Add metadata streams to foxml
			for (Entry<String, Element> metadataStream : sip.getMetadataStreams().entrySet()) {
				Datastream datastream = Datastream.getDatastream(metadataStream.getKey());
				if (datastream == null) {
					log.warn("Could not find properties for datastream name " + metadataStream.getKey() + ", ignoring.");
				} else {
					switch (datastream.getControlGroup()) {
						case INTERNAL: {
							log.debug("Adding internal datastream " + datastream.getName());
							FOXMLJDOMUtil.setInlineXMLDatastreamContent(foxml, datastream.getName(), datastream.getLabel(),
									metadataStream.getValue(), datastream.isVersionable());
							break;
						}
						case MANAGED: {
							log.debug("Adding managed datastream " + datastream.getName());
							Element managedElement = FOXMLJDOMUtil.makeXMLManagedDatastreamElement(datastream.getName(),
									datastream.getLabel(), "0", metadataStream.getValue(), datastream.isVersionable());
							foxml.addContent(managedElement);
							break;
						}
					}
				}
			}
		}
		if (label == null) {
			if (sip.getFileLabel() == null) {
				label = pid.getPid();
			} else {
				label = sip.getFileLabel();
			}
		}

		// MAKE RDF AWARE AIP
		RDFAwareAIPImpl rdfaip = null;
		try {
			rdfaip = new RDFAwareAIPImpl(aip);
		} catch (AIPException e) {
			throw new IngestException("Could not create RDF AIP for simplified RELS-EXT setup of agent", e);
		}
		
		this.assignFileTriples(pid, sip, record, foxml, label, rdfaip);
		
		this.setDataFile(pid, sip, foxml, rdfaip);
		
		rdfaip.saveFOXMLDocument(pid, foxml);
		
		return rdfaip;
	}
}
