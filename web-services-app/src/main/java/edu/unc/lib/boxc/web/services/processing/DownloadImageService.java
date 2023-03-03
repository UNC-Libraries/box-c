
package edu.unc.lib.boxc.web.services.processing;

import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.model.api.DatastreamType;
import edu.unc.lib.boxc.search.api.models.ContentObjectRecord;
import edu.unc.lib.boxc.search.api.models.Datastream;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;

/**
 * @author snluong
 */
public class DownloadImageService {
    private String iiifBasePath;

    public ResponseEntity<InputStreamResource> streamImage(String pidString, String size)
            throws IOException {

        String url = buildURL(pidString, size);
        InputStream input = new URL(url).openStream();
        InputStreamResource resource = new InputStreamResource(input);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=image_" + size + ".jpg")
                .contentType(MediaType.IMAGE_JPEG)
                .body(resource);
    }

    /*
    A method that builds the IIIF URL based on an assumption of full region, 0 rotation, and default quality.
     */
    private String buildURL(String id, String size) {
        var formattedSize = size;
        if (!Objects.equals(size, "full")) {
            // pixel length should be in !123,123 format
            formattedSize = "!" + size + "," + size;
        }
        return iiifBasePath + id + "/full/" + formattedSize + "/0/default.jpg";
    }

    public String getSize(ContentObjectRecord contentObjectRecord, String size) {
        if (!Objects.equals(size, "full")) {
            // format of dimensions is like 800x1200
            String dimensions = contentObjectRecord.getDatastreamObject(DatastreamType.ORIGINAL_FILE.getId()).getExtent();
            String[] dimensionParts = dimensions.split("x");
            int longerSide = Math.max(Integer.parseInt(dimensionParts[0]), Integer.parseInt(dimensionParts[1]));

            if (Integer.parseInt(size) >= longerSide) {
                // request is bigger than or equal to full size, so we will switch to full size
                return "full";
            }
        }
        return size;
    }

    public void setIiifBasePath(String iiifBasePath) {
        this.iiifBasePath = iiifBasePath;
    }
}
