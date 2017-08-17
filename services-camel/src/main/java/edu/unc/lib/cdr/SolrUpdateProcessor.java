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
import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.ATOM_NS;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.jms.JMSException;
import javax.jms.Session;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;

import edu.unc.lib.dl.data.ingest.solr.ChildSetRequest;
import edu.unc.lib.dl.data.ingest.solr.SolrUpdateRequest;
import edu.unc.lib.dl.fedora.PID;
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
    private String parent;
    private String mode;
    private List<String>subjects;
    private JmsTemplate jmsTemplate;

    @Override
    public void process(Exchange exchange) throws Exception {
        final Message in = exchange.getIn();
        Document body = (Document) in.getBody();
        Element contentBody = body.getRootElement().getChild("content", JDOMNamespaceUtil.ATOM_NS);

        if (contentBody == null || contentBody.getChildren().size() == 0) {
            return;
        }

        String solrActionType = (String) in.getHeader(CdrSolrUpdateAction);

        if (solrActionType.equals("none")) {
            return;
        }

        parent = contentBody.getChildText("parent", JDOMNamespaceUtil.CDR_MESSAGE_NS);
        mode = contentBody.getChildText("mode", JDOMNamespaceUtil.CDR_MESSAGE_NS);
        subjects = populateList("subjects", contentBody);
        String targetId = JMSMessageUtil.getPid(body);

        if (JMSMessageUtil.CDRActions.MOVE.equals(solrActionType)) {
            SolrUpdateRequest request = new ChildSetRequest(targetId, subjects,
                    IndexingActionType.MOVE);
            this.offer(request);
        } else if (JMSMessageUtil.CDRActions.ADD.equals(solrActionType)) {
            SolrUpdateRequest request = new ChildSetRequest(targetId, subjects,
                    IndexingActionType.ADD_SET_TO_PARENT);
            this.offer(request);
        } else if (JMSMessageUtil.CDRActions.REORDER.equals(solrActionType)) {
            // TODO this is a placeholder until a partial update for reorder is worked out
            for (String pidString : populateList("reordered", contentBody)) {
                this.offer(pidString, IndexingActionType.ADD);
            }
        } else if (JMSMessageUtil.CDRActions.INDEX.equals(solrActionType)) {
            String operation = contentBody.getName();
            IndexingActionType indexingAction = IndexingActionType.getAction(IndexingActionType.namespace
                    + operation);
            if (indexingAction != null) {
                if (IndexingActionType.SET_DEFAULT_WEB_OBJECT.equals(indexingAction)) {
                    SolrUpdateRequest request = new ChildSetRequest(targetId, subjects,
                            IndexingActionType.SET_DEFAULT_WEB_OBJECT);
                    this.offer(request);
                } else {
                    for (String pidString : subjects) {
                        this.offer(pidString, indexingAction);
                    }
                }
            }
        } else if (JMSMessageUtil.CDRActions.REINDEX.equals(solrActionType)) {
            // Determine which kind of reindex to perform based on the mode
            if (mode.equals("inplace")) {
                this.offer(parent, IndexingActionType.RECURSIVE_REINDEX);
            } else {
                this.offer(parent, IndexingActionType.CLEAN_REINDEX);
            }
        } else if (JMSMessageUtil.CDRActions.PUBLISH.equals(solrActionType)) {
            for (String pidString : subjects) {
                this.offer(pidString, IndexingActionType.UPDATE_STATUS);
            }
        } else if (JMSMessageUtil.CDRActions.EDIT_TYPE.equals(solrActionType)) {
            SolrUpdateRequest request = new ChildSetRequest(targetId, subjects,
                    IndexingActionType.UPDATE_TYPE);
            this.offer(request);
        }
    }

    private List<String> populateList(String field, Element contentBody){
        List<Element> children = contentBody.getChildren(field, JDOMNamespaceUtil.CDR_MESSAGE_NS);

        if (children == null || children.size() == 0) {
            return null;
        }

        List<String> list = new ArrayList<String>();
        for (Object node: children){
            Element element = (Element)node;
            for (Object pid: element.getChildren()){
                Element pidElement = (Element)pid;
                list.add(pidElement.getTextTrim());
            }
        }

        return list;
    }

    private void sendMessage(Document msg) {
        XMLOutputter out = new XMLOutputter();
        final String msgStr = out.outputString(msg);

        this.jmsTemplate.send(new MessageCreator() {

            @Override
            public javax.jms.Message createMessage(Session session) throws JMSException {
                return session.createTextMessage(msgStr);
            }

        });
    }

    public void setJmsTemplate(JmsTemplate jmsTemplate) {
        this.jmsTemplate = jmsTemplate;
    }

    private void offer(String pid, IndexingActionType solrActionType) {
        offer(new SolrUpdateRequest(pid, solrActionType));
    }

    private void offer(SolrUpdateRequest ingestRequest) {
        String pid = ingestRequest.getPid().getRepositoryPath();
        String solrActionType = ingestRequest.getUpdateAction().toString();

        List<String> children = null;

        if (ingestRequest instanceof ChildSetRequest) {
            children = new ArrayList<>();
            for (PID p : ((ChildSetRequest) ingestRequest).getChildren()) {
                children.add(p.getId());
            }
        }

        String childrenStr = children.stream().map(Object::toString)
            .collect(Collectors.joining(","));

        Document msg = new Document();
        Element entry = new Element("entry", ATOM_NS);
        msg.addContent(entry);
        entry.addContent(new Element("pid", ATOM_NS).setText(pid));
        entry.addContent(new Element("solrActionType", ATOM_NS).setText(solrActionType));
        entry.addContent(new Element("children", ATOM_NS).setText(childrenStr));

        sendMessage(msg);
    }
}
