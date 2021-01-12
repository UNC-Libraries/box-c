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
        if (StringUtils.isBlank(mimetype)) {
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
