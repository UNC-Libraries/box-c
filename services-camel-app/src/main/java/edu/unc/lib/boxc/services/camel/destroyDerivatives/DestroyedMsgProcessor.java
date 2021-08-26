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
package edu.unc.lib.boxc.services.camel.destroyDerivatives;

import static edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants.HASHED_PATH_DEPTH;
import static edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants.HASHED_PATH_SIZE;
import static edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPaths.idToPath;
import static edu.unc.lib.boxc.services.camel.util.CdrFcrepoHeaders.CdrBinaryMimeType;
import static edu.unc.lib.boxc.services.camel.util.CdrFcrepoHeaders.CdrBinaryPath;
import static edu.unc.lib.boxc.services.camel.util.CdrFcrepoHeaders.CdrBinaryPidId;
import static edu.unc.lib.boxc.services.camel.util.CdrFcrepoHeaders.CdrObjectType;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.jdom2.Document;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.xml.JDOMNamespaceUtil;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.services.camel.util.MessageUtil;

/**
 * Processor to set mimetype and Id of derivative to delete
 *
 * @author lfarrell
 *
 */
public class DestroyedMsgProcessor implements Processor {
    private final String srcBasePath;
    private static final Logger log = LoggerFactory.getLogger(DestroyedMsgProcessor.class);

    public DestroyedMsgProcessor(String srcBasePath) {
        this.srcBasePath = srcBasePath;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        final Message in = exchange.getIn();
        Document msgBody = MessageUtil.getDocumentBody(in);

        if (msgBody == null) {
            log.warn("Event message contained no body");
            return;
        }

        Element body = msgBody.getRootElement();
        Element content = body.getChild("objToDestroy", JDOMNamespaceUtil.CDR_MESSAGE_NS);
        String objType = content.getChildTextTrim("objType", JDOMNamespaceUtil.CDR_MESSAGE_NS);
        String mimeType = content.getChildTextTrim("mimeType", JDOMNamespaceUtil.CDR_MESSAGE_NS);
        String pidId = content.getChildTextTrim("pidId", JDOMNamespaceUtil.CDR_MESSAGE_NS);
        String binaryPath = content.getChildTextTrim("contentUri", JDOMNamespaceUtil.CDR_MESSAGE_NS);

        if (objType.equals(Cdr.Collection.getURI()) || objType.equals(Cdr.AdminUnit.getURI())) {
            String uuid = PIDs.get(pidId).getId();
            String binarySubPath = idToPath(uuid, HASHED_PATH_DEPTH, HASHED_PATH_SIZE);
            Path srcPath = Paths.get(srcBasePath, binarySubPath, uuid);

            if (Files.exists(srcPath)) {
                mimeType = "image/*";
                in.setHeader("CollectionThumb", true);
            }
        }

        if (mimeType == null) {
            mimeType = "";
            if (objType.equals(Cdr.FileObject.getURI())) {
                log.warn("No mimeType given for {} of object type {}", pidId, objType);
            }
        }

        in.setHeader(CdrBinaryMimeType, mimeType);
        in.setHeader(CdrBinaryPidId, pidId);
        in.setHeader(CdrBinaryPath, binaryPath);
        in.setHeader(CdrObjectType, objType);
    }
}
