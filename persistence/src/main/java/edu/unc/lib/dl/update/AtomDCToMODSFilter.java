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

import javax.xml.transform.TransformerException;

import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.util.AtomPubMetadataParserUtil;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.xml.ModsXmlHelper;

/**
 *
 * @author bbpennel
 *
 */
@Deprecated
public class AtomDCToMODSFilter extends MODSValidationUIPFilter {
    private static Logger log = LoggerFactory.getLogger(AtomDCToMODSFilter.class);
    private final String datastreamName = AtomPubMetadataParserUtil.ATOM_DC_DATASTREAM;

    public AtomDCToMODSFilter() {
        super();
    }

    @Override
    public UpdateInformationPackage doFilter(UpdateInformationPackage uip) throws UIPException {
        // Only run this filter for metadata update requests
        if (uip == null || !(uip instanceof MetadataUIP)) {
            return uip;
        }
        // Do not apply filter dcterms is populated AND there is no incoming mods
        if (!(uip.getIncomingData().containsKey(datastreamName) || uip.getModifiedData().containsKey(datastreamName))
                || uip.getIncomingData().get(ContentModelHelper.Datastream.MD_DESCRIPTIVE.getName()) != null) {
            return uip;
        }

        MetadataUIP metadataUIP = (MetadataUIP) uip;

        log.debug("Performing AtomDCToMODSFilter on " + uip.getPID().getPid());

        try {
            Element atomDCTerms = metadataUIP.getIncomingData().get(datastreamName);

            //Transform the DC to MODS
            Element mods = ModsXmlHelper.transformDCTerms2MODS(atomDCTerms).getRootElement();

            Element newModified = null;

            //Use the newly transformed mods as the incoming data, being sent to MD_DESCRIPTIVE
            switch (uip.getOperation()) {
            case REPLACE:
                newModified = performReplace(metadataUIP, ContentModelHelper.Datastream
                        .MD_DESCRIPTIVE.getName(), mods);
                break;
            case ADD:
                newModified = performAdd(metadataUIP, ContentModelHelper.Datastream
                        .MD_DESCRIPTIVE.getName(), mods);
                break;
            case UPDATE:
                // Doing add for update since the schema does not allow a way to indicate
                // a tag should replace another
                newModified = performAdd(metadataUIP, ContentModelHelper.Datastream
                        .MD_DESCRIPTIVE.getName(), mods);
                break;
            }

            if (newModified != null) {
                // Validate the new mods before storing
                validate(uip, newModified);
                metadataUIP.getModifiedData().put(ContentModelHelper.Datastream
                        .MD_DESCRIPTIVE.getName(), newModified);
            }
        } catch (TransformerException e) {
            throw new UIPException("Failed to transform DC Terms to MODS", e);
        }
        return uip;
    }

}
