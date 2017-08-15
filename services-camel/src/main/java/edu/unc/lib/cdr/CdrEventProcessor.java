package edu.unc.lib.cdr;

import static edu.unc.lib.cdr.headers.CdrFcrepoHeaders.CdrSolrUpdateAction;
import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.ATOM_NS;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.jdom2.Document;
import org.jdom2.input.SAXBuilder;

public class CdrEventProcessor implements Processor {
    @Override
    public void process(Exchange exchange) throws Exception {
        final Message in = exchange.getIn();
        String body = (String) in.getBody();

        try {
            SAXBuilder parseXML= new SAXBuilder();
            InputStream stream = new ByteArrayInputStream(body.getBytes("UTF-8"));
            Document solrMsg = parseXML.build(stream);

            String actionType = solrMsg.getRootElement().getChild("title", ATOM_NS).getTextTrim();
            in.setHeader(CdrSolrUpdateAction, actionType);
        } catch(NullPointerException e) {
            in.setHeader(CdrSolrUpdateAction, "none");
        }
    }

}