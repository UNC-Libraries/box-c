package edu.unc.lib.boxc.services.camel.longleaf;

import static edu.unc.lib.boxc.model.api.xml.JDOMNamespaceUtil.CDR_MESSAGE_NS;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.jdom2.Document;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.boxc.services.camel.util.MessageUtil;

/**
 * Retrieve contentUri from JDOM document and send out as a list
 *
 * @author lfarrell
 */
public class GetUrisProcessor implements Processor {
    private static final Logger log = LoggerFactory.getLogger(GetUrisProcessor.class);

    @Override
    public void process(Exchange exchange) throws Exception {
        Message in = exchange.getIn();
        Document doc = MessageUtil.getDocumentBody(in);

        if (doc == null) {
            log.warn("Event message contained no body with contentUri to deregister");
            return;
        }

        Element root = doc.getRootElement();
        List<String> contentUris = root.getChild("objToDestroy", CDR_MESSAGE_NS)
                .getChildren("contentUri", CDR_MESSAGE_NS)
                .stream().map(Element::getTextTrim).collect(Collectors.toList());

        in.setBody(contentUris);
    }
}
