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
