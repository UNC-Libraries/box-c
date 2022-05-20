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

import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingActionType;
import edu.unc.lib.boxc.services.camel.util.CdrFcrepoHeaders;
import edu.unc.lib.boxc.services.camel.util.IndexingActionUtil;
import edu.unc.lib.boxc.services.camel.util.MessageUtil;
import org.apache.camel.Body;
import org.apache.camel.Exchange;
import org.apache.camel.Header;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.fcrepo.camel.FcrepoHeaders;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static edu.unc.lib.boxc.model.api.xml.JDOMNamespaceUtil.ATOM_NS;

/**
 * Processor which prepares update messages for further processing
 *
 * @author bbpennel
 */
public class SolrUpdatePreprocessor implements Processor {
    private final static Logger log = LoggerFactory.getLogger(SolrUpdatePreprocessor.class);

    @Override
    public void process(Exchange exchange) throws Exception {
        log.debug("Preprocessing solr update");
        final Message in = exchange.getIn();

        Document msgBody = MessageUtil.getDocumentBody(in);
        Element body = msgBody.getRootElement();

        String action = body.getChild("actionType", ATOM_NS).getTextTrim();
        IndexingActionType actionType = IndexingActionType.getAction(action);

        // Store the action type as a header
        in.setHeader(CdrFcrepoHeaders.CdrSolrUpdateAction, actionType);
        String pidString = body.getChild("pid", ATOM_NS).getTextTrim();
        PID pid = PIDs.get(pidString);
        in.setHeader(FcrepoHeaders.FCREPO_URI, pid.getRepositoryPath());

        String priority = body.getChildTextTrim("category", ATOM_NS);
        if (priority != null) {
            in.setHeader(CdrFcrepoHeaders.CdrSolrIndexingPriority, priority);
        }
    }

    /**
     * @param action
     * @return true if the action is classified as large
     */
    public static boolean isLargeAction(@Header(CdrFcrepoHeaders.CdrSolrUpdateAction) IndexingActionType action) {
        return IndexingActionUtil.LARGE_ACTIONS.contains(action);
    }

    /**
     * @param action
     * @return true if the action is classified as small
     */
    public static boolean isSmallAction(@Header(CdrFcrepoHeaders.CdrSolrUpdateAction) IndexingActionType action) {
        return IndexingActionUtil.SMALL_ACTIONS.contains(action);
    }

    /**
     * Log an unknown solr update message
     * @param body
     */
    public void logUnknownSolrUpdate(@Body Object body) {
        if (body instanceof Document) {
            log.warn("Received unprocessable Solr indexing message:\n {}",
                    new XMLOutputter(Format.getPrettyFormat()).outputString((Document) body));
        } else {
            log.warn("Received unprocessable Solr indexing message: {}",  body);
        }
    }
}