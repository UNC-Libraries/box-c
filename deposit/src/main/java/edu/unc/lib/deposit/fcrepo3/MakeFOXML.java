package edu.unc.lib.deposit.fcrepo3;

import static edu.unc.lib.deposit.work.DepositGraphUtils.cdrprop;
import static edu.unc.lib.deposit.work.DepositGraphUtils.cmodel;
import static edu.unc.lib.deposit.work.DepositGraphUtils.dprop;
import static edu.unc.lib.deposit.work.DepositGraphUtils.fprop;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
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
import edu.unc.lib.dl.util.ContentModelHelper.DepositRelationship;
import edu.unc.lib.dl.util.ContentModelHelper.FedoraProperty;
import edu.unc.lib.dl.util.ContentModelHelper.Relationship;
import edu.unc.lib.dl.util.DepositConstants;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositField;
import edu.unc.lib.dl.xml.FOXMLJDOMUtil;
import edu.unc.lib.dl.xml.FOXMLJDOMUtil.ObjectProperty;

/**
 * Creates the Fedora Object XML files required for ingest into a Fedora 3.x repository.
 * @author count0
 *
 */
public class MakeFOXML extends AbstractDepositJob implements Runnable {
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
	public void run() {
		getSubdir(DepositConstants.FOXML_DIR).mkdir();
		
		Model m = ModelFactory.createDefaultModel();
		File modelFile = new File(getDepositDirectory(), DepositConstants.MODEL_FILE);
		m.read(modelFile.toURI().toString());
		
		// establish task size
		List<Resource> topDownObjects = DepositGraphUtils.getObjectsBreadthFirst(m, getDepositPID());
		setTotalClicks(topDownObjects.size()+1);
		
		Resource deposit = m.getResource(this.getDepositPID().getURI());

		writeDepositRecord(deposit);
		
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
			if(o.hasProperty(fprop(m, FedoraProperty.hasModel), cmodel(m, ContentModelHelper.Model.CONTAINER))) {
				Bag bag = m.getBag(o);
				NodeIterator contents = bag.iterator();
				while(contents.hasNext()) {
					relsExt.add(o, cdrprop(m, Relationship.contains), contents.next());
				}
			}
			
			// deposit link
			relsExt.add(o, cdrprop(m, Relationship.originalDeposit), deposit);
				
			// TODO translate default web object, already copied, check it
			
			// add DATA_FILE
			Property fileLocation = dprop(m, DepositRelationship.stagingLocation);
			if(o.hasProperty(fileLocation)) {
				String href = o.getProperty(fileLocation).getString();
				Property mimetype = dprop(m, DepositRelationship.mimetype);
				Property md5sum = dprop(m, DepositRelationship.md5sum);
				String mimeType = o.getProperty(mimetype).getString();
				String md5checksum = null;
				if(o.hasProperty(md5sum)) {
					md5checksum = o.getProperty(md5sum).getString();
				}
				String dsLabel = ContentModelHelper.Datastream.DATA_FILE.getLabel();
				Element el = FOXMLJDOMUtil.makeLocatorDatastream(ContentModelHelper.Datastream.DATA_FILE.getName(),
						"M", href, mimeType, "URL", dsLabel,
						true, md5checksum);
				foxml.getRootElement().addContent(el);
				// Add ALT_IDS - original location URI
				Statement origLoc = o.getProperty(dprop(m, DepositRelationship.originalLocation));
				if(origLoc != null) {
					el.setAttribute("ALT_IDS", origLoc.getResource().getURI());
				}
				// add sourceData property
				relsExt.add(o, cdrprop(m, CDRProperty.sourceData), new DatastreamPID(p.getPid()+"/DATA_FILE").getDatastreamURI());
				// TODO add create time RDF dateTime property
			}
			
			// add RELS-EXT
			saveRELSEXTtoFOXMl(relsExt, foxml);
			
			// add MD_EVENTS
			addEventsDS(p, foxml);
			
			// add MD_DESCRIPTIVE
			File mods = new File(getSubdir(DepositConstants.DESCRIPTION_DIR), p.getUUID()+".xml");
			if(mods.exists()) {
				Element el = FOXMLJDOMUtil.makeLocatorDatastream(ContentModelHelper.Datastream.MD_DESCRIPTIVE.getName(),
						"M",
						mods.getAbsoluteFile().toURI().toString(),
						"text/xml", "URL", ContentModelHelper.Datastream.MD_DESCRIPTIVE.getLabel(), false, null);
				foxml.getRootElement().addContent(el);
			}
			
			// add DC
			File dc = new File(getSubdir(DepositConstants.DUBLINCORE_DIR), p.getUUID()+".xml");
			if(dc.exists()) {
				Element el = FOXMLJDOMUtil.makeLocatorDatastream(ContentModelHelper.Datastream.DC.getName(),
						"M",
						dc.getAbsoluteFile().toURI().toString(),
						"text/xml", "URL", ContentModelHelper.Datastream.DC.getLabel(), false, null);
				foxml.getRootElement().addContent(el);
			}
			
			writeFOXML(p, foxml);
			addClicks(1);
		}
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
		File events = new File(getSubdir(DepositConstants.EVENTS_DIR), p.getUUID()+".xml");
		if(events.exists()) {
			Element el = FOXMLJDOMUtil.makeLocatorDatastream(ContentModelHelper.Datastream.MD_EVENTS.getName(),
					"M",
					events.getAbsoluteFile().toURI().toString(),
					"text/xml", "URL", ContentModelHelper.Datastream.MD_EVENTS.getLabel(), false, null);
			foxml.getRootElement().addContent(el);
		}
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
		File mets = getMETSFile();
		if(mets.exists()) {
			String dsLabel = ContentModelHelper.Datastream.DATA_MANIFEST.getLabel();
			Element el = FOXMLJDOMUtil.makeLocatorDatastream(ContentModelHelper.Datastream.DATA_MANIFEST.getName(),
					"M", mets.getAbsoluteFile().toURI().toString(), "text/xml", "URL", dsLabel,
					false, null);
			foxml.getRootElement().addContent(el);
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
	
	protected File getMETSFile() {
		File result = new File(getDepositDirectory(), "mets.xml");
		if(!result.exists()) {
			result = new File(getDepositDirectory(), "METS.xml");
		} else if(!result.exists()) {
			result = new File(getDepositDirectory(), "METS.XML");
		}
		return result;
	}

}
