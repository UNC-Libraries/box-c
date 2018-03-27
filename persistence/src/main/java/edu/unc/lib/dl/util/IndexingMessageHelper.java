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
package edu.unc.lib.dl.util;

import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.ATOM_NS;
import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.CDR_MESSAGE_NS;

import java.util.Collection;

import org.jdom2.Document;
import org.jdom2.Element;

import edu.unc.lib.dl.fedora.PID;

/**
 * Helper methods for generating indexing messages
 *
 * @author harring
 * @author bbpennel
 *
 */
public class IndexingMessageHelper {

    private IndexingMessageHelper() {
    }

    public static Document makeIndexingOperationBody(String userid, PID targetPid, Collection<PID> children,
            IndexingActionType actionType) {
        Document msg = new Document();
        Element entry = new Element("entry", ATOM_NS);
        msg.addContent(entry);
        entry.addContent(new Element("author", ATOM_NS).addContent(new Element("name", ATOM_NS).setText(userid)));
        entry.addContent(new Element("pid", ATOM_NS).setText(targetPid.getRepositoryPath()));
        if (children != null && children.size() > 0) {
            Element childEl = new Element("children", CDR_MESSAGE_NS);
            entry.addContent(childEl);
            for (PID child : children) {
                childEl.addContent(new Element("pid", CDR_MESSAGE_NS).setText(child.getRepositoryPath()));
            }
        }
        entry.addContent(new Element("actionType", ATOM_NS)
                .setText(actionType.getURI().toString()));

        return msg;
    }
}
