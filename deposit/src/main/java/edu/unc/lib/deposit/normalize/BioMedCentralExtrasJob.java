package edu.unc.lib.deposit.normalize;

import static edu.unc.lib.deposit.work.DepositGraphUtils.cdrprop;
import static edu.unc.lib.deposit.work.DepositGraphUtils.dprop;
import static edu.unc.lib.dl.util.ContentModelHelper.CDRProperty.hasSourceMetadataProfile;
import static edu.unc.lib.dl.util.ContentModelHelper.CDRProperty.sourceMetadata;
import static edu.unc.lib.dl.util.ContentModelHelper.Datastream.MD_SOURCE;
import static edu.unc.lib.dl.util.ContentModelHelper.DepositRelationship.hasDatastream;
import static edu.unc.lib.dl.util.ContentModelHelper.DepositRelationship.mimetype;
import static edu.unc.lib.dl.util.ContentModelHelper.DepositRelationship.stagingLocation;
import static edu.unc.lib.dl.util.MetadataProfileConstants.BIOMED_ARTICLE;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.filter.Filter;
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
import com.hp.hpl.jena.rdf.model.StmtIterator;

import edu.unc.lib.deposit.work.AbstractDepositJob;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.util.DepositConstants;
import edu.unc.lib.dl.util.PremisEventLogger.Type;

/**
 * Performs extra processing needed to prepare BioMed Central articles for ingest, such as extraction of labels for
 * supplemental files and other descriptive metadata from the article XML document
 *
 * @author count0
 * @author bbpennel
 * @date Jun 18, 2014
 */
public class BioMedCentralExtrasJob extends AbstractDepositJob implements Runnable {
	private static final Logger log = LoggerFactory.getLogger(BioMedCentralExtrasJob.class);

	public BioMedCentralExtrasJob() {
		super();
	}
	public BioMedCentralExtrasJob(String uuid, String depositUUID) {
		super(uuid, depositUUID);
	}

	@Override
	public void run() {
		log.debug("starting on {}", getDepositDirectory());
		Model model = ModelFactory.createDefaultModel();
		File modelFile = new File(getDepositDirectory(), DepositConstants.MODEL_FILE);
		model.read(modelFile.toURI().toString());
		log.debug("loaded RDF model {}", modelFile);

		// top level object must be aggregate
		Bag deposit = model.getBag(getDepositPID().getURI());
		Property hasModel = model.createProperty(ContentModelHelper.FedoraProperty.hasModel.getURI().toString());
		Resource aggregateModel = model.createProperty(ContentModelHelper.Model.AGGREGATE_WORK.getURI().toString());
		Property fileLocation = model.createProperty(ContentModelHelper.DepositRelationship.stagingLocation.toString());

		// Find the aggregate objects resource
		Bag aggregate = null;
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
			String location = child.getProperty(fileLocation).getString();
			log.debug("examining child location {}", location);
			if(location.matches("data/[\\w\\-]+\\-S\\d+\\.\\w+$")) continue;
			if(location.matches("data/[\\w\\-]+\\.[xX][mM][lL]$")) {
				log.debug("Found primary Biomed XML document {}", location);
				articleXMLPath = location;

				// Remove the article xml as a supplemental file
				child.removeProperties();
				StmtIterator sIt = model.listStatements(aggregate, null, child);
				aggregate.remove(sIt.nextStatement());
				sIt.close();
			} else {
				log.debug("Found primary Biomed content document {}", location);
				// If this is a main object, then designate it as a default web object for its parent container
				Property defaultObject = model.getProperty(ContentModelHelper.CDRProperty.defaultWebObject.getURI().toString());
				model.add(aggregate, defaultObject, child);
			}
		}

		if (articleXMLPath != null) {
			// Assign the article xml as a source metadata datastream
			setSourceMetadata(model, aggregate, articleXMLPath);

			// Build the descriptive MODS document from the article XML and any existing MODS
			File articleXMLFile = new File(getDepositDirectory(), articleXMLPath);
			PID aggregatePID = new PID(aggregate.getURI());
			File modsFile = new File(getDescriptionDir(), aggregatePID.getUUID()+".xml");

			try {
				// Disable DTD validation of the article xml
				SAXBuilder sb = new SAXBuilder(false);
				sb.setValidation(false);
				sb.setFeature("http://xml.org/sax/features/validation", false);
				sb.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
				sb.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

				Document articleDocument = sb.build(articleXMLFile);
				BioMedArticleHelper biohelper = new BioMedArticleHelper();
				Document mods = biohelper.extractMODS(articleDocument);
				Map<String, String> fileLC2supplementLabels = biohelper.getFilesLC2SupplementLabels(articleDocument);

				// Move all elements from the original MODS except names into the new document
				if (modsFile.exists()) {
					Document existingMODSDocument = sb.build(modsFile);

					List<Element> moving = new ArrayList<Element>();
					for (Object o : existingMODSDocument.getRootElement().getContent(new Filter() {
						private static final long serialVersionUID = 1L;

						@Override
						public boolean matches(Object obj) {
							if (!(obj instanceof Element))
								return false;
							return !"name".equals(((Element) obj).getName());
						}
					})) {
						moving.add((Element) o);
					}

					Element modsRoot = mods.getRootElement();
					for (Element el : moving)
						modsRoot.addContent(el.detach());
				} else {
					File descriptionDir = new File(getDepositDirectory(), DepositConstants.DESCRIPTION_DIR);
					if (!descriptionDir.exists())
						descriptionDir.mkdir();
				}

				// Output the new MODS file, overwriting the existing one if it was present
				try (FileOutputStream out = new FileOutputStream(modsFile, false)) {
					new XMLOutputter(Format.getPrettyFormat()).output(mods, out);
				}

				// Label the supplemental files with values from the article xml
				if (fileLC2supplementLabels != null) {
					for (NodeIterator children = aggregate.iterator(); children.hasNext();) {
						Resource child = children.nextNode().asResource();
						String location = child.getProperty(fileLocation).getString();
						String filename = location.substring("data/".length()).toLowerCase();
						if (fileLC2supplementLabels.containsKey(filename)) {
							Property label = model.createProperty(ContentModelHelper.FedoraProperty.label.getURI().toString());
							model.add(child, label, fileLC2supplementLabels.get(filename));
						}
					}
				}
			} catch (Exception e) {
				failJob(e, Type.NORMALIZATION, "Cannot extract metadata from BioMed Central article XML.");
			}
		}

		saveModel(model, DepositConstants.MODEL_FILE);
		recordDepositEvent(Type.NORMALIZATION, "Normalized BioMed Central article as aggregate with extracted description");
	}

	private void setSourceMetadata(Model model, Resource primaryResource, String path) {
		// Add the data file as a metadata datastream of the primary object
		PID sourceMDPID = new PID(primaryResource.getURI() + "/" + MD_SOURCE.getName());
		Resource sourceMDResource = model.createResource(sourceMDPID.getURI());
		model.add(primaryResource, dprop(model, hasDatastream), sourceMDResource);
		model.add(primaryResource, cdrprop(model, sourceMetadata), sourceMDResource);

		model.add(sourceMDResource, dprop(model, stagingLocation), path);
		model.add(primaryResource, cdrprop(model, hasSourceMetadataProfile), BIOMED_ARTICLE);
		model.add(sourceMDResource, dprop(model, mimetype), "text/xml");
	}

}
