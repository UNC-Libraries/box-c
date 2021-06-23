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

import static edu.unc.lib.boxc.common.metrics.TimerFactory.createTimerForClass;
import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.ATOM_NS;
import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.CDR_MESSAGE_NS;
import static java.util.stream.Collectors.toMap;

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
import edu.unc.lib.dl.services.camel.util.MessageUtil;
import edu.unc.lib.dl.util.IndexingActionType;
import io.dropwizard.metrics5.Timer;

/**
 * Processes solr update messages, triggering the requested solr update action.
 *
 * @author lfarrell
 *
 */
public class SolrUpdateProcessor implements Processor {
    private static final Logger log = LoggerFactory.getLogger(SolrUpdateProcessor.class);
    private static final Timer timer = createTimerForClass(SolrUpdateProcessor.class);

    private Map<IndexingActionType, IndexingAction> solrIndexingActionMap;

    @Override
    public void process(Exchange exchange) throws Exception {
        try (Timer.Context context = timer.time()) {
            log.debug("Processing solr update");
            final Message in = exchange.getIn();

            Document msgBody = MessageUtil.getDocumentBody(in);
            Element body = msgBody.getRootElement();

            String pid = body.getChild("pid", ATOM_NS).getTextTrim();
            String action = body.getChild("actionType", ATOM_NS).getTextTrim();
            IndexingActionType actionType = IndexingActionType.getAction(action);

            List<String> children = extractChildren(body);

            Map<String, String> params = extractParams(body);

            Element authorEl = body.getChild("author", ATOM_NS);
            String author = null;
            if (authorEl != null) {
                author = authorEl.getChildText("name", ATOM_NS);
            }

            SolrUpdateRequest updateRequest;
            if (children == null) {
                updateRequest = new SolrUpdateRequest(pid, actionType, null, author);
            } else {
                updateRequest = new ChildSetRequest(pid, children, actionType, author);
            }
            updateRequest.setParams(params);

            IndexingAction indexingAction = this.solrIndexingActionMap.get(actionType);
            if (indexingAction != null) {
                log.info("Performing action {} on object {}",
                        action, pid);
                indexingAction.performAction(updateRequest);
            }
        }
    }

    private List<String> extractChildren(Element body) {
        Element childrenEl = body.getChild("children", CDR_MESSAGE_NS);
        if (childrenEl == null) {
            return null;
        }
        return childrenEl.getChildren("pid", CDR_MESSAGE_NS).stream()
                .map(c -> c.getTextTrim())
                .collect(Collectors.toList());
    }

    private Map<String, String> extractParams(Element body) {
        Element paramsEl = body.getChild("params", CDR_MESSAGE_NS);
        if (paramsEl == null) {
            return null;
        }
        return paramsEl.getChildren("param", CDR_MESSAGE_NS).stream()
                .collect(toMap(p -> p.getAttributeValue("name"), p -> p.getTextTrim()));
    }

    /**
     * @param solrIndexingActionMap the solrIndexingActionMap to set
     */
    public void setSolrIndexingActionMap(Map<IndexingActionType, IndexingAction> solrIndexingActionMap) {
        this.solrIndexingActionMap = solrIndexingActionMap;
    }
}
