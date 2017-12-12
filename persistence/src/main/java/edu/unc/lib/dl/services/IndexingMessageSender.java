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

import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.CDR_MESSAGE_NS;

import java.util.Collection;

import org.jdom2.Document;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.IndexingActionType;
import edu.unc.lib.dl.util.JMSMessageUtil.CDRActions;

/**
 * Constructs and sends JMS messages describing CDR operations related to reindexing
 *
 * @author harring
 *
 */
public class IndexingMessageSender extends AbstractMessageSender {

    private static final Logger LOG = LoggerFactory.getLogger(IndexingMessageSender.class);

    /**
     * Sends message indicating that objects are being requested to be reindexed.
     *
     * @param userid id of user who triggered the operation
     * @param pids objects to be reindexed
     * @param type type of indexing action to perform
     * @return id of operation message
     */
    public String sendIndexingOperation(String userid, Collection<PID> pids, IndexingActionType type) {
        Element contentEl = createAtomEntry(userid, pids.iterator().next(),
                CDRActions.INDEX);

        Element indexEl = new Element(type.getName(), CDR_MESSAGE_NS);
        contentEl.addContent(indexEl);

        Element subjects = new Element("subjects", CDR_MESSAGE_NS);
        indexEl.addContent(subjects);
        for (PID sub : pids) {
            subjects.addContent(new Element("pid", CDR_MESSAGE_NS).setText(sub.getRepositoryPath()));
        }

        Document msg = contentEl.getDocument();
        sendMessage(msg);
        LOG.debug("sent indexing operation JMS message using JMS template: {}", this.getJmsTemplate());

        return getMessageId(msg);
    }

}
