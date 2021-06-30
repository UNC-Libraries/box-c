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
package edu.unc.lib.boxc.model.fcrepo.ids;

import static edu.unc.lib.boxc.model.api.objects.DatastreamType.MD_DESCRIPTIVE;
import static edu.unc.lib.boxc.model.api.objects.DatastreamType.MD_EVENTS;
import static edu.unc.lib.boxc.model.api.objects.DatastreamType.ORIGINAL_FILE;
import static edu.unc.lib.boxc.model.api.objects.DatastreamType.TECHNICAL_METADATA;
import static edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPathConstants.DATA_FILE_FILESET;
import static edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPathConstants.DEPOSIT_MANIFEST_CONTAINER;
import static edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPathConstants.METADATA_CONTAINER;

import edu.unc.lib.boxc.common.util.URIUtil;
import edu.unc.lib.boxc.model.api.ids.PID;

/**
 * Helper methods for calculating PIDs to datastreams
 *
 * @author bbpennel
 *
 */
public class DatastreamPids {

    public static final String HISTORY_SUFFIX = "_history";

    private DatastreamPids() {
    }

    public static PID getMdDescriptivePid(PID pid) {
        String path = URIUtil.join(pid.getRepositoryPath(), METADATA_CONTAINER, MD_DESCRIPTIVE.getId());
        return PIDs.get(path);
    }

    public static PID getOriginalFilePid(PID pid) {
        String path = URIUtil.join(pid.getRepositoryPath(), DATA_FILE_FILESET, ORIGINAL_FILE.getId());
        return PIDs.get(path);
    }

    public static PID getMdEventsPid(PID pid) {
        String path = URIUtil.join(pid.getRepositoryPath(), METADATA_CONTAINER, MD_EVENTS.getId());
        return PIDs.get(path);
    }

    public static PID getTechnicalMetadataPid(PID pid) {
        String path = URIUtil.join(pid.getRepositoryPath(), DATA_FILE_FILESET, TECHNICAL_METADATA.getId());
        return PIDs.get(path);
    }

    public static PID getDepositManifestPid(PID pid, String name) {
        String path = URIUtil.join(pid.getRepositoryPath(), DEPOSIT_MANIFEST_CONTAINER, name.toLowerCase());
        return PIDs.get(path);
    }

    /**
     * Get the PID for the history object for the given datastream.
     *
     * @param datastreamPid pid of the datastream binary
     * @return history object pid
     */
    public static PID getDatastreamHistoryPid(PID datastreamPid) {
        String path = datastreamPid.getRepositoryPath() + HISTORY_SUFFIX;
        return PIDs.get(path);
    }
}
