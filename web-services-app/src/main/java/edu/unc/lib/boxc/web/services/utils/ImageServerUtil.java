package edu.unc.lib.boxc.web.services.utils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPaths.idToPath;

/**
 * @author snluong
 */
public class ImageServerUtil {
    public static final String FULL_SIZE = "max";
    /**
     * Returns the object ID in proper encoded format with .jp2 extension
     * @param id
     * @return
     */
    public static String getImageServerEncodedId(String id) {
        var idPathEncoded = URLEncoder.encode(idToPath(id, 4, 2), StandardCharsets.UTF_8);
        var idEncoded = URLEncoder.encode(id, StandardCharsets.UTF_8);
        return idPathEncoded + idEncoded + ".jp2";
    }

    /**
     * A method that builds the IIIF URL based on an assumption of full region, 0 rotation, and default quality.
     * @param basePath iiif V3 base path
     * @param id the UUID of the file
     * @param size a string which is either "max" for full size or a pixel length like "1200"
     * @return a string which is the URL to request the IIIF server for the image
     */
    public static String buildURL(String basePath, String id, String size) {
        var formattedSize = size;
        var formattedId = getImageServerEncodedId(id);
        if (!Objects.equals(size, FULL_SIZE)) {
            // pixel length should be in !123,123 format
            formattedSize = "!" + size + "," + size;
        }
        return basePath + formattedId + "/full/" + formattedSize + "/0/default.jpg";
    }
}
