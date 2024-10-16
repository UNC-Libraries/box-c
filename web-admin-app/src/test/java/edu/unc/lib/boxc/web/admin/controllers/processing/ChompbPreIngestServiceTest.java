package edu.unc.lib.boxc.web.admin.controllers.processing;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.api.services.GlobalPermissionEvaluator;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

/**
 * @author bbpennel
 */
public class ChompbPreIngestServiceTest {
    @TempDir
    public Path tmpFolder;
    private AutoCloseable closeable;
    private static final String PROJ_NAME = "chompb_proj";
    private static final String VELO_JOB_NAME = "velocicroptor";
    private static final String DATA_JSON = "{ \"data\": [ ] }";
    private static final String JSON_FILENAME = "data.json";

    private ChompbPreIngestService service;
    @Mock
    private GlobalPermissionEvaluator globalPermissionEvaluator;
    @Mock
    private AgentPrincipals agentPrincipals;
    @Mock
    private Process mockProcess;

    @BeforeEach
    public void setup() {
        closeable = openMocks(this);
        service = new ChompbPreIngestService();
        service.setBaseProjectsPath(tmpFolder);
        service.setGlobalPermissionEvaluator(globalPermissionEvaluator);
        when(agentPrincipals.getPrincipals()).thenReturn(new AccessGroupSetImpl("group"));
        when(globalPermissionEvaluator.hasGlobalPermission(any(AccessGroupSet.class), eq(Permission.ingest))).thenReturn(true);
    }

    @AfterEach
    public void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void getProjectListsTest() throws Exception {
        String expectedOutput = "[ { \"projectPath\" : \"/path/to/project\" } ]";
        InputStream mockInputStream = new ByteArrayInputStream(expectedOutput.getBytes());
        when(mockProcess.getInputStream()).thenReturn(mockInputStream);
        when(mockProcess.waitFor()).thenReturn(0);

        try (MockedConstruction<ProcessBuilder> ignored = Mockito.mockConstruction(ProcessBuilder.class,
                (mock, context) -> {
                    when(mock.start()).thenReturn(mockProcess);
            })
        ){
            var output = service.getProjectLists(agentPrincipals);
            assertEquals(expectedOutput, output);
        }
    }

    @Test
    public void executeChompbCommandExitErrorTest() throws Exception {
        String expectedOutput = "error";
        InputStream mockInputStream = new ByteArrayInputStream(expectedOutput.getBytes());
        when(mockProcess.getInputStream()).thenReturn(mockInputStream);
        when(mockProcess.waitFor()).thenReturn(1);

        try (MockedConstruction<ProcessBuilder> ignored = Mockito.mockConstruction(ProcessBuilder.class,
                (mock, context) -> {
                    when(mock.start()).thenReturn(mockProcess);
                })
        ){
            var e = assertThrows(RuntimeException.class, () -> service.executeChompbCommand("chompb"));
            assertEquals("Command exited with status code 1: error", e.getMessage());
        }
    }

    @Test
    public void executeChompbCommandStreamErrorTest() throws Exception {
        try (MockedConstruction<ProcessBuilder> ignored = Mockito.mockConstruction(ProcessBuilder.class,
                (mock, context) -> {
                    when(mock.start()).thenThrow(new IOException("boom"));
                })
        ){
            var e = assertThrows(RuntimeException.class, () -> service.executeChompbCommand("chompb"));
            assertEquals("Failed to execute chompb command", e.getMessage());
        }
    }

    @Test
    public void getProjectListsNoPermissionsTest() {
        when(globalPermissionEvaluator.hasGlobalPermission(any(AccessGroupSet.class), eq(Permission.ingest))).thenReturn(false);

        assertThrows(AccessRestrictionException.class,
                () -> service.getProjectLists(agentPrincipals));
    }

    @Test
    public void getProcessingResultsTest() throws Exception {
        createDataJson();

        var results = service.getProcessingResults(agentPrincipals, PROJ_NAME, VELO_JOB_NAME, JSON_FILENAME);
        assertEquals(DATA_JSON, IOUtils.toString(results, StandardCharsets.UTF_8));
    }

    @Test
    public void getProcessingResultsNoPermissionTest() throws Exception {
        when(globalPermissionEvaluator.hasGlobalPermission(any(AccessGroupSet.class), eq(Permission.ingest))).thenReturn(false);
        createDataJson();

        assertThrows(AccessRestrictionException.class,
                () -> service.getProcessingResults(agentPrincipals, PROJ_NAME, VELO_JOB_NAME, JSON_FILENAME));
    }

    @Test
    public void getProcessingResultsImageTest() throws Exception {
        var imagePath = "images/path/to/image.jpg";
        var testContent = "test";
        createResultFile(imagePath, testContent);

        var results = service.getProcessingResults(agentPrincipals, PROJ_NAME, VELO_JOB_NAME, imagePath);
        assertEquals(testContent, IOUtils.toString(results, StandardCharsets.UTF_8));
    }

    @Test
    public void getProcessingResultsImageWithTraversalTest() throws Exception {
        var imagePath = "images/../../../../../../attack.jpg";

        assertThrows(AccessRestrictionException.class,
                () -> service.getProcessingResults(agentPrincipals, PROJ_NAME, VELO_JOB_NAME, imagePath));
    }

    @Test
    public void getProcessingResultsInvalidFilenameTest() throws Exception {
        assertThrows(IllegalArgumentException.class,
                () -> service.getProcessingResults(agentPrincipals, PROJ_NAME, VELO_JOB_NAME, "rando.json"));
    }

    private void createDataJson() throws IOException {
        createResultFile(JSON_FILENAME, DATA_JSON);
    }

    private void createResultFile(String filename, String testContent) throws IOException {
        var path = tmpFolder.resolve(PROJ_NAME)
                .resolve("processing/results")
                .resolve(VELO_JOB_NAME)
                .resolve("report")
                .resolve(filename);
        Files.createDirectories(path.getParent());
        Files.writeString(path, testContent);
    }
}
