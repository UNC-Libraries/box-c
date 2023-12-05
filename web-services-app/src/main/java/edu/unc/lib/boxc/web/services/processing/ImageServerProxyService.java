package edu.unc.lib.boxc.web.services.processing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.unc.lib.boxc.common.util.URIUtil;
import edu.unc.lib.boxc.web.common.exceptions.ClientAbortException;
import edu.unc.lib.boxc.web.common.utils.FileIOUtil;
import edu.unc.lib.boxc.web.services.utils.ImageServerUtil;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPaths.idToPath;

/**
 * Generates request, connects to, and streams the output from the image Server Proxy.  Sets pertinent headers.
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
     * @param outStream out stream from the response
     * @param response response object passed from the controller
     * @param retryServerError the number of times to retry after failure
     */
    public void getMetadata(String id, OutputStream outStream,
                            HttpServletResponse response, int retryServerError) {

        var path = new StringBuilder(getImageServerProxyBasePath());
        path.append(ImageServerUtil.getImageServerEncodedId(id)).append("/info.json");

        int statusCode = -1;
        String statusLine = null;
        do {
            HttpGet method = new HttpGet(path.toString());
            try (CloseableHttpResponse httpResp = httpClient.execute(method)) {
                statusCode = httpResp.getStatusLine().getStatusCode();
                statusLine = httpResp.getStatusLine().toString();
                if (statusCode == HttpStatus.SC_OK) {
                    if (response != null) {
                        response.setHeader("Content-Type", "application/json");
                        response.setHeader("content-disposition", "inline");

                        var mapper = new ObjectMapper();
                        var respData = mapper.readTree(httpResp.getEntity().getContent());
                        ((ObjectNode) respData).put("id", URIUtil.join(baseIiifv3Path, id));

                        HttpEntity updatedRespData = EntityBuilder.create()
                                .setText(mapper.writeValueAsString(respData))
                                .setContentType(ContentType.APPLICATION_JSON).build();
                        httpResp.setEntity(updatedRespData);

                        FileIOUtil.stream(outStream, httpResp);
                    }
                    return;
                }
            } catch (ClientAbortException e) {
                LOG.debug("User client aborted request to stream jp2 metadata for {}", id, e);
            } catch (Exception e) {
                LOG.error("Problem retrieving metadata for {}", path, e);
            } finally {
                method.releaseConnection();
            }
            retryServerError--;
        } while (retryServerError >= 0 && (statusCode == 500 || statusCode == 404));
        LOG.error("Unexpected failure while getting image server proxy path {}: {}", statusLine, path);
    }

    /**
     * Gets the datastream from the IIIF V3 image server for the requested ID
     * @param id ID of the requested object
     * @param region region of the image
     * @param size pixel size of the image, or max
     * @param rotation degree of rotation
     * @param quality quality of image
     * @param format format like png or jpg
     * @param outStream out stream of the response
     * @param response response object passed from the controller
     * @param retryServerError the number of times to retry after failure
     */
    public void streamJP2(String id, String region, String size, String rotation, String quality,
                          String format, OutputStream outStream, HttpServletResponse response,
                          int retryServerError) {

        StringBuilder path = new StringBuilder(getImageServerProxyBasePath());
        path.append(ImageServerUtil.getImageServerEncodedId(id))
                .append("/" + region).append("/" + size)
                .append("/" + rotation).append("/" + quality + "." + format);

        HttpGet method = new HttpGet(path.toString());

        try (CloseableHttpResponse httpResp = httpClient.execute(method)) {
            int statusCode = httpResp.getStatusLine().getStatusCode();

            if (statusCode == HttpStatus.SC_OK) {
                if (response != null) {
                    response.setHeader("Content-Type", "image/jpeg");
                    response.setHeader("content-disposition", "inline");

                    FileIOUtil.stream(outStream, httpResp);
                }
            } else {
                if ((statusCode == 500 || statusCode == 404) && retryServerError > 0) {
                    streamJP2(id, region, size, rotation, quality,
                            format, outStream, response, retryServerError - 1);
                } else {
                    LOG.error("Unexpected failure: {}", httpResp.getStatusLine());
                    LOG.error("Path was: {}", method.getURI());
                }
            }
        } catch (ClientAbortException e) {
            LOG.debug("User client aborted request to stream jp2 for {}", id, e);
        } catch (Exception e) {
            LOG.error("Problem retrieving metadata for {}", path, e);
        } finally {
            method.releaseConnection();
        }
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
