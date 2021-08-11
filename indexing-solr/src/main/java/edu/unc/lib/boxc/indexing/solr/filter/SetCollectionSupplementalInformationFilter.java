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
package edu.unc.lib.boxc.indexing.solr.filter;

import java.io.FileInputStream;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.boxc.indexing.solr.exception.IndexingException;
import edu.unc.lib.boxc.indexing.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.boxc.model.api.ids.ContentPathConstants;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.services.ContentPathFactory;

/**
 *
 * @author bbpennel
 *
 */
public class SetCollectionSupplementalInformationFilter implements IndexDocumentFilter {
    private static final Logger log = LoggerFactory.getLogger(SetCollectionSupplementalInformationFilter.class);

    private ContentPathFactory pathFactory;
    // Map of filters for specific collections.  Key is the pid of the parent collection
    private Map<String, IndexDocumentFilter> collectionFilters;

    public SetCollectionSupplementalInformationFilter() {
        collectionFilters = new HashMap<String, IndexDocumentFilter>();
    }
    @Override
    public void filter(DocumentIndexingPackage dip) throws IndexingException {
        String collection = dip.getDocument().getParentCollection();
        if (collection == null) {
            List<PID> pids = pathFactory.getAncestorPids(dip.getPid());
            if (pids.size() > ContentPathConstants.COLLECTION_DEPTH) {
                collection = pids.get(ContentPathConstants.COLLECTION_DEPTH).getId();
            } else if (pids.size() == ContentPathConstants.COLLECTION_DEPTH) {
                collection = dip.getPid().getId();
            } else {
                return;
            }
        }

        IndexDocumentFilter collectionFilter = collectionFilters.get(collection);
        if (collectionFilter == null) {
            return;
        }

        collectionFilter.filter(dip);
    }

    public void setCollectionFilters(String collectionFiltersPath) {
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(collectionFiltersPath));

            Iterator<Entry<Object,Object>> it = properties.entrySet().iterator();
            while (it.hasNext()) {
                Entry<Object,Object> entry = it.next();
                log.info("Loading class " + (String) entry.getValue() + " for collection " + (String) entry.getKey());
                Class<?> clazz = Class.forName((String) entry.getValue());
                Constructor<?> constructor = clazz.getConstructor();
                collectionFilters.put((String) entry.getKey(), (IndexDocumentFilter) constructor.newInstance());
            }
        } catch (Exception e) {
            log.error("Failed to load collection filters properties file " + collectionFiltersPath, e);
        }
    }
    public void setPathFactory(ContentPathFactory pathFactory) {
        this.pathFactory = pathFactory;
    }
}