package edu.unc.lib.boxc.operations.impl.pdf;

import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.fcrepo.exceptions.ServiceException;
import edu.unc.lib.boxc.model.api.exceptions.NotFoundException;
import edu.unc.lib.boxc.model.api.exceptions.ObjectTypeMismatchException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.fcrepo.ids.DatastreamPids;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.jms.pdf.PdfRequest;
import edu.unc.lib.boxc.search.api.SearchFieldKey;
import edu.unc.lib.boxc.search.api.facets.CutoffFacet;
import edu.unc.lib.boxc.search.api.models.ContentObjectRecord;
import edu.unc.lib.boxc.search.api.requests.SearchRequest;
import edu.unc.lib.boxc.search.api.requests.SearchState;
import edu.unc.lib.boxc.search.api.requests.SimpleIdRequest;
import edu.unc.lib.boxc.search.solr.facets.GenericFacet;
import edu.unc.lib.boxc.search.solr.services.SolrSearchService;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pdf4u.CLIMain;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static edu.unc.lib.boxc.search.api.SearchFieldKey.FILE_FORMAT_CATEGORY;

/**
 * Service for generating an aggregate PDF with OCR
 * @author krwong
 */
public class AggregatePdfService {
    private static final Logger log = LoggerFactory.getLogger(AggregatePdfService.class);

    private SolrSearchService solrSearchService;
    private RepositoryObjectLoader repositoryObjectLoader;

    private Path tmpDir = Paths.get(System.getProperty("java.io.tmpdir"));
    public Path tmpFilesDir = tmpDir.resolve("pdf4u");

    private static final int DEFAULT_PAGE_SIZE = 10000;

    private static final List<String> WORK_REQUEST_FIELDS = Arrays.asList(
            SearchFieldKey.ID.name(), SearchFieldKey.ANCESTOR_PATH.name());

    private static final List<String> FILE_REQUEST_FIELDS = Arrays.asList(
            SearchFieldKey.ID.name(), SearchFieldKey.FILE_FORMAT_TYPE.name(),
            SearchFieldKey.ANCESTOR_PATH.name(), SearchFieldKey.TRANSCRIPT.name());

    public AggregatePdfService() {
        try {
            initializeTempImageFilesDir();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void init() throws IOException {
        initializeTempImageFilesDir();
    }

    public Path generateAggregatePdf(PdfRequest request) throws IOException {
        var workPid = request.getWorkPid();
        String inputFiles = createInputListFile(request).toString();
        String transcriptFiles = createTranscriptListFile(request).toString();
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
            throw new ServiceException("Failed to generate aggregate PDF to " + tempPath + " for " + workPid, e);
        } finally {
            List<String> temporaryFiles = Arrays.asList(inputFiles, transcriptFiles);
            for (String tempFile : temporaryFiles) {
                Files.deleteIfExists(Path.of(tempFile));
            }
        }
    }

    /**
     * Create .txt file with list of input files
     * @param request PdfRequest
     * @return .txt path to input files
     */
    public Path createInputListFile(PdfRequest request) {
        var workPidString = request.getWorkPid();
        var workPid = PIDs.get(workPidString);
        var agent = request.getAgent();

        var inputFilePath = prepareTempPath(workPidString + "_input", ".txt");
        var parentRec = getParentRecord(workPid, agent);
        assertParentRecordValid(workPid, parentRec);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(inputFilePath.toFile()))) {
            List<ContentObjectRecord> children = getChildrenRecords(parentRec, agent);

            for (var child : children) {
                var filePid = child.getPid();
                var originalFilePid = DatastreamPids.getOriginalFilePid(filePid);
                var originalFilePath = repositoryObjectLoader.getBinaryObject(originalFilePid).getContentUri();
                if (originalFilePath != null) {
                    writer.write(originalFilePath + System.lineSeparator());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return inputFilePath;
    }

    /**
     * Retrieve transcript value and write to temp file, then create .txt file with list of all transcript files
     * @param request PdfRequest
     * @return .txt path to transcript files
     */
    public Path createTranscriptListFile(PdfRequest request) {
        var workPidString = request.getWorkPid();
        var workPid = PIDs.get(workPidString);
        var agent = request.getAgent();

        var transcriptListPath = prepareTempPath(workPid + "_transcriptlist", ".txt");
        var transcriptList = new ArrayList<>();
        var parentRec = getParentRecord(workPid, agent);
        assertParentRecordValid(workPid, parentRec);

        // retrieve transcript and write to temporary transcript file
        List<ContentObjectRecord> children = getChildrenRecords(parentRec, agent);
        for (var child : children) {
            var transcriptFilePath = prepareTempPath(child.getId() + "_transcript", ".txt");
            var transcriptValue = child.getTranscript();
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(transcriptFilePath.toFile()))) {
                writer.write(transcriptValue);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            transcriptList.add(transcriptFilePath);
        }

        // create .txt with list of temporary transcript file paths
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(transcriptListPath.toFile()))) {
            for (var transcriptFilePath : transcriptList) {
                writer.write(transcriptFilePath + System.lineSeparator());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return transcriptListPath;
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
        searchState.addFacet(new GenericFacet(FILE_FORMAT_CATEGORY.name(), "Image"));
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
    }

    /**
     * Create tmp pdf4u files directory for temporary files
     * @return tmpImageFilesDirectoryPath
     */
    private void initializeTempImageFilesDir() throws IOException {
        Path path = tmpFilesDir;
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }
    }

    /**
     * Create temporary file path and delete temporary file if it already exists
     * @return tmpImageFilesDirectoryPath
     */
    private Path prepareTempPath(String fileName, String extension) {
        String baseName = FilenameUtils.getBaseName(fileName);
        String uniqueName = baseName + "_" + UUID.randomUUID() + extension;
        return Path.of(System.getProperty("java.io.tmpdir"), uniqueName);
    }

    public void setRepositoryObjectLoader(RepositoryObjectLoader repositoryObjectLoader) {
        this.repositoryObjectLoader = repositoryObjectLoader;
    }

    public void setSolrSearchService(SolrSearchService solrSearchService) {
        this.solrSearchService = solrSearchService;
    }

    public void setTmpDir(Path tmpDir) {
        this.tmpDir = tmpDir;
    }
}
