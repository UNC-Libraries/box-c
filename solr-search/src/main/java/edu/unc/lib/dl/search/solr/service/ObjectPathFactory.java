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
package edu.unc.lib.dl.search.solr.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.solr.client.solrj.SolrServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;

import edu.unc.lib.dl.fcrepo4.RepositoryPaths;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadata;
import edu.unc.lib.dl.search.solr.model.HierarchicalFacetNode;
import edu.unc.lib.dl.search.solr.model.ObjectPath;
import edu.unc.lib.dl.search.solr.model.ObjectPathEntry;
import edu.unc.lib.dl.search.solr.model.SimpleIdRequest;
import edu.unc.lib.dl.search.solr.util.SearchFieldKeys;
import edu.unc.lib.dl.search.solr.util.SolrSettings;

/**
 * Factory for generating and retrieving hierarchical path information, including retrieving names of ancestors
 *
 * @author bbpennel
 * @date Mar 18, 2015
 */
public class ObjectPathFactory {

    private static final Logger log = LoggerFactory.getLogger(ObjectPathFactory.class);

    private SolrSearchService search;
    private SolrSettings solrSettings;

    // Max number of entries allowed in the cache
    private int cacheSize;
    // Amount of time, in millisecords, that a cached entry is considered valid
    private long timeToLiveMilli = 5000L;

    // Cache of path information. Key is the pid of the object cached
    private Map<String, PathCacheData> pathCache;

    private List<String> pathFields;
    private List<String> startObjectFields;
    private String titleFieldName;
    private String typeFieldName;

    @PostConstruct
    public void init() {
        ConcurrentLinkedHashMap.Builder<String, PathCacheData> mapBuilder = new ConcurrentLinkedHashMap.Builder<>();
        mapBuilder.maximumWeightedCapacity(cacheSize);
        this.pathCache = mapBuilder.build();

        titleFieldName = solrSettings.getFieldName(SearchFieldKeys.TITLE.name());
        typeFieldName = solrSettings.getFieldName(SearchFieldKeys.RESOURCE_TYPE.name());
        pathFields = Arrays.asList(titleFieldName, typeFieldName);
        startObjectFields = Arrays.asList(SearchFieldKeys.ID.name(),
                SearchFieldKeys.TITLE.name(), SearchFieldKeys.RESOURCE_TYPE.name(),
                SearchFieldKeys.ANCESTOR_PATH.name());
    }

    /**
     * Retrieve the name of the object identified by pid
     *
     * @param pid
     * @return
     */
    public String getName(String pid) {
        PathCacheData pathData = getPathData(pid);
        return pathData != null ? pathData.name : null;
    }

    /**
     * Retrieve path of ancestors leading up to and including the object
     * identified by pid.
     *
     * @param pid pid of object to retrieve path for
     * @return path
     */
    public ObjectPath getPath(PID pid) {
        SimpleIdRequest idRequest = new SimpleIdRequest(pid, startObjectFields, null);
        BriefObjectMetadata bom = search.getObjectById(idRequest);
        return getPath(bom);
    }

    /**
     * Retrieve the path of ancestor information leading up to and including the
     * provided object. If no title is present, then the object will not be
     * included in the result path, only the objects preceding it in the path
     * will be present.
     *
     * The path is determined by the ancestorPath field of the
     * BriefObjectMetadata object. If ancestorPath is not present, then null
     * will be returned.
     *
     * @param bom
     * @return
     */
    public ObjectPath getPath(BriefObjectMetadata bom) {
        if (bom.getAncestorPathFacet() == null && !RepositoryPaths.getContentRootPid().getId().equals(bom.getId())) {
            return null;
        }

        List<ObjectPathEntry> entries = new ArrayList<>();

        // Retrieve path data for each node in the ancestor path
        if (bom.getAncestorPathFacet() != null) {
            for (HierarchicalFacetNode node : bom.getAncestorPathFacet().getFacetNodes()) {
                String pid = node.getSearchKey();
                PathCacheData pathData = getPathData(pid);

                if (pathData != null) {
                    entries.add(new ObjectPathEntry(pid, pathData.name, pathData.isContainer));
                }
            }
        }

        if (bom.getTitle() != null) {
            // Refresh the cache for the object being looked up if it is a container
            if (isContainer(bom.getResourceType())) {
                try {
                    pathCache.put(bom.getId(), new PathCacheData(bom.getTitle(), true));
                } catch (InvalidPathDataException e) {
                    log.debug("Did not cache path data for the provided object {}", bom.getId(), e);
                }
            }

            // Add the provided metadata object into the path as the last entry, if it had a title
            entries.add(new ObjectPathEntry(bom.getId(), bom.getTitle(), true));
        }

        return new ObjectPath(entries);
    }

    /**
     * Returns a path data for a single entry, as identified by pid. The data is
     * either retrieved from the cache if it is not older than the allowed time
     * to live, or retrieved from the index
     *
     * @param pid
     * @return
     */
    private PathCacheData getPathData(String pid) {
        PathCacheData cacheData = pathCache.get(pid);
        // Check if the cached values are still up to date
        if (cacheData != null && System.currentTimeMillis() <= (cacheData.retrievedAt + timeToLiveMilli)) {
            log.debug("Retrieved path information for {} from cache", pid);
            return cacheData;
        }

        // Cache wasn't available, retrieve fresh data from solr
        try {
            Map<String, Object> fields = search.getFields(pid, pathFields);
            if (fields == null) {
                log.warn("Unable to retrieve solr record for object {}, it may not be present or indexed", pid);
                return null;
            }

            PathCacheData pathData = new PathCacheData((String) fields.get(titleFieldName),
                    isContainer((String) fields.get(typeFieldName)));

            // Cache the results for this entry
            pathCache.put(pid, pathData);

            log.debug("Retrieved path information for {} from solr", pid);

            return pathData;
        } catch (SolrServerException | InvalidPathDataException e) {
            log.error("Failed to get object path information for {}", pid, e);
        }
        return null;
    }

    private boolean isContainer(String resourceType) {
        return !"File".equals(resourceType);
    }

    public void setCacheSize(int cacheSize) {
        this.cacheSize = cacheSize;
    }

    public void setTimeToLiveMilli(long timeToLiveMilli) {
        this.timeToLiveMilli = timeToLiveMilli;
    }

    public void setSearch(SolrSearchService search) {
        this.search = search;
    }

    public void setSolrSettings(SolrSettings solrSettings) {
        this.solrSettings = solrSettings;
    }

    public static class PathCacheData {

        public String name;

        public boolean isContainer;

        public long retrievedAt;

        public PathCacheData(String name, boolean isContainer) throws InvalidPathDataException {
            if (name == null) {
                throw new InvalidPathDataException("No name value provided");
            }
            this.name = name;
            this.isContainer = isContainer;
            retrievedAt = System.currentTimeMillis();
        }
    }

    public static class InvalidPathDataException extends Exception {
        private static final long serialVersionUID = 1L;

        public InvalidPathDataException(String message) {
            super(message);
        }
    }
}
