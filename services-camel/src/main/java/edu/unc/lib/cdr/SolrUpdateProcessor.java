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

package edu.unc.lib.cdr;

import static edu.unc.lib.cdr.headers.CdrFcrepoHeaders.CdrSolrUpdateAction;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.data.ingest.solr.ChildSetRequest;
import edu.unc.lib.dl.data.ingest.solr.SolrUpdateRequest;
import edu.unc.lib.dl.data.ingest.solr.action.IndexingAction;
import edu.unc.lib.dl.data.ingest.solr.exception.IndexingException;
import edu.unc.lib.dl.util.IndexingActionType;
import edu.unc.lib.dl.util.JMSMessageUtil;
import edu.unc.lib.dl.xml.JDOMNamespaceUtil;

/**
 * Update objects in Solr
 *
 * @author lfarrell
 *
 */
public class SolrUpdateProcessor implements Processor {
    private static final Logger log = LoggerFactory.getLogger(SolrUpdateProcessor.class);

    private final int maxRetries;
    private final long retryDelay;
    private List<SolrUpdateRequest> updateRequests;
    private Map<IndexingActionType, IndexingAction> solrIndexingActionMap;

    public SolrUpdateProcessor(int maxRetries, long retryDelay) {
        this.maxRetries = maxRetries;
        this.retryDelay = retryDelay;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        final Message in = exchange.getIn();

        String fcrepoBinaryUri = (String) in.getHeader(FCREPO_URI);
        String solrActionType = (String) in.getHeader(CdrSolrUpdateAction);

        int retryAttempt = 0;

        while (true) {
            updateRequests = updateSolr(in, solrActionType, fcrepoBinaryUri);

            for (SolrUpdateRequest updateRequest : updateRequests) {
                try {
                    IndexingAction indexingAction = this.solrIndexingActionMap.get(updateRequest.getUpdateAction());

                    if (indexingAction != null) {
                        log.info("Performing action {} on object {}", updateRequest.getUpdateAction(),
                                updateRequest.getTargetID());
                        indexingAction.performAction(updateRequest);
                    }

                    // Reset retry count for outer loop
                    retryAttempt = 0;
                } catch (IndexingException e) {
                    log.error("Error attempting to perform action " + updateRequest.getAction() +
                            " on object " + updateRequest.getTargetID(), e);
                } catch (Exception e) {
                    if (retryAttempt == maxRetries) {
                        throw e;
                    }

                    retryAttempt++;
                    log.info("Unable to update solr for {}. Retrying, attempt {}",
                            fcrepoBinaryUri, retryAttempt);
                    TimeUnit.MILLISECONDS.sleep(retryDelay);
                }
            }
        }
    }

    private List<String> populateList(String field, Element contentBody) {
        List<Element> children = contentBody.getChildren(field, JDOMNamespaceUtil.CDR_MESSAGE_NS);
        if (children == null || children.size() == 0) {
            return null;
        }

        List<String> list = new ArrayList<String>();
        for (Object node: children) {
            Element element = (Element)node;
            for (Object pid: element.getChildren()) {
                Element pidElement = (Element)pid;
                list.add(pidElement.getTextTrim());
            }
        }
        return list;
    }

    private List<SolrUpdateRequest> updateSolr(Message message, String action, String pid) {
        Element messageBody = (Element) message.getBody();

        List<String> children = null;
        List<String> reordered = null;
        List<SolrUpdateRequest> updates = new ArrayList<SolrUpdateRequest>();
        final String parent;
        final String mode;

        children = populateList("subjects", messageBody);
        parent = messageBody.getChildText("parent", JDOMNamespaceUtil.CDR_MESSAGE_NS);
        mode = messageBody.getChildText("mode", JDOMNamespaceUtil.CDR_MESSAGE_NS);

        if (JMSMessageUtil.CDRActions.MOVE.equals(action)) {
            updates.add(new ChildSetRequest(pid, children,
                    IndexingActionType.MOVE));
        } else if (JMSMessageUtil.CDRActions.ADD.equals(action)) {
            updates.add(new ChildSetRequest(pid, children,
                    IndexingActionType.ADD_SET_TO_PARENT));
        } else if (JMSMessageUtil.CDRActions.REORDER.equals(action)) {
            reordered = populateList("reordered", messageBody);

            for (String pidString : reordered) {
                updates.add(new SolrUpdateRequest(pidString, IndexingActionType.ADD));
            }
        } else if (JMSMessageUtil.CDRActions.INDEX.equals(action)) {
            IndexingActionType indexingAction = IndexingActionType.getAction(IndexingActionType.namespace
                    + action);
            if (indexingAction != null) {
                if (IndexingActionType.SET_DEFAULT_WEB_OBJECT.equals(indexingAction)) {
                    updates.add(new ChildSetRequest(pid, children,
                            IndexingActionType.SET_DEFAULT_WEB_OBJECT));
                } else {
                    for (String pidString : children) {
                        updates.add(new SolrUpdateRequest(pidString, indexingAction));
                    }
                }
            }
        } else if (JMSMessageUtil.CDRActions.REINDEX.equals(action)) {
            // Determine which kind of reindex to perform based on the mode
            if (mode.equals("inplace")) {
                updates.add(new SolrUpdateRequest(parent, IndexingActionType.RECURSIVE_REINDEX));
            } else {
                updates.add(new SolrUpdateRequest(parent, IndexingActionType.CLEAN_REINDEX));
            }
        } else if (JMSMessageUtil.CDRActions.PUBLISH.equals(action)) {
            for (String pidString : children) {
                updates.add(new SolrUpdateRequest(pidString, IndexingActionType.UPDATE_STATUS));
            }
        } else if (JMSMessageUtil.CDRActions.EDIT_TYPE.equals(action)) {
            updates.add(new ChildSetRequest(pid, children,
                    IndexingActionType.UPDATE_TYPE));
        }

        return updates;
    }
}
