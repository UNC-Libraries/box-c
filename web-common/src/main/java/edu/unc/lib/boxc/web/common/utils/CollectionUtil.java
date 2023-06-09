package edu.unc.lib.boxc.web.common.utils;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
