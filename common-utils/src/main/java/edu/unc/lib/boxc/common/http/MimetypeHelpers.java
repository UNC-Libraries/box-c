package edu.unc.lib.boxc.common.http;

import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

/**
 * Helper methods for working with mimetypes
 *
 * @author bbpennel
 */
public class MimetypeHelpers {

    private final static Pattern VALID_MIMETYPE_PATTERN = Pattern.compile("\\w+/[-+.\\w]+");

    private MimetypeHelpers() {
    }

    /**
     * @param mimetype
     * @return true if the given mimetype appears to be a valid mimetype
     */
    public static boolean isValidMimetype(String mimetype) {
        if (StringUtils.isBlank(mimetype) || mimetype.contains("symlink")) {
            return false;
        }
        return VALID_MIMETYPE_PATTERN.matcher(mimetype).matches();
    }

    /**
     * Normalize the given mimetype, removing parameters and converting to lowercase
     * @param mimetype
     * @return
     */
    public static String formatMimetype(String mimetype) {
        return (mimetype != null) ? mimetype.trim().toLowerCase().split("[;,]")[0] : null;
    }
}
