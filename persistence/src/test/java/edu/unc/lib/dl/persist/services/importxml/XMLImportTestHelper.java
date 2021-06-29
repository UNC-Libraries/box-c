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
package edu.unc.lib.dl.persist.services.importxml;

import static edu.unc.lib.boxc.model.api.xml.JDOMNamespaceUtil.MODS_V3_NS;
import static org.apache.commons.io.FileUtils.copyInputStreamToFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;

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
        copyInputStreamToFile(documentToInputStream(doc), xmlFile);
        return xmlFile;
    }

    public static InputStream documentToInputStream(Document doc) throws Exception {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        new XMLOutputter().output(doc, outStream);
        return new ByteArrayInputStream(outStream.toByteArray());
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
