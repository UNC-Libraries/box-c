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
package edu.unc.lib.dl.services.camel;

import static edu.unc.lib.dl.rdf.Fcrepo4Repository.Binary;
import static edu.unc.lib.dl.services.camel.util.CdrFcrepoHeaders.CdrBinaryMimeType;
import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.ATOM_NS;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.jdom2.Document;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.services.camel.util.MessageUtil;

/**
 * @author lfarrell
 */
public class BinaryEnhancementProcessor implements Processor {
    private static final Logger log = LoggerFactory.getLogger(BinaryEnhancementProcessor.class);

    @Override
    public void process(final Exchange exchange) throws Exception {
        final Message in = exchange.getIn();
        String fcrepoBinaryUri = (String) in.getHeader(FCREPO_URI);

        if (fcrepoBinaryUri == null) {
            Document msgBody = MessageUtil.getDocumentBody(in);
            Element body = msgBody.getRootElement();

            String pidValue = body.getChild("pid", ATOM_NS).getTextTrim();
            String mimeType = body.getChild("mimeType", ATOM_NS).getTextTrim();

            log.info("Adding enhancement headers for " + pidValue);
            in.setHeader(FCREPO_URI, pidValue);
            in.setHeader(CdrBinaryMimeType, mimeType);
            in.setHeader("org.fcrepo.jms.resourceType", Binary.getURI());
        }
    }
}