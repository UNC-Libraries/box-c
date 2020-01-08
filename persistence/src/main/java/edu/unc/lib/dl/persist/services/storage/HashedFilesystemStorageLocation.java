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
package edu.unc.lib.dl.persist.services.storage;

import static edu.unc.lib.dl.fcrepo4.RepositoryPathConstants.HASHED_PATH_DEPTH;
import static edu.unc.lib.dl.fcrepo4.RepositoryPathConstants.HASHED_PATH_SIZE;

import java.net.URI;

import edu.unc.lib.dl.fcrepo4.RepositoryPaths;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.URIUtil;

/**
 * A filesystem based storage location which locates files in a hashed structure based on
 * the PID of the object.
 *
 * @author bbpennel
 *
 */
public class HashedFilesystemStorageLocation implements StorageLocation {
    public static final String TYPE_NAME = "hashed_fs";

    private String id;
    private String name;
    // The base path for files in this storage location
    private String base;
    private URI baseUri;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public StorageType getStorageType() {
        return StorageType.FILESYSTEM;
    }

    public String getBase() {
        return base;
    }

    public URI getBaseUri() {
        return baseUri;
    }

    public void setBase(String base) {
        this.base = base.replaceFirst(":///", ":/");
        if (!this.base.endsWith("/")) {
            this.base += "/";
        }
        baseUri = URI.create(this.base).normalize();
        if (baseUri.getScheme() == null) {
            this.base = "file:" + base;
            baseUri = URI.create(this.base).normalize();
        } else if (!"file".equals(baseUri.getScheme())) {
            throw new IllegalArgumentException("Only file URIs are acceptable in locations of type "
                    + getClass().getName());
        } else {
            // Ensure base string representation is normalized to match the uri representation
            this.base = baseUri.toString();
        }
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public URI getStorageUri(PID pid) {
        String objId = pid.getId();
        String derivativePath = RepositoryPaths
                .idToPath(objId, HASHED_PATH_DEPTH, HASHED_PATH_SIZE);

        URI storageUri;
        if (pid.getComponentPath() != null) {
            storageUri = URI.create(URIUtil.join(baseUri, derivativePath, objId, pid.getComponentPath()));
        } else {
            storageUri = URI.create(URIUtil.join(baseUri, derivativePath, objId));
        }
        return storageUri.normalize();
    }

    @Override
    public boolean isValidUri(URI uri) {
        return uri.toString().startsWith(baseUri.toString());
    }
}
