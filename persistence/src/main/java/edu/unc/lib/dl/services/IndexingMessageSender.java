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
package edu.unc.lib.dl.services;

import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.ATOM_NS;
import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.CDR_MESSAGE_NS;

import java.util.Collection;

import org.jdom2.Document;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.IndexingActionType;

/**
 * Constructs and sends JMS messages describing CDR operations related to reindexing
 *
 * @author harring
 *
 */
public class IndexingMessageSender extends AbstractMessageSender {

    private static final Logger LOG = LoggerFactory.getLogger(IndexingMessageSender.class);

    /**
     * Adds message to JMS queue for object(s) to be reindexed.
     *
     * @param userid id of user who triggered the operation
     * @param targetPid PID of object to be indexed
     * @param solrActionType type of indexing action to perform
     */
    public void sendIndexingOperation(String userid, PID targetPid, IndexingActionType solrActionType) {
        sendIndexingOperation(userid, targetPid, null, solrActionType);
    }

    /**
     * Adds message to JMS queue for object(s) to be reindexed.
     *
     * @param userid id of user who triggered the operation
     * @param targetPid PID of object to be indexed
     * @param children pids of other objects to be indexed
     * @param solrActionType type of indexing action to perform
     */
    public void sendIndexingOperation(String userid, PID targetPid, Collection<PID> children,
            IndexingActionType solrActionType) {
        Document msg = new Document();
        Element entry = new Element("entry", ATOM_NS);
        msg.addContent(entry);
        entry.addContent(new Element("author", ATOM_NS).addContent(new Element("name", ATOM_NS).setText(userid)));
        entry.addContent(new Element("pid", ATOM_NS).setText(targetPid.getRepositoryPath()));
        if (children != null && children.size() > 0) {
            Element childEl = new Element("children", CDR_MESSAGE_NS);
            entry.addContent(childEl);
            for (PID child : children) {
                childEl.addContent(new Element("pid", CDR_MESSAGE_NS).setText(child.getRepositoryPath()));
            }
        }
        entry.addContent(new Element("solrActionType", ATOM_NS)
                .setText(solrActionType.getURI().toString()));

        LOG.debug("sending solr update message for {} of type {}", targetPid, solrActionType.toString());
        sendMessage(msg);
        LOG.debug("sent indexing operation JMS message using JMS template: {}", this.getJmsTemplate());
    }
}
