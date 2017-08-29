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

import static edu.unc.lib.dl.fcrepo4.RepositoryPathConstants.HASHED_PATH_DEPTH;
import static edu.unc.lib.dl.fcrepo4.RepositoryPathConstants.HASHED_PATH_SIZE;
import static edu.unc.lib.dl.fcrepo4.RepositoryPaths.getBaseUri;

import java.net.URI;
import java.util.regex.Matcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.fedora.PID;

/**
 * Provides static methods for creating PID objects
 *
 * @author bbpennel
 *
 */
public class PIDs {

    private static final Logger log = LoggerFactory.getLogger(PIDs.class);

    private PIDs() {

    }
    /**
     * Get a PID object for the given URI.
     *
     * @param uri
     * @return new PID object for the given URI
     */
    public static PID get(URI uri) {
        return get(uri.toString());
    }

    /**
     * Get a PID object for the given identifier or URI string. Should either be
     * a fully qualified repository URI or follow the syntax for an identifier,
     * such as: deposit/uuid:0411cf7e-9ac0-4ab0-8c24-ff367e8e77f6
     *
     * @param value
     * @return new PID object for the given identifier or URI
     */
    public static PID get(String value) {
        if (value == null) {
            return null;
        }

        String id;
        String qualifier;
        String componentPath;
        String repositoryPath;

        if (value.startsWith(getBaseUri())) {
            // Given value was a fedora path. Remove the base and decompose
            String path = value.substring(getBaseUri().length());

            Matcher matcher = RepositoryPathConstants.repositoryPathPattern.matcher(path);
            if (matcher.matches()) {
                // extract the qualifier/category portion of the path, ex: deposit, content, etc.
                qualifier = matcher.group(2);
                // store the trailing component path, which is everything after the object identifier
                componentPath = matcher.group(8);
                // store the identifier for the main object
                id = matcher.group(5);
                if (id == null) {
                    id = matcher.group(6);
                }
                // Reconstruct the repository path from wanted components (excluding things like tx ids)
                repositoryPath = getRepositoryPath(matcher.group(3), qualifier, componentPath, false);
            } else {
                log.warn("Invalid path {}, cannot construct PID", value);
                // Value was an invalid path within the repository
                return null;
            }
        } else {
            // Determine if the value matches the pattern for an identifier
            Matcher matcher = RepositoryPathConstants.identifierPattern.matcher(value);
            if (matcher.matches()) {
                // Store the qualifier if specified, otherwise use the default "content" qualifier
                qualifier = matcher.group(2);
                if (qualifier == null) {
                    qualifier = RepositoryPathConstants.CONTENT_BASE;
                }
                // store the trailing component path
                componentPath = matcher.group(8);
                if (matcher.group(5) != null) {
                    // store the identifier for the main object
                    id = matcher.group(5);

                    // Expand the identifier into a repository path
                    repositoryPath = getRepositoryPath(id, qualifier, componentPath, true);
                } else {
                    // Reserved id found, path does not need to be expanded
                    id = matcher.group(6);

                    repositoryPath = getRepositoryPath(id, qualifier, componentPath, false);
                }
            } else {
                log.warn("Invalid qualified path {}, cannot construct PID", value);
                // Not a recognized format for constructing a pid
                return null;
            }
        }

        // Build and return the new pid object
        return new FedoraPID(id, qualifier, componentPath, URI.create(repositoryPath));
    }

    /**
     * Get a PID object with the given qualifier and id
     *
     * @param qualifier
     * @param id
     * @return
     */
    public static PID get(String qualifier, String id) {
        return get(qualifier + "/" + id);
    }

    /**
     * Expands the identifier for a repository object into the full repository path.
     *
     * @param id
     * @param qualifier
     * @param componentPath
     * @param expand if true, then the id will be prepended with hashed subfolders
     * @return
     */
    private static String getRepositoryPath(String id, String qualifier, String componentPath, boolean expand) {
        StringBuilder builder = new StringBuilder(getBaseUri());
        builder.append(qualifier).append('/');

        if (expand) {
            // Expand the id into chunked subfolders
            for (int i = 0; i < HASHED_PATH_DEPTH; i++) {
                builder.append(id.substring(i * HASHED_PATH_SIZE, i * HASHED_PATH_SIZE + HASHED_PATH_SIZE))
                        .append('/');
            }
        }

        builder.append(id);
        if (componentPath != null) {
            builder.append('/').append(componentPath);
        }
        return builder.toString();
    }
}
