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
package edu.unc.lib.dl.services.camel.destroyDerivatives;

import static edu.unc.lib.dl.services.camel.util.CdrFcrepoHeaders.CdrBinaryMimeType;
import static edu.unc.lib.dl.services.camel.util.CdrFcrepoHeaders.CdrBinaryPidId;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.jdom2.Document;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.services.camel.util.MessageUtil;
import edu.unc.lib.dl.xml.JDOMNamespaceUtil;

/**
 * Processor to set mimetype and Id of derivative to delete
 *
 * @author lfarrell
 *
 */
public class BinaryInfoProcessor implements Processor {
    private static final Logger log = LoggerFactory.getLogger(BinaryInfoProcessor.class);

    @Override
    public void process(Exchange exchange) throws Exception {
        final Message in = exchange.getIn();
        Document msgBody = MessageUtil.getDocumentBody(in);

        if (msgBody == null) {
            log.warn("Event message contained no body");
            return;
        }

        Element body = msgBody.getRootElement();
        Element content = body.getChild("content", JDOMNamespaceUtil.ATOM_NS);
        in.setHeader(CdrBinaryMimeType, content.getChild("mimetype").getTextTrim());
        in.setHeader(CdrBinaryPidId, content.getChild("pidId").getTextTrim());
    }
}
