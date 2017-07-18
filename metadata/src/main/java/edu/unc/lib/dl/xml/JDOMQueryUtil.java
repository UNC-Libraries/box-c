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
package edu.unc.lib.dl.xml;

import java.text.ParseException;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jdom2.Element;
import org.jdom2.Namespace;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.ContentModelHelper.Relationship;
import edu.unc.lib.dl.util.DateTimeUtil;

/**
 * Convenience methods for working with JDOM
 * @author bbpennel
 *
 */
public class JDOMQueryUtil {
    private JDOMQueryUtil() {

    }

    public static Element getElementByAttribute(final List<?> elements,
            final String attribute, final String value) {
        return getElementByAttribute(elements, attribute, null, value);
    }

    public static Element getElementByAttribute(final List<?> elements,
            final String attribute, final Namespace attributeNS, final String value) {
        for (Object elementObj : elements) {
            Element element = (Element) elementObj;
            String attrValue = attributeNS == null ? element
                    .getAttributeValue(attribute) : element.getAttributeValue(
                    attribute, attributeNS);
            if (attrValue == value
                    || (value != null && value.equals(attrValue))) {
                return element;
            }
        }
        return null;
    }

    public static Element getChildByAttribute(final Element parent, final String childName,
            final Namespace namespace, final String attribute, final String value) {
        List<?> elements;
        if (childName != null) {
            if (namespace != null) {
                elements = parent.getChildren(childName, namespace);
            } else {
                elements = parent.getChildren(childName);
            }
        } else {
            elements = parent.getChildren();
        }
        return getElementByAttribute(elements, attribute, value);
    }

    public static Date parseISO6392bDateChild(final Element parent, final String childName,
            final Namespace namespace) {
        String dateString = parent.getChildText(childName, namespace);
        if (dateString != null) {
            try {
                return DateTimeUtil.parseUTCToDate(dateString);
            } catch (ParseException e) {
                // Wasn't a valid date, ignore it.
            } catch (IllegalArgumentException e) {
                // Wasn't a valid date, ignore it.
            }
        }
        return null;
    }

    /**
     * Returns a set of object PIDs retrieved from the provided RDF datastream
     *
     * @param rdfRoot
     * @param relation
     * @return
     */
    public static Set<PID> getRelationSet(final Element rdfRoot, final Relationship relation) {
        List<Element> containsEls = rdfRoot.getChild("Description", JDOMNamespaceUtil.RDF_NS).getChildren(
                relation.name(), relation.getNamespace());
        Set<PID> children = new HashSet<>();
        for (Element containsEl : containsEls) {
            children.add(new PID(containsEl.getAttributeValue("resource", JDOMNamespaceUtil.RDF_NS)));
        }
        return children;
    }
}
