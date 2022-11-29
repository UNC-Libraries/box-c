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

    public static boolean contains(Collection<?> coll, Object target ) {
        if (coll == null) {
            return false;
        }
        return coll.contains(target);
    }

    public static List<?> subList(List<?> list, int fromIndex, int toIndex) {
        return list.subList(fromIndex, toIndex);
    }

    public static String joinLookup(Iterable<?> elements, String separator, Map<?,?> lookup ) {
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
            buf.append(lookup.get(o));
        }
        return buf.toString();
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

    public static String join(Map<?,?> elements, String keySeparator, String entrySeparator ) {
        if (elements == null) {
            return "";
        }
        StringBuilder buf = new StringBuilder();
        if (keySeparator == null) {
            keySeparator = " ";
        }
        if (entrySeparator == null) {
            entrySeparator = " ";
        }

        Iterator<?> elementsIt = elements.entrySet().iterator();
        while (elementsIt.hasNext() ) {
            if (buf.length() > 0) {
                buf.append(entrySeparator);
            }
            Map.Entry<?, ?> element = (Map.Entry<?, ?>)elementsIt.next();
            buf.append(element.getKey()).append(keySeparator).append(element.getValue());
        }
        return buf.toString();
    }

    public static void decrementLongMap(Map<String,Long> map, String key ) {
        if (!map.containsKey(key)) {
            return;
        }
        map.put(key, map.get(key) - 1);
    }
}
