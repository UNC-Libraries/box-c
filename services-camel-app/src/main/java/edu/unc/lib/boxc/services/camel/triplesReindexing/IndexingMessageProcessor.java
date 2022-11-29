package edu.unc.lib.boxc.services.camel.triplesReindexing;

import static edu.unc.lib.boxc.model.api.xml.JDOMNamespaceUtil.ATOM_NS;
import static edu.unc.lib.boxc.services.camel.util.CdrFcrepoHeaders.CdrUpdateAction;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.jdom2.Document;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingActionType;
import edu.unc.lib.boxc.services.camel.util.MessageUtil;

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
        final Message in = exchange.getIn();

        Document msgBody = MessageUtil.getDocumentBody(in);
        Element body = msgBody.getRootElement();

        String pidValue = body.getChild("pid", ATOM_NS).getTextTrim();
        log.debug("Processing indexing message for {}", pidValue);
        PID pid = PIDs.get(pidValue);
        String action = body.getChild("actionType", ATOM_NS).getTextTrim();
        IndexingActionType actionType = IndexingActionType.getAction(action);

        in.setHeader(FCREPO_URI, pid.getRepositoryPath());
        in.setHeader(CdrUpdateAction, actionType.getName());
    }
}
