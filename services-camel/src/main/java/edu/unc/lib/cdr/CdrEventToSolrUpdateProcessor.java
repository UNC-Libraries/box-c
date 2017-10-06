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
import static edu.unc.lib.dl.util.JMSMessageUtil.CDRActions.ADD;
import static edu.unc.lib.dl.util.JMSMessageUtil.CDRActions.INDEX;
import static edu.unc.lib.dl.util.JMSMessageUtil.CDRActions.MOVE;
import static edu.unc.lib.dl.util.JMSMessageUtil.CDRActions.PUBLISH;
import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.ATOM_NS;

import java.util.ArrayList;
import java.util.List;

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

import edu.unc.lib.dl.util.IndexingActionType;
import edu.unc.lib.dl.util.JMSMessageUtil;
import edu.unc.lib.dl.xml.JDOMNamespaceUtil;

/**
 * Processes CDR events to determine the actions required to update solr records
 * for the changed objects. Produces messages for individual solr update actions
 * and sends them to a queue.
 *
 * @author lfarrell
 * @author bbpennel
 *
 */
public class CdrEventToSolrUpdateProcessor implements Processor {
    private static final Logger log = LoggerFactory.getLogger(CdrEventToSolrUpdateProcessor.class);

    private JmsTemplate jmsTemplate;

    @Override
    public void process(Exchange exchange) throws Exception {
        final Message in = exchange.getIn();
        Document body = (Document) in.getBody();
        if (body == null) {
            log.warn("Event message contained no body");
            return;
        }

        Element content = body.getRootElement().getChild("content", JDOMNamespaceUtil.ATOM_NS);
        Element contentBody = content.getChildren().get(0);

        if (contentBody == null || contentBody.getChildren().size() == 0) {
            log.warn("Event message contained no body content");
            return;
        }

        String targetId = JMSMessageUtil.getPid(body);
        String solrActionType = (String) in.getHeader(CdrSolrUpdateAction);

        if (solrActionType == null || solrActionType.equals("none")) {
            log.warn("No solr update action specified, ignoring event for object {}", targetId);
            return;
        }

        List<String> subjects = populateList("subjects", contentBody);

        if (MOVE.equals(solrActionType)) {
            offer(targetId, IndexingActionType.MOVE, subjects);
        } else if (ADD.equals(solrActionType)) {
            offer(targetId, IndexingActionType.ADD_SET_TO_PARENT, subjects);
        } else if (INDEX.equals(solrActionType)) {
            String operation = contentBody.getName();
            IndexingActionType indexingAction = IndexingActionType.getAction(IndexingActionType.namespace
                    + operation);
            if (indexingAction != null) {
                if (IndexingActionType.SET_DEFAULT_WEB_OBJECT.equals(indexingAction)) {
                    offer(targetId, IndexingActionType.SET_DEFAULT_WEB_OBJECT, subjects);
                } else {
                    for (String pidString : subjects) {
                        offer(pidString, indexingAction);
                    }
                }
            }
        } else if (PUBLISH.equals(solrActionType)) {
            for (String pidString : subjects) {
                offer(pidString, IndexingActionType.UPDATE_STATUS);
            }
        } else {
            log.warn("Invalid solr update action {}, ignoring event for object {}", solrActionType, targetId);
            return;
        }
    }

    private List<String> populateList(String field, Element contentBody) {
        List<Element> children = contentBody.getChildren(field, JDOMNamespaceUtil.CDR_MESSAGE_NS);

        if (children == null || children.size() == 0) {
            return null;
        }

        List<String> list = new ArrayList<>();
        for (Object node : children) {
            Element element = (Element) node;
            for (Object pid : element.getChildren()) {
                Element pidElement = (Element) pid;
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
        offer(pid, solrActionType, null);
    }

    private void offer(String pid, IndexingActionType solrActionType, List<String> children) {

        Document msg = new Document();
        Element entry = new Element("entry", ATOM_NS);
        msg.addContent(entry);
        entry.addContent(new Element("pid", ATOM_NS).setText(pid));
        entry.addContent(new Element("solrActionType", ATOM_NS)
                .setText(solrActionType.getURI().toString()));

        if (children != null && children.size() > 0) {
            String childrenStr = String.join(",", children);
            entry.addContent(new Element("children", ATOM_NS).setText(childrenStr));
        }

        sendMessage(msg);
    }
}
