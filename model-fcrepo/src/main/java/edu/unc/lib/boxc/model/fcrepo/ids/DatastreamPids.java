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

import static edu.unc.lib.boxc.model.api.DatastreamType.ACCESS_SURROGATE;
import static edu.unc.lib.boxc.model.api.DatastreamType.MD_DESCRIPTIVE;
import static edu.unc.lib.boxc.model.api.DatastreamType.MD_EVENTS;
import static edu.unc.lib.boxc.model.api.DatastreamType.ORIGINAL_FILE;
import static edu.unc.lib.boxc.model.api.DatastreamType.TECHNICAL_METADATA;
import static edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants.DATA_FILE_FILESET;
import static edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants.DEPOSIT_MANIFEST_CONTAINER;

import edu.unc.lib.boxc.common.util.URIUtil;
import edu.unc.lib.boxc.model.api.DatastreamType;
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

    /**
     * Construct a PID for the provided datastream belonging the given PID.
     *
     * Does not include deposit manifests.
     *
     * @param pid
     * @param dsType
     * @return Constructed datastream PID.
     */
    public static PID getDatastreamPid(PID pid, DatastreamType dsType) {
        switch (dsType) {
        case MD_DESCRIPTIVE_HISTORY:
            return getDatastreamHistoryPid(getMdDescriptivePid(pid));
        case TECHNICAL_METADATA_HISTORY:
            return getDatastreamHistoryPid(getTechnicalMetadataPid(pid));
        default:
            return constructPid(pid, dsType);
        }
    }

    public static PID getMdDescriptivePid(PID pid) {
        return constructPid(pid, MD_DESCRIPTIVE);
    }

    public static PID getOriginalFilePid(PID pid) {
        return constructPid(pid, ORIGINAL_FILE);
    }

    public static PID getMdEventsPid(PID pid) {
        return constructPid(pid, MD_EVENTS);
    }

    public static PID getTechnicalMetadataPid(PID pid) {
        return constructPid(pid, TECHNICAL_METADATA);
    }

    /**
     * Construct a PID for a deposit manifest datastream using the provided name.
     *
     * @param pid
     * @param name
     * @return PID of manifest
     */
    public static PID getDepositManifestPid(PID pid, String name) {
        String path = URIUtil.join(pid.getRepositoryPath(), DEPOSIT_MANIFEST_CONTAINER, name.toLowerCase());
        return PIDs.get(path);
    }

    public static PID getAccessSurrogatePid(PID pid) {
        return constructPid(pid, ACCESS_SURROGATE);
    }

    private static PID constructPid(PID pid, DatastreamType dsType) {
        String container = DATA_FILE_FILESET;
        if (dsType.getContainer() != null) {
            container = dsType.getContainer();
        }
        String path = URIUtil.join(pid.getRepositoryPath(), container, dsType.getId());
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
