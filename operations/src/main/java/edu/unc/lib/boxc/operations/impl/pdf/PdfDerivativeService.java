package edu.unc.lib.boxc.operations.impl.pdf;

import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.model.api.DatastreamType;
import edu.unc.lib.boxc.model.api.ResourceType;
import edu.unc.lib.boxc.model.api.exceptions.InvalidOperationForObjectType;
import edu.unc.lib.boxc.model.api.exceptions.NotFoundException;
import edu.unc.lib.boxc.model.api.exceptions.ObjectTypeMismatchException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.jms.pdf.PdfRequest;
import edu.unc.lib.boxc.search.api.SearchFieldKey;
import edu.unc.lib.boxc.search.api.facets.CutoffFacet;
import edu.unc.lib.boxc.search.api.models.ContentObjectRecord;
import edu.unc.lib.boxc.search.api.requests.SearchRequest;
import edu.unc.lib.boxc.search.api.requests.SearchState;
import edu.unc.lib.boxc.search.api.requests.SimpleIdRequest;
import edu.unc.lib.boxc.search.solr.services.SolrSearchService;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pdf4u.CLIMain;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static edu.unc.lib.boxc.operations.api.order.MemberOrderHelper.formatUnsupportedMessage;
import static edu.unc.lib.boxc.operations.api.order.MemberOrderHelper.supportsMemberOrdering;

/**
 * Service for generating a derivative PDF with OCR
 * @author krwong
 */
public class PdfDerivativeService {
    private static final Logger log = LoggerFactory.getLogger(PdfDerivativeService.class);

    private SolrSearchService solrSearchService;
    private RepositoryObjectLoader repositoryObjectLoader;

    public Path tmpDir = Paths.get(System.getProperty("java.io.tmpdir"));
    public Path tmpFilesDir = tmpDir.resolve("pdf4u");

    private static final int DEFAULT_PAGE_SIZE = 10000;

    private static final List<String> WORK_REQUEST_FIELDS = Arrays.asList(
            SearchFieldKey.ID.name(), SearchFieldKey.ANCESTOR_PATH.name());

    private static final List<String> FILE_REQUEST_FIELDS = Arrays.asList(
            SearchFieldKey.ID.name(), SearchFieldKey.FILE_FORMAT_TYPE.name(), SearchFieldKey.DATASTREAM.name(),
            SearchFieldKey.ANCESTOR_PATH.name(), SearchFieldKey.TRANSCRIPT.name());

    public PdfDerivativeService() {
        try {
            initializeTempImageFilesDir();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Path generatePdfDerivative(PdfRequest request) throws Exception {
        var workPid = request.getWorkPid();
        String inputFiles = getInputFiles(request);
        String transcriptFiles = getTranscriptFiles(request);
        Path tempPath = prepareTempPath(workPid, ".pdf");
        String textType = getTextTypes(request);

        try {
            // check that object is a work object
            repositoryObjectLoader.getWorkObject(PIDs.get(workPid));

            String[] command = new String[]{"pdf4u", "add_ocr", "-i", inputFiles, "-o", tempPath.toString(),
                    "-t", transcriptFiles, "-tt", textType};
            log.debug("Run pdf4u command {} for work {}", command, workPid);
            CLIMain.runCommand(command);

            return tempPath;
        } catch (ObjectTypeMismatchException e) {
            log.debug("Object {} is not a work object", request.getWorkPid(), e);
            throw new IllegalArgumentException("Object " + workPid + " is not a work object");
        } catch (Exception e) {
            log.error("Failed to generate pdf derivative to {} for {}", tempPath, workPid);
            throw e;
        } finally {
            List<String> temporaryFiles = Arrays.asList(inputFiles, transcriptFiles);
            for (String tempFile : temporaryFiles) {
                Files.deleteIfExists(Path.of(tempFile));
            }
        }
    }

    /**
     * Get path for input file(s)
     * @param request PdfRequest
     * @return .txt path to input files
     */
    public String getInputFiles(PdfRequest request) throws Exception {
        var workPidString = request.getWorkPid();
        var workPid = PIDs.get(workPidString);
        var agent = request.getAgent();

        var inputFilePath = prepareTempPath(workPidString + "_input", ".txt");
        try {
            var parentRec = getParentRecord(workPid, agent);
            assertParentRecordValid(workPid, parentRec);

            BufferedWriter writer = new BufferedWriter(new FileWriter(inputFilePath.toFile()));
            List<ContentObjectRecord> children = getChildrenRecords(parentRec, agent);
            for (var child : children) {
                var original = child.getDatastreamObject(DatastreamType.ORIGINAL_FILE.getId());
                if (original != null && !StringUtils.isBlank(original.getFilename())) {
                    writer.write(original.getFilename() + System.lineSeparator());
                }
            }
            writer.close();

            return inputFilePath.toString();
        } catch (Exception e) {
            throw new Exception("Failed to generate input txt file for " + workPid, e);
        }
    }

    /**
     * Get path for transcript file(s)
     * @param request PdfRequest
     * @return .txt path to transcript files
     */
    public String getTranscriptFiles(PdfRequest request) throws Exception {
        //TODO: get transcript files from the alt text review
        var workPid = request.getWorkPid();
        var agent = request.getAgent();

        var inputTranscriptPath = prepareTempPath(workPid + "_transcript", ".txt");

        return inputTranscriptPath.toString();
    }

    /**
     * Get text type from boxctron's alt text review
     * @param request PdfRequest
     * @return textType
     */
    public String getTextTypes(PdfRequest request) {
        //TODO: get text type from the alt text review

        return "HANDWRITTEN-PRINT";
    }

    // Query for all immediate children/members of the specified record, in default sort order
    private List<ContentObjectRecord> getChildrenRecords(ContentObjectRecord parentRec, AgentPrincipals agent) {
        SearchState searchState = new SearchState();
        searchState.setIgnoreMaxRows(true);
        searchState.setRowsPerPage(DEFAULT_PAGE_SIZE);
        CutoffFacet selectedPath = parentRec.getPath();
        searchState.addFacet(selectedPath);
        searchState.setSortType("default");
        searchState.setResultFields(FILE_REQUEST_FIELDS);
        var searchRequest = new SearchRequest(searchState, agent.getPrincipals());
        return solrSearchService.getSearchResults(searchRequest).getResultList();
    }

    private ContentObjectRecord getParentRecord(PID pid, AgentPrincipals agent) {
        var parentRequest = new SimpleIdRequest(pid, WORK_REQUEST_FIELDS, agent.getPrincipals());
        return solrSearchService.getObjectById(parentRequest);
    }

    private void assertParentRecordValid(PID pid, ContentObjectRecord parentRec) {
        if (parentRec == null) {
            throw new NotFoundException("Unable to find requested record " + pid.getId()
                    + ", it either does not exist or is not accessible");
        }
        var resourceType = ResourceType.valueOf(parentRec.getResourceType());
        if (!supportsMemberOrdering(resourceType)) {
            throw new InvalidOperationForObjectType(formatUnsupportedMessage(pid, resourceType));
        }
    }

    /**
     * Create tmp pdf4u files directory for temporary files
     * @return tmpImageFilesDirectoryPath
     */
    private void initializeTempImageFilesDir() throws Exception {
        Path path = tmpFilesDir;
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }
    }

    /**
     * Create temporary file path and delete temporary file if it already exists
     * @return tmpImageFilesDirectoryPath
     */
    private Path prepareTempPath(String fileName, String extension) throws Exception {
        Path tempPath = Files.createTempFile(tmpFilesDir, FilenameUtils.getName(fileName), extension);
        // delete temporary path so that it can be written over by whatever utility has requested a path
        Files.delete(tempPath);
        return tempPath;
    }

    public void setRepositoryObjectLoader(RepositoryObjectLoader repositoryObjectLoader) {
        this.repositoryObjectLoader = repositoryObjectLoader;
    }

    public void setSolrSearchService(SolrSearchService solrSearchService) {
        this.solrSearchService = solrSearchService;
    }
}
