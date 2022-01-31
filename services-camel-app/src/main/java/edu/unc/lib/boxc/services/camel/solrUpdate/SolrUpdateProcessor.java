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
package edu.unc.lib.boxc.services.camel.solrUpdate;

import edu.unc.lib.boxc.indexing.solr.ChildSetRequest;
import edu.unc.lib.boxc.indexing.solr.SolrUpdateRequest;
import edu.unc.lib.boxc.indexing.solr.action.IndexingAction;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.jms.MessageSender;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingActionType;
import edu.unc.lib.boxc.services.camel.util.MessageUtil;
import io.dropwizard.metrics5.Timer;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.jdom2.Document;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static edu.unc.lib.boxc.common.metrics.TimerFactory.createTimerForClass;
import static edu.unc.lib.boxc.model.api.xml.JDOMNamespaceUtil.ATOM_NS;
import static edu.unc.lib.boxc.model.api.xml.JDOMNamespaceUtil.CDR_MESSAGE_NS;
import static java.util.stream.Collectors.toMap;

/**
 * Processes solr update messages, triggering the requested solr update action.
 *
 * @author lfarrell
 *
 */
public class SolrUpdateProcessor implements Processor {
    private static final Logger log = LoggerFactory.getLogger(SolrUpdateProcessor.class);
    private static final Timer timer = createTimerForClass(SolrUpdateProcessor.class);

    private RepositoryObjectLoader repoObjLoader;
    private MessageSender updateWorkSender;
    private Map<IndexingActionType, IndexingAction> solrIndexingActionMap;
    private Set<IndexingActionType> NEED_UPDATE_PARENT_WORK = EnumSet.of(
            IndexingActionType.DELETE, IndexingActionType.ADD,
            IndexingActionType.UPDATE_DATASTREAMS, IndexingActionType.UPDATE_FULL_TEXT);

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
            if (indexingAction == null) {
                return;
            }
            log.info("Performing action {} on object {}", action, pid);
            indexingAction.performAction(updateRequest);

            // Trigger update of parent work obj for files if the action requires it
            if (NEED_UPDATE_PARENT_WORK.contains(actionType)) {
                var targetPid = PIDs.get(pid);
                var targetObj = repoObjLoader.getRepositoryObject(targetPid);
                if (targetObj instanceof FileObject) {
                    var parent = targetObj.getParent();
                    if (parent instanceof WorkObject) {
                        log.debug("Requesting indexing of work {} containing file {}", parent.getPid().getId(), pid);
                        updateWorkSender.sendMessage(parent.getPid().getQualifiedId());
                    }
                }
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

    public void setRepositoryObjectLoader(RepositoryObjectLoader repoObjLoader) {
        this.repoObjLoader = repoObjLoader;
    }

    public void setUpdateWorkSender(MessageSender updateWorkSender) {
        this.updateWorkSender = updateWorkSender;
    }
}
