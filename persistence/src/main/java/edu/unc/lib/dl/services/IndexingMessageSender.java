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

import java.util.List;

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
     * @param objPid object to be reindexed
     * @param children any children of object to be reindexed (optional)
     * @param solrActionType type of indexing action to perform
     */
    public void sendIndexingOperation(PID objPid, List<String> children, IndexingActionType solrActionType) {
        Document msg = new Document();
        Element entry = new Element("entry", ATOM_NS);
        msg.addContent(entry);
        entry.addContent(new Element("pid", ATOM_NS).setText(objPid.getRepositoryPath()));
        entry.addContent(new Element("solrActionType", ATOM_NS)
                .setText(solrActionType.getURI().toString()));

        if (children != null && children.size() > 0) {
            String childrenStr = String.join(",", children);
            entry.addContent(new Element("children", ATOM_NS).setText(childrenStr));
        }

        LOG.debug("sending solr update message for {} of type {}", objPid, solrActionType.toString());
        sendMessage(msg);
        LOG.debug("sent indexing operation JMS message using JMS template: {}", this.getJmsTemplate());
    }
}
