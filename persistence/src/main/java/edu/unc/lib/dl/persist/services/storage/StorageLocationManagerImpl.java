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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;

import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.RepositoryObject;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.services.ContentPathFactory;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.dl.persist.api.storage.StorageLocation;
import edu.unc.lib.dl.persist.api.storage.StorageLocationManager;
import edu.unc.lib.dl.persist.api.storage.UnknownStorageLocationException;

/**
 * Storage location manager implementation
 *
 * @author bbpennel
 *
 */
public class StorageLocationManagerImpl implements StorageLocationManager {
    private static final Logger log = LoggerFactory.getLogger(StorageLocationManagerImpl.class);

    private static final int COLLECTION_PATH_DEPTH = 2;
    protected static final String MAPPING_CONTAINER_KEY = "id";
    protected static final String MAPPING_DEFAULT_LOC_KEY = "defaultLocation";

    private String configPath;
    private String mappingPath;
    private Map<String, StorageLocation> idToStorageLocation;
    private List<StorageLocation> storageLocations;
    private Map<PID, StorageLocation> pidToStorageLocation;
    private ContentPathFactory pathFactory;
    private RepositoryObjectLoader repositoryObjectLoader;

    public StorageLocationManagerImpl() {
        pidToStorageLocation = new HashMap<>();

    }

    public void init() throws IOException {
        deserializeConfig();
        deserializeMapping();
    }

    private void deserializeConfig() throws IOException {
        InputStream configStream = new FileInputStream(new File(configPath));
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerSubtypes(
                new NamedType(HashedFilesystemStorageLocation.class, HashedFilesystemStorageLocation.TYPE_NAME),
                new NamedType(HashedPosixStorageLocation.class, HashedPosixStorageLocation.TYPE_NAME));
        storageLocations = mapper.readValue(configStream,
                new TypeReference<List<StorageLocation>>() {});
        idToStorageLocation = storageLocations.stream()
                .collect(Collectors.toMap(StorageLocation::getId, sl -> sl));
    }

    private void deserializeMapping() throws IOException {
        InputStream mappingStream = new FileInputStream(new File(mappingPath));
        ObjectMapper mapper = new ObjectMapper();
        List<StorageLocationMapping> mappingList = mapper.readValue(mappingStream,
                new TypeReference<List<StorageLocationMapping>>() {});

        for (StorageLocationMapping mapping: mappingList) {
            PID pid = PIDs.get(mapping.getId());
            String defaultLoc = mapping.getDefaultLocation();

            if (!idToStorageLocation.containsKey(defaultLoc)) {
                throw new UnknownStorageLocationException("Mapping for " + pid.getId()
                    + " refers to unknown storage location " + defaultLoc);
            }
            if (pidToStorageLocation.containsKey(pid)) {
                throw new IllegalStateException("Duplicate container key " + pid.getId());
            }

            pidToStorageLocation.put(pid, idToStorageLocation.get(defaultLoc));
        }
    }

    @Override
    public StorageLocation getDefaultStorageLocation(PID pid) {
        List<PID> ancestors = pathFactory.getAncestorPids(pid);
        ancestors.add(pid);

        // Search for the default location between the root and the collection level, nearest to furthest
        for (int i = Math.min(COLLECTION_PATH_DEPTH, ancestors.size() - 1); i >= 0; i--) {
            StorageLocation location = pidToStorageLocation.get(ancestors.get(i));
            if (location != null) {
                return location;
            }
        }

        throw new UnknownStorageLocationException(
                "Unable to determine the default storage location for object " + pid.getId());
    }

    @Override
    public List<StorageLocation> listAvailableStorageLocations(PID pid) {
        List<PID> ancestors = pathFactory.getAncestorPids(pid);
        ancestors.add(pid);

        return new ArrayList<>(ancestors.stream()
            .limit(COLLECTION_PATH_DEPTH + 1l)
            .map(pidToStorageLocation::get)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet()));
    }

    @Override
    public StorageLocation getStorageLocation(PID pid) {
        RepositoryObject repoObj = repositoryObjectLoader.getRepositoryObject(pid);
        return getStorageLocation(repoObj);
    }

    @Override
    public StorageLocation getStorageLocation(RepositoryObject repoObj) {
        Resource resc = repoObj.getResource();
        Statement locationProp = resc.getProperty(Cdr.storageLocation);
        if (locationProp != null) {
            StorageLocation location = getStorageLocationById(locationProp.getString());
            if (location == null) {
                throw new UnknownStorageLocationException("Object " + repoObj.getPid().getRepositoryPath()
                        + " is assigned to an unconfigured storage location " + locationProp.getString());
            }
            return location;
        }

        log.debug("Storage location not recorded for object {}, falling back to default",
                repoObj.getPid().getId());
        return getDefaultStorageLocation(repoObj.getPid());
    }

    @Override
    public StorageLocation getStorageLocationById(String id) {
        return idToStorageLocation.get(id);
    }

    @Override
    public StorageLocation getStorageLocationForUri(URI uri) {
        URI normalizedUri = uri.normalize();
        return storageLocations.stream()
                .filter(sl -> sl.isValidUri(normalizedUri))
                .findFirst()
                .orElseThrow(() -> new UnknownStorageLocationException(
                        "No configured storage locations matched " + uri));
    }

    public void setPathFactory(ContentPathFactory pathFactory) {
        this.pathFactory = pathFactory;
    }

    public void setRepositoryObjectLoader(RepositoryObjectLoader repositoryObjectLoader) {
        this.repositoryObjectLoader = repositoryObjectLoader;
    }

    public void setConfigPath(String configPath) {
        this.configPath = configPath;
    }

    public void setMappingPath(String mappingPath) {
        this.mappingPath = mappingPath;
    }

    public static class StorageLocationMapping {
        private String id;
        private String defaultLocation;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getDefaultLocation() {
            return defaultLocation;
        }

        public void setDefaultLocation(String defaultLocation) {
            this.defaultLocation = defaultLocation;
        }
    }
}
