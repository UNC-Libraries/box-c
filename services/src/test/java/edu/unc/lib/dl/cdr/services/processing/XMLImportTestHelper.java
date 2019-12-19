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
package edu.unc.lib.dl.cdr.services.processing;

import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.MODS_V3_NS;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;

import org.apache.commons.io.FileUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.XMLOutputter;

import edu.unc.lib.dl.fedora.PID;

/**
 * @author bbpennel
 *
 */
public class XMLImportTestHelper {

    private XMLImportTestHelper() {
    }

    public static File writeToFile(Document doc) throws Exception {
        File xmlFile = File.createTempFile("doc", ".xml");
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        new XMLOutputter().output(doc, outStream);
        FileUtils.copyInputStreamToFile(new ByteArrayInputStream(outStream.toByteArray()), xmlFile);
        return xmlFile;
    }

    public static Document makeUpdateDocument() {
        Document doc = new Document();
        doc.addContent(new Element("bulkMetadata"));
        return doc;
    }

    public static Element addObjectUpdate(Document doc, PID pid, String lastModified) {
        Element objEl = addObject(doc, pid);
        Element updateEl = new Element("update");
        updateEl.setAttribute("type", "MODS");
        if (lastModified != null) {
            updateEl.setAttribute("lastModified", lastModified);
        }
        objEl.addContent(updateEl);

        return updateEl;
    }

    public static Element addObject(Document doc, PID pid) {
        Element objEl = new Element("object");
        if (pid != null) {
            objEl.setAttribute("pid", pid.getId());
        }
        doc.getRootElement().addContent(objEl);
        return objEl;
    }

    public static Element modsWithTitleAndDate(String title, String dateCreated) {
        Element modsEl = new Element("mods", MODS_V3_NS)
                .addContent(new Element("titleInfo", MODS_V3_NS)
                        .addContent(new Element("title", MODS_V3_NS).setText(title)));
        if (dateCreated != null) {
            modsEl.addContent(new Element("originInfo", MODS_V3_NS)
                    .addContent(new Element("dateCreated", MODS_V3_NS)
                            .setAttribute("encoding", "iso8601")
                            .setAttribute("keyDate", "yes")
                            .setText(dateCreated)));
        }

        return modsEl;
    }
}
