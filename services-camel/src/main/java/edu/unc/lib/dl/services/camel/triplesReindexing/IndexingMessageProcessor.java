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
package edu.unc.lib.dl.services.camel.triplesReindexing;

import static edu.unc.lib.dl.services.camel.util.CdrFcrepoHeaders.CdrUpdateAction;
import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.ATOM_NS;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.jdom2.Document;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.services.camel.util.MessageUtil;
import edu.unc.lib.dl.util.IndexingActionType;

/**
 * Processes an indexing message into actionable headers.
 *
 * @author bbpennel
 *
 */
public class IndexingMessageProcessor implements Processor {
    final Logger log = LoggerFactory.getLogger(IndexingMessageProcessor.class);

    @Override
    public void process(Exchange exchange) throws Exception {
        log.debug("Processing solr update");
        final Message in = exchange.getIn();

        Document msgBody = MessageUtil.getDocumentBody(in);
        Element body = msgBody.getRootElement();

        String pidValue = body.getChild("pid", ATOM_NS).getTextTrim();
        PID pid = PIDs.get(pidValue);
        String action = body.getChild("actionType", ATOM_NS).getTextTrim();
        IndexingActionType actionType = IndexingActionType.getAction(action);

        in.setHeader(FCREPO_URI, pid.getRepositoryPath());
        in.setHeader(CdrUpdateAction, actionType.getName());
    }
}
