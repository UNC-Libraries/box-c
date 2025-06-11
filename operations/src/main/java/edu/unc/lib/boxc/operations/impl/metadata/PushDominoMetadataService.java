package edu.unc.lib.boxc.operations.impl.metadata;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.common.util.URIUtil;
import edu.unc.lib.boxc.model.api.exceptions.RepositoryException;
import edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPaths;
import edu.unc.lib.boxc.operations.impl.utils.EmailHandler;
import org.apache.commons.io.IOUtils;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service that pushes metadata for new digital objects to the Domino system.
 * @author bbpennel
 */
@Component
@EnableScheduling
public class PushDominoMetadataService {
    private static final Logger LOG = LoggerFactory.getLogger(PushDominoMetadataService.class);
    public static final String AGENT_NAME = "PushDominoMetadataAgent";

    private ExportDominoMetadataService exportDominoMetadataService;
    private String dominoUrl;
    private String dominoManagerSubpath;
    private String dominoUsername;
    private String dominoPassword;
    private String runConfigPath;
    private String adminEmailAddress;
    private AgentPrincipals agentPrincipals;
    private EmailHandler emailHandler;
    private HttpClientConnectionManager connectionManager;
    private CloseableHttpClient httpClient;
    private ObjectWriter configWriter;
    private ObjectReader configReader;
    private ObjectReader authReader;

    public PushDominoMetadataService() {
        configWriter = new ObjectMapper().writerFor(DominoPushConfig.class);
        configReader = new ObjectMapper().readerFor(DominoPushConfig.class);
        authReader = new ObjectMapper().readerFor(new TypeReference<HashMap<String, Object>>() {});
    }

    /**
     * Pushes works with ref ids that have been updated since the last run to DOMino for digital object creation.
     */
    @Scheduled(cron = "${domino.push.schedule}")
    public void pushNewDigitalObjects() {
        LOG.debug("Running scheduled push of new digital objects to Domino");
        // Load previous run config
        var config = loadConfig();

        var endDate = ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
        Path csvPath = null;
        try {
            // Export the new objects to a CSV file
            csvPath = exportNewObjectsCsv(config, endDate);

            if (csvPath == null) {
                LOG.debug("No new digital objects found since last run, skipping push to Domino");
            } else {
                // Upload the CSV file to Domino
                pushToDomino(csvPath);
            }

            // Update the last run timestamp in the config and persist it
            config.setLastNewObjectsRunTimestamp(endDate);
            persistConfig(config);
        } catch (Exception e) {
            LOG.error("Error pushing new digital objects to Domino", e);
            // Send notification of failure to admin email address if there was an error
            sendNotificationToAdmin(convertExceptionToString(e));
        } finally {
            // Cleanup the CSV file
            cleanupCsv(csvPath);
        }
    }

    private void cleanupCsv(Path csvPath) {
        if (csvPath == null) {
            return;
        }
        try {
            Files.deleteIfExists(csvPath);
        } catch (IOException e) {
            LOG.error("Error deleting file {}", csvPath, e);
        }
    }

    private Path exportNewObjectsCsv(DominoPushConfig config, String endDate) {
        var pidList = List.of(RepositoryPaths.getContentRootPid());
        var lastRunTimestamp = config.getLastNewObjectsRunTimestamp();
        try {
            return exportDominoMetadataService.exportCsv(pidList, agentPrincipals, lastRunTimestamp, endDate);
        } catch (ExportDominoMetadataService.NoRecordsExportedException e) {
            return null;
        } catch (IOException e) {
            throw new RepositoryException(e);
        }
    }

    private String getAspaceAuthToken() {
        var client = getHttpClient();
        String requestUrl = URIUtil.join(dominoUrl, "users", dominoUsername, "login");
        var postMethod = new HttpPost(requestUrl);
        // Set form parameters for authentication
        List<NameValuePair> params = List.of(new BasicNameValuePair("password", dominoPassword));

        LOG.info("Retrieving Aspace auth token from {}", requestUrl);
        try {
            postMethod.setEntity(new UrlEncodedFormEntity(params));
            try (var response = client.execute(postMethod)) {
                if (response.getStatusLine().getStatusCode() >= 300) {
                    throw new RepositoryException("Failed to authenticate with Aspace as URL " + requestUrl
                            + ": " + response.getStatusLine());
                }
                try (var body = response.getEntity().getContent()) {
                    var bodyMap = authReader.readValue(body, Map.class);
                    return (String) bodyMap.get("session");
                }
            }
        } catch (IOException e) {
            LOG.error("Error reading response from Aspace authentication at {}", requestUrl, e);
            throw new RepositoryException("Error reading response from Aspace authentication at " + requestUrl, e);
        } finally {
            postMethod.releaseConnection();
        }
    }

    private void pushToDomino(Path csvPath) {
        var authToken = getAspaceAuthToken();
        var client = getHttpClient();
        String requestUrl = URIUtil.join(dominoUrl, dominoManagerSubpath, "manage?source=dcr&delete=none");
        LOG.info("Pushing new digital objects to Domino at {}", requestUrl);
        var postMethod = new HttpPost(requestUrl);
        postMethod.setHeader("X-ArchivesSpace-Session", authToken);
        postMethod.setHeader("Content-Type", "text/csv");
        try {
            InputStreamEntity bodyEntity = new InputStreamEntity(Files.newInputStream(csvPath));
            postMethod.setEntity(bodyEntity);
            try (var response = client.execute(postMethod)) {
                if (response.getStatusLine().getStatusCode() == 400) {
                    var responseBody = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
                    var errorMessage = "Bad request to push new digital objects to Domino, some updates were rejected: "
                            + response.getStatusLine() + "\n" + responseBody;
                    LOG.warn(errorMessage);
                    sendNotificationToAdmin(errorMessage);
                } else if (response.getStatusLine().getStatusCode() >= 300) {
                    throw new RepositoryException("Unexpected response from Domino: " + response.getStatusLine());
                }
            }
        } catch (IOException e) {
            LOG.error("Error reading response from Domino at {}", requestUrl, e);
            throw new RepositoryException("Error reading response from Domino at " + requestUrl, e);
        } catch (Exception e) {
            LOG.error("Error communicating with Domino at {}", requestUrl, e);
            throw e;
        } finally {
            postMethod.releaseConnection();
        }
    }

    public CloseableHttpClient getHttpClient() {
        if (httpClient == null) {
            CredentialsProvider credsProvider = new BasicCredentialsProvider();
            credsProvider.setCredentials(
                    AuthScope.ANY,
                    new UsernamePasswordCredentials(dominoUsername, dominoPassword)
            );

            httpClient = HttpClients.custom()
                    .setDefaultCredentialsProvider(credsProvider)
                    .setConnectionManager(connectionManager)
                    .build();
        }
        return httpClient;
    }

    protected DominoPushConfig loadConfig() {
        Path configPath = Path.of(runConfigPath);
        try {
            if (Files.size(configPath) == 0) {
                LOG.warn("Domino push config file is empty, initializing with default values");
                return new DominoPushConfig();
            }
            return configReader.readValue(configPath.toFile(), DominoPushConfig.class);
        } catch (IOException e) {
            throw new RepositoryException("Failed to read Domino push config from " + runConfigPath, e);
        }
    }

    // Synchronized method to ensure thread safety when writing the config
    protected synchronized void persistConfig(DominoPushConfig config) {
        try {
            Path configPath = Path.of(runConfigPath);
            configWriter.writeValue(configPath.toFile(), config);
        } catch (IOException e) {
            throw new RepositoryException("Failed to write Domino push config to " + runConfigPath, e);
        }
    }

    private String convertExceptionToString(Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    private void sendNotificationToAdmin(String errorBody) {
        LOG.warn("Sending notification to admin at {}", adminEmailAddress);

        emailHandler.sendEmail(adminEmailAddress,
                "Error pushing new digital objects to Domino",
                "An error occurred while pushing digital objects to Domino:\n"
                        + errorBody,
                null, null);
    }

    public void setExportDominoMetadataService(ExportDominoMetadataService exportDominoMetadataService) {
        this.exportDominoMetadataService = exportDominoMetadataService;
    }

    public void setDominoUrl(String dominoUrl) {
        this.dominoUrl = dominoUrl;
    }

    public void setDominoManagerSubpath(String dominoManagerSubpath) {
        this.dominoManagerSubpath = dominoManagerSubpath;
    }

    public void setDominoUsername(String dominoUsername) {
        this.dominoUsername = dominoUsername;
    }

    public void setDominoPassword(String dominoPassword) {
        this.dominoPassword = dominoPassword;
    }

    public void setRunConfigPath(String runConfigPath) {
        this.runConfigPath = runConfigPath;
    }

    public void setConnectionManager(HttpClientConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public void setAdminEmailAddress(String adminEmailAddress) {
        this.adminEmailAddress = adminEmailAddress;
    }

    public void setAccessGroups(AccessGroupSet accessGroups) {
        this.agentPrincipals = new AgentPrincipalsImpl(AGENT_NAME, accessGroups);
    }

    public void setEmailHandler(EmailHandler emailHandler) {
        this.emailHandler = emailHandler;
    }

    public static class DominoPushConfig {
        private String lastNewObjectsRunTimestamp = "1970-01-01T00:00:00Z"; // Default to epoch start

        public String getLastNewObjectsRunTimestamp() {
            return lastNewObjectsRunTimestamp;
        }

        public void setLastNewObjectsRunTimestamp(String lastNewObjectsRunTimestamp) {
            this.lastNewObjectsRunTimestamp = lastNewObjectsRunTimestamp;
        }
    }
}
