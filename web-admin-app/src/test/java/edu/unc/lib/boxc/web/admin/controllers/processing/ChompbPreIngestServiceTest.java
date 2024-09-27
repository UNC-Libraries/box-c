package edu.unc.lib.boxc.web.admin.controllers.processing;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.api.services.GlobalPermissionEvaluator;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
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

        var results = service.getProcessingResults(agentPrincipals, PROJ_NAME, VELO_JOB_NAME);
        assertEquals(DATA_JSON, results);
    }

    @Test
    public void getProcessingResultsNoPermissionTest() throws Exception {
        when(globalPermissionEvaluator.hasGlobalPermission(any(AccessGroupSet.class), eq(Permission.ingest))).thenReturn(false);
        createDataJson();

        assertThrows(AccessRestrictionException.class,
                () -> service.getProcessingResults(agentPrincipals, PROJ_NAME, VELO_JOB_NAME));
    }

    private void createDataJson() throws IOException {
        var dataJsonPath = tmpFolder.resolve(PROJ_NAME)
                .resolve("processing/results")
                .resolve(VELO_JOB_NAME)
                .resolve("report/data.json");
        Files.createDirectories(dataJsonPath.getParent());
        Files.writeString(dataJsonPath, DATA_JSON);
    }
}
