package edu.unc.lib.boxc.operations.impl.pdf;

import com.fasterxml.jackson.databind.JsonNode;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.fcrepo.exceptions.ServiceException;
import edu.unc.lib.boxc.model.api.exceptions.NotFoundException;
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
import edu.unc.lib.boxc.search.solr.services.MachineGeneratedContentService;
import edu.unc.lib.boxc.search.solr.services.SolrSearchService;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pdf4u.CLIMain;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static edu.unc.lib.boxc.search.api.SearchFieldKey.FILE_FORMAT_CATEGORY;
import static edu.unc.lib.boxc.search.solr.services.MachineGeneratedContentService.RESULT_HANDWRITTEN_CURSIVE;

/**
 * Service for generating an aggregate PDF with OCR
 * @author krwong
 */
public class AggregatePdfService {
    private static final Logger log = LoggerFactory.getLogger(AggregatePdfService.class);

    private MachineGeneratedContentService machineGeneratedContentService;
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
    }

    public void init() throws IOException {
        initializeTempImageFilesDir();
    }

    public Path generateAggregatePdf(PdfRequest request) throws IOException {
        var workPid = request.getWorkPid();
        String inputFiles = createInputListFile(request).toString();
        String transcriptFiles = createTranscriptListFile(request).toString();
        Path tempPath = prepareTempPath(workPid, ".pdf");
        String textTypeList = createTextTypeList(request).stream().map(Object::toString)
                .collect(Collectors.joining(","));

        try {
            String[] command = new String[]{"pdf4u", "add_ocr", "-i", inputFiles, "-o", tempPath.toString(),
                    "-t", transcriptFiles, "-tt", textTypeList};
            log.debug("Run pdf4u command {} for work {}", command, workPid);
            CLIMain.runCommand(command);

            return tempPath;
        } catch (Exception e) {
            throw new ServiceException("Failed to generate aggregate PDF to " + tempPath + " for " + workPid, e);
        } finally {
            // delete input list file, transcript list file, and all transcript files
            List<String> temporaryFiles = new ArrayList<>(Arrays.asList(inputFiles, transcriptFiles));
            temporaryFiles.addAll(Files.readAllLines(Path.of(transcriptFiles), StandardCharsets.UTF_8));
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

        var transcriptListPath = prepareTempPath(workPid.getId() + "_transcriptlist", ".txt");
        var transcriptList = new ArrayList<>();
        var parentRec = getParentRecord(workPid, agent);
        assertParentRecordValid(workPid, parentRec);

        // retrieve transcript and write to temporary transcript file
        List<ContentObjectRecord> children = getChildrenRecords(parentRec, agent);
        for (var child : children) {
            var transcriptValue = child.getTranscript();
            if (transcriptValue != null) {
                var transcriptFilePath = prepareTempPath(child.getId() + "_transcript", ".txt");
                try {
                    Files.write(transcriptFilePath, transcriptValue.getBytes(StandardCharsets.UTF_8));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                transcriptList.add(transcriptFilePath);
            }
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
     * Retrieve text type value from boxctron's alt text review and create list of all text types
     * text types: printed, typed, handwritten printed, handwritten cursive, mixed
     * @param request PdfRequest
     * @return list of text types
     */
    public List<String> createTextTypeList(PdfRequest request) {
        var workPidString = request.getWorkPid();
        var workPid = PIDs.get(workPidString);
        var agent = request.getAgent();
        var parentRec = getParentRecord(workPid, agent);
        assertParentRecordValid(workPid, parentRec);

        var textTypeList = new ArrayList<String>();

        List<ContentObjectRecord> children = getChildrenRecords(parentRec, agent);
        for (var child : children) {
            var filePid = child.getPid();
            String mgdString = getMachineGeneratedDescriptionJson(filePid);
            JsonNode mgdNode = null;
            if (mgdString != null) {
                mgdNode = machineGeneratedContentService.deserializeMachineGeneratedDescription(mgdString);
                log.debug("Loaded machine gen datastream for {}", filePid);
            }

            var textType = machineGeneratedContentService.extractTextType(mgdNode);
            if (textType != null) {
                textTypeList.add(textType);
            }
        }

        return textTypeList;
    }

    private String getMachineGeneratedDescriptionJson(PID filePid) {
        try {
            return machineGeneratedContentService.loadMachineGeneratedDescription(filePid);
        } catch (NoSuchFileException e) {
            log.debug("No machine generated description datastream found for {}", filePid);
            return null;
        } catch (IOException e) {
            throw new ServiceException("Failed to read machine generated description for " + filePid, e);
        }
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
     */
    private void initializeTempImageFilesDir() throws IOException {
        tmpFilesDir = tmpDir.resolve("pdf4u");
        if (!Files.exists(tmpFilesDir)) {
            Files.createDirectories(tmpFilesDir);
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

    public void setMachineGeneratedContentService(MachineGeneratedContentService machineGeneratedContentService) {
        this.machineGeneratedContentService = machineGeneratedContentService;
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
