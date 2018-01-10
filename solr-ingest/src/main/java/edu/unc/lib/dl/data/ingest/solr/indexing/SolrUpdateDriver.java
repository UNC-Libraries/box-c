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
package edu.unc.lib.dl.data.ingest.solr.indexing;

import static edu.unc.lib.dl.search.solr.util.SearchFieldKeys.ID;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrClient;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.data.ingest.solr.exception.IndexingException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.search.solr.model.IndexDocumentBean;
import edu.unc.lib.dl.search.solr.util.SolrSettings;

/**
 * Performs batch add/delete/update operations to a Solr index.
 *
 * @author bbpennel
 *
 */
public class SolrUpdateDriver {
    private static final Logger log = LoggerFactory.getLogger(SolrUpdateDriver.class);

    private SolrClient solrClient;
    private SolrClient updateSolrClient;
    private SolrSettings solrSettings;

    private int autoPushCount;
    private int updateThreads;

    private static String SET_OPERATION = "set";
    private static String UPDATE_TIMESTAMP = "timestamp";

    public void init() {
        log.debug("Instantiating concurrent udpate solr server for " + solrSettings.getUrl());


        solrClient = new ConcurrentUpdateSolrClient.Builder(solrSettings.getUrl())
                .withThreadCount(updateThreads)
                .withQueueSize(autoPushCount)
                .build();
        updateSolrClient = new ConcurrentUpdateSolrClient.Builder(solrSettings.getUrl())
                .withThreadCount(updateThreads)
                .withQueueSize(autoPushCount)
                .build();
    }

    public void addDocument(IndexDocumentBean idb) throws IndexingException {
        try {
            log.info("Queuing {} for full indexing", idb.getId());
            // Providing a version value, indicating that it doesn't matter if record exists
            idb.set_version_(0l);
            solrClient.addBean(idb);
        } catch (IOException e) {
            throw new IndexingException("Failed to add document to solr", e);
        } catch (SolrServerException e) {
            throw new IndexingException("Failed to add document to solr", e);
        }
    }

    /**
     * Perform a partial document update from a IndexDocumentBean. Null fields are considered to be unspecified and will
     * not be changed, except for the update timestamp field which is always set.
     *
     * @param operation
     * @param idb
     * @throws IndexingException
     */
    public void updateDocument(IndexDocumentBean idb) throws IndexingException {
        Map<String, Object> fields = idb.getFields();

        for (String field : solrSettings.getRequiredFields()) {
            if (!fields.containsKey(field)) {
                throw new IndexingException("Required indexing field {" + field + "} was not present");
            }
        }

        try {
            log.info("Queuing {} for atomic updating", idb.getId());
            SolrInputDocument sid = new SolrInputDocument();
            for (Entry<String, Object> field : fields.entrySet()) {
                String fieldName = field.getKey();
                Object value = field.getValue();

                // Id field needs to be set like a non-partial update
                if (ID.getSolrField().equals(fieldName)) {
                    sid.addField(fieldName, value);
                    continue;
                }

                // Allowing values and explicitly nulled fields through
                updateField(sid, fieldName, value);
            }

            // Set timestamp to now, auto population not working with atomic update #SOLR-8966
            updateField(sid, UPDATE_TIMESTAMP, new Date());

            if (log.isDebugEnabled()) {
                log.debug("Performing partial update:\n{}", ClientUtils.toXML(sid));
            }
            updateSolrClient.add(sid);
        } catch (IOException e) {
            throw new IndexingException("Failed to add document to solr", e);
        } catch (SolrServerException e) {
            throw new IndexingException("Failed to add document to solr", e);
        }
    }

    private void updateField(SolrInputDocument sid, String fieldName, Object value) {
        Map<String, Object> partialUpdate = new HashMap<>();
        partialUpdate.put(SET_OPERATION, value);
        sid.setField(fieldName, partialUpdate);
    }

    public void delete(PID pid) throws IndexingException {
        this.delete(pid.toString());
    }

    public void delete(String pid) throws IndexingException {
        try {
            solrClient.deleteById(pid);
        } catch (IOException e) {
            throw new IndexingException("Failed to delete document from solr", e);
        } catch (SolrServerException e) {
            throw new IndexingException("Failed to delete document from solr", e);
        }
    }

    public void deleteByQuery(String query) throws IndexingException {
        try {
            solrClient.deleteByQuery(query);
        } catch (IOException e) {
            throw new IndexingException("Failed to add document batch to solr", e);
        } catch (SolrServerException e) {
            throw new IndexingException("Failed to add document batch to solr", e);
        }
    }

    /**
     * Force a commit of the currently staged updates.
     */
    public void commit() throws IndexingException {
        try {
            solrClient.commit();
            updateSolrClient.commit();
        } catch (SolrServerException e) {
            throw new IndexingException("Failed to commit changes to solr", e);
        } catch (IOException e) {
            throw new IndexingException("Failed to commit changes to solr", e);
        }
    }

    public int getAutoPushCount() {
        return autoPushCount;
    }

    public void setAutoPushCount(int autoPushCount) {
        this.autoPushCount = autoPushCount;
    }

    public int getUpdateThreads() {
        return updateThreads;
    }

    public void setUpdateThreads(int updateThreads) {
        this.updateThreads = updateThreads;
    }

    /**
     * @param solrClient the solrClient to set
     */
    public void setSolrClient(SolrClient solrClient) {
        this.solrClient = solrClient;
    }

    /**
     * @param updateSolrClient the updateSolrClient to set
     */
    public void setUpdateSolrClient(SolrClient updateSolrClient) {
        this.updateSolrClient = updateSolrClient;
    }

    public void setSolrSettings(SolrSettings solrSettings) {
        this.solrSettings = solrSettings;
    }
}
