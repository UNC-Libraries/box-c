package edu.unc.lib.boxc.web.services.processing;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import edu.unc.lib.boxc.operations.api.images.ImageServerUtil;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
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
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.digitalcollections.iiif.model.ImageContent;
import de.digitalcollections.iiif.model.image.ImageApiProfile;
import de.digitalcollections.iiif.model.image.ImageService;
import de.digitalcollections.iiif.model.jackson.IiifObjectMapper;
import de.digitalcollections.iiif.model.sharedcanvas.Canvas;
import de.digitalcollections.iiif.model.sharedcanvas.Manifest;
import de.digitalcollections.iiif.model.sharedcanvas.Sequence;
import edu.unc.lib.boxc.common.util.URIUtil;
import edu.unc.lib.boxc.model.api.DatastreamType;
import edu.unc.lib.boxc.model.api.ResourceType;
import edu.unc.lib.boxc.search.api.models.ContentObjectRecord;
import edu.unc.lib.boxc.search.api.models.Datastream;
import edu.unc.lib.boxc.web.common.exceptions.ClientAbortException;

/**
 * Generates request, connects to, and streams the output from an iiif v2 server.  Sets pertinent headers.
 * @author bbpennel
 */
public class ImageServerV2Service {
    private static final Logger LOG = LoggerFactory.getLogger(ImageServerV2Service.class);

    private CloseableHttpClient httpClient;
    private HttpClientConnectionManager httpClientConnectionManager;

    private String imageServerProxyBasePath;
    private String basePath;
    private String accessAppPath;
    private ObjectMapper iiifMapper = new IiifObjectMapper();

    public void setHttpClientConnectionManager(HttpClientConnectionManager manager) {
        this.httpClientConnectionManager = manager;

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(2000)
                .setConnectionRequestTimeout(5000)
                .build();

        this.httpClient = HttpClients.custom()
                .setConnectionManager(httpClientConnectionManager)
                .setDefaultRequestConfig(requestConfig)
                .build();
    }

    public void getMetadata(String simplepid, OutputStream outStream, HttpServletResponse response) {
        getMetadata(simplepid, outStream, response, 1);
    }

    public void getMetadata(String simplepid, OutputStream outStream,
            HttpServletResponse response, int retryServerError) {

        StringBuilder path = new StringBuilder(getImageServerProxyBasePath());
        path.append(ImageServerUtil.getImageServerEncodedId(simplepid)).append("/info.json");

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

                        ImageService respData = iiifMapper.readValue(httpResp.getEntity().getContent(),
                                ImageService.class);
                        respData.setIdentifier(new URI(URIUtil.join(basePath, "iiif", "v2", simplepid)));

                        HttpEntity updatedRespData = EntityBuilder.create()
                                .setText(iiifMapper.writeValueAsString(respData))
                                .setContentType(ContentType.APPLICATION_JSON).build();
                        httpResp.setEntity(updatedRespData);

                        try (InputStream in = httpResp.getEntity().getContent()) {
                            IOUtils.copy(in, outStream);
                            outStream.flush();
                        } catch (IOException e) {
                            throw new ClientAbortException(e);
                        }
                    }
                    return;
                }
            } catch (ClientAbortException e) {
                LOG.debug("User client aborted request to stream metadata for {}", simplepid, e);
            } catch (Exception e) {
                LOG.error("Problem retrieving metadata for {}", path, e);
            } finally {
                method.releaseConnection();
            }
            retryServerError--;
        } while (retryServerError >= 0 && (statusCode == 500 || statusCode == 404));
        LOG.error("Unexpected failure while getting image server path {}: {}", statusLine, path);
    }

    public void streamJP2(String simplepid, String region, String size, String rotatation, String quality,
            String format, OutputStream outStream, HttpServletResponse response ) {
        this.streamJP2(simplepid, region, size, rotatation, quality, format, outStream, response, 1);
    }

    public void streamJP2(String simplepid, String region, String size, String rotation, String quality,
            String format, OutputStream outStream, HttpServletResponse response,
            int retryServerError) {

        StringBuilder path = new StringBuilder(getImageServerProxyBasePath());
        // Remap "full" size to "max" since cantaloupe's v2 api doesn't seem to be able to handle full/full correctly
        if ("full".equals(size)) {
            size = ImageServerUtil.FULL_SIZE;
        }
        path.append(ImageServerUtil.getImageServerEncodedId(simplepid))
                .append("/").append(region).append("/").append(size)
                .append("/").append(rotation).append("/").append(quality)
                .append(".").append(format);

        HttpGet method = new HttpGet(path.toString());

        try (CloseableHttpResponse httpResp = httpClient.execute(method)) {
            int statusCode = httpResp.getStatusLine().getStatusCode();

            if (statusCode == HttpStatus.SC_OK) {
                if (response != null) {
                    response.setHeader("Content-Type", "image/jpeg");
                    response.setHeader("content-disposition", "inline");

                    try (InputStream in = httpResp.getEntity().getContent()) {
                        IOUtils.copy(in, outStream);
                        outStream.flush();
                    } catch (IOException e) {
                        throw new ClientAbortException(e);
                    }
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

    public String getManifest(String id, List<ContentObjectRecord> briefObjs)
            throws JsonProcessingException {
        String manifestBase = getRecordPath(id);
        ContentObjectRecord rootObj = briefObjs.get(0);

        String title = getTitle(rootObj);

        Manifest manifest = new Manifest(URIUtil.join(manifestBase, "manifest"), title);

        String abstractText = rootObj.getAbstractText();
        List<String> creators = rootObj.getCreator();
        List<String> subjects = rootObj.getSubject();
        List<String> language = rootObj.getLanguage();

        if (abstractText != null) {
            manifest.addDescription(abstractText);
        }

        manifest.addLogo(new ImageContent(URIUtil.join(basePath, "static", "images", "unc-icon.png")));

        setMetadataField(manifest, "Creators", creators);
        setMetadataField(manifest, "Subjects", subjects);
        setMetadataField(manifest, "Languages", language);
        manifest.addMetadata("", "<a href=\"" +
                URIUtil.join(accessAppPath, "record", rootObj.getId()) + "\">View full record</a>");
        String attribution = "University of North Carolina Libraries, Digital Collections Repository";
        String collection = rootObj.getParentCollectionName();
        if (collection != null) {
            attribution += " - Part of " + collection;
        }
        manifest.addMetadata("Attribution", attribution);

        Sequence seq = createSequence(manifestBase, briefObjs);

        return iiifMapper.writeValueAsString(manifest.addSequence(seq));
    }

    public String getSequence(String id, List<ContentObjectRecord> briefObjs)
            throws JsonProcessingException {
        String path = getRecordPath(id);
        return iiifMapper.writeValueAsString(createSequence(path, briefObjs));
    }

    public String getCanvas(String id, ContentObjectRecord briefObj)
            throws JsonProcessingException {
        String path = getRecordPath(id);
        return iiifMapper.writeValueAsString(createCanvas(path, briefObj));
    }

    private Sequence createSequence(String seqPath, List<ContentObjectRecord> briefObjs) {
        Sequence seq = new Sequence(URIUtil.join(seqPath, "sequence", "normal"));

        ContentObjectRecord rootObj = briefObjs.get(0);
        if (rootObj.getResourceType().equals(ResourceType.Work.name())) {
            String rootJp2Id = jp2Pid(rootObj);
            briefObjs.remove(0);
            // Move the primary object to the beginning of the sequence
            if (rootJp2Id != null && !rootJp2Id.equals(rootObj.getId())) {
                for (int i = 0; i < briefObjs.size(); i++) {
                    ContentObjectRecord briefObj = briefObjs.get(i);
                    if (briefObj.getId().equals(rootJp2Id)) {
                        if (i != 0) {
                            briefObjs.remove(i);
                            briefObjs.add(0, briefObj);
                        }
                        break;
                    }
                }
            }
        }

        for (ContentObjectRecord briefObj : briefObjs) {
            String datastreamUuid = jp2Pid(briefObj);
            if (!StringUtils.isEmpty(datastreamUuid)) {
                Canvas canvas = createCanvas(seqPath, briefObj);
                seq.addCanvas(canvas);
            }
        }

        return seq;
    }

    private Canvas createCanvas(String path, ContentObjectRecord briefObj) {
        String title = getTitle(briefObj);
        String uuid = jp2Pid(briefObj);

        Canvas canvas = new Canvas(path, title);
        if (StringUtils.isEmpty(uuid)) {
            LOG.warn("No jp2 id was found for {}. IIIF canvas is empty.", briefObj.getId());
            return canvas;
        }

        String canvasPath = URIUtil.join(basePath, "iiif", "v2", uuid);

        Datastream fileDs = briefObj.getDatastreamObject(DatastreamType.ORIGINAL_FILE.getId());
        String extent = fileDs.getExtent();
        if (extent != null && !extent.isEmpty()) {
            String[] imgDimensions = extent.split("x");
            canvas.setHeight(Integer.parseInt(imgDimensions[0]));
            canvas.setWidth(Integer.parseInt(imgDimensions[1]));
        }

        canvas.addIIIFImage(canvasPath, ImageApiProfile.LEVEL_TWO);
        ImageContent thumb = new ImageContent(URIUtil.join(basePath,
                "services", "api", "thumb", uuid, "large"));
        canvas.addImage(thumb);

        return canvas;
    }

    private String getRecordPath(String uuid) {
        return URIUtil.join(basePath, "iiif", "v2", uuid);
    }

    private String jp2Pid(ContentObjectRecord briefObj) {
        Datastream datastream = briefObj.getDatastreamObject(DatastreamType.JP2_ACCESS_COPY.getId());
        if (datastream != null) {
            String id = datastream.getOwner();
            if (id.isEmpty()) {
                return briefObj.getId();
            }
            return id;
        }

        return null;
    }

    private void setMetadataField(Manifest manifest, String fieldName, List<String> field) {
        if (!CollectionUtils.isEmpty(field)) {
            manifest.addMetadata(fieldName, String.join(", ", field));
        }
    }

    private String getTitle(ContentObjectRecord briefObj) {
        String title = briefObj.getTitle();
        return (title != null) ? title : "";
    }

    public void setImageServerProxyBasePath(String fullPath) {
        this.imageServerProxyBasePath = fullPath;
    }

    public String getImageServerProxyBasePath() {
        return imageServerProxyBasePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public String getBasePath() {
        return basePath;
    }

    public void setAccessAppPath(String accessAppPath) {
        this.accessAppPath = accessAppPath;
    }
}
