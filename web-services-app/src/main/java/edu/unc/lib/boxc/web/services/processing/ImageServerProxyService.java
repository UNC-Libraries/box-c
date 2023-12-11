package edu.unc.lib.boxc.web.services.processing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.unc.lib.boxc.common.util.URIUtil;
import edu.unc.lib.boxc.web.services.utils.ImageServerUtil;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * Generates request, connects to, and streams the output from the image Server Proxy.
 * @author bbpennel, snluong
 */
public class ImageServerProxyService {
    private static final Logger LOG = LoggerFactory.getLogger(ImageServerProxyService.class);
    private CloseableHttpClient httpClient;
    private String imageServerProxyBasePath;
    private String baseIiifv3Path;

    public void setHttpClientConnectionManager(HttpClientConnectionManager manager) {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(2000)
                .setConnectionRequestTimeout(5000)
                .build();

        this.httpClient = HttpClients.custom()
                .setConnectionManager(manager)
                .setDefaultRequestConfig(requestConfig)
                .build();
    }

    /**
     * Gets metadata from the IIIF V3 image server about the requested ID
     * @param id ID of the requested object
     */
    public JsonNode getMetadata(String id) throws IOException {
        HttpGet method = new HttpGet(getImageServerProxyBasePath() + ImageServerUtil.getImageServerEncodedId(id) + "/info.json");
        CloseableHttpResponse httpResp = httpClient.execute(method);
        var statusCode = httpResp.getStatusLine().getStatusCode();
        if (statusCode == HttpStatus.SC_OK) {
            var mapper = new ObjectMapper();
            var respData = mapper.readTree(httpResp.getEntity().getContent());
            ((ObjectNode) respData).put("id", URIUtil.join(baseIiifv3Path, id));
            return respData;
        }
        method.releaseConnection();

        LOG.error("Unexpected failure while getting image server proxy path {}", method);
        return null;
    }


    /**
     * Gets the datastream from the IIIF V3 image server for the requested ID
     * @param id ID of the requested object
     * @param region region of the image
     * @param size pixel size of the image, or max
     * @param rotation degree of rotation
     * @param quality quality of image
     * @param format format like png or jpg
     */

    public ResponseEntity<InputStreamResource> streamJP2(String id, String region, String size, String rotation,
                                                         String quality, String format) throws IOException {

        StringBuilder path = new StringBuilder(getImageServerProxyBasePath());
        path.append(ImageServerUtil.getImageServerEncodedId(id))
                .append("/" + region).append("/" + size)
                .append("/" + rotation).append("/" + quality + "." + format);

        InputStream input = new URL(path.toString()).openStream();
        InputStreamResource resource = new InputStreamResource(input);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .contentType(MediaType.IMAGE_JPEG)
                .body(resource);
    }

    public void setImageServerProxyBasePath(String fullPath) {
        this.imageServerProxyBasePath = fullPath;
    }

    public String getImageServerProxyBasePath() {
        return imageServerProxyBasePath;
    }

    public void setBaseIiifv3Path(String baseIiifv3Path) {
        this.baseIiifv3Path = baseIiifv3Path;
    }
}
