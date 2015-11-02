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
import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.EPDCX_NS;
import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.METS_NS;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;

import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.input.sax.XMLReaders;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jdom2.transform.JDOMResult;
import org.jdom2.transform.JDOMSource;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Bag;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.util.ContentModelHelper.CDRProperty;
import edu.unc.lib.dl.util.ContentModelHelper.DepositRelationship;
import edu.unc.lib.dl.util.DepositConstants;
import edu.unc.lib.dl.util.PackagingType;
import edu.unc.lib.dl.util.PremisEventLogger.Type;
import edu.unc.lib.dl.xml.JDOMNamespaceUtil;
import edu.unc.lib.dl.xml.METSProfile;

/**
 * @author bbpennel
 * @date Oct 28, 2015
 */
public class BioMedToN3BagJob extends AbstractMETS2N3BagJob {
	
	private static final Logger log = LoggerFactory.getLogger(BioMedToN3BagJob.class);

	private static final String fLocatHrefPath =
			"/m:mets/m:fileSec/m:fileGrp/m:file[@ID = '%s']/m:FLocat/@xlink:href";
	private static final Pattern mainArticlePattern = Pattern.compile(".*\\_Article\\_.*\\.[pP][dD][fF]");
	
	private Transformer epdcx2modsTransformer = null;
	
	public BioMedToN3BagJob(String uuid, String depositUUID) {
		super(uuid, depositUUID);
	}

	public Transformer getEpdcx2modsTransformer() {
		return epdcx2modsTransformer;
	}

	public void setEpdcx2modsTransformer(Transformer epdcx2modsTransformer) {
		this.epdcx2modsTransformer = epdcx2modsTransformer;
	}
	
	@Override
	public void runJob() {
		validateMETS();

		// Store a reference to the manifest file
		addManifestURI();

		validateProfile(METSProfile.DSPACE_SIP);
		Document mets = loadMETS();
		assignPIDs(mets); // assign any missing PIDs
		saveMETS(mets); // manifest updated to have record of all PIDs

		Model model = getWritableModel();
		METSHelper helper = new METSHelper(mets);

		// deposit RDF bag
		Bag top = model.createBag(getDepositPID().getURI().toString());
		// add aggregate work bag
		Element aggregateEl = helper.mets.getRootElement().getChild("structMap", METS_NS).getChild("div", METS_NS);
		
		List<Element> topChildren = new ArrayList<>();
		String metadataFileName = retrieveChildrenMinusMetadata(aggregateEl, helper.mets, topChildren);
		
		Resource rootResource = constructResources(model, aggregateEl, topChildren, helper);
		top.add(rootResource);
		
		if (topChildren.size() > 1) {
			setDefaultWebObject(model, model.getBag(rootResource));
		}

		extractEPDCX(helper.mets, rootResource);
		
		try {
			addSourceMetadata(model, rootResource, metadataFileName);
		} catch (JDOMException | IOException e) {
			failJob(e, "Failed to add source metadata.");
		}

		recordDepositEvent(Type.NORMALIZATION, "Normalized deposit package from {0} to {1}", PackagingType.METS_DSPACE_SIP_1.getUri(), PackagingType.BAG_WITH_N3.getUri());
	}
	
	private String retrieveChildrenMinusMetadata(Element aggregateEl, Document mets, List<Element> topChildren) {
		XPathFactory xFactory = XPathFactory.instance();
		String metadataFileName = null;
		
		// Get the list of children minus the metadata document if it exists
		for (Element child : aggregateEl.getChildren("div", METS_NS)) {
			// Detect the metadata file if it has not already been located
			if (metadataFileName == null) {
				// Find the filename for current div
				String fileId = child.getChild("fptr", METS_NS).getAttributeValue("FILEID");
				XPathExpression<Attribute> xPath = xFactory.compile(String.format(fLocatHrefPath, fileId),
						Filters.attribute(), null, METS_NS, JDOMNamespaceUtil.XLINK_NS);
				String fileName = xPath.evaluateFirst(mets).getValue();
				
				// Is it the metadata document?
				if (fileName.endsWith(".xml.Meta")) {
					// Capture reference to the xml document
					metadataFileName = fileName;
					continue;
				}
			}
			
			// Add all other children to the list
			topChildren.add(child);
		}
		
		return metadataFileName;
	}
	
	private Resource constructResources(Model model, Element aggregateEl, List<Element> topChildren, METSHelper helper) {
		Property hasModel = model.createProperty(ContentModelHelper.FedoraProperty.hasModel.getURI().toString());
		Property fileLocation = model.createProperty(DepositRelationship.stagingLocation.toString());
		
		if (topChildren.size() == 1) {
			Resource rootResource = model.createResource(METSHelper.getPIDURI(topChildren.get(0)));
			model.add(rootResource, hasModel, model.createResource(ContentModelHelper.Model.SIMPLE.getURI().toString()));
			
			helper.addFileAssociations(model, true);
			
			// Move properties for data to the root resource
			String location = rootResource.getProperty(fileLocation).getString();
			String filename = location.substring("data/".length()).toLowerCase();
			model.add(rootResource, dprop(model, DepositRelationship.label), filename);
			return rootResource;
		}
		
		Bag rootObject = model.createBag(METSHelper.getPIDURI(aggregateEl));
		
		model.add(rootObject, hasModel, model.createResource(ContentModelHelper.Model.CONTAINER.getURI().toString()));
		model.add(rootObject, hasModel, model.createResource(ContentModelHelper.Model.AGGREGATE_WORK.getURI().toString()));
		
		for (Element childEl : topChildren) {
			Resource child = model.createResource(METSHelper.getPIDURI(childEl));
			rootObject.add(child);
		}
		
		helper.addFileAssociations(model, true);
		
		// Add labels to aggregate children
		NodeIterator children = rootObject.iterator();
		try {
			while (children.hasNext()) {
				Resource child = children.nextNode().asResource();
				String location = child.getProperty(fileLocation).getString();
				String filename = location.substring("data/".length()).toLowerCase();
				model.add(child, dprop(model, DepositRelationship.label), filename);
			}
		} finally {
			children.close();
		}
		
		return rootObject;
	}
	
	private void extractEPDCX(Document mets, Resource rootResource) {
	// extract EPDCX from mets
		FileOutputStream fos = null;
		try {
			Element epdcxEl = mets.getRootElement().getChild("dmdSec", METS_NS).getChild("mdWrap", METS_NS)
					.getChild("xmlData", METS_NS).getChild("descriptionSet", EPDCX_NS);
			
			JDOMResult mods = new JDOMResult();
			epdcx2modsTransformer.transform(new JDOMSource(epdcxEl), mods);
			final File modsFolder = getDescriptionDir();
			modsFolder.mkdir();
			File modsFile = new File(modsFolder, new PID(rootResource.getURI()).getUUID()+".xml");
			fos = new FileOutputStream(modsFile);
			new XMLOutputter(Format.getPrettyFormat()).output(mods.getDocument(), fos);
		} catch(NullPointerException ignored) {
			log.debug("NPE", ignored);
			// no embedded metadata
		} catch (TransformerException | IOException e) {
			failJob(e, "Failed during transform of EPDCX to MODS.");
		}
	}
	
	private void addSourceMetadata(Model model, Resource rootResource, String metadataFileName)
			throws JDOMException, IOException {
		if (metadataFileName == null) {
			return;
		}
		
		PID sourceMDPID = new PID(rootResource.getURI() + "/" + MD_SOURCE.getName());
		Resource sourceMDResource = model.createResource(sourceMDPID.getURI());
		model.add(rootResource, dprop(model, hasDatastream), sourceMDResource);
		model.add(rootResource, cdrprop(model, sourceMetadata), sourceMDResource);

		model.add(sourceMDResource, dprop(model, stagingLocation),
				this.getDataDirectory().getName() + "/" + metadataFileName);
		model.add(rootResource, cdrprop(model, hasSourceMetadataProfile), BIOMED_ARTICLE);
		model.add(sourceMDResource, dprop(model, mimetype), "text/xml");
		
		File modsFile = new File(getDescriptionDir(), new PID(rootResource.getURI()).getUUID() + ".xml");

		SAXBuilder sb = new SAXBuilder(XMLReaders.NONVALIDATING);
		sb.setFeature("http://xml.org/sax/features/validation", false);
		sb.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
		sb.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
		
		Document existingModsDocument = null;
		// Start from an existing MODS document if there is one
		if (modsFile.exists()) {
			existingModsDocument = sb.build(modsFile);
		} else {
			// Make sure the description directory exists since there was no MODS doc
			File descriptionDir = new File(getDepositDirectory(), DepositConstants.DESCRIPTION_DIR);
			if (!descriptionDir.exists())
				descriptionDir.mkdir();
		}
		
		Document metadataDocument = sb.build(new File(this.getDataDirectory(), metadataFileName));
		BioMedArticleHelper biohelper = new BioMedArticleHelper();
		Document mods = biohelper.extractMODS(metadataDocument, existingModsDocument);
		
		// Output the new MODS file, overwriting the existing one if it was present
		try (FileOutputStream out = new FileOutputStream(modsFile, false)) {
			new XMLOutputter(Format.getPrettyFormat()).output(mods, out);
		}
	}
	
	private void setDefaultWebObject(Model model, Bag rootObject) {
		
		Property fileLocation = model.createProperty(ContentModelHelper.DepositRelationship.stagingLocation.toString());
		
		// Find the main article file
		for (NodeIterator children = rootObject.iterator(); children.hasNext();) {
			Resource child = children.nextNode().asResource();
			String location = child.getProperty(fileLocation).getString();
			// filename will be the article ID, but not XML
			if (!mainArticlePattern.matcher(location).matches()) {
				continue;
			}

			log.debug("Found primary Biomed content document {}", location);
			// If this is a main object, then designate it as a default web object for its parent container
			Property defaultObject = model.getProperty(CDRProperty.defaultWebObject.getURI().toString());
			model.add(rootObject, defaultObject, child);
			return;
		}
	}
}
