package edu.unc.lib.bag.f3ingest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
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

import edu.unc.lib.dl.fedora.DatastreamPID;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.BagConstants;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.util.ContentModelHelper.CDRProperty;
import edu.unc.lib.dl.util.ContentModelHelper.FedoraProperty;
import edu.unc.lib.dl.xml.FOXMLJDOMUtil;
import edu.unc.lib.workers.AbstractBagJob;

public class MakeFOXML extends AbstractBagJob implements Runnable {
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

	public MakeFOXML(String uuid, String bagDirectory, String depositId) {
		super(uuid, bagDirectory, depositId);
	}

	public MakeFOXML() {}

	@Override
	public void run() {
		Model m = ModelFactory.createDefaultModel();
		File modelFile = new File(getBagDirectory(), BagConstants.MODEL_FILE);
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

		File foxmlDir = new File(getBagDirectory(), BagConstants.FOXML_DIR);
		foxmlDir.mkdir();
		File eventsDir = new File(getBagDirectory(), BagConstants.EVENTS_DIR);
		File modsDir = new File(getBagDirectory(), BagConstants.DESCRIPTION_DIR);
		File dcDir = new File(getBagDirectory(), BagConstants.DUBLINCORE_DIR);
		Property hasModel = m.getProperty(ContentModelHelper.FedoraProperty.hasModel.getURI().toString());
		Resource container = m.getProperty(ContentModelHelper.Model.CONTAINER.getURI().toString());
		Property originalDeposit = m.getProperty(ContentModelHelper.Relationship.originalDeposit.getURI().toString());
		Property sourceData = m.getProperty(ContentModelHelper.CDRProperty.sourceData.getURI().toString());
		NodeIterator ni = m.listObjectsOfProperty(contains);
		while(ni.hasNext()) { // all object content is contained, except deposit record
			Resource o = ni.nextNode().asResource();
			PID p = new PID(o.getURI());
			Document foxml = FOXMLJDOMUtil.makeFOXMLDocument(p.getPid());
			Model relsExt = ModelFactory.createDefaultModel();

			// copy Fedora and CDR property statements
			StmtIterator properties = o.listProperties();
			while(properties.hasNext()) {
				Statement s = properties.nextStatement();
				if(copyPropertyURIs.contains(s.getPredicate().getURI())) {
					relsExt.add(s);
				} else if(/* TODO is a role property*/true ) {
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
			
			// TODO copy role statements to relsExt			
			// TODO translate supplemental this is already copied, check it
			// TODO translate default web object, already copied, check it
			
			// add DATA_FILE
			Property fileLocation = m.createProperty(BagConstants.FILE_LOCATOR_URI);
			if(o.hasProperty(fileLocation)) {
				String href = o.getProperty(fileLocation).getString();
				Property hasMimetype = m.createProperty(CDRProperty.hasSourceMimeType.getURI().toString());
				Property hasChecksum = m.createProperty(CDRProperty.hasChecksum.getURI().toString());
				String mimeType = o.getProperty(hasMimetype).toString();
				String md5checksum = o.getProperty(hasChecksum).toString();
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

}
