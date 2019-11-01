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
package edu.unc.lib.dl.model;

import static edu.unc.lib.dl.fcrepo4.RepositoryPathConstants.DATA_FILE_FILESET;
import static edu.unc.lib.dl.fcrepo4.RepositoryPathConstants.DEPOSIT_MANIFEST_CONTAINER;
import static edu.unc.lib.dl.fcrepo4.RepositoryPathConstants.METADATA_CONTAINER;
import static edu.unc.lib.dl.model.DatastreamType.MD_DESCRIPTIVE;
import static edu.unc.lib.dl.model.DatastreamType.MD_EVENTS;
import static edu.unc.lib.dl.model.DatastreamType.ORIGINAL_FILE;
import static edu.unc.lib.dl.model.DatastreamType.TECHNICAL_METADATA;

import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.URIUtil;

/**
 * Helper methods for calculating PIDs to datastreams
 *
 * @author bbpennel
 *
 */
public class DatastreamPids {

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
        String path = URIUtil.join(pid.getRepositoryPath(), DEPOSIT_MANIFEST_CONTAINER, name);
        return PIDs.get(path);
    }
}
