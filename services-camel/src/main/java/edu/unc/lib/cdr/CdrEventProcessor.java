package edu.unc.lib.cdr;

import static edu.unc.lib.cdr.headers.CdrFcrepoHeaders.CdrSolrUpdateAction;
import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.ATOM_NS;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.jdom2.Document;

public class CdrEventProcessor implements Processor {
    @Override
    public void process(Exchange exchange) throws Exception {
        final Message in = exchange.getIn();
        Document body = (Document) in.getBody();

        try {
            String actionType = body.getRootElement().getChild("title", ATOM_NS).getTextTrim();
            in.setHeader(CdrSolrUpdateAction, actionType);
        } catch(NullPointerException e) {
            in.setHeader(CdrSolrUpdateAction, "none");
        }
    }

}