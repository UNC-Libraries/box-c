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

import java.util.UUID;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.URIUtil;

/**
 * Minter of PIDs for content objects, deposit records, and PREMIS events.
 *
 * @author bbpennel
 * @author harring
 *
 */
public class RepositoryPIDMinter {

    /**
     * Mint a PID for a new deposit record object
     *
     * @return PID in the deposit record path
     */
    public PID mintDepositRecordPid() {
        String uuid = UUID.randomUUID().toString();
        String id = URIUtil.join(RepositoryPathConstants.DEPOSIT_RECORD_BASE, uuid);

        return PIDs.get(id);
    }

    /**
     * Mint a PID for a new content object
     *
     * @return PID in the content path
     */
    public PID mintContentPid() {
        String uuid = UUID.randomUUID().toString();
        String id = URIUtil.join(RepositoryPathConstants.CONTENT_BASE, uuid);

        return PIDs.get(id);
    }

    /**
     * Mints a URL for a new event object belonging to the provided parent object
     *
     * @param parentPid The object which this event will belong to.
     * @return
     */
    public PID mintPremisEventPid(PID parentPid) {
        String uuid = UUID.randomUUID().toString();
        String eventUrl = URIUtil.join(parentPid.getRepositoryPath(),
                RepositoryPathConstants.EVENTS_CONTAINER, uuid);
        return PIDs.get(eventUrl);
    }

}
