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

import java.io.File;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.GroupsThreadStore;

/**
 * @author bbpennel
 * @date Jul 31, 2015
 */
public class BulkMetadataUpdateJob implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(BulkMetadataUpdateJob.class);

    private UIPProcessor uipProcessor;
    private final BulkMetadataUIP uip;

    public BulkMetadataUpdateJob(String updateId, String email, String username, Collection<String> groups,
            String importPath, String originalFilename) throws UIPException {
        AccessGroupSet groupSet = new AccessGroupSet();
        groupSet.addAll(groups);

        uip = new BulkMetadataUIP(updateId, email, username, groupSet, new File(importPath), originalFilename);
    }

    @Override
    public void run() {
        try {
            GroupsThreadStore.storeGroups(uip.getGroups());

            uipProcessor.process(uip);
        } catch (UpdateException | UIPException e) {
            log.error("Failed to update metadata for {}", uip.getUser(), e);
        } finally {
            GroupsThreadStore.clearStore();
        }
    }

    public UIPProcessor getUipProcessor() {
        return uipProcessor;
    }

    public void setUipProcessor(UIPProcessor uipProcessor) {
        this.uipProcessor = uipProcessor;
    }
}
