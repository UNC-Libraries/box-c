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
package edu.unc.lib.dl.services;

import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.ATOM_NS;
import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.CDR_MESSAGE_NS;

import java.net.URI;
import java.util.Map;

import org.jdom2.Document;
import org.jdom2.Element;

import edu.unc.lib.dl.fedora.PID;

/**
 * Helper methods for destroy objects messages
 *
 * @author lfarrell
 */
public class DestroyObjectsMessageHelpers {
    private DestroyObjectsMessageHelpers() {
    }

    /**
     * Sends a remove object message from the repository message
     *
     * @param  userid user making request
     * @param objsToDestroy objects removed
     * @return id of operation message
     */
    public static Document makeDestroyOperationBody(String userid, PID pid,
                                                    Map<URI, Map<String, String>> objsToDestroy) {
        Document msg = new Document();
        Element entry = new Element("entry", ATOM_NS);

        entry.addContent(new Element("author", ATOM_NS)
                .addContent(new Element("name", ATOM_NS).setText(userid)));

        entry.addContent(new Element("pid", CDR_MESSAGE_NS).setText(pid.getRepositoryPath()));

        Element pidList = new Element("objsToDestroy", ATOM_NS);

        objsToDestroy.forEach((contentUri, metadata) -> {
            Element entryValues = new Element("contentUri").setText(contentUri.toString());
            entryValues.addContent(new Element("mimetype").setText(metadata.get("mimeType")));
            entryValues.addContent(new Element("pidId").setText(metadata.get("pid")));
            pidList.addContent(entryValues);
        });

        entry.addContent(pidList);
        msg.addContent(entry);

        return msg;
    }
}
