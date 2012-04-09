package edu.unc.lib.dl.ingest.sip;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
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
import edu.unc.lib.dl.pidgen.PIDGenerator;
import edu.unc.lib.dl.util.AtomPubMetadataParserUtil;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.util.FileUtils;
import edu.unc.lib.dl.util.JRDFGraphUtil;
import edu.unc.lib.dl.util.PathUtil;
import edu.unc.lib.dl.util.TripleStoreQueryService;
import edu.unc.lib.dl.util.ContentModelHelper.Datastream;
import edu.unc.lib.dl.xml.FOXMLJDOMUtil;
import edu.unc.lib.dl.xml.JDOMNamespaceUtil;
import edu.unc.lib.dl.xml.ModsXmlHelper;
import edu.unc.lib.dl.xml.FOXMLJDOMUtil.ObjectProperty;

public class AtomPubEntrySIPProcessor implements SIPProcessor {
	private static final Log log = LogFactory.getLog(AtomPubEntrySIPProcessor.class);

	private PIDGenerator pidGenerator;
	private TripleStoreQueryService tripleStoreQueryService;

	@Override
	public ArchivalInformationPackage createAIP(SubmissionInformationPackage genericSIP, DepositRecord record)
			throws IngestException {

		if (genericSIP == null)
			return null;
		if (!(genericSIP instanceof AtomPubEntrySIP))
			throw new IngestException("Invalid SIP, SIP must be of type " + AtomPubEntrySIP.class.getName());

		AtomPubEntrySIP sip = (AtomPubEntrySIP) genericSIP;

		PID pid = pidGenerator.getNextPID();

		// Prepare the temporary ingest directory
		File batchPrepDir = null;
		try {
			batchPrepDir = FileUtils.createTempDirectory("ingest-prep");
		} catch (IOException e) {
			throw new IngestException("Unexpected IO error", e);
		}
		File sipDataSubDir = new File(batchPrepDir, "data");
		sipDataSubDir.mkdir();

		AIPImpl aip = new AIPImpl(batchPrepDir, record);

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
			if (sip.getFilename() == null) {
				label = pid.getPid();
			} else {
				label = sip.getFilename();
			}
		}

		// set the label
		FOXMLJDOMUtil.setProperty(foxml, ObjectProperty.label, label);

		// Set object to be active or not depending on if it is in progress.
		if (sip.isInProgress()) {
			FOXMLJDOMUtil.setProperty(foxml, ObjectProperty.state, "Inactive");
		} else {
			FOXMLJDOMUtil.setProperty(foxml, ObjectProperty.state, "Active");
		}

		// Add the pid to the topPID set
		Set<PID> topPIDs = new HashSet<PID>();
		topPIDs.add(pid);
		aip.setTopPIDs(topPIDs);
		aip.setContainerPlacement(sip.getContainerPID(), pid, null, null, label);

		// save FOXML to AIP
		aip.saveFOXMLDocument(pid, foxml);

		// MAKE RDF AWARE AIP
		RDFAwareAIPImpl rdfaip = null;
		try {
			rdfaip = new RDFAwareAIPImpl(aip);
		} catch (AIPException e) {
			throw new IngestException("Could not create RDF AIP for simplified RELS-EXT setup of agent", e);
		}

		// set owner
		JRDFGraphUtil.addFedoraPIDRelationship(rdfaip.getGraph(), pid, ContentModelHelper.Relationship.owner, record
				.getOwner().getPID());

		// set content model
		JRDFGraphUtil.addFedoraProperty(rdfaip.getGraph(), pid, ContentModelHelper.FedoraProperty.hasModel,
				ContentModelHelper.Model.SIMPLE.getURI());

		// set slug using either default or suggested slug, detecting sibling slug conflicts and incrementing
		String slug = PathUtil.makeSlug(label);
		if (sip.getSuggestedSlug() == null) {
			slug = PathUtil.makeSlug(label);
		} else {
			slug = sip.getSuggestedSlug();
		}
		String containerPath = tripleStoreQueryService.lookupRepositoryPath(sip.getContainerPID());
		while (tripleStoreQueryService.fetchByRepositoryPath(containerPath + "/" + slug) != null) {
			slug = PathUtil.incrementSlug(slug);
		}
		JRDFGraphUtil.addCDRProperty(rdfaip.getGraph(), pid, ContentModelHelper.CDRProperty.slug, slug);
		
		rdfaip.saveFOXMLDocument(pid, foxml);
		
		return rdfaip;
	}

	public PIDGenerator getPidGenerator() {
		return pidGenerator;
	}

	public void setPidGenerator(PIDGenerator pidGenerator) {
		this.pidGenerator = pidGenerator;
	}

	public TripleStoreQueryService getTripleStoreQueryService() {
		return tripleStoreQueryService;
	}

	public void setTripleStoreQueryService(TripleStoreQueryService tripleStoreQueryService) {
		this.tripleStoreQueryService = tripleStoreQueryService;
	}
}
