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
package edu.unc.lib.dl.persist.services.transfer;

import edu.unc.lib.dl.fcrepo4.RepositoryObject;
import edu.unc.lib.dl.persist.api.ingest.IngestSourceManager;
import edu.unc.lib.dl.persist.api.storage.StorageLocation;
import edu.unc.lib.dl.persist.api.storage.StorageLocationManager;
import edu.unc.lib.dl.persist.api.transfer.BinaryTransferService;
import edu.unc.lib.dl.persist.api.transfer.BinaryTransferSession;
import edu.unc.lib.dl.persist.api.transfer.MultiDestinationTransferSession;

/**
 * Default implementation of a binary transfer service
 *
 * @author bbpennel
 *
 */
public class BinaryTransferServiceImpl implements BinaryTransferService {

    private IngestSourceManager sourceManager;

    private StorageLocationManager storageLocationManager;

    @Override
    public MultiDestinationTransferSession getSession() {
        return new MultiDestinationTransferSessionImpl(sourceManager, storageLocationManager);
    }

    @Override
    public BinaryTransferSession getSession(StorageLocation destination) {
        return new BinaryTransferSessionImpl(sourceManager, destination);
    }

    @Override
    public BinaryTransferSession getSession(RepositoryObject repoObj) {
        StorageLocation loc = storageLocationManager.getStorageLocation(repoObj);
        return getSession(loc);
    }

    /**
     * @param sourceManager the sourceManager to set
     */
    public void setIngestSourceManager(IngestSourceManager sourceManager) {
        this.sourceManager = sourceManager;
    }

    public void setStorageLocationManager(StorageLocationManager storageLocationManager) {
        this.storageLocationManager = storageLocationManager;
    }

}
