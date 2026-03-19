package edu.unc.lib.boxc.services.camel.longleaf;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.unc.lib.boxc.common.util.URIUtil;
import edu.unc.lib.boxc.fcrepo.exceptions.ServiceException;
import edu.unc.lib.boxc.persist.impl.transfer.FileSystemTransferHelpers;
import io.dropwizard.metrics5.Histogram;
import io.dropwizard.metrics5.Timer;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.http.HttpEntity;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import edu.unc.lib.boxc.common.metrics.HistogramFactory;
import edu.unc.lib.boxc.common.metrics.TimerFactory;

/**
 * Processor which deregisters binaries in longleaf
 *
 * @author bbpennel
 */
public class DeregisterLongleafProcessor extends AbstractLongleafProcessor {
    private static final Logger log = LoggerFactory.getLogger(DeregisterLongleafProcessor.class);

    private static final Histogram batchSizeHistogram = HistogramFactory
            .createHistogram("longleafDeregisterBatchSize");
    private static final Timer timer = TimerFactory.createTimerForClass(DeregisterLongleafProcessor.class);

    private String longleafBaseUri;
    private HttpClientConnectionManager httpClientConnectionManager;
    private CloseableHttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * The exchange here is expected to be a batch message containing a List
     * of binary uris for deregistration, where each uri is in string form.
     */
    @SuppressWarnings("unchecked")
    @Override
    public void process(Exchange exchange) throws Exception {
        Message aggrMsg = exchange.getIn();

        List<String> messages = aggrMsg.getBody(List.class);
        if (messages.isEmpty()) {
            return;
        }
        int entryCount = messages.size();

        log.debug("Deregistering {} binaries from longleaf", entryCount);

        String deregList = messages.stream().map(m -> {
            URI uri = URI.create(m);
            Path filePath;
            if ("file".equals(uri.getScheme())) {
                filePath = Paths.get(uri);
            } else if (uri.getScheme() == null && m.startsWith("/")) {
                // No scheme, assume it is a file path
                filePath = Paths.get(m);
            } else {
                log.warn("Ignoring invalid content URI during deregistration: {}", m);
                return null;
            }
            // Translate the content URI into its base logical path
            return FileSystemTransferHelpers.getBaseBinaryPath(filePath.normalize());
        }).filter(m -> m != null).collect(Collectors.joining("\n"));
        // No valid content URIs to deregister
        if (deregList.isEmpty()) {
            return;
        }

        try (Timer.Context context = timer.time()) {
            deregisterFiles(messages, deregList, entryCount);
        }
    }

    /**
     * Executes longleaf deregister via HTTP DELETE API for a batch of file paths
     *
     * @param messages list of original content URIs from the exchange
     * @param deregList newline-separated list of base file paths to deregister
     * @param entryCount number of entries being deregistered
     */
    private void deregisterFiles(List<String> messages, String deregList, int entryCount) {
        batchSizeHistogram.update(entryCount);

        String requestUrl = URIUtil.join(longleafBaseUri, "api/deregister");
        var postMethod = new HttpPost(requestUrl);
        try {
            var bodyMap = Map.of(
                    "from_list", "@-",
                    "body", deregList);
            HttpEntity entity = EntityBuilder.create()
                    .setText(objectMapper.writeValueAsString(bodyMap))
                    .setContentType(ContentType.APPLICATION_JSON).build();
            postMethod.setEntity(entity);

            try (var response = getHttpClient().execute(postMethod)) {
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode != 200) {
                    log.error("Longleaf deregistration request failed with status {}", statusCode);
                    throw new ServiceException("Failed to deregister " + entryCount
                            + " entries in Longleaf. HTTP status: " + statusCode);
                }

                String responseBody = EntityUtils.toString(response.getEntity());
                JsonNode responseJson = objectMapper.readTree(responseBody);

                JsonNode failureNode = responseJson.get("failure");
                JsonNode successNode = responseJson.get("success");
                boolean hasFailures = failureNode != null && !failureNode.isEmpty();

                if (!hasFailures) {
                    log.info("Successfully deregistered {} entries in longleaf", entryCount);
                } else {
                    // Trim successfully deregistered files from the message before throwing exception
                    if (successNode != null && !successNode.isEmpty()) {
                        for (JsonNode successPath : successNode) {
                            messages.remove(Paths.get(successPath.asText()).toUri().toString());
                        }
                    }
                    if (messages.isEmpty()) {
                        log.error("Result from longleaf indicates deregistration failed, but there are "
                                + "no failed URIs remaining. See longleaf logs for details.");
                        return;
                    }
                    throw new ServiceException("Failed to deregister " + entryCount + " entries in Longleaf. "
                            + failureNode.size() + " failures reported.");
                }
            }
        } catch (IOException e) {
            throw new ServiceException("Error communicating with longleaf at " + requestUrl, e);
        } finally {
            postMethod.releaseConnection();
        }
    }

    private CloseableHttpClient getHttpClient() {
        if (httpClient == null) {
            httpClient = HttpClients.custom()
                    .setConnectionManager(httpClientConnectionManager)
                    .build();
        }
        return httpClient;
    }

    public void destroy() {
        if (httpClient != null) {
            try {
                httpClient.close();
            } catch (Exception e) {
                log.error("Failed to close http client", e);
            }
        }
    }

    public void setLongleafBaseUri(String longleafBaseUri) {
        this.longleafBaseUri = longleafBaseUri;
    }

    public void setHttpClientConnectionManager(HttpClientConnectionManager httpClientConnectionManager) {
        this.httpClientConnectionManager = httpClientConnectionManager;
    }
}
