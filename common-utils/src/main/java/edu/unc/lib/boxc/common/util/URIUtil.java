package edu.unc.lib.boxc.common.util;

import static java.text.Normalizer.Form.NFD;
import static java.util.Locale.ENGLISH;

import java.net.URI;
import java.text.Normalizer;
import java.util.regex.Pattern;

/**
 *
 * @author bbpennel
 *
 */
public class URIUtil {
    private URIUtil() {
    }

    /**
     * Join path segments to a base URI
     *
     * @param base
     * @param segments
     * @return
     */
    public static String join(URI base, String... segments) {
        return join(base != null ? base.toString() : null, segments);
    }

    /**
     * Join path segments to form a URI path. Path will not end with a trailing
     * slash.
     *
     * @param base
     *            Base of the path. If null or empty, the resulting path will be
     *            relative
     * @param segments
     *            Segments to join to the base path.
     * @return
     */
    public static String join(String base, String... segments) {
        StringBuilder pathBuilder = new StringBuilder();
        if (base != null && !base.isEmpty()) {
            if (base.charAt(base.length() - 1) == '/') {
                pathBuilder.append(base.substring(0, base.length() - 1));
            } else {
                pathBuilder.append(base);
            }
        }

        int i = 0;
        for (String segment : segments) {
            i++;
            if (segments[i - 1] == null || segment.equals("")) {
                continue;
            }
            if (segment.charAt(0) != '/' && pathBuilder.length() > 0) {
                pathBuilder.append('/');
            }
            if (segment.charAt(segment.length() - 1) == '/') {
                pathBuilder.append(segment.substring(0, segment.length() - 1));
            } else {
                pathBuilder.append(segment);
            }
        }

        return pathBuilder.toString();
    }

    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

    /**
     * Encode the provided value as a slug
     *
     * @param value value to encode
     * @return value encoded as a slug
     */
    public static String toSlug(String value) {
        String nowhitespace = WHITESPACE.matcher(value).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, NFD);
        String slug = NONLATIN.matcher(normalized).replaceAll("");
        return slug.toLowerCase(ENGLISH);
      }
}
