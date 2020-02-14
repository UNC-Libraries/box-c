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
package edu.unc.lib.dcr.migration.content;

import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fedora.PID;

/**
 * Service which transforms a tree of content objects starting from a single root.
 *
 * @author bbpennel
 */
public class ContentTransformationService {

    private PID startingPid;
    private ContentObjectTransformerManager transformerManager;

    public ContentTransformationService(String startingId, boolean topLevelAsUnit) {
        this.startingPid = PIDs.get(startingId);
    }

    /**
     * Perform the transformation of the tree of content objects
     *
     * @return result code
     */
    public int perform() {
        transformerManager.createTransformer(startingPid, null)
                .fork();

        return transformerManager.awaitTransformers();
    }

    public void setTransformerManager(ContentObjectTransformerManager transformerManager) {
        this.transformerManager = transformerManager;
    }
}
