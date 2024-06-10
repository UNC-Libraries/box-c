package edu.unc.lib.boxc.services.camel.solrUpdate;

import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingActionType;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingMessageSender;
import edu.unc.lib.boxc.search.solr.config.SolrSettings;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;

/**
 * Sends individual indexing operations from aggregated collections of object identifiers
 *
 * @author bbpennel
 */
public class AggregateUpdateProcessor implements Processor {
    private static final Logger log = LoggerFactory.getLogger(AggregateUpdateProcessor.class);
    private IndexingMessageSender messageSender;
    private IndexingActionType actionType;
    private SolrSettings solrSettings;
    private SolrClient solrClient;
    private boolean forceCommit;

    public void init() {
        solrClient = solrSettings.getSolrClient();
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        final Message in = exchange.getIn();
        var idCollection = in.getBody(Collection.class);
        if (idCollection.isEmpty()) {
            return;
        }
        if (forceCommit) {
            // Force commit of any pending solr updates before sending indexing operations
            try {
                solrClient.commit();
            } catch (SolrServerException | IOException e) {
                log.error("Failed to commit solr updates prior to indexing children of a collection");
            }
        }
        for (Object idObj : idCollection) {
            PID pid = PIDs.get(idObj.toString());
            // Make sure the object exists in solr before attempting to update it
            if (solrClient.getById(pid.getId()) != null) {
                messageSender.sendIndexingOperation(null, pid, actionType);
            }
        }
    }

    public void setIndexingMessageSender(IndexingMessageSender messageSender) {
        this.messageSender = messageSender;
    }

    public void setSolrSettings(SolrSettings solrSettings) {
        this.solrSettings = solrSettings;
    }

    public void setSolrClient(SolrClient solrClient) {
        this.solrClient = solrClient;
    }

    public void setActionType(IndexingActionType actionType) {
        this.actionType = actionType;
    }

    public void setForceCommit(boolean forceCommit) {
        this.forceCommit = forceCommit;
    }
}
