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
package edu.unc.lib.dl.util;

import java.net.URI;

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
            if (segments[i - 1] == null) {
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
}
