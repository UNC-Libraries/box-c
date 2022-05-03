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
package edu.unc.lib.boxc.indexing.solr.utils;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import edu.unc.lib.boxc.model.api.exceptions.RepositoryException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.BinaryObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.fcrepo.ids.DatastreamPids;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * Service for retrieving technical metadata information about binaries
 *
 * @author bbpennel
 */
public class TechnicalMetadataService {
    private RepositoryObjectLoader repositoryObjectLoader;

    // Max number of entries allowed in the cache
    private final static int CACHE_SIZE = 64;

    private Map<String, Document> techMdCache;

    public void init() {
        var mapBuilder = new ConcurrentLinkedHashMap.Builder<String, Document>();
        mapBuilder.maximumWeightedCapacity(CACHE_SIZE);
        techMdCache = mapBuilder.build();
    }

    /**
     * Retrieve the techmd datastream for the specified file object, as an xml Document
     * @param filePid
     * @return
     */
    public Document retrieveDocument(PID filePid) {
        var techMdPid = DatastreamPids.getTechnicalMetadataPid(filePid);
        // Use cached version if available
        if (techMdCache.containsKey(techMdPid.getId())) {
            return techMdCache.get(techMdPid.getId());
        }
        var techMdObj = repositoryObjectLoader.getBinaryObject(techMdPid);
        return retrieveAndDeserialize(techMdObj);
    }

    /**
     * Retrieve the techmd datastream for the specified techmd binary object, as an xml Document
     * @param techMdObj techmd binary object
     * @return
     */
    public Document retrieveDocument(BinaryObject techMdObj) {
        var techMdId = techMdObj.getPid().getId();
        // Use cached version if available
        if (techMdCache.containsKey(techMdId)) {
            return techMdCache.get(techMdId);
        }
        return retrieveAndDeserialize(techMdObj);
    }

    private Document retrieveAndDeserialize(BinaryObject techMdObj) {
        InputStream techMdData = techMdObj.getBinaryStream();
        String techMdId = techMdObj.getPid().getId();

        try {
            SAXBuilder builder = new SAXBuilder();
            var doc = builder.build(techMdData);
            // Cache the value for future use
            techMdCache.put(techMdId, doc);
            return doc;
        } catch (JDOMException | IOException e) {
            throw new RepositoryException("Unable to parse technical metadata for " + techMdId, e);
        }
    }

    public void setRepositoryObjectLoader(RepositoryObjectLoader repositoryObjectLoader) {
        this.repositoryObjectLoader = repositoryObjectLoader;
    }
}
