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
package edu.unc.lib.dl.services.camel.longleaf;

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

import edu.unc.lib.dl.services.camel.util.MessageUtil;

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
