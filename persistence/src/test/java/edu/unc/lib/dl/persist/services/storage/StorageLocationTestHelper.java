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
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.unc.lib.dl.persist.services.storage.StorageLocationManagerImpl.StorageLocationMapping;

/**
 * @author bbpennel
 *
 */
public class StorageLocationTestHelper {

    private List<StorageLocationMapping> mappingList;
    private List<Map<String, String>> locationList;

    public StorageLocationTestHelper() {
        mappingList = new ArrayList<>();
        locationList = new ArrayList<>();
    }

    public void addStorageLocation(String id, String name, String base) throws IOException {
        Map<String, String> info = new HashMap<>();
        info.put("id", id);
        info.put("name", name);
        info.put("type", HashedFilesystemStorageLocation.TYPE_NAME);
        info.put("base", base);

        locationList.add(info);
    }

    public void addMapping(String id, String defaultLoc) {
        StorageLocationMapping mapping = new StorageLocationMapping();
        mapping.setId(id);
        mapping.setDefaultLocation(defaultLoc);
        mappingList.add(mapping);
    }

    public String serializeLocationConfig() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        File jsonFile = Files.createTempFile("locConfig", ".json").toFile();
        objectMapper.writeValue(jsonFile, locationList);
        return jsonFile.getAbsolutePath();
    }

    public String serializeLocationMappings() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        File jsonFile = Files.createTempFile("locMapping", ".json").toFile();
        objectMapper.writeValue(jsonFile, mappingList);
        return jsonFile.getAbsolutePath();
    }
}
