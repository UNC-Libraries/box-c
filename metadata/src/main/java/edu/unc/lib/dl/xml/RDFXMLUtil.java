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

import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.RDF_NS;

import java.util.Iterator;

import org.jdom2.Element;
import org.jdom2.Namespace;

import edu.unc.lib.dl.fedora.PID;

/**
 * @author bbpennel
 * @date Aug 24, 2015
 */
public class RDFXMLUtil {
	
	public static boolean removeRelationship(Element root, String predicate, Namespace ns, PID object) {
		return removeTriple(root, predicate, ns, false, object.getURI(), null);
	}
	
	public static boolean removeLiteral(Element root, String predicate, Namespace ns, String value, String datatype) {
		return removeTriple(root, predicate, ns, true, value, datatype);
	}
	
	public static boolean removeTriple(Element root, String predicate, Namespace ns, boolean isLiteral,
			String value, String datatype) {
		Element descEl = root.getChild("Description", RDF_NS);
		if (value == null) {
			// Specific value not specified, remove all by predicate
			return descEl.removeChildren(predicate, ns);
		}
		
		boolean removed = false;
		Iterator<Element> elIt = descEl.getChildren(predicate, ns).iterator();
		while (elIt.hasNext()) {
			Element el = elIt.next();
			if (isLiteral) {
				if (datatype != null && !datatype.equals(el.getAttributeValue("resource", RDF_NS))) {
					continue;
				}
				if (value.equals(el.getText())) {
					elIt.remove();
					removed = true;
				}
			} else {
				String atVal = el.getAttributeValue("resource", RDF_NS);
				if (value.equals(atVal)) {
					elIt.remove();
					removed = true;
				}
			}
		}
		return removed;
	}
	
	public static void setExclusiveRelation(Element root, String predicate, Namespace ns, PID object) {
		setExclusiveTriple(root, predicate, ns, false, object.getURI(), null);
	}
	
	public static void setExclusiveLiteral(Element root, String predicate, Namespace ns,
			String value, String datatype) {
		setExclusiveTriple(root, predicate, ns, true, value, datatype);
	}

	public static void setExclusiveTriple(Element root, String predicate, Namespace ns, boolean isLiteral,
			String value, String datatype) {
		Element descEl = root.getChild("Description", JDOMNamespaceUtil.RDF_NS);
		
		descEl.removeChildren(predicate, ns);
		
		addTriple(root, predicate, ns, isLiteral, value, datatype);
	}
	
	public static void addTriple(Element root, String predicate, Namespace ns, boolean isLiteral,
			String value, String datatype) {
		Element descEl = root.getChild("Description", JDOMNamespaceUtil.RDF_NS);
		
		Element relEl = new Element(predicate, ns);
		if (isLiteral) {
			if (datatype != null) {
				relEl.setAttribute("datatype", datatype, JDOMNamespaceUtil.RDF_NS);
			}
			relEl.setText(value);
		} else {
			relEl.setAttribute("resource", value, JDOMNamespaceUtil.RDF_NS);
		}
		
		descEl.addContent(relEl);
	}
}
