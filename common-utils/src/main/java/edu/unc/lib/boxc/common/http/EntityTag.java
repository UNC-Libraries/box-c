package edu.unc.lib.boxc.common.http;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Object containing the value of an entity tag http header
 *
 * @author bbpennel
 *
 */
public class EntityTag {

    private static final Pattern ETAG_PATTERN = Pattern.compile("(W/)?\"([^\"]+)\"");

    private boolean weak;
    private String value;

    public EntityTag(String etagString) {
        Matcher etagMatcher = ETAG_PATTERN.matcher(etagString);
        if (!etagMatcher.matches()) {
            throw new IllegalArgumentException("Invalid etag header " + etagString);
        }

        weak = etagMatcher.group(1) != null;
        value = etagMatcher.group(2);
    }

    /**
     * True if the header is a weak etag
     *
     * @return
     */
    public boolean isWeak() {
        return weak;
    }

    /**
     * Value of the etag header
     *
     * @return
     */
    public String getValue() {
        return value;
    }

}
