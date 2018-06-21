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

package edu.unc.lib.dl.services.camel.solr;

import static edu.unc.lib.dl.services.camel.util.CdrFcrepoHeaders.CdrUpdateAction;
import static edu.unc.lib.dl.util.JMSMessageUtil.CDRActions.ADD;
import static edu.unc.lib.dl.util.JMSMessageUtil.CDRActions.MOVE;
import static edu.unc.lib.dl.util.JMSMessageUtil.CDRActions.PUBLISH;
import static edu.unc.lib.dl.util.JMSMessageUtil.CDRActions.UPDATE_DESCRIPTION;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.jdom2.Document;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.services.IndexingMessageSender;
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
 * @author harring
 *
 */
public class CdrEventToSolrUpdateProcessor implements Processor {
    private static final Logger log = LoggerFactory.getLogger(CdrEventToSolrUpdateProcessor.class);

    private IndexingMessageSender messageSender;

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
        String solrActionType = (String) in.getHeader(CdrUpdateAction);
        String userid = (String) in.getHeader("name");

        if (solrActionType == null || solrActionType.equals("none")) {
            log.warn("No solr update action specified, ignoring event for object {}", targetId);
            return;
        }

        List<String> subjects = populateList("subjects", contentBody);
        List<PID> childPids = new ArrayList<>();
        for (String subject : subjects) {
            childPids.add(PIDs.get(subject));
        }

        if (MOVE.equals(solrActionType)) {
            messageSender.sendIndexingOperation(userid, PIDs.get(targetId), childPids,
                    IndexingActionType.MOVE);
        } else if (ADD.equals(solrActionType)) {
            messageSender.sendIndexingOperation(userid, PIDs.get(targetId), childPids,
                    IndexingActionType.ADD_SET_TO_PARENT);
        } else if (UPDATE_DESCRIPTION.equals(solrActionType)) {
            messageSender.sendIndexingOperation(userid, PIDs.get(targetId), childPids,
                    IndexingActionType.UPDATE_DESCRIPTION);
        } else if (PUBLISH.equals(solrActionType)) {
            for (PID childPid : childPids) {
                messageSender.sendIndexingOperation(userid, childPid,
                        IndexingActionType.UPDATE_STATUS);
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

    public void setIndexingMessageSender(IndexingMessageSender indexingMessageSender) {
        this.messageSender = indexingMessageSender;
    }

}
