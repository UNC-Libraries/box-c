package edu.unc.lib.boxc.indexing.solr.indexing;

import edu.unc.lib.boxc.indexing.solr.exception.IndexingException;
import edu.unc.lib.boxc.indexing.solr.exception.RecoverableIndexingException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.search.solr.config.SolrSettings;
import edu.unc.lib.boxc.search.solr.models.IndexDocumentBean;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import static edu.unc.lib.boxc.search.api.SearchFieldKey.ID;

/**
 * Performs batch add/delete/update operations to a Solr index.
 *
 * @author bbpennel
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

        solrClient = new TolerantConcurrentUpdateSolrClient.Builder(solrSettings.getUrl())
                .withThreadCount(updateThreads)
                .withQueueSize(autoPushCount)
                .build();
        updateSolrClient = new TolerantConcurrentUpdateSolrClient.Builder(solrSettings.getUrl())
                .withThreadCount(updateThreads)
                .withQueueSize(autoPushCount)
                .build();
    }

    public void addDocument(IndexDocumentBean idb) throws IndexingException {
        Map<String, Object> fields = idb.getFields();

        for (String field : solrSettings.getRequiredFields()) {
            if (!fields.containsKey(field)) {
                throw new RecoverableIndexingException("Required indexing field {" + field + "} was not present for "
                        + idb.getId());
            }
        }

        try {
            log.info("Queuing {} for full indexing", idb.getId());
            // Providing a version value, indicating that it doesn't matter if record exists
            idb.set_version_(0l);
            solrClient.addBean(idb);
        } catch (IOException | SolrServerException e) {
            throw new IndexingException("Failed to add document to solr", e);
        }
    }

    /**
     * Perform a partial document update from a IndexDocumentBean. Null fields are considered to be unspecified and will
     * not be changed, except for the update timestamp field which is always set.
     *
     * @param idb
     * @throws IndexingException
     */
    public void updateDocument(IndexDocumentBean idb) throws IndexingException {
        Map<String, Object> fields = idb.getFields();

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
            // Index dynamic fields if present
            Map<String, Object> dynamics = idb.getDynamicFields();
            if (dynamics != null) {
                for (Entry<String, Object> field : dynamics.entrySet()) {
                    String fieldName = field.getKey();
                    Object value = field.getValue();

                    // Allowing values and explicitly nulled fields through
                    updateField(sid, fieldName, value);
                }

            }

            // Set timestamp to now, auto population not working with atomic update #SOLR-8966
            updateField(sid, UPDATE_TIMESTAMP, new Date());

            if (log.isDebugEnabled()) {
                log.debug("Performing partial update:\n{}", ClientUtils.toXML(sid));
            }
            updateSolrClient.add(sid);
        } catch (IOException | SolrServerException e) {
            throw new RecoverableIndexingException("Failed to add document to solr", e);
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
