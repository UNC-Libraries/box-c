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
package edu.unc.lib.dl.fcrepo4;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Premis;

/**
 *
 * @author bbpennel
 *
 */
public class PremisEventObject extends RepositoryObject implements Comparable<PremisEventObject> {
    private static final Logger log = LoggerFactory.getLogger(PremisEventObject.class);

    public PremisEventObject(PID pid, RepositoryObjectDriver driver, RepositoryObjectFactory repoObjFactory) {
        super(pid, driver, repoObjFactory);
    }

    @Override
    public PremisEventObject validateType() throws FedoraException {
        return this;
    }

    /**
     * Default sort order for events is chronological by event date.
     */
    @Override
    public int compareTo(PremisEventObject o) {
        try {
            String d1 = getResource().getProperty(Premis.hasEventDateTime).getString();
            String d2 = o.getResource().getProperty(Premis.hasEventDateTime).getString();
            return d1.compareTo(d2);
        } catch (FedoraException e) {
            log.error("Failed to parse event date while ordering", e);
            return 0;
        }
    }

    @Override
    public RepositoryObject getParent() {
        return driver.getParentObject(this);
    }

    /**
     * Override to assume that the remote version will not change after creation
     * of the event. Supports offline creation of PREMIS event objects
     */
    @Override
    public boolean isUnmodified() {
        return true;
    }
}
