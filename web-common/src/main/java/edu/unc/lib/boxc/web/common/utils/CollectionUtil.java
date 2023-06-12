package edu.unc.lib.boxc.web.common.utils;

/**
 * 
 * @author count0
 *
 */
public class CollectionUtil {
    private CollectionUtil() {
    }

    public static String join(Iterable<?> elements, String separator ) {
        if (elements == null) {
            return "";
        }
        StringBuilder buf = new StringBuilder();
        if (separator == null) {
            separator = " ";
        }

        for (Object o: elements) {
            if (buf.length() > 0) {
                buf.append(separator);
            }
            buf.append(o);
        }
        return buf.toString();
    }
}
