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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static edu.unc.lib.boxc.search.api.SearchFieldKey.FILE_FORMAT_CATEGORY;

/**
 * Service for generating an aggregate PDF with OCR
 * @author krwong
 */
public class AggregatePdfService {
    private static final Logger log = LoggerFactory.getLogger(AggregatePdfService.class);

    private MachineGeneratedContentService machineGeneratedContentService;
    private SolrSearchService solrSearchService;
    private RepositoryObjectLoader repositoryObjectLoader;

    private String tmpDir;
    public Path tmpFilesDir;

    private static final int DEFAULT_PAGE_SIZE = 10000;

    private static final List<String> FILENAME_REQUEST_FIELDS = Arrays.asList(
            SearchFieldKey.ID.name(), SearchFieldKey.COLLECTION_ID.name(), SearchFieldKey.HOOK_ID.name(),
            SearchFieldKey.TITLE.name());

    private static final List<String> WORK_REQUEST_FIELDS = Arrays.asList(
            SearchFieldKey.ID.name(), SearchFieldKey.ANCESTOR_PATH.name());

    private static final List<String> FILE_REQUEST_FIELDS = Arrays.asList(
            SearchFieldKey.ID.name(), SearchFieldKey.FILE_FORMAT_TYPE.name(),
            SearchFieldKey.ANCESTOR_PATH.name(), SearchFieldKey.TRANSCRIPT.name());

    public AggregatePdfService(String tmpDir) {
        this.tmpDir = tmpDir;
        this.tmpFilesDir = Path.of(tmpDir, "pdf");

        try {
            Files.createDirectories(tmpFilesDir);
        } catch (IOException e) {
            throw new RuntimeException("Cannot create " + tmpFilesDir, e);
        }
    }

    /**
     * Generate aggregate PDF with pdf4u
     * @param request PdfRequest
     * @return path to aggregate PDF file
     */
    public Path generateAggregatePdf(PdfRequest request) throws IOException {
        var workPid = request.getWorkPid();
        String inputFiles = createInputListFile(request).toString();
        String transcriptFiles = createTranscriptListFile(request).toString();
        Path tempPath = prepareTempPath(workPid, ".pdf");
        String textTypeList = createTextTypeList(request).stream().map(Object::toString)
                .collect(Collectors.joining(","));

        String[] command = new String[]{"pdf4u", "add_ocr", "-i", inputFiles,
                "-o", tempPath.toString(), "-t", transcriptFiles, "-tt", textTypeList};

        try {
            log.info("Run pdf4u command {} for work {}", command, workPid);
            int exitCode = CLIMain.runCommand(command);

            log.debug("pdf4u exit code: {}", exitCode);
            if (exitCode != 0) {
                throw new RuntimeException("pdf4u command " + Arrays.toString(command)
                        + " failed to execute for " + workPid);
            }

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
                var originalFilePath = Path.of(repositoryObjectLoader.getBinaryObject(originalFilePid).getContentUri());
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
        // if transcript value is null, write no transcript
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
            } else {
                transcriptList.add("no transcript");
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
     * text types: printed, typed, handwritten printed, handwritten cursive, mixed, no text
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
            // if no textType retrieved, set to 'no text'
            textTypeList.add(Objects.requireNonNullElse(textType, "no text"));
        }

        return textTypeList;
    }

    /**
     * Create aggregate PDF filename using the parent collection's collection id and the work's hook id
     * If collection id and hook id are unavailable, use normalized work title
     * @param request PdfRequest
     * @return aggregate PDF filename
     */
    public String createPdfFilename(PdfRequest request) {
        String filename;
        var workPidString = request.getWorkPid();
        var workPid = PIDs.get(workPidString);
        var agent = request.getAgent();

        var filenameFields = getFilenameRecord(workPid, agent);

        String collectionId = filenameFields.getCollectionId();
        String hookId = filenameFields.getHookId();
        if ((collectionId != null && !collectionId.equals("null")) && (hookId != null && !hookId.equals("null"))) {
            filename = collectionId + "_" + hookId + ".pdf";
        } else {
            String workTitle = filenameFields.getTitle();
            filename = normalizeWorkTitle(workTitle) + "_aggregate_pdf.pdf";
        }

        return filename;
    }

    /**
     * Create a cleaner aggregate PDF filename using the work title
     * Remove file extension, remove all punctuation except dashes and underscores,
     *      replace whitespace with underscores, and lowercase work title
     * @param workTitle title of work
     * @return normalized work title
     */
    private String normalizeWorkTitle(String workTitle) {
        workTitle = FilenameUtils.removeExtension(workTitle);
        return workTitle.replaceAll("\\s+", "_")
                .replaceAll("[^a-zA-Z0-9_-]", "").toLowerCase();
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

    private ContentObjectRecord getFilenameRecord(PID pid, AgentPrincipals agent) {
        var filenameRequest = new SimpleIdRequest(pid, FILENAME_REQUEST_FIELDS, agent.getPrincipals());
        return solrSearchService.getObjectById(filenameRequest);
    }

    private void assertParentRecordValid(PID pid, ContentObjectRecord parentRec) {
        if (parentRec == null) {
            throw new NotFoundException("Unable to find requested record " + pid.getId()
                    + ", it either does not exist or is not accessible");
        }
    }

    /**
     * Create temporary file path and delete temporary file if it already exists
     * @return tmpImageFilesDirectoryPath
     */
    private Path prepareTempPath(String fileName, String extension) {
        return tmpFilesDir.resolve(fileName + extension);
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
}
