/**
 * Copyright © 2008 The University of North Carolina at Chapel Hill (cdr@unc.edu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.unc.lib.dl.cdr.services.techmd;

import org.jdom2.Element;

import edu.unc.lib.dl.cdr.services.AbstractDatastreamEnhancementService;
import edu.unc.lib.dl.cdr.services.Enhancement;
import edu.unc.lib.dl.cdr.services.model.EnhancementMessage;
import edu.unc.lib.dl.util.ContentModelHelper.Datastream;

/**
 * This service will enhance repository objects by extracting technical metadata from source data. It will store
 * technical metadata in a MD_TECHNICAL data stream. It will add a techData relationship between the object and this new
 * data stream.
 * 
 * @author Gregory Jansen
 * 
 */
public class TechnicalMetadataEnhancementService extends AbstractDatastreamEnhancementService {
    public static final String enhancementName = "Technical Metadata Extraction";

    public TechnicalMetadataEnhancementService() {
        super();
    }

    public void init() {
        mimetypePattern = null;
        derivativeDatastream = Datastream.MD_TECHNICAL.getName();
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.unc.lib.dl.cdr.services.ObjectEnhancementService#getEnhancementTask (edu.unc.lib.dl.fedora.PID)
     */
    @Override
    public Enhancement<Element> getEnhancement(EnhancementMessage message) {
        return new TechnicalMetadataEnhancement(this, message);
    }

    @Override
    public String getName() {
        return enhancementName;
    }
}
