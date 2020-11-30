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

import static edu.unc.lib.dl.util.JMSMessageUtil.CDRActions.RUN_ENHANCEMENTS;
import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.CDR_MESSAGE_NS;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.jdom2.Document;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.RepositoryObject;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;
import edu.unc.lib.dl.fedora.ObjectTypeMismatchException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.services.camel.util.MessageUtil;

/**
 * Sets headers related to identifying binary objects to run enhancement operations on
 *
 * @author lfarrell
 */
public class BinaryEnhancementProcessor implements Processor {
    private static final Logger log = LoggerFactory.getLogger(BinaryEnhancementProcessor.class);

    private RepositoryObjectLoader repoObjLoader;

    @Override
    public void process(final Exchange exchange) throws Exception {
        final Message in = exchange.getIn();
        String fcrepoBinaryUri = (String) in.getHeader(FCREPO_URI);

        if (fcrepoBinaryUri == null) {
            Document msgBody = MessageUtil.getDocumentBody(in);
            Element body = msgBody.getRootElement();

            Element enhancementsEl = body.getChild(RUN_ENHANCEMENTS.getName(), CDR_MESSAGE_NS);
            if (enhancementsEl != null) {
                String pidValue = enhancementsEl.getChild("pid", CDR_MESSAGE_NS).getTextTrim();
                PID objPid = PIDs.get(pidValue);

                try {
                    RepositoryObject repoObj = repoObjLoader.getRepositoryObject(objPid);

                    log.info("Adding enhancement headers for " + pidValue);
                    in.setHeader(FCREPO_URI, pidValue);
                    in.setHeader(FcrepoJmsConstants.RESOURCE_TYPE, String.join(",", repoObj.getTypes()));

                    Element forceText = enhancementsEl.getChild("force", CDR_MESSAGE_NS);
                    if (forceText != null) {
                        in.setHeader("force", forceText.getTextTrim());
                    } else {
                        in.setHeader("force", "false");
                    }
                } catch (ObjectTypeMismatchException e) {
                    log.warn("{} is not a repository object. No enhancement headers added", objPid.getURI());
                }
            }
        }
    }

    /**
     * @param repoObjLoader the repoObjLoader to set
     */
    public void setRepositoryObjectLoader(RepositoryObjectLoader repoObjLoader) {
        this.repoObjLoader = repoObjLoader;
    }
}