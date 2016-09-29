package edu.unc.lib.deposit.fcrepo3;

import static edu.unc.lib.deposit.work.DepositGraphUtils.cdrprop;
import static edu.unc.lib.deposit.work.DepositGraphUtils.cmodel;
import static edu.unc.lib.deposit.work.DepositGraphUtils.dprop;
import static edu.unc.lib.deposit.work.DepositGraphUtils.fprop;
import static edu.unc.lib.dl.util.ContentModelHelper.Datastream.DC;
import static edu.unc.lib.dl.util.ContentModelHelper.Datastream.MD_DESCRIPTIVE;
import static edu.unc.lib.dl.util.ContentModelHelper.Model.CONTAINER;
import static edu.unc.lib.dl.util.ContentModelHelper.Model.DEPOSIT_RECORD;
import static edu.unc.lib.dl.util.ContentModelHelper.Model.PRESERVEDOBJECT;
import static edu.unc.lib.dl.util.ContentModelHelper.Model.SIMPLE;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.input.sax.XMLReaders;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Bag;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

import edu.unc.lib.deposit.work.AbstractDepositJob;
import edu.unc.lib.deposit.work.DepositGraphUtils;
import edu.unc.lib.dl.acl.util.UserRole;
import edu.unc.lib.dl.fedora.DatastreamPID;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.util.ContentModelHelper.CDRProperty;
import edu.unc.lib.dl.util.ContentModelHelper.Datastream;
import edu.unc.lib.dl.util.ContentModelHelper.DepositRelationship;
import edu.unc.lib.dl.util.ContentModelHelper.FedoraProperty;
import edu.unc.lib.dl.util.ContentModelHelper.Relationship;
import edu.unc.lib.dl.util.DepositConstants;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositField;
import edu.unc.lib.dl.xml.FOXMLJDOMUtil;
import edu.unc.lib.dl.xml.FOXMLJDOMUtil.ObjectProperty;
import edu.unc.lib.dl.xml.JDOMNamespaceUtil;

/**
 * Creates the Fedora Object XML files required for ingest into a Fedora 3.x repository.
 * @author count0
 *
 */
public class MakeFOXML extends AbstractDepositJob {
	private static final Logger log = LoggerFactory.getLogger(MakeFOXML.class);

	private static Set<String> copyPropertyURIs = null;

	static {
		copyPropertyURIs = new HashSet<String>();
		for(CDRProperty p : ContentModelHelper.CDRProperty.values()) {
			copyPropertyURIs.add(p.getURI().toString());
		}
		for(FedoraProperty p : ContentModelHelper.FedoraProperty.values()) {
			copyPropertyURIs.add(p.getURI().toString());
		}
	}

	public MakeFOXML(String uuid, String depositUUID) {
		super(uuid, depositUUID);
	}

	public MakeFOXML() {}

	@Override
	public void runJob() {
		getSubdir(DepositConstants.FOXML_DIR).mkdir();

		Model m = getReadOnlyModel();

		Map<String, String> status = getDepositStatus();
		boolean excludeDepositRecord = Boolean.parseBoolean(status.get(DepositField.excludeDepositRecord.name()));

		String publishObjectsValue = status.get(DepositField.publishObjects.name());
		boolean publishObjects = !"false".equals(publishObjectsValue);
		Property publishedProperty = cdrprop(m, CDRProperty.isPublished);

		// establish task size
		List<Resource> topDownObjects = DepositGraphUtils.getObjectsBreadthFirst(m, getDepositPID());
		setTotalClicks(topDownObjects.size() + (excludeDepositRecord ? 0 : 1));

		Resource deposit = m.getResource(this.getDepositPID().getURI());

		if (!excludeDepositRecord) {
			writeDepositRecord(deposit);
			addClicks(1);
		}

		Property hasModelP = fprop(m, FedoraProperty.hasModel);

		for(Resource o : topDownObjects) {
			PID p = new PID(o.getURI());
			log.debug("making FOXML for: {}", p);
			Document foxml = FOXMLJDOMUtil.makeFOXMLDocument(p.getPid());
			FOXMLJDOMUtil.setProperty(foxml, ObjectProperty.state, "A");
			Statement lstmt = o.getProperty(dprop(m, DepositRelationship.label));
			if(lstmt != null) {
				FOXMLJDOMUtil.setProperty(foxml, ObjectProperty.label, lstmt.getString());
			}

			Model relsExt = ModelFactory.createDefaultModel();
			// copy Fedora and CDR property statements
			StmtIterator properties = o.listProperties();
			while(properties.hasNext()) {
				Statement s = properties.nextStatement();
				if(copyPropertyURIs.contains(s.getPredicate().getURI())) {
					relsExt.add(s);
				} else if(UserRole.getUserRole(s.getPredicate().getURI()) != null) {
					relsExt.add(s);
				}
			}

			// add contains statements
			if (o.hasProperty(hasModelP, cmodel(m, CONTAINER))) {
				Bag bag = m.getBag(o);
				NodeIterator contents = bag.iterator();
				try {
					while(contents.hasNext()) {
						relsExt.add(o, cdrprop(m, Relationship.contains), contents.next());
					}
				} finally {
					contents.close();
				}
			}

			// Add implicit content models
			if (!o.hasProperty(hasModelP, cmodel(m, DEPOSIT_RECORD))) {
				// If no models assigned, then it is assumed to be a Simple object

				if (!o.hasProperty(hasModelP))
					relsExt.add(o, hasModelP, m.createResource(SIMPLE.toString()));
				// All non-deposit records are preserved objects
				relsExt.add(o, hasModelP, m.createResource(PRESERVEDOBJECT.toString()));
			}

			// deposit link
			if (!excludeDepositRecord)
				relsExt.add(o, cdrprop(m, Relationship.originalDeposit), deposit);

			// TODO translate default web object, already copied, check it

			// Add in invalid term triples
			Property invalidTermProp = cdrprop(m, CDRProperty.invalidTerm);
			if (o.hasProperty(invalidTermProp)) {
				StmtIterator props = o.listProperties(invalidTermProp);

				while (props.hasNext()) {
					Statement prop = props.next();
					relsExt.add(o, invalidTermProp, prop.getLiteral());
				}
			}

			// Mark all objects being ingested as unpublished
			if (!publishObjects) {
				relsExt.add(o, publishedProperty, "no");
			}

			// add MD_EVENTS
			addEventsDS(p, foxml);

			// add MD_DESCRIPTIVE
			File mods = new File(getSubdir(DepositConstants.DESCRIPTION_DIR), p.getUUID() + ".xml");
			if (mods.exists()) {
				SAXBuilder sb = new SAXBuilder(XMLReaders.NONVALIDATING);
				Document modsDoc;
				try {
					modsDoc = sb.build(mods);
					Element el = FOXMLJDOMUtil.makeInlineXMLDatastreamElement(MD_DESCRIPTIVE.getName(),
							MD_DESCRIPTIVE.getLabel(), modsDoc.detachRootElement(), MD_DESCRIPTIVE.isVersionable());
					foxml.getRootElement().addContent(el);
				} catch (JDOMException | IOException e) {
					log.error("Failed to add MODS datastream from {}", mods.getAbsolutePath(), e);
				}

//				Element el = FOXMLJDOMUtil.makeLocatorDatastream(MD_DESCRIPTIVE.getName(), MD_DESCRIPTIVE.getControlGroup()
//						.getAttributeValue(),
//						DepositConstants.DESCRIPTION_DIR + "/" + mods.getName(), "text/xml", "URL",
//						MD_DESCRIPTIVE.getLabel(), MD_DESCRIPTIVE.isVersionable(), null);
			}

			// add DC
			File dc = new File(getSubdir(DepositConstants.DUBLINCORE_DIR), p.getUUID() + ".xml");
			if (dc.exists()) {
				Element el = FOXMLJDOMUtil.makeLocatorDatastream(DC.getName(), DC.getControlGroup().getAttributeValue(),
						DepositConstants.DUBLINCORE_DIR + "/" + dc.getName(), "text/xml", "URL", DC.getLabel(),
						DC.isVersionable(), null);
				foxml.getRootElement().addContent(el);
			}

			// Adding other datastreams before DATA_FILE to avoid temporary files being deleted before ingesting
			Property hasDatastream = dprop(m, DepositRelationship.hasDatastream);
			if (o.hasProperty(hasDatastream)) {
				StmtIterator dsIt = o.listProperties(hasDatastream);

				while (dsIt.hasNext()) {
					Statement dsStmt = dsIt.next();
					addDatastream(m, dsStmt.getResource(), foxml);
				}
			}

			// add DATA_FILE
			Property fileLocation = dprop(m, DepositRelationship.stagingLocation);
			if (o.hasProperty(fileLocation)) {
				addDatastream(m, o, foxml);

				// add sourceData and defaultWebData properties
				Resource dataFileResource = m.createResource(new DatastreamPID(p.getPid() + "/DATA_FILE")
						.getDatastreamURI());
				relsExt.add(o, cdrprop(m, CDRProperty.sourceData), dataFileResource);
				relsExt.add(o, cdrprop(m, CDRProperty.defaultWebData), dataFileResource);
			}

			// add RELS-EXT
			saveRELSEXTtoFOXMl(relsExt, foxml);

			writeFOXML(p, foxml);
			addClicks(1);
		}
	}

	private void addDatastream(Model m, Resource dsResource, Document foxml) {
		// Determine what datastream we're setting
		String dsPID = new PID(dsResource.getURI()).getPid();
		int dsIndex = dsPID.lastIndexOf('/');
		// DATA_FILE is the default if the datastream is not specified
		Datastream datastream = Datastream.DATA_FILE;
		if (dsIndex > 0) {
			datastream = Datastream.getDatastream(dsPID.substring(dsIndex + 1));
			if (datastream == null) {
				log.warn("Invalid datastream {} specified, ignoring", dsPID);
			}
		}

		Property fileLocation = dprop(m, DepositRelationship.stagingLocation);
		Property mimetype = dprop(m, DepositRelationship.mimetype);
		Property md5sum = dprop(m, DepositRelationship.md5sum);

		String href = dsResource.getProperty(fileLocation).getString();
		// FIXME if href starts with "data/" then it needs more path/URI

		String mimeType = null;
		if (dsResource.hasProperty(mimetype)) {
			mimeType = dsResource.getProperty(mimetype).getString();
		} else {
			mimeType = "application/octet-stream";
		}
		String md5checksum = null;
		if (dsResource.hasProperty(md5sum)) {
			md5checksum = dsResource.getProperty(md5sum).getString();
		}

		// Create the datastream element, using defaults for the datastream where necessary
		Element el = FOXMLJDOMUtil.makeLocatorDatastream(datastream.getName(), datastream.getControlGroup()
				.getAttributeValue(), href, mimeType, "URL", datastream.getLabel(), datastream.isVersionable(),
				md5checksum);
		foxml.getRootElement().addContent(el);

		// Add ALT_IDS - original location URI
		Statement origLoc = dsResource.getProperty(dprop(m, DepositRelationship.originalLocation));
		if (origLoc != null) {
			el.getChild("datastreamVersion", JDOMNamespaceUtil.FOXML_NS).setAttribute("ALT_IDS", origLoc.getResource().getURI());
		}

		// TODO add create time RDF dateTime property
	}

	/**
	 * save foxml to file
	 * @param p
	 * @param foxml
	 */
	private void writeFOXML(PID p, Document foxml) {
		File foxmlFile = new File(getSubdir(DepositConstants.FOXML_DIR), p.getUUID()+".xml");
		try(FileOutputStream fos = new FileOutputStream(foxmlFile)) {
			new XMLOutputter(Format.getPrettyFormat()).output(foxml, fos);
		} catch (FileNotFoundException e) {
			throw new Error("Unexpected error creating foxml file.", e);
		} catch (IOException e) {
			throw new Error("Unexpected error creating foxml file.", e);
		}
	}

	private void addEventsDS(PID p, Document foxml) {
		// Add locator datastream for events, referencing the events file identified by p
		File events = getEventsFile(p);

		// Use a path relative to the deposit directory
		Path absolute = Paths.get(events.getAbsolutePath());
		Path base = Paths.get(getDepositDirectory().getAbsolutePath());
		Path relative = base.relativize(absolute);

		Element el = FOXMLJDOMUtil.makeLocatorDatastream(Datastream.MD_EVENTS.getName(), "M", relative.toString(),
				"text/xml", "URL", Datastream.MD_EVENTS.getLabel(), false, null);
		foxml.getRootElement().addContent(el);
	}

	private void saveRELSEXTtoFOXMl(Model relsExt, Document foxml) {
		try {
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			relsExt.write(os, "RDF/XML");
			os.flush();
			ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());
			Element relsEl = new SAXBuilder().build(is).detachRootElement();
			FOXMLJDOMUtil.setInlineXMLDatastreamContent(foxml, "RELS-EXT", "Relationship Metadata", relsEl, false);
		} catch(IOException e) {
			log.error("trouble making RELS-EXT", e);
		} catch (JDOMException e) {
			log.error("trouble making RELS-EXT", e);
		}
	}

	private void writeDepositRecord(Resource deposit) {
		Model m = deposit.getModel();
		Document foxml = FOXMLJDOMUtil.makeFOXMLDocument(getDepositPID().getPid());
		FOXMLJDOMUtil.setProperty(foxml, ObjectProperty.state, "A");
		Statement lstmt = deposit.getProperty(dprop(m, DepositRelationship.label));
		if(lstmt != null) {
			FOXMLJDOMUtil.setProperty(foxml, ObjectProperty.label, lstmt.getString());
		}

		// add manifest DS
		String dsLabel = Datastream.DATA_MANIFEST.getLabel();
		List<File> manifestFiles = getManifestFiles();
		if (!manifestFiles.isEmpty()) {
			int i = 0;
			Element el ;
			for (File manifest : manifestFiles) {
				el	= FOXMLJDOMUtil.makeLocatorDatastream(Datastream.DATA_MANIFEST.getName() + i++,
						"M", manifest.getAbsolutePath(), "text/xml", "URL", dsLabel, false, null);
				foxml.getRootElement().addContent(el);
				log.info("Manifest files have been added to foxml");
			} 
		} else {
			log.warn("No manifest files were found for the deposit");
		}

		addEventsDS(getDepositPID(), foxml);

		// add RELS
		Map<String, String> status = getDepositStatus();
		Model rels = ModelFactory.createDefaultModel();
		Resource d = rels.createResource(deposit.getURI());
		rels.add(d, fprop(m, FedoraProperty.hasModel), cmodel(m, ContentModelHelper.Model.DEPOSIT_RECORD));
		String depositPackageType = status.get(DepositField.packagingType.name());
		if(depositPackageType != null) {
			Resource t = rels.getResource(depositPackageType);
			rels.add(d, cdrprop(rels, ContentModelHelper.CDRProperty.depositPackageType), t);
		}
		String method = status.get(DepositField.depositMethod.name());
		if(method != null) {
			rels.add(d, cdrprop(rels, ContentModelHelper.CDRProperty.depositMethod), method);
		}
		String by = status.get(DepositField.depositorName.name());
		if(by != null) {
			rels.add(d, cdrprop(rels, ContentModelHelper.CDRProperty.depositedOnBehalfOf), by);
		}
		saveRELSEXTtoFOXMl(rels, foxml);
		writeFOXML(getDepositPID(), foxml);
	}

}
