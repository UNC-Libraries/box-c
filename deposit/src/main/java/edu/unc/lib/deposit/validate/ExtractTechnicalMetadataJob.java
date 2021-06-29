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
package edu.unc.lib.deposit.validate;

import static edu.unc.lib.boxc.common.xml.SecureXMLFactory.createSAXBuilder;
import static edu.unc.lib.boxc.model.api.rdf.CdrDeposit.mimetype;
import static edu.unc.lib.boxc.model.api.xml.JDOMNamespaceUtil.FITS_NS;
import static edu.unc.lib.boxc.model.api.xml.JDOMNamespaceUtil.PREMIS_V3_NS;
import static edu.unc.lib.boxc.model.api.xml.JDOMNamespaceUtil.XSI_NS;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.newBufferedWriter;
import static org.apache.commons.lang3.StringUtils.substringBeforeLast;
import static org.springframework.util.MimeTypeUtils.APPLICATION_OCTET_STREAM_VALUE;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;

import javax.annotation.PostConstruct;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.MimeTypeUtils;

import com.google.common.base.CharMatcher;

import edu.unc.lib.boxc.common.util.URIUtil;
import edu.unc.lib.deposit.work.AbstractConcurrentDepositJob;
import edu.unc.lib.deposit.work.JobFailedException;
import edu.unc.lib.deposit.work.JobInterruptedException;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.model.DatastreamPids;
import edu.unc.lib.dl.util.MimetypeHelpers;

/**
 * Job which performs technical metadata extraction on binary files included in
 * this deposit and then stores the resulting details in a PREMIS report file
 *
 * @author bbpennel
 *
 */
public class ExtractTechnicalMetadataJob extends AbstractConcurrentDepositJob {
    private static final Logger log = LoggerFactory.getLogger(ExtractTechnicalMetadataJob.class);

    private static final String FITS_SINGLE_STATUS = "SINGLE_RESULT";
    private final static String FITS_EXAMINE_PATH = "examine";

    private CloseableHttpClient httpClient;

    // server path of the FITS application
    private String baseFitsUri;
    // URI to the examine servlet in the FITS application
    private URI fitsExamineUri;

    private boolean processFilesLocally;

    private Model model;

    public ExtractTechnicalMetadataJob(String uuid, String depositUUID) {
        super(uuid, depositUUID);
    }

    @PostConstruct
    public void initJob() {
        init();
        fitsExamineUri = URI.create(URIUtil.join(baseFitsUri, FITS_EXAMINE_PATH));
    }

    @Override
    public void runJob() {
        model = getReadOnlyModel();

        // Get the list of files that need processing
        List<Entry<PID, String>> stagingList = generateStagingLocationsToProcess(model);
        setTotalClicks(stagingList.size());

        startResultRegistrar();

        try {
            for (Entry<PID, String> stagedPair : stagingList) {
                interruptJobIfStopped();

                PID objPid = stagedPair.getKey();

                if (isObjectCompleted(objPid)) {
                    addClicks(1);
                    continue;
                }

                waitForQueueCapacity();

                String stagedPath = stagedPair.getValue();
                PID originalPid = DatastreamPids.getOriginalFilePid(objPid);
                final String providedMimetype = getProvidedMimetype(originalPid, model);

                submitTask(new ExtractTechnicalMetadataRunnable(objPid, originalPid, stagedPath, providedMimetype));
            }

            waitForCompletion();
        } finally {
            awaitRegistrarShutdown();
        }
    }

    private String getProvidedMimetype(PID originalPid, Model model) {
        Resource originalResc = model.getResource(originalPid.getRepositoryPath());
        Statement mimetypeStmt = originalResc.getProperty(mimetype);
        if (mimetypeStmt != null) {
            return MimetypeHelpers.formatMimetype(mimetypeStmt.getString());
        } else {
            return null;
        }
    }

    @Override
    protected void registrationAction() {
        List<Object> results = new ArrayList<>();
        resultsQueue.drainTo(results);
        log.debug("Registering batch of {} transfer results", results.size());
        commit(() -> {
            results.forEach(resultObj -> {
                ExtractTechnicalMetadataResult result = (ExtractTechnicalMetadataResult) resultObj;
                Resource originalResc = model.getResource(result.originalPid.getRepositoryPath());
                if (result.mimetype != null) {
                    if (result.hasProvidedMimetype) {
                        originalResc.removeAll(mimetype);
                    }
                    originalResc.addProperty(mimetype, result.mimetype);
                }
                addClicks(1);
                markObjectCompleted(result.objPid);
            });
        });
    }

    private class ExtractTechnicalMetadataResult {
        private PID objPid;
        private PID originalPid;
        private boolean hasProvidedMimetype;
        private String mimetype;
    }

    private class ExtractTechnicalMetadataRunnable implements Runnable {
        private PID objPid;
        private PID originalPid;
        private String stagedPath;
        private String providedMimetype;
        private ExtractTechnicalMetadataResult result = new ExtractTechnicalMetadataResult();

        public ExtractTechnicalMetadataRunnable(PID objPid, PID originalPid, String stagedPath,
                String providedMimetype) {
            this.objPid = objPid;
            this.originalPid = originalPid;
            this.stagedPath = stagedPath;
            this.providedMimetype = providedMimetype;
            result.objPid = objPid;
            result.originalPid = originalPid;
            result.hasProvidedMimetype = providedMimetype != null;
        }

        @Override
        public void run() {
            if (isInterrupted.get()) {
                return;
            }

            interruptJobIfStopped();

            // Generate the FITS report as a document
            Document fitsDoc = getFitsDocument(objPid, stagedPath);

            try {
                // Create the PREMIS report wrapper for the FITS results
                Document premisDoc = generatePremisReport(objPid, fitsDoc);
                Element premisObjCharsEl = getObjectCharacteristics(premisDoc);

                // Record the format info for this file
                addFileIdentification(fitsDoc, premisObjCharsEl);

                addFileinfoToReport(fitsDoc, premisObjCharsEl);

                addFitsResults(premisDoc, fitsDoc);

                // Store the premis report to file
                writePremisReport(objPid, premisDoc);

                receiveResult(result);
            } catch (JobFailedException | JobInterruptedException e) {
                throw e;
            } catch (Exception e) {
                failJob(e, "Failed to extract FITS details for {0} from document:\n{1}",
                        objPid, getXMLOutputter().outputString(fitsDoc));
            }
        }

        /**
         * Adds format and mimetype information for the give object, to both the
         * PREMIS report and the deposit model to be included as part of the ingest.
         *
         * @param fitsDoc
         * @param premisObjCharsEl
         */
        private void addFileIdentification(Document fitsDoc, Element premisObjCharsEl) {
            // Retrieve the FITS generate mimetype if available
            Element identity = getFitsIdentificationInformation(fitsDoc);

            String fitsMimetype = null;
            String format;
            if (identity != null) {
                fitsMimetype = identity.getAttributeValue("mimetype");
                format = identity.getAttributeValue("format");
            } else {
                format = "Unknown";
                log.warn("FITS unable to conclusively identify file: {}", originalPid);
            }

            // Add format to the premis report
            premisObjCharsEl.addContent(
                    new Element("format", PREMIS_V3_NS)
                        .addContent(new Element("formatDesignation", PREMIS_V3_NS)
                        .addContent(new Element("formatName", PREMIS_V3_NS)
                        .setText(format))));

            // Replace the mimetype registered for this item in the deposit if necessary
            overrideDepositMimetype(fitsMimetype);
        }

        /**
         * Overrides the mimetype for this object in the deposit model when the FITS
         * generated value is preferred.
         *
         * @param objResc
         * @param fitsExtractMimetype
         */
        private void overrideDepositMimetype(String fitsExtractMimetype) {
            String rescId = originalPid.getRepositoryPath();
            // normalize fits mimetype
            final String fitsMimetype = MimetypeHelpers.formatMimetype(fitsExtractMimetype);

            if (fitsMimetype != null && Objects.equals(providedMimetype, fitsMimetype)) {
                log.debug("FITS mimetype and provided mimetype {} agree for {}, skipping override",
                        providedMimetype, rescId);
                return;
            }

            int fitsRank = rankMimetype(fitsMimetype);
            int providedRank = rankMimetype(providedMimetype);

            // No meaningful mimetypes, so remove provided and replace with default
            if (providedRank < 0 && fitsRank < 0) {
                result.mimetype = APPLICATION_OCTET_STREAM_VALUE;
                log.warn("No meaningful mimetype for {}, removed provided value '{}' and added default",
                        rescId, providedMimetype);
                return;
            }

            if (fitsRank >= providedRank) {
                result.mimetype = fitsMimetype;
                log.debug("Overrode provided mimetype {} for {} with extracted mimetype {}",
                        providedMimetype, rescId, fitsMimetype);
            } else {
                log.debug("Retaining provided mimetype {} for {}", providedMimetype, originalPid);
            }
        }
    }

    /**
     * Generate the FITS report for the given object/file and return it as an
     * XML document
     *
     * @param objPid
     * @param stagedUriString
     * @return
     */
    private Document getFitsDocument(PID objPid, String stagedUriString) {
        HttpUriRequest request;
        URI stagedUri = URI.create(stagedUriString);
        Path stagedPath;
        if (!stagedUri.isAbsolute()) {
            stagedPath = Paths.get(getDepositDirectory().toString(), stagedUriString);
            stagedUri = stagedPath.toUri();
        } else {
            stagedPath = Paths.get(stagedUri);
        }

        Path sanitizedPath = null;
        // FITS cannot currently handle file paths that contain unicode characters, so need to upload
        if (processFilesLocally) {
            // Files are available locally to FITS, so just pass along path
            URI fitsUri = null;
            try {
                sanitizedPath = sanitizePath(stagedPath);
                URIBuilder builder = new URIBuilder(fitsExamineUri);
                builder.addParameter("file", (sanitizedPath == null ? stagedPath : sanitizedPath).toString());
                fitsUri = builder.build();

                log.debug("Requesting FITS document for {} using local file via URI {}", objPid, fitsUri);
            } catch (URISyntaxException e) {
                failJob(e, "Failed to construct FITs report uri for {0}", objPid);
            } catch (IOException e) {
                failJob(e, "Failed to create symbolic link to file for extract {0} for {1}", stagedPath, objPid);
            }

            request = new HttpGet(fitsUri);
        } else {
            // Files are to be processed remotely, so upload them via a post request
            HttpEntity entity;
            try {
                entity = MultipartEntityBuilder.create()
                        .addPart("datafile", new InputStreamBody(Files.newInputStream(stagedPath),
                                stagedPath.getFileName().toString()))
                        .build();
            } catch (IOException e) {
                failJob(e, "Unable to read file {0}", stagedPath);
                return null;
            }

            HttpPost postRequest = new HttpPost(fitsExamineUri);
            postRequest.setEntity(entity);
            request = postRequest;

            log.debug("Requesting FITS document for {} using remote file from {}", objPid, stagedPath);
        }

        try (CloseableHttpResponse resp = httpClient.execute(request)) {
            // Write the report response to file
            InputStream respBodyStream = resp.getEntity().getContent();

            if (resp.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                failJob(null, "Failed to retrieve report for {0}, status {1} response {2}",
                        objPid, resp.getStatusLine().getStatusCode(), IOUtils.toString(respBodyStream, UTF_8));
            }

            return createSAXBuilder().build(respBodyStream);
        } catch (IOException | JDOMException e) {
            failJob(e, "Failed to stream report for {0} from server to report document",
                    objPid);
        } finally {
            // Cleanup symbolic link if one was created
            if (sanitizedPath != null) {
                try {
                    Files.deleteIfExists(sanitizedPath);
                } catch (IOException e) {
                    log.warn("Failed to cleanup sanitized path {}: {}", sanitizedPath, e.getMessage());
                }
            }
        }
        return null;
    }

    private Path sanitizePath(Path path) throws IOException {
        if (CharMatcher.ascii().matchesAllOf(path.toString())) {
            return null;
        }
        String ext = FilenameUtils.getExtension(path.getFileName().toString());
        if (!ext.equals("")) {
            ext = "." + ext;
        }
        // Get a temp path for the symbolic link to be created at, using the same extension as the original
        Path linkPath = Files.createTempFile("extract", ext);
        Files.delete(linkPath);
        Files.createSymbolicLink(linkPath, path);
        return linkPath;
    }

    /**
     * Build a list of staging locations in this deposit which need to have FITS
     * reports generated for them. If a report has been previously generated in
     * a resumed deposit, then it will be excluded
     *
     * @param model
     * @return
     */
    private List<Entry<PID, String>> generateStagingLocationsToProcess(Model model) {
        List<Entry<PID, String>> stagingList = getOriginalStagingPairList(model);

        // If the deposit was not resumed, then return list of all staging locations
        boolean resumed = getDepositStatusFactory().isResumedDeposit(getDepositUUID());
        if (!resumed) {
            return stagingList;
        }

        // Get the list of existing techmd reports from previous runs
        File techmdDir = getTechMdDirectory();
        try {
            Files.walk(techmdDir.toPath())
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".xml"))
                    .map(p -> PIDs.get(substringBeforeLast(p.getFileName().toString(), ".")))
                    .forEach(existingPid -> {
                        Iterator<Entry<PID, String>> stagingIt = stagingList.iterator();
                        while (stagingIt.hasNext()) {
                            Entry<PID, String> entry = stagingIt.next();
                            if (entry.getKey().equals(existingPid)) {
                                stagingIt.remove();
                            }
                        }
                    });
        } catch (IOException e) {
            failJob(e, "Failed to list techmd files in {0}", techmdDir);
        }

        return stagingList;
    }

    /**
     * Retrieves the file identification information from the FITS report,
     * resolving conflicts when necessary
     *
     * @param fitsDoc
     * @return
     */
    private Element getFitsIdentificationInformation(Document fitsDoc) {
        Element identification = fitsDoc.getRootElement().getChild("identification", FITS_NS);
        String identityStatus = identification.getAttributeValue("status");
        // If there was no conflict, use the first identity
        if (identityStatus == null || FITS_SINGLE_STATUS.equals(identityStatus)) {
            return identification.getChild("identity", FITS_NS);
        } else {
            if ("UNKNOWN".equals(identification.getAttributeValue("status"))) {
                return null;
            }

            // Conflicting identification from FITS, try to resolve
            // Don't trust Exiftool if it detects a symlink, which is does not follow to the file.
            // Trust any answer agreed on by multiple tools
            for (Element el : identification.getChildren("identity", FITS_NS)) {
                if (el.getChildren("tool", FITS_NS).size() > 1
                        || !("Exiftool".equals(el.getChild("tool", FITS_NS).getAttributeValue("toolname"))
                                && "application/x-symlink".equals(el.getAttributeValue("mimetype")))) {
                    return el;
                }
            }
        }

        return null;
    }

    private int rankMimetype(String mimetype) {
        if (!MimetypeHelpers.isValidMimetype(mimetype)) {
            return -1;
        }
        if (mimetype.equals(APPLICATION_OCTET_STREAM_VALUE)) {
            return  0;
        }
        if (mimetype.equals(MimeTypeUtils.TEXT_PLAIN_VALUE)) {
            return  1;
        }
        return 2;
    }

    private Element getObjectCharacteristics(Document premisDoc) {
        return premisDoc.getRootElement()
                .getChild("object", PREMIS_V3_NS)
                .getChild("objectCharacteristics", PREMIS_V3_NS);
    }

    /**
     * Add file info, including md5 checksum and filesize to the premis report and
     *
     * @param fitsDoc
     * @param premisObjCharsEl
     */
    private void addFileinfoToReport(Document fitsDoc, Element premisObjCharsEl) {
        Element fileinfoEl = fitsDoc.getRootElement().getChild("fileinfo", FITS_NS);

        // Add file size and composition level
        String filesize = fileinfoEl.getChildTextTrim("size", FITS_NS);
        if (filesize != null) {
            premisObjCharsEl.addContent(new Element("size", PREMIS_V3_NS).setText(filesize));
        }
    }

    /**
     * Constructs a PREMIS document with basic information about the given
     * object
     *
     * @param objPid
     * @param fitsDoc
     * @return
     */
    private Document generatePremisReport(PID objPid, Document fitsDoc) {
        Document premisDoc = new Document();
        Element premisEl = new Element("premis", PREMIS_V3_NS);
        premisDoc.addContent(premisEl);

        Element premisObjEl = new Element("object", PREMIS_V3_NS)
                .setAttribute("type", PREMIS_V3_NS.getPrefix() + ":file", XSI_NS);
        premisEl.addContent(premisObjEl);
        Element premisObjCharsEl = new Element("objectCharacteristics", PREMIS_V3_NS)
                .addContent(new Element("compositionLevel", PREMIS_V3_NS).setText("0"));
        premisObjEl.addContent(premisObjCharsEl);

        // Add object identifier referencing this objects repository uri
        premisObjEl.addContent(new Element("objectIdentifier", PREMIS_V3_NS)
                .addContent(new Element("objectIdentifierType", PREMIS_V3_NS)
                        .setText("Fedora Datastream PID"))
                .addContent(new Element("objectIdentifierValue", PREMIS_V3_NS)
                        .setText(objPid.getRepositoryPath())));

        return premisDoc;
    }

    private void addFitsResults(Document premisDoc, Document fitsDoc) {
        Element premisObjCharsEl = getObjectCharacteristics(premisDoc);

        // Attach the original report to the premis, and set its composition level.
        premisObjCharsEl
                .addContent(new Element("objectCharacteristicsExtension", PREMIS_V3_NS)
                        .addContent(fitsDoc.detachRootElement()));
    }

    private void writePremisReport(PID objPid, Document premisDoc) {
        Path reportFile = getTechMdPath(objPid, true);

        try (BufferedWriter writer = newBufferedWriter(reportFile)) {
            getXMLOutputter().output(premisDoc, writer);
        } catch (IOException e) {
            failJob(e, "Failed to persist premis report for object {0} to path {1}", objPid, reportFile);
        }
    }

    private XMLOutputter getXMLOutputter() {
        return new XMLOutputter(Format.getPrettyFormat());
    }

    public void setHttpClient(CloseableHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public void setBaseFitsUri(String baseFitsUri) {
        this.baseFitsUri = baseFitsUri;
    }

    public void setProcessFilesLocally(boolean processFilesLocally) {
        this.processFilesLocally = processFilesLocally;
    }
}
