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
package edu.unc.lib.dl.fcrepo4;

import java.util.regex.Pattern;

/**
 * Constants for constructing standard paths within the repository.
 *
 * @author bbpennel
 *
 */
public abstract class RepositoryPathConstants {

    public static final String REPOSITORY_ROOT_ID = "bxc:root";

    // Repository base paths

    public static final String CONTENT_BASE = "content";

    public static final String DEPOSIT_RECORD_BASE = "deposits";

    // Container identifiers

    public static final String DEPOSIT_MANIFEST_CONTAINER = "manifest";

    public static final String EVENTS_CONTAINER = "event";

    public static final String DATA_FILE_FILESET = "datafs";

    public static final String MEMBER_CONTAINER = "member";

    // Named objects

    public static final String ORIGINAL_FILE = "original_file";

    public static final String CONTENT_ROOT_ID = "collections";

    // Special Fedora paths

    public static final String FCR_METADATA = "fcr:metadata";
    public static final String FCR_TOMBSTONE = "fcr:tombstone";

    // Base path generation and decomposition values

    // Number of levels of hierarchy to generate when create a hashed path
    public static final int HASHED_PATH_DEPTH = 4;

    // Number of characters to use per level of hierarchy in a hashed path
    public static final int HASHED_PATH_SIZE = 2;

    // Regex pattern for decomposing a repository URI for an object or component of an object
    // Group 1 = transaction id
    // Group 2 = base container
    // Group 3 = expanded object id
    // Group 4 = hashed path to object
    // Group 5 = object uuid
    // Group 6 = reserved object id
    // Group 7 = component path (nested resources in object)
    // Group 8 = relative component path
    public static final Pattern repositoryPathPattern = Pattern
            .compile("/?(tx:[a-f0-9\\-]+/)?([a-zA-Z]+)/(([a-f0-9]{"
                    + HASHED_PATH_SIZE + "}/){" + HASHED_PATH_DEPTH + "}"
                    + "([a-f0-9\\-]+)|(" + CONTENT_ROOT_ID + "))(/(.+)?)?");

    // Regex pattern for decomposing an identifier for an object or component
    public static final Pattern identifierPattern = Pattern
            .compile("(([a-zA-Z]+)/)?(uuid:)?(([a-f0-9\\-]+)|(" + CONTENT_ROOT_ID + "))(/(.+)?)?");
}
