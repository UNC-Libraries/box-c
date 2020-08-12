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
package edu.unc.lib.dl.services.camel.solrUpdate;

import static edu.unc.lib.dl.util.IndexingActionType.ADD_SET_TO_PARENT;
import static edu.unc.lib.dl.util.IndexingActionType.CLEAN_REINDEX;
import static edu.unc.lib.dl.util.IndexingActionType.DELETE_CHILDREN_PRIOR_TO_TIMESTAMP;
import static edu.unc.lib.dl.util.IndexingActionType.DELETE_SOLR_TREE;
import static edu.unc.lib.dl.util.IndexingActionType.RECURSIVE_REINDEX;
import static edu.unc.lib.dl.util.IndexingActionType.UPDATE_ACCESS;
import static edu.unc.lib.dl.util.IndexingActionType.UPDATE_ACCESS_TREE;
import static edu.unc.lib.dl.util.IndexingActionType.UPDATE_DATASTREAMS;
import static edu.unc.lib.dl.util.IndexingActionType.UPDATE_DESCRIPTION;
import static edu.unc.lib.dl.util.IndexingActionType.UPDATE_PATH;
import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.ATOM_NS;

import java.util.EnumSet;
import java.util.Set;

import org.apache.camel.Body;
import org.apache.camel.Exchange;
import org.apache.camel.Header;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.services.camel.util.CdrFcrepoHeaders;
import edu.unc.lib.dl.services.camel.util.MessageUtil;
import edu.unc.lib.dl.util.IndexingActionType;

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
        // Serialize the message for persistence
        in.setBody(new XMLOutputter().outputString(msgBody));
    }

    private static final Set<IndexingActionType> LARGE_ACTIONS =
            EnumSet.of(RECURSIVE_REINDEX, CLEAN_REINDEX, DELETE_SOLR_TREE, IndexingActionType.MOVE,
                    ADD_SET_TO_PARENT, UPDATE_ACCESS_TREE, DELETE_CHILDREN_PRIOR_TO_TIMESTAMP);

    /**
     * @param action
     * @return true if the action is classified as large
     */
    public static boolean isLargeAction(@Header(CdrFcrepoHeaders.CdrSolrUpdateAction) IndexingActionType action) {
        return LARGE_ACTIONS.contains(action);
    }

    private static final Set<IndexingActionType> SMALL_ACTIONS =
            EnumSet.of(IndexingActionType.ADD, UPDATE_DESCRIPTION, UPDATE_ACCESS, UPDATE_PATH,
                    UPDATE_DATASTREAMS, IndexingActionType.COMMIT, IndexingActionType.DELETE);

    /**
     * @param action
     * @return true if the action is classified as small
     */
    public static boolean isSmallAction(@Header(CdrFcrepoHeaders.CdrSolrUpdateAction) IndexingActionType action) {
        return SMALL_ACTIONS.contains(action);
    }

    /**
     * Log an unknown solr update message
     * @param body
     */
    public void logUnknownSolrUpdate(@Body Object body) {
        if (body instanceof Document) {
            log.debug("Received unprocessable Solr indexing message:\n {}",
                    new XMLOutputter(Format.getPrettyFormat()).outputString((Document) body));
        } else {
            log.debug("Received unprocessable Solr indexing message: {}",  body);
        }
    }
}