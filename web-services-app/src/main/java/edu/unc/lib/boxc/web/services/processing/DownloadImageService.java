
package edu.unc.lib.boxc.web.services.processing;

import edu.unc.lib.boxc.model.api.DatastreamType;
import edu.unc.lib.boxc.model.api.exceptions.NotFoundException;
import edu.unc.lib.boxc.search.api.models.ContentObjectRecord;
import edu.unc.lib.boxc.search.api.models.Datastream;
import edu.unc.lib.boxc.operations.api.images.ImageServerUtil;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static edu.unc.lib.boxc.operations.api.images.ImageServerUtil.FULL_SIZE;

/**
 * Service to process access copy image downloads
 * @author snluong
 */
public class DownloadImageService {
    private String iiifBasePath;

    public static final String INVALID_SIZE_MESSAGE = "Unable to determine size for access copy download";

    /**
     * Method contacts the IIIF server for the requested access copy image and returns it
     * @param contentObjectRecord solr record of the file
     * @param size a string which is either "max" for full size or a pixel length like "1200"
     * @return a response entity which contains headers and content of the access copy image
     * @throws IOException
     */
    public ResponseEntity<InputStreamResource> streamImage(ContentObjectRecord contentObjectRecord, String size, boolean attachment)
            throws IOException {
        if (contentObjectRecord.getDatastreamObject(DatastreamType.JP2_ACCESS_COPY.getId()) == null) {
            return ResponseEntity.notFound().build();
        }

        var contentDispositionHeader = "inline;";
        if (attachment) {
            String filename = getDownloadFilename(contentObjectRecord, size);
            contentDispositionHeader = "attachment; filename=" + filename;
        }

        var url = "";
        var contentType = MediaType.IMAGE_JPEG;
        if (needsPlaceholder(contentObjectRecord, size)) {
            url = getPlaceholderUrl(size);
            contentType = MediaType.IMAGE_PNG;
        } else {
            String pidString = contentObjectRecord.getPid().getId();
            url = ImageServerUtil.buildURL(iiifBasePath, pidString, size);
        }

        InputStream input = new URL(url).openStream();
        InputStreamResource resource = new InputStreamResource(input);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDispositionHeader)
                .contentType(contentType)
                .body(resource);
    }

    /**
     * Determines size based on original dimensions of requested file, unless requested size is full size.
     * @param contentObjectRecord solr record of the file
     * @param size string of the requested size of the image
     * @return validated size string
     */
    public String getSize(ContentObjectRecord contentObjectRecord, String size) {
        if (!Objects.equals(size, FULL_SIZE)) {
            int integerSize = parseSize(size);
            var longerSide = getLongestSide(contentObjectRecord);
            // request is bigger than or equal to full size, so we will switch to full size
            if (integerSize >= longerSide) {
                return FULL_SIZE;
            }
        }
        return size;
    }

    public int parseSize(String size) {
        try {
            var integerSize = Integer.parseInt(size);
            if (integerSize > 0) {
                return integerSize;
            }
        } catch (NumberFormatException e) {
            // Triggers IllegalArgumentException below
        }
        throw new IllegalArgumentException(INVALID_SIZE_MESSAGE);
    }

    /**
     * Formats the original filename to include size for access download filename
     * @param contentObjectRecord solr record of the file
     * @param size validated size string from getSize
     * @return a filename for the download like "filename_full.jpg" or "filename_800px.jpg
     */
    public String getDownloadFilename(ContentObjectRecord contentObjectRecord, String size) {
        var formattedSize = Objects.equals(size, FULL_SIZE) ?  FULL_SIZE : size + "px";

        var originalFilename = getDatastream(contentObjectRecord).getFilename();
        var nameOnly = FilenameUtils.removeExtension(originalFilename);

        return nameOnly + "_" + formattedSize + ".jpg";
    }

    private Datastream getDatastream(ContentObjectRecord contentObjectRecord) {
        var originalFileDatastream = contentObjectRecord.getDatastreamObject(
                DatastreamType.ORIGINAL_FILE.getId());
        if (originalFileDatastream == null) {
            return contentObjectRecord.getDatastreamObject(DatastreamType.JP2_ACCESS_COPY.getId());
        }
        return originalFileDatastream;
    }

    /**
     * Get the extent value for the JP2 datastream, falling back to the original file if not set
     * @param contentObjectRecord
     * @return extent in string format
     */
    private String getExtent(ContentObjectRecord contentObjectRecord) {
        var ds = contentObjectRecord.getDatastreamObject(DatastreamType.JP2_ACCESS_COPY.getId());
        String extent = ds == null ? null : ds.getExtent();
        if (StringUtils.isEmpty(extent)) {
            ds = contentObjectRecord.getDatastreamObject(DatastreamType.ORIGINAL_FILE.getId());
        }
        if (ds == null) {
            throw new NotFoundException("No jp2 or original_file available for " + contentObjectRecord.getId());
        }
        return ds.getExtent();
    }

    /**
     * If the shortest side of the content object is smaller than the size requested, return the placeholder
     * @param contentObjectRecord
     * @param size
     * @return true if service needs to return a placeholder image
     */
    private boolean needsPlaceholder(ContentObjectRecord contentObjectRecord, String size) {
        if (Objects.equals(FULL_SIZE, size)) {
            return false;
        }
        var shortestSide = getShortestSide(contentObjectRecord);
        return shortestSide < Integer.parseInt(size);
    }

    private String[] getImageDimensions(ContentObjectRecord contentObjectRecord) {
        var extent = getExtent(contentObjectRecord);
        if (extent == null || extent.isEmpty()) {
            return ArrayUtils.EMPTY_STRING_ARRAY;
        }
        // format of dimensions is like 800x1200, heightxwidth
        return extent.split("x");
    }

    private int getLongestSide(ContentObjectRecord contentObjectRecord) {
        var extent = getImageDimensions(contentObjectRecord);
        if (extent.length == 0) {
            return 0;
        }

        return Math.max(Integer.parseInt(extent[0]), Integer.parseInt(extent[1]));
    }

    private int getShortestSide(ContentObjectRecord contentObjectRecord) {
        var extent = getImageDimensions(contentObjectRecord);
        if (extent.length == 0) {
            return 0;
        }
        return Math.min(Integer.parseInt(extent[0]), Integer.parseInt(extent[1]));
    }

    private String getPlaceholderUrl(String size) {
        // pixel length should be in !123,123 format
        var formattedSize = "!" + size + "," + size;
        var fileId = URLEncoder.encode("/default_images/placeholder.png", StandardCharsets.UTF_8);
        return iiifBasePath + fileId + "/full/" + formattedSize + "/0/default.jpg";
    }

    public void setIiifBasePath(String iiifBasePath) {
        this.iiifBasePath = iiifBasePath;
    }
}
