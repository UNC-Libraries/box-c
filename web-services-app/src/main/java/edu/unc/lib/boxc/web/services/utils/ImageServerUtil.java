package edu.unc.lib.boxc.web.services.utils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPaths.idToPath;

/**
 * @author snluong
 */
public class ImageServerUtil {
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
}
