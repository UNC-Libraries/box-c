package edu.unc.lib.deposit.fcrepo3;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.IOUtils;
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
import edu.unc.lib.dl.fedora.DatastreamPID;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.util.ContentModelHelper.CDRProperty;
import edu.unc.lib.dl.util.ContentModelHelper.FedoraProperty;
import edu.unc.lib.dl.util.DepositConstants;
import edu.unc.lib.dl.xml.FOXMLJDOMUtil;

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
		copyPropertyURIs.remove(ContentModelHelper.CDRProperty.sourceData.getURI().toString());
	}

	public MakeFOXML(String uuid, String depositUUID) {
		super(uuid, depositUUID);
	}

	public MakeFOXML() {}

	@Override
	public void run() {
		Model m = ModelFactory.createDefaultModel();
		File modelFile = new File(getDepositDirectory(), DepositConstants.MODEL_FILE);
		m.read(modelFile.toURI().toString());
		
		// establish task size
		Property contains = m.getProperty(ContentModelHelper.Relationship.contains.getURI().toString());
		NodeIterator ncounter = m.listObjectsOfProperty(contains);
		int count = 0;
		while(ncounter.hasNext()) {
			ncounter.next(); count++;
		}
		setTotalClicks(count);
		
		// gov.loc.repository.bagit.Bag bagit = loadBag();
		Resource deposit = m.getResource(this.getDepositPID().getURI());

		File foxmlDir = new File(getDepositDirectory(), DepositConstants.FOXML_DIR);
		foxmlDir.mkdir();
		File eventsDir = new File(getDepositDirectory(), DepositConstants.EVENTS_DIR);
		File modsDir = new File(getDepositDirectory(), DepositConstants.DESCRIPTION_DIR);
		File dcDir = new File(getDepositDirectory(), DepositConstants.DUBLINCORE_DIR);
		Property hasModel = m.getProperty(ContentModelHelper.FedoraProperty.hasModel.getURI().toString());
		Resource container = m.getProperty(ContentModelHelper.Model.CONTAINER.getURI().toString());
		Property originalDeposit = m.getProperty(ContentModelHelper.Relationship.originalDeposit.getURI().toString());
		Property sourceData = m.getProperty(ContentModelHelper.CDRProperty.sourceData.getURI().toString());
		List<Resource> topDownObjects = getBreadthFirstTree(m);
		for(Resource o : topDownObjects) { // all object content is contained, except deposit record
			PID p = new PID(o.getURI());
			log.debug("making FOXML for: {}", p); 
			Document foxml = FOXMLJDOMUtil.makeFOXMLDocument(p.getPid());
			// TODO set object properties: label, state, etc..
			
			Model relsExt = ModelFactory.createDefaultModel();

			// copy Fedora and CDR property statements
			StmtIterator properties = o.listProperties();
			while(properties.hasNext()) {
				Statement s = properties.nextStatement();
				if(copyPropertyURIs.contains(s.getPredicate().getURI())) {
					relsExt.add(s);
				} else if(/* TODO is a role property*/false ) {
					// TODO copy role statements to relsExt		
					relsExt.add(s);
				}
			}
			
			// add contains statements
			if(o.hasProperty(hasModel, container)) {
				Bag bag = m.getBag(o);
				NodeIterator contents = bag.iterator();
				while(contents.hasNext()) {
					relsExt.add(o, contains, contents.next());
				}
			}
			
			// deposit link
			relsExt.add(o, originalDeposit, deposit);
				
			// TODO translate supplemental this is already copied, check it
			// TODO translate default web object, already copied, check it
			
			// add DATA_FILE
			Property fileLocation = m.createProperty(DepositConstants.FILE_LOCATOR_URI);
			if(o.hasProperty(fileLocation)) {
				String href = o.getProperty(fileLocation).getString();
				Property hasMimetype = m.createProperty(CDRProperty.hasSourceMimeType.getURI().toString());
				Property hasChecksum = m.createProperty(CDRProperty.hasChecksum.getURI().toString());
				String mimeType = o.getProperty(hasMimetype).getString();
				String md5checksum = null;
				if(o.hasProperty(hasChecksum)) {
					md5checksum = o.getProperty(hasChecksum).getString();
				}
				// TODO set label to original name
				Element el = FOXMLJDOMUtil.makeLocatorDatastream(ContentModelHelper.Datastream.DATA_FILE.getName(),
						"M", href, mimeType, "URL", ContentModelHelper.Datastream.DATA_FILE.getLabel(),
						true, md5checksum);
				foxml.getRootElement().addContent(el);
				// add sourceData property
				relsExt.add(o, sourceData, new DatastreamPID(p.getPid()+"/DATA_FILE").getDatastreamURI());
			}
			
			// add RELS-EXT
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
			
			// TODO feature: add tag file checksums
			
			// add MD_EVENTS
			File events = new File(eventsDir, p.getUUID()+".xml");
			if(events.exists()) {
				Element el = FOXMLJDOMUtil.makeLocatorDatastream(ContentModelHelper.Datastream.MD_EVENTS.getName(),
						"M",
						events.getAbsoluteFile().toURI().toString(),
						"text/xml", "URL", ContentModelHelper.Datastream.MD_EVENTS.getLabel(), false, null);
				foxml.getRootElement().addContent(el);
			}
			
			// add MD_DESCRIPTIVE
			File mods = new File(modsDir, p.getUUID()+".xml");
			if(mods.exists()) {
				Element el = FOXMLJDOMUtil.makeLocatorDatastream(ContentModelHelper.Datastream.MD_DESCRIPTIVE.getName(),
						"M",
						mods.getAbsoluteFile().toURI().toString(),
						"text/xml", "URL", ContentModelHelper.Datastream.MD_DESCRIPTIVE.getLabel(), false, null);
				foxml.getRootElement().addContent(el);
			}
			
			// add DC
			File dc = new File(dcDir, p.getUUID()+".xml");
			if(dc.exists()) {
				Element el = FOXMLJDOMUtil.makeLocatorDatastream(ContentModelHelper.Datastream.DC.getName(),
						"M",
						dc.getAbsoluteFile().toURI().toString(),
						"text/xml", "URL", ContentModelHelper.Datastream.DC.getLabel(), false, null);
				foxml.getRootElement().addContent(el);
			}
			
			// save foxml to file
			File foxmlFile = new File(foxmlDir, p.getUUID()+".xml");
			FileOutputStream fos = null;
			try {
				fos = new FileOutputStream(foxmlFile);
				new XMLOutputter(Format.getPrettyFormat()).output(foxml, fos);
			} catch (FileNotFoundException e) {
				throw new Error("Unexpected error creating foxml file.", e);
			} catch (IOException e) {
				throw new Error("Unexpected error creating foxml file.", e);
			} finally {
				if(fos != null)	IOUtils.closeQuietly(fos);
			}
			addClicks(1);
		}
	}

	private List<Resource> getBreadthFirstTree(Model m) {
		List<Resource> result = new ArrayList<Resource>();
		Bag bag = m.getBag(this.getDepositPID().getURI());
		addChildren(bag, result);
		log.debug("tree list: {}", result);
		return result;
	}

	private void addChildren(Bag bag, List<Resource> result) {
		NodeIterator iterator = bag.iterator();
		List<Bag> bags = new ArrayList<Bag>();
		while(iterator.hasNext()) {
			Resource n = (Resource)iterator.next();
			result.add(n);
			Bag b = n.getModel().getBag(n.getURI());
			if(b != null) {
				bags.add(b);
			}
		}
		iterator.close();
		for(Bag b : bags) {
			addChildren(b, result);
		}
	}

}
