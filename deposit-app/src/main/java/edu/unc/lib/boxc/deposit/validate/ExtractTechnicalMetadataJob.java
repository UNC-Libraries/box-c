package edu.unc.lib.boxc.deposit.validate;

import com.google.common.base.CharMatcher;
import edu.unc.lib.boxc.common.http.MimetypeHelpers;
import edu.unc.lib.boxc.common.util.URIUtil;
import edu.unc.lib.boxc.deposit.work.AbstractConcurrentDepositJob;
import edu.unc.lib.boxc.deposit.work.JobFailedException;
import edu.unc.lib.boxc.deposit.work.JobInterruptedException;
import edu.unc.lib.boxc.model.api.exceptions.RepositoryException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.rdf.CdrDeposit;
import edu.unc.lib.boxc.model.fcrepo.ids.DatastreamPids;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
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

import javax.annotation.PostConstruct;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static edu.unc.lib.boxc.common.xml.SecureXMLFactory.createSAXBuilder;
import static edu.unc.lib.boxc.model.api.rdf.CdrDeposit.mimetype;
import static edu.unc.lib.boxc.model.api.xml.JDOMNamespaceUtil.FITS_NS;
import static edu.unc.lib.boxc.model.api.xml.JDOMNamespaceUtil.PREMIS_V3_NS;
import static edu.unc.lib.boxc.model.api.xml.JDOMNamespaceUtil.XSI_NS;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.newBufferedWriter;
import static org.apache.commons.lang3.StringUtils.substringBeforeLast;
import static org.springframework.util.MimeTypeUtils.APPLICATION_OCTET_STREAM_VALUE;

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
    private static final String FITS_EXAMINE_PATH = "examine";
    private static final String MIMETYPE_ATTR = "mimetype";

    private CloseableHttpClient httpClient;

    // server path of the FITS application
    private String baseFitsUri;
    // URI to the examine servlet in the FITS application
    private URI fitsExamineUri;
    private String fitsHomePath;
    private Path fitsCommandPath;
    private int maxFileSizeForWebService;
    private Set<String> FILE_EXTS_FOR_CLI = new HashSet<>(Arrays.asList("mov"));

    private Model model;

    public ExtractTechnicalMetadataJob(String uuid, String depositUUID) {
        super(uuid, depositUUID);
    }

    @PostConstruct
    public void initJob() {
        init();
        fitsExamineUri = URI.create(URIUtil.join(baseFitsUri, FITS_EXAMINE_PATH));
        fitsCommandPath = Paths.get(fitsHomePath, "fits.sh");
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
                final String providedLabel = getProvidedLabel(objPid, model);

                submitTask(new ExtractTechnicalMetadataRunnable(objPid, originalPid, stagedPath,
                        providedMimetype, providedLabel));
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

    private String getProvidedLabel(PID filePid, Model model) {
        Resource fileResc = model.getResource(filePid.getRepositoryPath());
        Statement labelStmt = fileResc.getProperty(CdrDeposit.label);
        if (labelStmt != null) {
            return labelStmt.getString();
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
        private String providedLabel;
        private ExtractTechnicalMetadataResult result = new ExtractTechnicalMetadataResult();

        public ExtractTechnicalMetadataRunnable(PID objPid, PID originalPid, String stagedPath,
                String providedMimetype, String providedLabel) {
            this.objPid = objPid;
            this.originalPid = originalPid;
            this.stagedPath = stagedPath;
            this.providedMimetype = providedMimetype;
            this.providedLabel = providedLabel;
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

            // Symlink the file before processing
            Path linkPath = makeSymlinkForStagedPath(stagedPath, providedLabel);

            Document fitsDoc = null;
            try {
                // Generate the FITS report as a document
                fitsDoc = getFitsDocument(objPid, linkPath);

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
            } catch (JobFailedException | JobInterruptedException | RepositoryException e) {
                throw e;
            } catch (Exception e) {
                failJob(e, "Failed to extract FITS details for file '{0}' with id {1} from document:\n{2}",
                        stagedPath, objPid.getId(), fitsDoc != null ? getXMLOutputter().outputString(fitsDoc) : "null");
            } finally {
                try {
                    Files.delete(linkPath);
                    Files.delete(linkPath.getParent());
                } catch (IOException e) {
                    log.warn("Failed to delete symlink", e);
                }
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
                fitsMimetype = identity.getAttributeValue(MIMETYPE_ATTR);
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
     * @param filePath
     * @return
     */
    private Document getFitsDocument(PID objPid, Path filePath) {
        if (shouldProcessWithWebService(filePath)) {
            return extractUsingWebService(objPid, filePath);
        } else {
            return extractUsingCLI(objPid, filePath);
        }
    }

    /**
     * Creates a symlink to the provided stagedUri, where the symlink is sanitized of problematic characters
     * and uses the label as the filename to ensure the original file extension is present, if available.
     * @param objPid
     * @param stagedUriString
     * @param label
     * @return
     */
    protected Path makeSymlinkForStagedPath(String stagedUriString, String label)  {
        // Resolve the path from a URI and make it absolute
        URI stagedUri = URI.create(stagedUriString);
        Path stagedPath;
        File depositDirectory = getDepositDirectory();
        if (!stagedUri.isAbsolute()) {
            stagedPath = Paths.get(depositDirectory.toString(), stagedUriString);
        } else {
            stagedPath = Paths.get(stagedUri);
        }
        try {
            // Create a unique parent directory for the symlink to avoid filename conflicts
            var parentDir = Files.createTempDirectory(depositDirectory.toPath(), "fits_staging");
            // Assign the same permissions as the parent directory to the temp dir, since createTempDirectory is restrictive
            Files.setPosixFilePermissions(parentDir, Files.getPosixFilePermissions(parentDir.getParent()));
            // Create a symlink to the file to make use of the original filename and avoid issues with non-ascii characters
            String symlinkName = label != null ? label : stagedPath.getFileName().toString();
            var linkPath = sanitizeCliPath(parentDir.resolve(symlinkName));
            Files.createSymbolicLink(linkPath, stagedPath);
            return linkPath;
        } catch (IOException e) {
            throw new JobFailedException("Failed to create symlink for file " + stagedPath, e);
        }
    }

    private boolean shouldProcessWithWebService(Path path) {
        String filename = path.getFileName().toString();
        String extension = FilenameUtils.getExtension(filename).toLowerCase();
        if (FILE_EXTS_FOR_CLI.contains(extension)) {
            log.debug("File {} not applicable for web service due to file extension", path);
            return false;
        }
        try {
            if (Files.size(path) <= maxFileSizeForWebService) {
                log.debug("File {} is applicable for web service", path);
                return true;
            } else {
                log.debug("File {} not applicable for web service due to file size restriction", path);
            }
        } catch (IOException e) {
            failJob(e, "Unable to inspect file");
        }
        return false;
    }

    private Document extractUsingWebService(PID objPid, Path stagedPath) {
        // Files are available locally to FITS, so just pass along path
        URI fitsUri = null;
        try {
            URIBuilder builder = new URIBuilder(fitsExamineUri);
            builder.addParameter("file", stagedPath.toString());
            fitsUri = builder.build();

            log.debug("Requesting FITS document for {} using local file via URI {}", objPid, fitsUri);
        } catch (URISyntaxException e) {
            failJob(e, "Failed to construct FITs report uri for {0}", objPid);
        }

        HttpUriRequest request = new HttpGet(fitsUri);

        try (CloseableHttpResponse resp = httpClient.execute(request)) {
            // Write the report response to file
            InputStream respBodyStream = resp.getEntity().getContent();

            if (resp.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                failJob(null, "Failed to retrieve report for {0}, status {1} response {2}",
                        objPid, resp.getStatusLine().getStatusCode(), IOUtils.toString(respBodyStream, UTF_8));
            }

            return createSAXBuilder().build(respBodyStream);
        } catch (IOException | JDOMException e) {
            failJob(e, "Failed to stream report for file ''{0}'' with id {1} from server to report document",
                    stagedPath, objPid.getId());
        }
        return null;
    }

    // Pattern to match reserved characters for bash commands that need to be escaped
    private static Pattern ESCAPE_CLI_PATTERN = Pattern.compile("[$*?\\[\\]|;&><\"'`\\\\!#~]+");

    /**
     * Sanitize a staging path so that it is safe for usage in a CLI call
     * @param stagedPath
     * @return sanitized path
     */
    protected Path sanitizeCliPath(Path stagedPath) {
        var path = stagedPath.toString();
        path = CharMatcher.ascii().negate().replaceFrom(path, '_');
        path = ESCAPE_CLI_PATTERN.matcher(path).replaceAll("_");
        return Paths.get(path);
    }

    private Document extractUsingCLI(PID objPid, Path targetPath) {
        String stdout = null;
        try {
            String[] command = new String[] { fitsCommandPath.toString(), "-i", targetPath.toString() };
            Process process = Runtime.getRuntime().exec(command);
            int exitCode = process.waitFor();
            stdout = IOUtils.toString(process.getInputStream(), UTF_8);
            if (exitCode != 0) {
                String stderr = IOUtils.toString(process.getErrorStream(), UTF_8);
                failJob(null, "Failed to generate report for {0}, using command:\n{1}\n"
                        + "Script returned {3} with output:\n{4} {5}",
                        objPid, Arrays.toString(command), process.exitValue(), stdout, stderr);
            }
            return createSAXBuilder().build(new ByteArrayInputStream(stdout.getBytes(UTF_8)));
        } catch (IOException | JDOMException | InterruptedException e) {
            failJob(e, "Failed to generate report for file {0} with id {1}, output was:\n{2}",
                    targetPath, objPid.getId(), stdout);
        }
        return null;
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

            // Sort the identification elements to find the best value returned by FITS
            var identityEls = identification.getChildren("identity", FITS_NS).stream()
                    // Filter out any invalid entries
                    .filter(el -> MimetypeHelpers.isValidMimetype(el.getAttributeValue(MIMETYPE_ATTR)))
                    // Primarily sort by the best ranking mimetype
                    .sorted(Comparator.comparingInt((Element el) -> rankMimetype(el.getAttributeValue(MIMETYPE_ATTR)))
                    // Then rank by the number of tools that agreed on the mimetype
                    .thenComparingInt(el -> el.getChildren("tool", FITS_NS).size())
                    // Reverse so both rank and tool count is in descending order
                    .reversed()
                    // And then favor more application specific mimetypes
                    .thenComparingInt(el -> el.getAttributeValue(MIMETYPE_ATTR).contains("x-") ? -1 : 0))
                    .collect(Collectors.toList());
            // Return the best ranking identification, or null if none are valid
            return identityEls.isEmpty() ? null : identityEls.get(0);
        }
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

    public void setFitsHomePath(String fitsHomePath) {
        this.fitsHomePath = fitsHomePath;
    }

    public void setMaxFileSizeForWebService(int maxFileSizeForWebService) {
        this.maxFileSizeForWebService = maxFileSizeForWebService;
    }
}
