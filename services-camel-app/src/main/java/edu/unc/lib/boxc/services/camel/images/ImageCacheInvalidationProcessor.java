package edu.unc.lib.boxc.services.camel.images;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.unc.lib.boxc.common.util.URIUtil;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.api.images.ImageServerUtil;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.http.HttpEntity;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;

/**
 * Processor which invalidates the image cache for a given object in the image server
 *
 * @author bbpennel
 */
public class ImageCacheInvalidationProcessor implements Processor {
    private static final Logger log = LoggerFactory.getLogger(ImageCacheInvalidationProcessor.class);
    private String imageServerBasePath;
    private HttpClientConnectionManager connectionManager;
    private ObjectMapper objectMapper = new ObjectMapper();
    private CloseableHttpClient httpClient;
    private String imageServerUsername;
    private String imageServerPassword;

    @Override
    public void process(Exchange exchange) throws Exception {
        final Message in = exchange.getIn();

        String fcrepoUri = (String) in.getHeader(FCREPO_URI);
        PID pid = PIDs.get(fcrepoUri);
        var client = getHttpClient();
        var imageId = ImageServerUtil.getImageServiceId(pid.getId());
        log.debug("Invalidating image cache for {} for image identifier {}", pid.getId(), imageId);
        var postMethod = new HttpPost(URIUtil.join(imageServerBasePath, "tasks"));
        var bodyMap = Map.of("verb", "PurgeItemFromCache",
                "identifier", imageId);
        // Serialize the body map to JSON and set it as the entity of the post method
        HttpEntity purgeEntity = EntityBuilder.create()
                .setText(objectMapper.writeValueAsString(bodyMap))
                .setContentType(ContentType.APPLICATION_JSON).build();
        postMethod.setEntity(purgeEntity);
        try (var response = client.execute(postMethod)) {
            if (response.getStatusLine().getStatusCode() != 202) {
                log.warn("Failed to invalidate image cache for {}, status {}: {}",
                        pid.getId(), response.getStatusLine().getStatusCode(), response.getEntity().toString());
            } else {
                log.debug("Successfully invalidated image cache for {}", pid.getId());
            }
        }
    }

    private CloseableHttpClient getHttpClient() {
        if (httpClient == null) {
            CredentialsProvider credsProvider = new BasicCredentialsProvider();
            credsProvider.setCredentials(
                    AuthScope.ANY,
                    new UsernamePasswordCredentials(imageServerUsername, imageServerPassword)
            );

            httpClient = HttpClients.custom()
                                    .setDefaultCredentialsProvider(credsProvider)
                                    .setConnectionManager(connectionManager)
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

    public void setImageServerBasePath(String imageServerBasePath) {
        this.imageServerBasePath = imageServerBasePath;
    }

    public void setConnectionManager(HttpClientConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public void setImageServerUsername(String imageServerUsername) {
        this.imageServerUsername = imageServerUsername;
    }

    public void setImageServerPassword(String imageServerPassword) {
        this.imageServerPassword = imageServerPassword;
    }
}
