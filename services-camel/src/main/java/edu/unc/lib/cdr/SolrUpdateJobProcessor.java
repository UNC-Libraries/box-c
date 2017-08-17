package edu.unc.lib.cdr;

import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.ATOM_NS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.jdom2.Document;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.data.ingest.solr.ChildSetRequest;
import edu.unc.lib.dl.data.ingest.solr.SolrUpdateRequest;
import edu.unc.lib.dl.data.ingest.solr.action.IndexingAction;
import edu.unc.lib.dl.data.ingest.solr.exception.IndexingException;
import edu.unc.lib.dl.util.IndexingActionType;

public class SolrUpdateJobProcessor implements Processor {
    final Logger log = LoggerFactory.getLogger(SolrUpdateJobProcessor.class);
    private Map<IndexingActionType, IndexingAction> solrIndexingActionMap;
    private SolrUpdateRequest updateRequest;
    private List<String> children;

    @Override
    public void process(Exchange exchange) throws Exception {
        final Message in = exchange.getIn();
        Document msgBody = (Document) in.getBody();
        Element body = msgBody.getRootElement();

        String pid = body.getChild("pid", ATOM_NS).getTextTrim();
        String action = body.getChild("solrActionType", ATOM_NS).getTextTrim();

        String getChildren = body.getChild("children", ATOM_NS).getTextTrim();

        if (getChildren != null) {
            children = new ArrayList<String>(Arrays.asList(getChildren.split(",")));
        }

        try {
            if (children == null) {
                updateRequest = new SolrUpdateRequest(pid, IndexingActionType.getAction(action));
            } else {
                updateRequest = new ChildSetRequest(pid, children, IndexingActionType.getAction(action));
            }

            IndexingAction indexingAction = this.solrIndexingActionMap.get(action);
            if (indexingAction != null) {
                log.info("Performing action {} on object {}",
                        action, pid);
                indexingAction.performAction(updateRequest);
            }
        } catch (IndexingException e) {
            log.error("Error attempting to perform action " + action +
                    " on object " + pid, e);
        }
    }

}
