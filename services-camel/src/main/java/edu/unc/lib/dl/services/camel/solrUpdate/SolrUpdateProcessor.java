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
package edu.unc.lib.dl.services.camel.solrUpdate;

import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.ATOM_NS;
import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.CDR_MESSAGE_NS;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
import edu.unc.lib.dl.services.camel.util.MessageUtil;
import edu.unc.lib.dl.util.IndexingActionType;

/**
 * Processes solr update messages, triggering the requested solr update action.
 *
 * @author lfarrell
 *
 */
public class SolrUpdateProcessor implements Processor {
    final Logger log = LoggerFactory.getLogger(SolrUpdateProcessor.class);

    private Map<IndexingActionType, IndexingAction> solrIndexingActionMap;

    @Override
    public void process(Exchange exchange) throws Exception {
        log.debug("Processing solr update");
        final Message in = exchange.getIn();

        Document msgBody = MessageUtil.getDocumentBody(in);
        Element body = msgBody.getRootElement();

        String pid = body.getChild("pid", ATOM_NS).getTextTrim();
        String action = body.getChild("actionType", ATOM_NS).getTextTrim();
        IndexingActionType actionType = IndexingActionType.getAction(action);

        List<String> children = null;
        Element childrenEl = body.getChild("children", CDR_MESSAGE_NS);
        if (childrenEl != null) {
            children = childrenEl.getChildren("pid", CDR_MESSAGE_NS).stream()
                    .map(c -> c.getTextTrim())
                    .collect(Collectors.toList());
        }

        try {
            SolrUpdateRequest updateRequest;
            if (children == null) {
                updateRequest = new SolrUpdateRequest(pid, actionType);
            } else {
                updateRequest = new ChildSetRequest(pid, children, actionType);
            }

            IndexingAction indexingAction = this.solrIndexingActionMap.get(actionType);
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

    /**
     * @param solrIndexingActionMap the solrIndexingActionMap to set
     */
    public void setSolrIndexingActionMap(Map<IndexingActionType, IndexingAction> solrIndexingActionMap) {
        this.solrIndexingActionMap = solrIndexingActionMap;
    }
}
