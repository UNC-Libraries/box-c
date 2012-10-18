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
package edu.unc.lib.dl.data.ingest.solr.util;

import java.text.ParseException;
import java.util.Date;
import java.util.List;

import org.jdom.Element;
import org.jdom.Namespace;

import edu.unc.lib.dl.util.DateTimeUtil;
import edu.unc.lib.dl.xml.JDOMNamespaceUtil;

public class JDOMQueryUtil {
	public static Element getElementByAttribute(List<?> elements, String attribute, String value) {
		for (Object elementObj : elements) {
			Element element = (Element) elementObj;
			String attrValue = element.getAttributeValue(attribute);
			if (attrValue == value || (value != null && value.equals(element.getAttributeValue(attribute)))) {
				return element;
			}
		}
		return null;
	}
	
	public static Element getChildByAttribute(Element parent, String childName, Namespace namespace, String attribute, String value) {
		List<?> elements;
		if (childName != null) {
			if (namespace != null)
				elements = parent.getChildren(childName, namespace);
			else elements = parent.getChildren(childName);
		} else {
			elements = parent.getChildren();
		}
		return getElementByAttribute(elements, attribute, value);
	}
	
	public static Element getMostRecentDatastreamVersion(List<?> elements) {
		if (elements == null || elements.size() == 0)
			return null;
		if (elements.size() == 1)
			return (Element) elements.get(0);
		
		String mostRecentDate = "";
		Element mostRecent = null;
		for (Object datastreamVersionObj: elements) {
			Element datastreamVersion = (Element) datastreamVersionObj;
			String created = datastreamVersion.getAttributeValue("CREATED");
			if (mostRecentDate.compareTo(created) < 0){
				mostRecentDate = created;
				mostRecent = datastreamVersion;
			}
		}
		if (mostRecent != null)
			return mostRecent;
		return (Element) elements.get(0);
	}
	
	public static Date parseISO6392bDateChild(Element parent, String childName, Namespace namespace) {
		String dateString = parent.getChildText(childName, namespace);
		if (dateString != null) {
			try {
				return DateTimeUtil.parsePartialUTCToDate(dateString);
			} catch (ParseException e) {
				// Wasn't a valid date, ignore it.
			}
		}
		return null;
	}
	
	public static String getRelationValue(String relationName, Namespace relationNS, Element relsExt) {
		Element relationEl = relsExt.getChild(relationName, relationNS);
		if (relationEl != null) {
			return relationEl.getAttributeValue("resource", JDOMNamespaceUtil.RDF_NS);
		}
		return null;
	}
}
