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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathBuilder;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

import edu.unc.lib.dl.ui.service.XMLRetrievalService;

/**
 * Stores lookup mapping objects and handles lookups for values contained within them
 * @author bbpennel
 *
 */
public class LookupMappingsSettings {

    private static List<String> sourcePaths;
    private static Map<String,Map<String,String>> mappings;

    public LookupMappingsSettings() {
        mappings = new HashMap<String,Map<String,String>>();
    }

    public static Map<String, String> getMapping(String mapping) {
        return mappings.get(mapping);
    }

    public static String getLookup(String mapping, String key) {
        return getLookup(mapping, key, null);
    }

    public static String getLookup(String mapping, String key, String type) {
        try {
            if (type != null) {
                String lookupValue = mappings.get(mapping).get(key + "|" + type);
                if (lookupValue != null) {
                    return lookupValue;
                }
            }
            return mappings.get(mapping).get(key);
        } catch (Exception e) {
            //Invalid mapping was most likely requested
        }
        return null;
    }

    public void init() {
        updateMappings();
    }

    public static void updateMappings() {
        for (String sourcePath: sourcePaths) {
            try {
                Document document = XMLRetrievalService.getXMLDocument(sourcePath);
                XPathFactory xpf = XPathFactory.instance();
                XPathExpression<Element> xpath =
                        new XPathBuilder<Element>("/mappings/mapping", Filters.element()).compileWith(xpf);
                List<Element> nodes = xpath.evaluate(document);
                for (Element node: nodes) {
                    Map<String,String> mapping;
                    Attribute mappingKey = node.getAttribute("key");
                    //If the mapping doesn't have a key, then don't process it
                    if (mappingKey == null) {
                        continue;
                    }
                    Attribute ordered = node.getAttribute("ordered");
                    if (ordered == null || !Boolean.parseBoolean(ordered.getValue())) {
                        mapping = new HashMap<String,String>();
                    } else {
                        mapping = new LinkedHashMap<String,String>();
                    }
                    mappings.put(mappingKey.getValue(), mapping);

                    XPathExpression<Element> xpathPair =
                            new XPathBuilder<Element>("pair", Filters.element()).compileWith(xpf);
                    List<Element> pairNodes = xpathPair.evaluate(node);
                    for (Element pairNode: pairNodes) {
                        Attribute key = pairNode.getAttribute("key");
                        if (key != null) {
                            mapping.put(key.getValue(), pairNode.getValue());
                        }

                    }
                }
            } catch (Exception e) {

            }
        }
    }

    public List<String> getSourcePaths() {
        return sourcePaths;
    }

    public void setSourcePaths(List<String> sourcePaths) {
        LookupMappingsSettings.sourcePaths = sourcePaths;
    }

}
