/**
 * Copyright 2008 The University of North Carolina at Chapel Hill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.unc.lib.dl.ui.util;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Iterator;

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
