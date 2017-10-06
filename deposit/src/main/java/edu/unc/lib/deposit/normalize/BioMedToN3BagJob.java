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

import static edu.unc.lib.dl.util.MetadataProfileConstants.BIOMED_ARTICLE;
import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.EPDCX_NS;
import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.METS_NS;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;

import org.apache.jena.rdf.model.Bag;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
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

import edu.unc.lib.dl.event.PremisLogger;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.rdf.CdrDeposit;
import edu.unc.lib.dl.rdf.Premis;
import edu.unc.lib.dl.util.DepositConstants;
import edu.unc.lib.dl.util.PackagingType;
import edu.unc.lib.dl.util.SoftwareAgentConstants.SoftwareAgent;
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

        PID depositPID = getDepositPID();
        PremisLogger premisDepositLogger = getPremisLogger(depositPID);
        Resource premisDepositEvent = premisDepositLogger.buildEvent(Premis.Normalization)
                .addEventDetail("Normalized deposit package from {0} to {1}",
                        PackagingType.METS_DSPACE_SIP_1.getUri(), PackagingType.BAG_WITH_N3.getUri())
                .addSoftwareAgent(SoftwareAgent.depositService.getFullname())
                .create();
        premisDepositLogger.writeEvent(premisDepositEvent);
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

    private Resource constructResources(Model model, Element aggregateEl,
            List<Element> topChildren, METSHelper helper) {

        if (topChildren.size() == 1) {
            Resource rootResource = model.createResource(METSHelper.getPIDURI(topChildren.get(0)));
            model.add(rootResource, RDF.type, Cdr.FileObject);

            helper.addFileAssociations(model, true);

            // Move properties for data to the root resource
            String location = rootResource.getProperty(CdrDeposit.stagingLocation).getString();
            String filename = location.substring("data/".length()).toLowerCase();
            model.add(rootResource, CdrDeposit.label, filename);
            return rootResource;
        }

        Bag rootObject = model.createBag(METSHelper.getPIDURI(aggregateEl));

        model.add(rootObject, RDF.type, Cdr.Work);

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
                String location = child.getProperty(CdrDeposit.stagingLocation).getString();
                String filename = location.substring("data/".length()).toLowerCase();
                model.add(child, CdrDeposit.label, filename);
            }
        } finally {
            children.close();
        }

        return rootObject;
    }

    private void extractEPDCX(Document mets, Resource rootResource) {
        // extract EPDCX from mets
        Document modsDoc = null;
        try {
            Element epdcxEl = getEpcdxElement(mets);
            if (epdcxEl == null) {
                log.debug("No EPDCX metadata provided in package for {}", rootResource.getURI());
                return;
            }

            JDOMResult mods = new JDOMResult();
            epdcx2modsTransformer.transform(new JDOMSource(epdcxEl), mods);
            modsDoc = mods.getDocument();
        } catch (TransformerException e) {
            failJob(e, "Failed during transform of EPDCX to MODS.");
        }

        final File modsFolder = getDescriptionDir();
        modsFolder.mkdir();
        File modsFile = new File(modsFolder, PIDs.get(rootResource.getURI()).getUUID() + ".xml");
        try (OutputStream fos = new FileOutputStream(modsFile)) {
            new XMLOutputter(Format.getPrettyFormat()).output(modsDoc, fos);
        } catch (IOException e) {
            failJob(e, "Failed to write transformed EPDCX to MODS at path {}", modsFile.getAbsolutePath());
        }
    }

    private Element getEpcdxElement(Document mets) {
        Element dmdEl = mets.getRootElement().getChild("dmdSec", METS_NS);
        if (dmdEl == null) {
            return null;
        }
        Element mdWrapEl = dmdEl.getChild("mdWrap", METS_NS);
        if (mdWrapEl == null) {
            return null;
        }

        Element xmlDataEl = mdWrapEl.getChild("xmlData", METS_NS);
        if (xmlDataEl == null) {
            return null;
        }

        Element descEl = xmlDataEl.getChild("descriptionSet", EPDCX_NS);
        return descEl;
    }

    private void addSourceMetadata(Model model, Resource rootResource, String metadataFileName)
            throws JDOMException, IOException {
        if (metadataFileName == null) {
            return;
        }

        PID sourceMDPID = pidMinter.mintContentPid();
        Resource sourceMDResource = model.createResource(sourceMDPID.getURI());
        model.add(rootResource, CdrDeposit.hasDatastream, sourceMDResource);
        model.add(rootResource, CdrDeposit.hasSourceMetadata, sourceMDResource);

        model.add(sourceMDResource, CdrDeposit.stagingLocation,
                this.getDataDirectory().getName() + "/" + metadataFileName);
        model.add(rootResource, Cdr.hasSourceMetadataProfile, BIOMED_ARTICLE);
        model.add(sourceMDResource, CdrDeposit.mimetype, "text/xml");

        File modsFile = new File(getDescriptionDir(), PIDs.get(rootResource.getURI()).getUUID() + ".xml");

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
            if (!descriptionDir.exists()) {
                descriptionDir.mkdir();
            }
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

        NodeIterator children = rootObject.iterator();
        try {
            // Find the main article file
            while (children.hasNext()) {
                Resource child = children.nextNode().asResource();
                String location = child.getProperty(CdrDeposit.stagingLocation).getString();
                // filename will be the article ID, but not XML
                if (!mainArticlePattern.matcher(location).matches()) {
                    continue;
                }

                log.debug("Found primary Biomed content document {}", location);
                model.add(rootObject, Cdr.primaryObject, child);
                return;
            }
        } finally {
            children.close();
        }
    }
}
