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

        return xmlDoc;
    }
}