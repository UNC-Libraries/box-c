package edu.unc.lib.boxc.services.camel.solrUpdate;

import edu.unc.lib.boxc.indexing.solr.ChildSetRequest;
import edu.unc.lib.boxc.indexing.solr.SolrUpdateRequest;
import edu.unc.lib.boxc.indexing.solr.action.IndexingAction;
import edu.unc.lib.boxc.model.api.objects.AdminUnit;
import edu.unc.lib.boxc.model.api.objects.CollectionObject;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.jms.MessageSender;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingActionType;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingMessageSender;
import edu.unc.lib.boxc.search.solr.config.SolrSettings;
import edu.unc.lib.boxc.search.solr.services.TitleRetrievalService;
import edu.unc.lib.boxc.services.camel.util.MessageUtil;
import io.dropwizard.metrics5.Timer;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.jdom2.Document;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

    private TitleRetrievalService titleRetrievalService;
    private RepositoryObjectLoader repoObjLoader;
    private MessageSender updateWorkSender;
    private SolrClient solrClient;
    private SolrSettings solrSettings;
    private IndexingMessageSender indexingMessageSender;
    private Map<IndexingActionType, IndexingAction> solrIndexingActionMap;
    private Set<IndexingActionType> NEED_UPDATE_PARENT_WORK = EnumSet.of(
            IndexingActionType.DELETE, IndexingActionType.ADD);
    private Set<IndexingActionType> NEED_UPDATE_CHILDREN_PATH_INFO = EnumSet.of(
            IndexingActionType.UPDATE_DESCRIPTION);

    public void init() {
        solrClient = solrSettings.getSolrClient();
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        try (Timer.Context context = timer.time()) {
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
            var targetPid = PIDs.get(pid);
            var targetObj = repoObjLoader.getRepositoryObject(targetPid);
            String previousTitle = null;
            if (needsUpdateOfChildrenPathInfo(targetObj, actionType)) {
                previousTitle = titleRetrievalService.retrieveCachedTitle(targetPid);
            }
            log.info("Performing action {} on object {}", action, pid);
            indexingAction.performAction(updateRequest);

            triggerFollowupActions(targetObj, actionType, previousTitle);
        }
    }

    private void triggerFollowupActions(RepositoryObject targetObj, IndexingActionType actionType,
                                        String previousTitle) {
        // Trigger update of parent work obj for files if the action requires it
        if (NEED_UPDATE_PARENT_WORK.contains(actionType)) {
            if (targetObj instanceof FileObject) {
                var parent = targetObj.getParent();
                if (parent instanceof WorkObject) {
                    log.debug("Requesting indexing of work {} containing file {}",
                            parent.getPid().getId(), targetObj.getPid().getId());
                    updateWorkSender.sendMessage(parent.getPid().getQualifiedId());
                }
            }
        } else if (needsUpdateOfChildrenPathInfo(targetObj, actionType)) {
            // Trigger update of path info of unit/collection objects if their description changes
            log.debug("Requesting indexing of {}'s children to update path info", targetObj.getPid().getId());
            // Only trigger update of path info at this time if the title of this object has changed
            var newTitle = titleRetrievalService.retrieveTitle(targetObj.getPid());
            if (Objects.equals(newTitle, previousTitle)) {
                return;
            }
            // Force commit of the parent object before indexing children
            try {
                solrClient.commit();
            } catch (SolrServerException | IOException e) {
                log.error("Failed to commit solr updates prior to indexing children of a collection");
            }
            indexingMessageSender.sendIndexingOperation("", targetObj.getPid(),
                    IndexingActionType.UPDATE_PARENT_PATH_TREE);
        }
    }

    private boolean needsUpdateOfChildrenPathInfo(RepositoryObject targetObj, IndexingActionType actionType) {
        return NEED_UPDATE_CHILDREN_PATH_INFO.contains(actionType) &&
                (targetObj instanceof AdminUnit || targetObj instanceof CollectionObject);
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

    public void setTitleRetrievalService(TitleRetrievalService titleRetrievalService) {
        this.titleRetrievalService = titleRetrievalService;
    }

    public void setIndexingMessageSender(IndexingMessageSender indexingMessageSender) {
        this.indexingMessageSender = indexingMessageSender;
    }

    public void setSolrClient(SolrClient solrClient) {
        this.solrClient = solrClient;
    }

    public void setSolrSettings(SolrSettings solrSettings) {
        this.solrSettings = solrSettings;
    }
}
