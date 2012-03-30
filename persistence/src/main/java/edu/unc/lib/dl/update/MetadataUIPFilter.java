package edu.unc.lib.dl.update;

import java.util.List;

import org.jdom.Element;

public abstract class MetadataUIPFilter implements UIPUpdateFilter {

	protected Element getNewModifiedElement(MetadataUIP uip, String datastreamName){
		Element incoming = uip.getIncomingData().get(datastreamName);
		return getNewModifiedElement(uip, datastreamName, incoming);
	}
	
	protected Element getNewModifiedElement(MetadataUIP uip, String datastreamName, Element incoming){
		if (incoming == null)
			return null;
		
		//If this is a replace operation, then the new modified element is simply the incoming element.
		if (uip.operation.equals(UpdateOperation.REPLACE))
			return (Element)incoming.clone();

		Element modified = uip.getModifiedData().get(datastreamName);
		Element original = uip.getOriginalData().get(datastreamName);

		Element newModified = null;

		if (modified == null) {
			// If there is no original or modified data, than return the incoming as new modified
			if (original == null) {
				return (Element) incoming.clone();
			} else {
				// Set the base for the new modified object to the original data
				newModified = (Element) original.clone();
			}
		} else {
			// Use the previous modified data
			newModified = (Element) modified.clone();
		}
		
		return newModified;
	}
	
	/**
	 * Performs an add operation assuming there are no uniqueness restrictions
	 * @param uip
	 * @return
	 * @throws UIPException
	 */
	protected Element performAdd(MetadataUIP uip, String datastreamName) throws UIPException {
		Element incoming = uip.getIncomingData().get(datastreamName);
		return performAdd(uip, datastreamName, incoming);
	}
		
	protected Element performAdd(MetadataUIP uip, String datastreamName, Element incoming) throws UIPException {
		Element newModified = getNewModifiedElement(uip, datastreamName, incoming);
		if (newModified == null)
			return null;

		// Clone all the child elements of the incoming metadata
		@SuppressWarnings("unchecked")
		List<Element> incomingElements = (List<Element>) incoming.getChildren();
		// Add all the incoming element children to the base modified object
		for (Element incomingElement : incomingElements) {
			newModified.addContent((Element) incomingElement.clone());
		}

		return newModified;
	}
	
	protected Element performReplace(MetadataUIP uip, String datastreamName) throws UIPException {
		return getNewModifiedElement(uip, datastreamName);
	}
	
	protected Element performReplace(MetadataUIP uip, String datastreamName, Element incoming) throws UIPException {
		return getNewModifiedElement(uip, datastreamName, incoming);
	}
}
