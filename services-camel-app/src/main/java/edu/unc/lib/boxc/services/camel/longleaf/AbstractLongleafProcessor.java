package edu.unc.lib.boxc.services.camel.longleaf;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.unc.lib.boxc.fcrepo.exceptions.ServiceException;
import org.apache.camel.Processor;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Abstract processor for executing longleaf HTTP API requests
 *
 * @author bbpennel
 */
public abstract class AbstractLongleafProcessor implements Processor {
    private static final Logger log = LoggerFactory.getLogger(AbstractLongleafProcessor.class);

    protected String longleafBaseUri;

    private HttpClientConnectionManager httpClientConnectionManager;
    private CloseableHttpClient httpClient;
    protected final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Execute a POST request to the longleaf API, parse the JSON response, and return
     * the success and failure path lists.
     *
     * @param requestUrl full URL to POST to
     * @param bodyMap map of fields to serialize as the JSON request body
     * @return parsed result containing success and failure path lists
     * @throws ServiceException if the server returns a non-200 status or communication fails
     */
    protected LongleafApiResult executeHttpPost(String requestUrl, Map<String, Object> bodyMap) {
        var postMethod = new HttpPost(requestUrl);
        try {
            HttpEntity entity = EntityBuilder.create()
                    .setText(objectMapper.writeValueAsString(bodyMap))
                    .setContentType(ContentType.APPLICATION_JSON).build();
            postMethod.setEntity(entity);

            try (var response = getHttpClient().execute(postMethod)) {
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode != 200) {
                    throw new ServiceException("Longleaf API request to " + requestUrl
                            + " failed with HTTP status: " + statusCode);
                }

                String responseBody = EntityUtils.toString(response.getEntity());
                JsonNode responseJson = objectMapper.readTree(responseBody);

                List<String> successes = parsePathList(responseJson.get("success"));
                List<String> failures = parsePathList(responseJson.get("failure"));
                return new LongleafApiResult(successes, failures);
            }
        } catch (IOException e) {
            throw new ServiceException("Error communicating with longleaf at " + requestUrl, e);
        } finally {
            postMethod.releaseConnection();
        }
    }

    private List<String> parsePathList(JsonNode node) {
        List<String> paths = new ArrayList<>();
        if (node != null) {
            node.forEach(n -> paths.add(n.asText()));
        }
        return paths;
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

    /**
     * Result of a longleaf API call, containing lists of paths that succeeded and failed.
     */
    protected static class LongleafApiResult {
        protected final List<String> successes;
        protected final List<String> failures;

        protected LongleafApiResult(List<String> successes, List<String> failures) {
            this.successes = successes;
            this.failures = failures;
        }

        protected boolean hasFailures() {
            return !failures.isEmpty();
        }
    }
}
