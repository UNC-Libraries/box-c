package edu.unc.lib.cdr;

import static edu.unc.lib.cdr.headers.CdrFcrepoHeaders.CdrSolrUpdateAction;

import java.util.Iterator;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.jdom2.Content;
import org.jdom2.Document;

import edu.unc.lib.dl.util.JMSMessageUtil.CDRActions;

public class CdrEventProcessor implements Processor {
    @Override
    public void process(Exchange exchange) throws Exception {
        final Message in = exchange.getIn();
        Document body = (Document) in.getBody();
        String msgValue = null;

        List<Content> msgContents = body.getContent();
        Iterator<Content> contentsIterator = msgContents.iterator();

        while (contentsIterator.hasNext()) {
            Content msgElement = contentsIterator.next();

            if (msgElement.getCType().name() == "title") {
                msgValue = msgElement.getValue();
                if (CDRActions.getAction(msgValue) != null) {
                    in.setHeader(CdrSolrUpdateAction, msgValue);
                }
            }
        }

        if (msgValue == null) {
            in.setHeader(CdrSolrUpdateAction, "none");
        }
    }

}