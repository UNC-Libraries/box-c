package edu.unc.lib.boxc.operations.impl.metadata;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.model.api.exceptions.RepositoryException;
import edu.unc.lib.boxc.operations.impl.utils.EmailHandler;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author bbpennel
 */
@WireMockTest
public class PushDominoMetadataServiceTest {
    private static final String AUTH_TOKEN = "testauthtoken";
    private static final String DOM_SUB_PATH = "repositories/2/digital_object_manager";

    private PushDominoMetadataService service;

    @Mock
    private ExportDominoMetadataService exportService;

    @Mock
    private EmailHandler emailHandler;

    private AccessGroupSet accessGroups;

    private PoolingHttpClientConnectionManager connectionManager;

    @TempDir
    Path tempDir;

    private Path configPath;
    private Path testCsvPath;
    private String lastRunTimestamp;
    private String adminEmail = "admin@example.com";

    private AutoCloseable closeable;

    @BeforeEach
    public void setup(WireMockRuntimeInfo wireMockInfo) throws IOException {
        closeable = openMocks(this);

        // Create connection manager that allows 1 connection
        connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(1);
        connectionManager.setDefaultMaxPerRoute(1);

        // Create test CSV file
        testCsvPath = tempDir.resolve("test_export.csv");
        Files.writeString(testCsvPath, "uuid,ref_id,title\ntest1,ref1,Work 1\ntest2,ref2,Work 2");

        // Create initial config file with a timestamp
        lastRunTimestamp = ZonedDateTime.now(ZoneOffset.UTC).minusDays(7)
                .format(DateTimeFormatter.ISO_INSTANT);
        configPath = tempDir.resolve("domino_config.json");
        var config = new PushDominoMetadataService.DominoPushConfig();
        config.setLastNewObjectsRunTimestamp(lastRunTimestamp);

        accessGroups = new AccessGroupSetImpl("test_group");
        // Set up the service
        service = new PushDominoMetadataService();
        service.setExportDominoMetadataService(exportService);
        service.setEmailHandler(emailHandler);
        service.setConnectionManager(connectionManager);
        service.setRunConfigPath(configPath.toString());
        service.setAdminEmailAddress(adminEmail);
        service.setDominoUrl(wireMockInfo.getHttpBaseUrl());
        service.setDominoManagerSubpath(DOM_SUB_PATH);
        service.setDominoUsername("testuser");
        service.setDominoPassword("testpass");
        service.setAccessGroups(accessGroups);

        service.persistConfig(config);

        // Set up export service to return our test CSV
        when(exportService.exportCsv(any(), any(), eq(lastRunTimestamp), anyString()))
                .thenReturn(testCsvPath);
    }

    @AfterEach
    public void tearDown() throws Exception {
        connectionManager.shutdown();
        closeable.close();
        WireMock.reset();
    }

    @Test
    public void testPushNewDigitalObjectsSuccess() throws IOException {
        stubSuccessfulLogin();
        stubSuccessfulPush();

        // Execute the push
        service.pushNewDigitalObjects();

        // Verify the request was made
        WireMock.verify(WireMock.postRequestedFor(WireMock.urlPathMatching("/users/testuser/login")));
        WireMock.verify(WireMock.postRequestedFor(WireMock.urlPathMatching("/" + DOM_SUB_PATH + "/manage.*")));

        // Verify the config was updated
        var updatedConfig = service.loadConfig();
        assertTrue(updatedConfig.getLastNewObjectsRunTimestamp().compareTo(lastRunTimestamp) > 0,
                "Last run timestamp should be updated");

        // Verify no email was sent (since there was no error)
        assertNoEmailSent();

        // Verify CSV file was cleaned up
        assertTrue(Files.notExists(testCsvPath), "CSV file should be deleted after successful push");
    }

    @Test
    public void testPushNewDigitalObjectsAuthServerError() throws IOException {
        stubFor(WireMock.post(WireMock.urlPathMatching("/users/testuser/login"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")));

        service.pushNewDigitalObjects();

        WireMock.verify(WireMock.postRequestedFor(WireMock.urlPathMatching("/users/testuser/login")));

        // Verify the config was NOT updated
        var updatedConfig = service.loadConfig();
        assertEquals(lastRunTimestamp, updatedConfig.getLastNewObjectsRunTimestamp(),
                "Last run timestamp should not be updated after error");

        assertErrorEmailSent();

        assertTrue(Files.notExists(testCsvPath), "CSV file should be deleted after successful push");
    }

    @Test
    public void testPushNewDigitalObjectsServerError() throws IOException {
        stubSuccessfulLogin();
        // Setup WireMock stub for server error
        stubFor(WireMock.post(WireMock.urlPathMatching("/" + DOM_SUB_PATH + "/manage.*"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")));

        service.pushNewDigitalObjects();

        WireMock.verify(WireMock.postRequestedFor(WireMock.urlPathMatching("/users/testuser/login")));
        WireMock.verify(WireMock.postRequestedFor(WireMock.urlPathMatching("/" + DOM_SUB_PATH + "/manage.*")));

        // Verify the config was NOT updated
        var updatedConfig = service.loadConfig();
        assertEquals(lastRunTimestamp, updatedConfig.getLastNewObjectsRunTimestamp(),
                "Last run timestamp should not be updated after error");

        assertErrorEmailSent();

        assertTrue(Files.notExists(testCsvPath));
    }

    @Test
    public void testPushNewDigitalObjectsBadRequest() throws IOException {
        stubSuccessfulLogin();
        // Setup WireMock stub for server error
        stubFor(WireMock.post(WireMock.urlPathMatching("/" + DOM_SUB_PATH + "/manage.*"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withBody("There were issues with the input")));

        service.pushNewDigitalObjects();

        WireMock.verify(WireMock.postRequestedFor(WireMock.urlPathMatching("/users/testuser/login")));
        WireMock.verify(WireMock.postRequestedFor(WireMock.urlPathMatching("/" + DOM_SUB_PATH + "/manage.*")));

        // Verify the config was updated
        var updatedConfig = service.loadConfig();
        assertTrue(updatedConfig.getLastNewObjectsRunTimestamp().compareTo(lastRunTimestamp) > 0,
                "Last run timestamp should be updated");

        assertErrorEmailSent("An error occurred while pushing digital objects to Domino:\n" +
                "Bad request to push new digital objects to Domino, some updates were rejected: " +
                "HTTP/1.1 400 Bad Request\nThere were issues with the input");

        assertTrue(Files.notExists(testCsvPath));
    }

    @Test
    public void testExportServiceFailure() throws IOException {
        stubSuccessfulLogin();
        // Setup export service to throw exception
        doThrow(new IOException("Export failed"))
                .when(exportService).exportCsv(any(), isNull(), anyString(), anyString());

        service.pushNewDigitalObjects();

        // Verify the config was NOT updated
        var updatedConfig = service.loadConfig();
        assertEquals(lastRunTimestamp, updatedConfig.getLastNewObjectsRunTimestamp(),
                "Last run timestamp should not be updated after error");

        assertErrorEmailSent();
    }

    @Test
    public void testExportNoNewRecords() throws IOException {
        stubSuccessfulLogin();
        // Setup export service to throw exception
        doThrow(new ExportDominoMetadataService.NoRecordsExportedException("Nothing new"))
                .when(exportService).exportCsv(any(), any(), anyString(), anyString());

        service.pushNewDigitalObjects();

        // Verify the config was updated even though no records were exported
        var updatedConfig = service.loadConfig();
        assertTrue(updatedConfig.getLastNewObjectsRunTimestamp().compareTo(lastRunTimestamp) > 0,
                "Last run timestamp should be updated");

        assertNoEmailSent();
    }

    @Test
    public void testInvalidConfigFile() throws IOException {
        // Corrupt the config file
        Files.writeString(configPath, "{ invalid json }");

        var error = assertThrows(RepositoryException.class, () -> service.pushNewDigitalObjects());
        assertTrue(error.getMessage().contains("Failed to read Domino push config"),
                "Message did not contain expected text, was: " + error.getMessage());
    }

    @Test
    public void testEmptyConfigFile() throws IOException {
        stubSuccessfulLogin();
        stubSuccessfulPush();

        // Overwrite config file to be empty
        Files.writeString(configPath, "");

        when(exportService.exportCsv(any(), any(), eq("1970-01-01T00:00:00Z"), anyString()))
                .thenReturn(testCsvPath);

        service.pushNewDigitalObjects();

        WireMock.verify(WireMock.postRequestedFor(WireMock.urlPathMatching("/" + DOM_SUB_PATH + "/manage.*")));

        // The config file should now exist and have a last run timestamp
        assertTrue(Files.exists(configPath));
        var updatedConfig = service.loadConfig();
        assertTrue(updatedConfig.getLastNewObjectsRunTimestamp().compareTo(lastRunTimestamp) > 0,
                "Last run timestamp should be updated");

        // Verify no email was sent (since there was no error)
        assertNoEmailSent();
    }

    private void assertErrorEmailSent() {
        assertErrorEmailSent(null);
    }

    private void assertErrorEmailSent(String message) {
        verify(emailHandler).sendEmail(
                eq(adminEmail),
                eq("Error pushing new digital objects to Domino"),
                message == null ? anyString() : eq(message),
                isNull(),
                isNull());
    }

    private void assertNoEmailSent() {
        verify(emailHandler, never()).sendEmail(anyString(), anyString(), anyString(), anyString(), any());
    }

    private void stubSuccessfulLogin() {
        stubFor(WireMock.post(WireMock.urlPathMatching("/users/testuser/login"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("{ \"session\": \"" + AUTH_TOKEN + "\" }")));
    }

    private void stubSuccessfulPush() {
        stubFor(WireMock.post(WireMock.urlPathMatching("/" + DOM_SUB_PATH + "/manage.*"))
                .withQueryParam("source", equalTo("dcr"))
                .withQueryParam("delete", equalTo("none"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("Success")));
    }
}
