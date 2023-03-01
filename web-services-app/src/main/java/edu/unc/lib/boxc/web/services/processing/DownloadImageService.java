
package edu.unc.lib.boxc.web.services.processing;

import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URL;

/**
 * @author snluong
 */
public class DownloadImageService {
    private AccessControlService aclService;

    public ResponseEntity<InputStreamResource> streamImage(String id, String size, AccessGroupSet principals, HttpServletResponse response) throws FileNotFoundException {
        response.setContentType("image/jpeg");


        MediaType mediaType = MediaTypeUtils.getMediaTypeForFileName(this.servletContext, file);
        InputStream input = new URL("http://localhost:48080/loris/").openStream();
        InputStreamResource resource = new InputStreamResource(input);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=generic_file_name.bin")
                .contentType(mediaType)
                .body(resource);
    }

    /*
    A method that builds the IIIF URL based on an assumption of full region, 0 rotation, and default quality.
     */
    private String buildURL(String id, String size) {
        String base = "http://localhost:48080/loris/";
        return base + id + "/full/" + size + "/0/default.jpg";
    }
}
