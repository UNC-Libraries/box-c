package edu.unc.lib.boxc.web.services.processing;

import static edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPaths.idToPath;

import java.io.OutputStream;
import java.net.URI;

import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectReader;
import info.freelibrary.iiif.presentation.v3.services.ImageService3;
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

import com.fasterxml.jackson.databind.ObjectMapper;

import de.digitalcollections.iiif.model.image.ImageService;
import de.digitalcollections.iiif.model.jackson.IiifObjectMapper;
import edu.unc.lib.boxc.common.util.URIUtil;
import edu.unc.lib.boxc.web.common.exceptions.ClientAbortException;
import edu.unc.lib.boxc.web.common.utils.FileIOUtil;



/**
 * Generates request, connects to, and streams the output from the image Server Proxy.  Sets pertinent headers.
 * @author bbpennel, snluong
 */
public class ImageServerProxyService {
    private static final Logger LOG = LoggerFactory.getLogger(ImageServerProxyService.class);
    private CloseableHttpClient httpClient;
    private String imageServerProxyPath;
    private String basePath;

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

    public void getMetadata(String simplepid, OutputStream outStream,
                            HttpServletResponse response, int retryServerError) {

        StringBuilder path = new StringBuilder(getImageServerProxyPath());
        path.append(idToPath(simplepid, 4, 2))
                .append(simplepid).append(".jp2").append("/info.json");

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

                        ObjectReader iiifReader = new ObjectMapper().readerFor(ImageService3.class);
                        ImageService3 respData = iiifReader.readValue(httpResp.getEntity().getContent());
                        var iiifWriter = new ObjectMapper().writerFor(ImageService3.class);

                        respData.setID(new URI(URIUtil.join(imageServerProxyPath, simplepid)));

                        HttpEntity updatedRespData = EntityBuilder.create()
                                .setText(iiifWriter.writeValueAsString(respData))
                                .setContentType(ContentType.APPLICATION_JSON).build();
                        httpResp.setEntity(updatedRespData);

                        FileIOUtil.stream(outStream, httpResp);
                    }
                    return;
                }
            } catch (ClientAbortException e) {
                LOG.debug("User client aborted request to stream jp2 metadata for {}", simplepid, e);
            } catch (Exception e) {
                LOG.error("Problem retrieving metadata for {}", path, e);
            } finally {
                method.releaseConnection();
            }
            retryServerError--;
        } while (retryServerError >= 0 && (statusCode == 500 || statusCode == 404));
        LOG.error("Unexpected failure while getting image server proxy path {}: {}", statusLine, path);
    }

    public void streamJP2(String simplepid, String region, String size, String rotation, String quality,
                          String format, OutputStream outStream, HttpServletResponse response,
                          int retryServerError) {

        StringBuilder path = new StringBuilder(getImageServerProxyPath());
        path.append(idToPath(simplepid, 4, 2)).append(simplepid).append(".jp2")
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
                    streamJP2(simplepid, region, size, rotation, quality,
                            format, outStream, response, retryServerError - 1);
                } else {
                    LOG.error("Unexpected failure: {}", httpResp.getStatusLine());
                    LOG.error("Path was: {}", method.getURI());
                }
            }
        } catch (ClientAbortException e) {
            LOG.debug("User client aborted request to stream jp2 for {}", simplepid, e);
        } catch (Exception e) {
            LOG.error("Problem retrieving metadata for {}", path, e);
        } finally {
            method.releaseConnection();
        }
    }

    public void setImageServerProxyPath(String fullPath) {
        this.imageServerProxyPath = fullPath;
    }

    public String getImageServerProxyPath() {
        return imageServerProxyPath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }
}
