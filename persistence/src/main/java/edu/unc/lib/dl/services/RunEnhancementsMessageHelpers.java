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

import static edu.unc.lib.dl.util.JMSMessageUtil.CDRActions.ENHANCEMENTS;
import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.ATOM_NS;
import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.CDR_MESSAGE_NS;

import org.jdom2.Document;
import org.jdom2.Element;

import edu.unc.lib.dl.fedora.PID;

/**
 * Helper methods for run enhancement messages
 *
 * @author bbpennel
 */
public class RunEnhancementsMessageHelpers {

    private RunEnhancementsMessageHelpers() {
    }

    /**
     * Generate the body for a run enhancement request message
     *
     * @param userid
     * @param pid
     * @param force
     * @return
     */
    public static Document makeEnhancementOperationBody(String userid, PID pid, Boolean force) {
        Document msg = new Document();
        Element entry = new Element("entry", ATOM_NS);
        entry.addContent(new Element("author", ATOM_NS)
                .addContent(new Element("name", ATOM_NS).setText(userid)));

        Element paramForce = new Element("force", CDR_MESSAGE_NS);

        if (force) {
            paramForce.setText("true");
        } else {
            paramForce.setText("false");
        }

        Element enhancements = new Element(ENHANCEMENTS.getName(), ATOM_NS);
        enhancements.addContent(new Element("pid", ATOM_NS).setText(pid.getRepositoryPath()));
        enhancements.addContent(paramForce);
        entry.addContent(enhancements);

        msg.addContent(entry);

        return msg;
    }
}
