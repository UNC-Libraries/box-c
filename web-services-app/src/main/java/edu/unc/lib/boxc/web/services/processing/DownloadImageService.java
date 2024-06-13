
package edu.unc.lib.boxc.web.services.processing;

import edu.unc.lib.boxc.model.api.DatastreamType;
import edu.unc.lib.boxc.model.api.exceptions.NotFoundException;
import edu.unc.lib.boxc.search.api.models.ContentObjectRecord;
import edu.unc.lib.boxc.search.api.models.Datastream;
import edu.unc.lib.boxc.operations.api.images.ImageServerUtil;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
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
    public ResponseEntity<InputStreamResource> streamImage(ContentObjectRecord contentObjectRecord, String size)
            throws IOException {
        if (contentObjectRecord.getDatastreamObject(DatastreamType.JP2_ACCESS_COPY.getId()) == null) {
            return ResponseEntity.notFound().build();
        }

        String pidString = contentObjectRecord.getPid().getId();
        String url = ImageServerUtil.buildURL(iiifBasePath, pidString, size);
        InputStream input = new URL(url).openStream();
        InputStreamResource resource = new InputStreamResource(input);
        String filename = getDownloadFilename(contentObjectRecord, size);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.IMAGE_JPEG)
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
        var id = DatastreamType.ORIGINAL_FILE.getId();
        return contentObjectRecord.getDatastreamObject(id);
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

    private int getLongestSide(ContentObjectRecord contentObjectRecord) {
        var extent = getExtent(contentObjectRecord);
        if (extent == null || extent.isEmpty()) {
            return 0;
        }
        // format of dimensions is like 800x1200, heightxwidth
        String[] dimensionParts = extent.split("x");
        return Math.max(Integer.parseInt(dimensionParts[0]), Integer.parseInt(dimensionParts[1]));
    }

    public void setIiifBasePath(String iiifBasePath) {
        this.iiifBasePath = iiifBasePath;
    }
}
