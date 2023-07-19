
package edu.unc.lib.boxc.web.services.processing;
import static edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPaths.idToPath;

import edu.unc.lib.boxc.model.api.DatastreamType;
import edu.unc.lib.boxc.search.api.models.ContentObjectRecord;
import edu.unc.lib.boxc.search.api.models.Datastream;
import org.apache.commons.io.FilenameUtils;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Objects;

/**
 * Service to process access copy image downloads
 * @author snluong
 */
public class DownloadImageService {
    private String iiifBasePath;
    public static final String FULL_SIZE = "full";
    public static final String INVALID_SIZE_MESSAGE = "Unable to determine size for access copy download";

    /**
     * Method contacts the IIIF server for the requested access copy image and returns it
     * @param contentObjectRecord solr record of the file
     * @param size a string which is either "full" for full size or a pixel length like "1200"
     * @param pidString the UUID of the file
     * @return a response entity which contains headers and content of the access copy image
     * @throws IOException
     */
    public ResponseEntity<InputStreamResource> streamImage(ContentObjectRecord contentObjectRecord, String size)
            throws IOException {

        String pidString = contentObjectRecord.getPid().getId();
        String url = buildURL(pidString, size);
        InputStream input = new URL(url).openStream();
        InputStreamResource resource = new InputStreamResource(input);
        String filename = getDownloadFilename(contentObjectRecord, size);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.IMAGE_JPEG)
                .body(resource);
    }

    /**
     * A method that builds the IIIF URL based on an assumption of full region, 0 rotation, and default quality.
     * @param id the UUID of the file
     * @param size a string which is either "full" for full size or a pixel length like "1200"
     * @return a string which is the URL to request the IIIF server for the image
     */
    private String buildURL(String id, String size) {
        var formattedSize = size;
        var hash = idToPath(id, 4, 2);
        var formattedId = hash + id + ".jp2";
        if (!Objects.equals(size, FULL_SIZE)) {
            // pixel length should be in !123,123 format
            formattedSize = "!" + size + "," + size;
        }
        return iiifBasePath + formattedId + "/full/" + formattedSize + "/0/default.jpg";
    }

    /**
     * Determines size based on original dimensions of requested file, unless requested size is full size.
     * @param contentObjectRecord solr record of the file
     * @param size string of the requested size of the image
     * @return validated size string
     */
    public String getSize(ContentObjectRecord contentObjectRecord, String size) {
        if (!Objects.equals(size, FULL_SIZE)) {
            try {
                var integerSize = Integer.parseInt(size);

                if (integerSize <= 0 ) {
                    throw new IllegalArgumentException(INVALID_SIZE_MESSAGE);
                } else {
                    // format of dimensions is like 800x1200
                    var datastreamObject = getDatastream(contentObjectRecord);
                    String dimensions = datastreamObject.getExtent();
                    String[] dimensionParts = dimensions.split("x");
                    int longerSide = Math.max(Integer.parseInt(dimensionParts[0]), Integer.parseInt(dimensionParts[1]));
                    // request is bigger than or equal to full size, so we will switch to full size
                    if (integerSize >= longerSide) {
                        return FULL_SIZE;
                    }
                }
            } catch (Exception e) {
                throw new IllegalArgumentException(INVALID_SIZE_MESSAGE);
            }

        }
        return size;
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

    public void setIiifBasePath(String iiifBasePath) {
        this.iiifBasePath = iiifBasePath;
    }
}
