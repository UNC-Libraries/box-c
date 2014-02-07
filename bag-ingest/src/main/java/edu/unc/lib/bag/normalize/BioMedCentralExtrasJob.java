package edu.unc.lib.bag.normalize;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.apache.commons.io.IOUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.filter.Filter;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.BagConstants;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.util.PremisEventLogger.Type;
import edu.unc.lib.workers.AbstractBagJob;
import gov.loc.repository.bagit.Bag;

public class BioMedCentralExtrasJob extends AbstractBagJob implements Runnable {
	private static final Logger log = LoggerFactory.getLogger(BioMedCentralExtrasJob.class);
	
	public BioMedCentralExtrasJob() {
		super();
	}
	public BioMedCentralExtrasJob(String uuid, String bagDirectory, String depositId) {
		super(uuid, bagDirectory, depositId);
	}
	
	@Override
	public void run() {
		log.debug("starting on {}", getBagDirectory());
		Bag bag = loadBag();
		log.debug("loaded bag {}", getBagDirectory());
		Model model = ModelFactory.createDefaultModel();
		File modelFile = new File(getBagDirectory(), BagConstants.MODEL_FILE);
		model.read(modelFile.toURI().toString());
		log.debug("loaded RDF model {}", modelFile);
		
		// top level object must be aggregate
		com.hp.hpl.jena.rdf.model.Bag deposit = model.getBag(getDepositPID().getURI());
		Property hasModel = model.createProperty(ContentModelHelper.FedoraProperty.hasModel.getURI().toString());
		Resource aggregateModel = model.createProperty(ContentModelHelper.Model.AGGREGATE_WORK.getURI().toString());
		Property fileLocation = model.createProperty(BagConstants.FILE_LOCATOR_URI);
		com.hp.hpl.jena.rdf.model.Bag aggregate = null;
		try {
			NodeIterator ni = model.getBag(deposit).iterator();
			aggregate = model.getBag(ni.next().asResource());
			ni.close();
			if(!model.contains(aggregate, hasModel, aggregateModel)) {
				failJob(Type.NORMALIZATION, "Cannot find aggregate work", null);
			}
		} catch(NoSuchElementException e) {
			failJob(e, Type.NORMALIZATION, "Cannot find top contents of deposit");
		}
		log.debug("identified aggregate {}", aggregate);
		
		// structure aggregate work, find the article XML
		String articleXMLPath = null;
		for(NodeIterator children = aggregate.iterator(); children.hasNext();) {
			Resource child = children.nextNode().asResource();
			//Resource file = child.getProperty(sourceData).getResource();
			String location = child.getProperty(fileLocation).getString();
			log.debug("examining child location {}", location);
			if(location.matches("data/[\\w\\-]+\\-S\\d+\\.\\w+$")) continue;
			if(location.matches("data/[\\w\\-]+\\.[xX][mM][lL]$")) {
				log.debug("Found primary Biomed XML document {}", location);
				articleXMLPath = location;
				Property allowIndexing = model.getProperty(ContentModelHelper.CDRProperty.allowIndexing.getURI().toString());
				model.add(child, allowIndexing, "no");
			} else {
				log.debug("Found primary Biomed content document {}", location);
				// If this is a main object, then designate it as a default web object for its parent container
				Property defaultObject = model.getProperty(ContentModelHelper.CDRProperty.defaultWebObject.getURI().toString());
				model.add(aggregate, defaultObject, child);
			}
		}
		
		if(articleXMLPath != null) {
			File articleXMLFile = new File(getBagDirectory(), articleXMLPath);
			PID aggregatePID = new PID(aggregate.getURI());
			File modsFile = new File(getDescriptionDir(), aggregatePID.getUUID()+".xml");
			SAXBuilder sb = new SAXBuilder();
			FileOutputStream out = null;
			Map<String, String> fileLC2supplementLabels = null;
			try {
				Document articleDocument = sb.build(articleXMLFile);
				Document existingMODSDocument = null;
				if(modsFile.exists()) existingMODSDocument = sb.build(modsFile);
				BioMedArticleHelper biohelper = new BioMedArticleHelper();
				Document mods = biohelper.extractMODS(articleDocument);
				
				List<Element> moving = new ArrayList<Element>();
				if(existingMODSDocument != null) {
					for(Object o : existingMODSDocument.getRootElement().getContent(new Filter() {
						private static final long serialVersionUID = 1L;
						@Override
						public boolean matches(Object obj) {
							if(!(obj instanceof Element)) return false;
							return !"name".equals(((Element)obj).getName());
						}})) moving.add((Element)o);
				}
				for(Element el : moving) mods.getRootElement().addContent(el.detach());
				// overwrite MODS XML
				out = new FileOutputStream(modsFile); 
				new XMLOutputter(Format.getPrettyFormat()).output(mods, out);
				fileLC2supplementLabels = biohelper.getFilesLC2SupplementLabels(articleDocument);
			} catch (Exception e) {
				failJob(e, Type.NORMALIZATION, "Cannot extract metadata from BioMed Central article XML.");
			} finally {
				IOUtils.closeQuietly(out);
			}
			
			if(fileLC2supplementLabels != null) {
				for(NodeIterator children = aggregate.iterator(); children.hasNext();) {
					Resource child = children.nextNode().asResource();
					String location = child.getProperty(fileLocation).getString();
					String filename = location.substring("data/".length()).toLowerCase();
					if(fileLC2supplementLabels.containsKey(filename)) {
						Property label = model.createProperty(ContentModelHelper.FedoraProperty.label.getURI().toString()); 
						model.add(child, label, fileLC2supplementLabels.get(filename));
					}
				}
			}
		}
		
		saveModel(model, BagConstants.MODEL_FILE);
		recordDepositEvent(Type.NORMALIZATION, "Normalized BioMed Central article as aggregate with extracted description");
		saveBag(bag);
	}

}
