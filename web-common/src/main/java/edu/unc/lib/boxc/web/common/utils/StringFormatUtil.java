package edu.unc.lib.boxc.web.common.utils;

/**
 * String manipulation utility methods for formatting and rendering string data
 * to web clients.
 *
 * @author bbpennel
 *
 */
public class StringFormatUtil {
    private StringFormatUtil() {
    }

    /**
     * Removes a query parameter from a URL
     *
     * @param query
     * @param key
     * @return
     */
    public static String removeQueryParameter(String query, String key) {
        int index = query.indexOf("&" + key + "=");
        boolean isFirstParameter = false;
        if (index == -1) {
            index = query.indexOf(key + "=");
            if (index == -1) {
                return query;
            }
            isFirstParameter = index > 0 && query.charAt(index - 1) == '?';
            if (!isFirstParameter) {
                return query;
            }
        }

        int end = query.indexOf('&', index + 1);
        if (end == -1) {
            return query.substring(0, index - (isFirstParameter ? 1 : 0));
        }
        return query.substring(0, index) + query.substring(end + (isFirstParameter ? 1 : 0));
    }
}
