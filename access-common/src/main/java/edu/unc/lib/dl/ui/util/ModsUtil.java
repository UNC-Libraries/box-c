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
package edu.unc.lib.dl.ui.util;

import org.jdom2.Document;
import org.jdom2.Element;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class to remove empty nodes from MODS documents
 *
 * @author lfarrell
 */
public class ModsUtil {
    public static Document removeEmptyNodes(Document doc) {
        List<Element> updatedMods = new ArrayList<Element>();

        Element docRoot = doc.getRootElement();
        List<Element> modsRoot = docRoot.getChildren();
        List<Element> emptyNodes = recursiveRemoveEmptyContent(modsRoot, updatedMods);

        for (int i = 0; i < emptyNodes.size(); i++) {
            docRoot.removeContent(emptyNodes.get(i));
        }

        return doc;
    }

    private static List<Element> recursiveRemoveEmptyContent(List<Element> content, List<Element> updatedMods) {
        for (int i = 0; i < content.size(); i++) {
            Element node = content.get(i);
            Element parent = node.getParentElement();

            if (node.getChildren().size() > 0) {
                recursiveRemoveEmptyContent(node.getChildren(), updatedMods);
            } else if (node.getTextTrim().equals("")) {
                updatedMods.add(node);
                List<Element> parentElements = new ArrayList<Element>();
                updatedMods.addAll(emptyParent(parent, parentElements));
            } else if (updatedMods.contains(parent)) {
              updatedMods.remove(parent);
            }
        }

        return updatedMods;
    }

    private static List<Element> emptyParent(Element parent, List<Element> parentElements) {
        if (parent == null) {
            return parentElements;
        }

        if (parent.getTextTrim().equals("")) {
            Element grandparent = parent.getParentElement();
            parentElements.add(parent);
            emptyParent(grandparent, parentElements);
        }

        return parentElements;
    }
}
