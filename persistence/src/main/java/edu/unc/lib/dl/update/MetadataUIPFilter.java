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

import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author bbpennel
 *
 */
public abstract class MetadataUIPFilter implements UIPUpdateFilter {
    private static Logger log = LoggerFactory.getLogger(MetadataUIPFilter.class);

    protected Element getNewModifiedElement(MetadataUIP uip, String datastreamName) {
        Element incoming = uip.getIncomingData().get(datastreamName);
        return getNewModifiedElement(uip, datastreamName, incoming);
    }

    protected Element getNewModifiedElement(MetadataUIP uip, String datastreamName, Element incoming) {
        log.debug("Getting new modified element using base " + datastreamName + " and " + incoming);
        if (incoming == null) {
            return null;
        }        // If this is a replace operation, then the new modified element is simply the incoming element.
        if (uip.getOperation().equals(UpdateOperation.REPLACE)) {
            return incoming.clone();
        }
        return this.getBaseElement(uip, datastreamName, incoming);
    }

    protected Element getBaseElement(MetadataUIP uip, String datastreamName, Element incoming) {
        Element modified = uip.getModifiedData().get(datastreamName);
        Element original = uip.getOriginalData().get(datastreamName);

        Element newModified = null;

        if (modified == null) {
            // If there is no original or modified data, than return the incoming as new modified
            if (original == null) {
                return incoming.clone();
            } else {
                // Set the base for the new modified object to the original data
                newModified = original.clone();
            }
        } else {
            // Use the previous modified data
            newModified = modified.clone();
        }

        return newModified;
    }

    /**
     * Performs an add operation assuming there are no uniqueness restrictions
     *
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
        if (newModified == null) {
            return null;
        }
        // Clone all the child elements of the incoming metadata
        List<Element> incomingElements = incoming.getChildren();
        // Add all the incoming element children to the base modified object
        for (Element incomingElement : incomingElements) {
            newModified.addContent(incomingElement.clone());
        }

        return newModified;
    }

    protected Element performReplace(MetadataUIP uip, String datastreamName) throws UIPException {
        return getNewModifiedElement(uip, datastreamName);
    }

    protected Element performReplace(MetadataUIP uip, String baseDatastream, String incomingDatastream)
            throws UIPException {
        return getNewModifiedElement(uip, baseDatastream, uip.getIncomingData().get(incomingDatastream));
    }

    protected Element performReplace(MetadataUIP uip, String datastreamName, Element incoming) throws UIPException {
        return getNewModifiedElement(uip, datastreamName, incoming);
    }
}
