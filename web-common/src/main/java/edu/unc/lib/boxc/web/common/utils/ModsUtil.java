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
package edu.unc.lib.boxc.web.common.utils;

import java.util.List;

import org.jdom2.Document;
import org.jdom2.Element;

/**
 * Helper class to remove empty nodes from MODS documents
 *
 * @author lfarrell
 */
public class ModsUtil {
    private ModsUtil() {
    };

    public static Document removeEmptyNodes(Document doc) {
        Element docRoot = doc.getRootElement();
        recursiveRemoveEmptyContent(docRoot);
        return doc;
    }

    private static void recursiveRemoveEmptyContent(Element content) {
        List<Element> children = content.getChildren();
        if (children.size() > 0) {
            int i = children.size();

            while (i > 0) {
                Element node = children.get(i - 1);

                if (node.getChildren().size() > 0) {
                    recursiveRemoveEmptyContent(node);
                } else if (node.getTextTrim().equals("")) {
                    Element parent = node.getParentElement();
                    parent.removeContent(node);
                    emptyParent(parent);
                }

                i--;
            }
        }
    }

    private static void emptyParent(Element parent) {
        if (parent.getTextNormalize().equals("") && parent.getChildren().size() == 0) {
            Element grandparent = parent.getParentElement();

            if (grandparent != null) {
                grandparent.removeContent(parent);
                emptyParent(grandparent);
            }
        }
    }
}
