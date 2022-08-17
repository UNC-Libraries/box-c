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
package edu.unc.lib.boxc.integration.factories;

import org.jdom2.Document;
import org.jdom2.Element;

import static edu.unc.lib.boxc.model.api.xml.JDOMNamespaceUtil.MODS_V3_NS;

import java.util.Map;

/**
 * @author sharonluong
 */
public class ModsFactory {
    /**
     * @param options
     * @return MODS document with specified fields, or null of no fields were set
     */
    public Document createDocument(Map<String, String> options) {
        var xmlDoc = new Document().addContent(new Element("mods",MODS_V3_NS));
        var modsElement = xmlDoc.getRootElement();
        if (options.containsKey("title")) {
            modsElement.addContent(new Element("titleInfo", MODS_V3_NS)
                    .addContent(new Element("title", MODS_V3_NS)
                            .setText(options.get("title"))));
        }

        if (options.containsKey("abstract")) {
            modsElement.addContent(new Element("abstract", MODS_V3_NS)
                        .setText(options.get("abstract")));
        }

        if (options.containsKey("dateCreated"))  {
            modsElement.addContent(new Element("originInfo", MODS_V3_NS)
                    .addContent(new Element("dateCreated", MODS_V3_NS).setAttribute("encoding", "iso8601")
                    .setText(options.get("dateCreated"))));
        }

        if (options.containsKey("languageTerm")) {
            modsElement.addContent(new Element("language", MODS_V3_NS)
                    .addContent(new Element("languageTerm", MODS_V3_NS)
                            .setAttribute("authority", "iso639-2b")
                            .setAttribute("type", "code")
                    .setText(options.get("languageTerm"))));
        }

        if (options.containsKey("topic")) {
            modsElement.addContent(new Element("subject", MODS_V3_NS)
                    .addContent(new Element("topic", MODS_V3_NS).setText(options.get("topic"))));
        }

        if (options.containsKey("collectionNumber")) {
            modsElement.addContent(new Element("identifier", MODS_V3_NS)
                    .setAttribute("type", "local")
                    .setAttribute("displayLabel", "Collection Number")
                    .setText(options.get("collectionNumber")));
        }

        if (options.containsKey("creator")) {
            modsElement.addContent(new Element("name", MODS_V3_NS).setAttribute("type", "personal")
                    .addContent(new Element("namePart", MODS_V3_NS).setText(options.get("creator")))
                    .addContent(new Element("role", MODS_V3_NS)
                        .addContent(new Element("roleTerm", MODS_V3_NS)
                            .setAttribute("authority", "marcrelator")
                            .setAttribute("type", "text")
                            .setText("creator"))));
        }

        return modsElement.getChildren().isEmpty() ? null : xmlDoc;
    }
}
