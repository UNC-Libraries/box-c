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
package edu.unc.lib.dcr.migration;

import static java.lang.String.format;

import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Constants for the migration itself
 *
 * @author bbpennel
 *
 */
public class MigrationConstants {

    private MigrationConstants() {
    }

    public static final Pattern UUID_PATTERN = Pattern.compile(
            ".*([0-9oa-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}).*");

    public static final String OUTPUT_LOGGER = "output";

    /**
     * Extract the UUID portion of a file path
     *
     * @param path path of the file
     * @return
     */
    public static String extractUUIDFromPath(Path path) {
        return extractUUIDFromPath(path.toString());
    }

    /**
     * Extract the UUID portion of a string. If no uuid found, then an
     * IllegalArgumentException is thrown.
     *
     * @param path string representation of a path
     * @return uuid
     */
    public static String extractUUIDFromPath(String path) {
        Matcher pidMatcher = UUID_PATTERN.matcher(path.toString());
        if (pidMatcher.matches()) {
            return pidMatcher.group(1);
        } else {
            throw new IllegalArgumentException(format("Path %s does not contain a UUID", path.toString()));
        }
    }

    public static final String PREMIS_DS = "MD_EVENTS";
    public static final String ORIGINAL_DS = "DATA_FILE";
    public static final String FITS_DS = "MD_TECHNICAL";
    public static final String MANIFEST_DS = "DATA_MANIFEST";
}
