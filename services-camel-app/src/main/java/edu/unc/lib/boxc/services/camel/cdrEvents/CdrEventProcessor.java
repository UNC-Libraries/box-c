package edu.unc.lib.boxc.services.camel.cdrEvents;

import static edu.unc.lib.boxc.model.api.xml.JDOMNamespaceUtil.ATOM_NS;
import static edu.unc.lib.boxc.services.camel.util.CdrFcrepoHeaders.CdrUpdateAction;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.jdom2.Document;

import edu.unc.lib.boxc.services.camel.util.MessageUtil;

/**
 * Processes CDR Events, extracting the body and headers
 *
 * @author lfarrell
 *
 */
public class CdrEventProcessor implements Processor {
    @Override
    public void process(Exchange exchange) throws Exception {
        final Message in = exchange.getIn();
        Document document = MessageUtil.getDocumentBody(in);
        if (document == null) {
            return;
        }

        String actionType = document.getRootElement().getChildTextTrim("title", ATOM_NS);
        if (actionType == null) {
            in.setHeader(CdrUpdateAction, null);
            return;
        }
        in.setHeader(CdrUpdateAction, actionType);

        String author = document.getRootElement().getChildTextTrim("name", ATOM_NS);
        in.setHeader("name", author);

        // Pass the body document along for future processors
        in.setBody(document);
    }

}