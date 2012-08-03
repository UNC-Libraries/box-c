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
package edu.unc.lib.dl.update;

import java.util.List;

import org.apache.log4j.Logger;
import org.jdom.Attribute;
import org.jdom.Element;

import edu.unc.lib.dl.util.ContentModelHelper.Datastream;
import edu.unc.lib.dl.xml.JDOMNamespaceUtil;

public class RELSEXTUIPFilter extends MetadataUIPFilter {
	private static Logger log = Logger.getLogger(RELSEXTUIPFilter.class);
	
	private final String datastreamName = Datastream.RELS_EXT.getName();
	
	@Override
	public UpdateInformationPackage doFilter(UpdateInformationPackage uip) throws UIPException {
	// Only run this filter for metadata update requests
		if (uip == null || !(uip instanceof MetadataUIP))
			return uip;

		// Do not apply filter unless the mods ds is being targeted.
		if (!(uip.getIncomingData().containsKey(datastreamName) || uip.getModifiedData().containsKey(datastreamName)))
			return uip;
		
		log.debug("Performing MODS filter operation " + uip.getOperation().name() + " on " + uip.getPID().getPid());
		
		MetadataUIP metadataUIP = (MetadataUIP) uip;
		
		Element newModified = null;
		
		switch (uip.getOperation()) {
			case REPLACE:
				newModified = performReplace(metadataUIP, datastreamName);
				break;
			case ADD:
				newModified = performAdd(metadataUIP, datastreamName);
				break;
			case UPDATE:
				// Doing add for update since the schema does not allow a way to indicate a tag should replace another
				newModified = performAdd(metadataUIP, datastreamName);
				break;
		}

		if (newModified != null) {
			// no validation yet
			validate(metadataUIP, newModified);
			metadataUIP.getModifiedData().put(datastreamName, newModified);
		}
		
		return uip;
	}
	
	protected Element performAdd(MetadataUIP uip, String datastreamName) throws UIPException {
		Element incoming = uip.getIncomingData().get(datastreamName);
		Element newModified = getNewModifiedElement(uip, datastreamName, incoming);
		if (newModified == null)
			return null;
		
		// If there is no rdf:Description tag in the incoming data, then there is nothing to add.
		if (incoming.getChildren().size() == 0)
			return newModified;
		
		Element incomingDescription = (Element)incoming.getChildren().get(0);
		Element newDescription = null;
		
		// If the previous rels-ext didn't have a description, then use the new one
		if (newModified.getChildren().size() == 0){
			newModified.addContent((Element)incomingDescription.clone());
			return newModified;
		} else {
			newDescription = (Element)newModified.getChildren().get(0);
		}
		
		// Clone all the child elements of the incoming rdf:Description tag
		@SuppressWarnings("unchecked")
		List<Element> incomingElements = (List<Element>) incomingDescription.getChildren();
		// Add all the incoming element children to the base modified object
		for (Element incomingElement : incomingElements) {
			newDescription.addContent((Element) incomingElement.clone());
		}

		return newModified;
	}

	public void validate(MetadataUIP uip, Element relsEXT){
		//Make sure Description has rdf:about set, and that is is the objects pid
		Element descriptionElement = relsEXT.getChild("Description", JDOMNamespaceUtil.RDF_NS);
		if (descriptionElement.getAttribute("about", JDOMNamespaceUtil.RDF_NS) == null || 
				(descriptionElement.getAttribute("about", JDOMNamespaceUtil.RDF_NS) != null && !uip.getPID().getURI().equals(descriptionElement.getAttributeValue("about", JDOMNamespaceUtil.RDF_NS)))){
			Attribute aboutAttribute = new Attribute("about", uip.getPID().getURI(), JDOMNamespaceUtil.RDF_NS);
			descriptionElement.setAttribute(aboutAttribute);
		}
	}
}
