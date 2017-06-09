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

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.util.ContentModelHelper.Datastream;
import edu.unc.lib.dl.util.VocabularyHelperManager;

/**
 * @author bbpennel
 * @date Aug 7, 2015
 */
public class MODSVocabularyUIPFilter extends MetadataUIPFilter {
    private static Logger log = Logger.getLogger(MODSVocabularyUIPFilter.class);

    private final String datastreamName = Datastream.MD_DESCRIPTIVE.getName();

    @Autowired
    private VocabularyHelperManager vocabManager;

    @Override
    public UpdateInformationPackage doFilter(UpdateInformationPackage uip) throws UIPException {
        // Only run this filter for metadata update requests
        if (uip == null || !(uip instanceof MetadataUIP)) {
            return uip;
        }
        // Do not apply filter unless the mods ds is being targeted.
        if (!uip.getIncomingData().containsKey(datastreamName) && !uip.getModifiedData().containsKey(datastreamName)) {
            return uip;
        }
        MetadataUIP metadataUIP = (MetadataUIP) uip;

        log.debug("Performing vocabulary update filter on " + uip.getPID().getPid());

        try {
            vocabManager.updateInvalidTermsRelations(uip.getPID(), metadataUIP.getIncomingData().get(datastreamName));
        } catch (FedoraException e) {
            throw new UIPException("Failed to update vocabulary terms for " + uip.getPID(), e);
        }

        return uip;
    }
}
