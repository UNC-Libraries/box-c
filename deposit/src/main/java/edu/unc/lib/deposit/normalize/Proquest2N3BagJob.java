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

import static edu.unc.lib.dl.util.MetadataProfileConstants.PROQUEST_ETD;
import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.MODS_V3_NS;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Bag;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jdom2.transform.JDOMResult;
import org.jdom2.transform.JDOMSource;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.UriUtils;

import edu.unc.lib.deposit.work.AbstractDepositJob;
import edu.unc.lib.dl.event.PremisLogger;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.rdf.CdrAcl;
import edu.unc.lib.dl.rdf.CdrDeposit;
import edu.unc.lib.dl.rdf.Premis;
import edu.unc.lib.dl.util.DateTimeUtil;
import edu.unc.lib.dl.util.PackagingType;
import edu.unc.lib.dl.util.SoftwareAgentConstants.SoftwareAgent;
import edu.unc.lib.dl.util.ZipFileUtil;

/**
 * Normalizes a Proquest ETD deposit object into an N3 deposit structure.
 *
 * Expects to receive a deposit directory with the data directory containing the already expanded contents of the
 * Proquest package.
 *
 * @author bbpennel
 * @date Apr 23, 2014
 */
public class Proquest2N3BagJob extends AbstractDepositJob {

    private static final Logger log = LoggerFactory.getLogger(Proquest2N3BagJob.class);

    public static final String DATA_SUFFIX = "_DATA.xml";

    private Transformer proquest2ModsTransformer = null;

    public Proquest2N3BagJob() {
    }

    public Proquest2N3BagJob(String uuid, String depositUUID) {
        super(uuid, depositUUID);
    }

    @Override
    public void runJob() {

        unzipPackages();

        // deposit RDF bag
        PID depositPID = getDepositPID();
        Model model = getWritableModel();
        Bag depositBag = model.createBag(depositPID.getURI().toString());

        File[] packageDirs = this.getDataDirectory().listFiles();
        for (File packageDir : packageDirs) {
            if (packageDir.isDirectory()) {
                normalizePackage(packageDir, model, depositBag);
            }
        }

        // Add normalization event to deposit record
        PremisLogger premisDepositLogger = getPremisLogger(depositPID);
        Resource premisDepositEvent = premisDepositLogger.buildEvent(Premis.Normalization)
                .addEventDetail("Normalized deposit package from {0} to {1}",
                        PackagingType.PROQUEST_ETD.getUri(), PackagingType.BAG_WITH_N3.getUri())
                .addSoftwareAgent(SoftwareAgent.depositService.getFullname())
                .create();
        premisDepositLogger.writeEvent(premisDepositEvent);
    }

    private void normalizePackage(File packageDir, Model model, Bag depositBag) {

        // Generate a uuid for the main object
        PID mainPID = pidMinter.mintContentPid();
        Resource mainResource;

        // Identify the important files from the deposit
        File dataFile = null;
        File contentFile = null;
        File attachmentDir = null;

        File[] files = packageDir.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                attachmentDir = file;
            } else if (file.getName().endsWith(DATA_SUFFIX)) {
                dataFile = file;
            } else {
                contentFile = file;
            }
        }

        long lastModified = -1;
        File zipFile = new File(packageDir.getAbsolutePath() + ".zip");
        try (ZipFile zip = new ZipFile(zipFile)) {
            ZipArchiveEntry entry = zip.getEntry(contentFile.getName());
            lastModified = entry.getTime();
        } catch (IOException e) {
            log.error("Failed to read zip file located at {}.zip", packageDir.getAbsolutePath(), e);
        }

        if (lastModified == -1) {
            lastModified = zipFile.lastModified();
        }

        DateTime modified = new DateTime(lastModified, DateTimeZone.UTC);

        // Deserialize the data document
        SAXBuilder builder = new SAXBuilder();
        Element dataRoot = null;
        Document mods = null;
        try {

            Document dataDocument = builder.build(dataFile);
            dataRoot = dataDocument.getRootElement();

            // Transform the data into MODS and store it to its final resting place
            mods = extractMods(mainPID, dataRoot, modified);
        } catch (TransformerException e) {
            failJob(e, "Failed to transform metadata to MODS.");
        } catch (Exception e) {
            failJob(e, "Unable to deserialize the metadata file.");
        }

        // Detect if there are any attachments
        List<?> attachmentElements = dataRoot.getChild("DISS_content").getChildren("DISS_attachment");

        String title = mods.getRootElement().getChild("titleInfo", MODS_V3_NS).getChildText("title", MODS_V3_NS);

        mainResource = populateWork(model, mainPID, attachmentElements, attachmentDir, contentFile, title);
        // Store main resource as child of the deposit
        depositBag.add(mainResource);

        // Add the data file as a metadata datastream of the main object
        setSourceMetadata(model, mainResource, dataFile);

        // Capture other metadata, like embargoes
        setEmbargoUntil(model, mainResource, dataRoot);
    }

    private void unzipPackages() {
        File dataDirectory = this.getDataDirectory();
        File zipFiles[] = dataDirectory.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File directory, String fileName) {
                return fileName.endsWith(".zip");
            }
        });

        for (File packageFile : zipFiles) {
            try {
                String packageDir = packageFile.getName();
                packageDir = packageDir.substring(0, packageDir.length() - 4);
                ZipFileUtil.unzipToDir(packageFile, new File(dataDirectory, packageDir));
            } catch (IOException e) {
                throw new Error("Unable to unpack your deposit: " + getDepositPID().getUUID(), e);
            }
        }
    }

    /**
     * Transform the given root element from the data document into MODS and stores it as the metadata for the object
     * being ingested
     *
     * @param mainPID
     * @param dataRoot
     * @param modified
     * @throws TransformerException
     * @throws FileNotFoundException
     * @throws IOException
     */
    private Document extractMods(PID mainPID, Element dataRoot, DateTime modified) throws TransformerException,
            FileNotFoundException,
            IOException {

        int month = modified.getMonthOfYear();
        String gradSemester;

        if (month >= 2 && month <= 6) {
            gradSemester = "Spring";
        } else if (month >= 7 && month <= 9) {
            gradSemester = "Summer";
        } else {
            gradSemester = "Winter";
        }

        JDOMResult mods = new JDOMResult();
        // Transform the metadata into MODS
        synchronized (proquest2ModsTransformer) {
            proquest2ModsTransformer.setParameter("graduationSemester", gradSemester + " " + modified.getYear());
            proquest2ModsTransformer.transform(new JDOMSource(dataRoot), mods);
        }

        // Create the description folder and write the MODS out to it
        final File modsFolder = getDescriptionDir();
        modsFolder.mkdir();

        File modsFile = new File(modsFolder, mainPID.getUUID() + ".xml");

        try (FileOutputStream fos = new FileOutputStream(modsFile)) {
            new XMLOutputter(Format.getPrettyFormat()).output(mods.getDocument(), fos);
        }

        return mods.getDocument();
    }

    private Resource populateWork(Model model, PID mainPID, List<?> attachmentElements, File attachmentDir,
            File contentFile, String title) {

        // Create the main resource as a bag
        Bag mainBag = model.createBag(mainPID.getURI());

        model.add(mainBag, RDF.type, Cdr.Work);
        // Assign title to the main object as a label
        if (title.length() > 128) {
            model.add(mainBag, CdrDeposit.label, title.substring(0, 128));
        } else {
            model.add(mainBag, CdrDeposit.label, title);
        }

        // Create primary object child entry for the main document
        PID primaryObjectPID = pidMinter.mintContentPid();
        Resource primaryObjectResource = model.createResource(primaryObjectPID.getURI());
        mainBag.add(primaryObjectResource);

        // Store the main content on the child
        model.add(primaryObjectResource, RDF.type, Cdr.FileObject);
        model.add(primaryObjectResource, CdrDeposit.label, contentFile.getName());
        model.add(primaryObjectResource, CdrDeposit.stagingLocation, getRelativePath(contentFile));

        // Store reference to content as the primary object
        model.add(mainBag, Cdr.primaryObject, primaryObjectResource);

        // Add the attachments as supplemental files
        for (Object attachmentObj : attachmentElements) {
            Element attachEl = (Element) attachmentObj;

            String filename = attachEl.getChildText("DISS_file_name");
            String description = attachEl.getChildText("DISS_file_descr");

            // Make the child entry with a new uuid
            PID pid = pidMinter.mintContentPid();
            Resource child = model.createResource(pid.getURI());
            mainBag.add(child);
            model.add(child, RDF.type, Cdr.FileObject);

            // Use the description as a label if one was provided
            if (description != null && description.trim().length() > 0) {
                model.add(child, CdrDeposit.label, description);
            } else {
                model.add(child, CdrDeposit.label, filename);
            }

            // Link the file to the child entry
            model.add(child, CdrDeposit.stagingLocation, getRelativePath(new File(attachmentDir, filename)));
        }

        return mainBag;
    }

    private void setSourceMetadata(Model model, Resource mainResource, File dataFile) {
        // Add the data file as a metadata datastream of the main object
        PID sourceMDPID = pidMinter.mintContentPid();
        Resource sourceMDResource = model.createResource(sourceMDPID.getURI());
        model.add(mainResource, CdrDeposit.hasDatastream, sourceMDResource);
        model.add(mainResource, CdrDeposit.hasSourceMetadata, sourceMDResource);

        model.add(sourceMDResource, CdrDeposit.stagingLocation, getRelativePath(dataFile));
        model.add(mainResource, Cdr.hasSourceMetadataProfile, PROQUEST_ETD);
        model.add(sourceMDResource, CdrDeposit.mimetype, "text/xml");
    }

    private void setEmbargoUntil(Model model, Resource mainResource, Element dataRoot) {

        String embargoCode = dataRoot.getAttributeValue("embargo_code");
        log.debug("Deposit {} has embargo code of {}", getDepositPID(), embargoCode);

        if (embargoCode != null) {

            DateTime currentDate = new DateTime();

            // Get the completion year and create a date time out of the end of the year,
            // to make the most generous embargo possible
            String compDateString = dataRoot.getChild("DISS_description")
                    .getChild("DISS_dates").getChildText("DISS_comp_date");
            DateTime compDate = new DateTime(Integer.parseInt(compDateString), 12, 31, 0, 0, 0, 0);

            // Embargo start time is the lowest of either the current date or the completion date
            DateTime embargoEnd = currentDate.compareTo(compDate) < 0 ? currentDate : compDate;

            if ("2".equals(embargoCode)) {
                embargoEnd = embargoEnd.plusYears(1);
            } else if ("3".equals(embargoCode) || "4".equals(embargoCode)) {
                embargoEnd = embargoEnd.plusYears(2);
            } else {
                embargoEnd = null;
            }
            log.debug("Setting embargo end date of {} for deposit {}", embargoEnd, getDepositPID());

            // If the embargo end date isn't coming from comp_date then make sure it hasn't already expired
            if (embargoEnd != null && embargoEnd != currentDate && embargoEnd.compareTo(currentDate) < 0) {
                // Embargo has already expired, no need to set it
                embargoEnd = null;
            }

            // Add the embargo end date as a triple
            if (embargoEnd != null) {
                model.add(mainResource, CdrAcl.embargoUntil,
                        DateTimeUtil.utcYMDFormatter.print(embargoEnd) + "T00:00:00",
                        XSDDatatype.XSDdateTime);
            }
        }
    }

    private String getRelativePath(File file) {
        try {
            return UriUtils.encodePath(
                    file.getAbsolutePath().substring(getDepositDirectory().getAbsolutePath().length() + 1), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            log.error("Failed to encode file path", e);
            return null;
        }
    }

    public void setProquest2ModsTransformer(Transformer proquest2ModsTransformer) {
        this.proquest2ModsTransformer = proquest2ModsTransformer;
    }
}
