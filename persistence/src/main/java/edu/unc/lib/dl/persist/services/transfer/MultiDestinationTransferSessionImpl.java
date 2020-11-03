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

import static org.springframework.util.Assert.notNull;

import java.util.HashMap;
import java.util.Map;

import edu.unc.lib.dl.fcrepo4.RepositoryObject;
import edu.unc.lib.dl.persist.api.ingest.IngestSourceManager;
import edu.unc.lib.dl.persist.api.storage.StorageLocation;
import edu.unc.lib.dl.persist.api.storage.StorageLocationManager;
import edu.unc.lib.dl.persist.api.transfer.BinaryTransferSession;
import edu.unc.lib.dl.persist.api.transfer.MultiDestinationTransferSession;

/**
 * Implementation of a multiple destination transfer session
 *
 * @author bbpennel
 *
 */
public class MultiDestinationTransferSessionImpl implements MultiDestinationTransferSession {

    private StorageLocationManager storageLocationManager;
    private IngestSourceManager sourceManager;
    private Map<String, BinaryTransferSession> sessionMap;

    /**
     *
     * @param sourceManager
     * @param storageLocationManager
     */
    public MultiDestinationTransferSessionImpl(IngestSourceManager sourceManager,
            StorageLocationManager storageLocationManager) {
        sessionMap = new HashMap<>();
        this.sourceManager = sourceManager;
        this.storageLocationManager = storageLocationManager;
    }

    @Override
    public void close() {
        sessionMap.entrySet().forEach(e -> e.getValue().close());
    }

    @Override
    public BinaryTransferSession forDestination(StorageLocation dest) {
        notNull(dest, "Must provide a destination location");
        BinaryTransferSession session = sessionMap.get(dest.getId());
        if (session == null) {
            session = new BinaryTransferSessionImpl(sourceManager, dest);
            sessionMap.put(dest.getId(), session);
        }
        return session;
    }

    @Override
    public BinaryTransferSession forObject(RepositoryObject repoObj) {
        StorageLocation loc = storageLocationManager.getStorageLocation(repoObj);
        return forDestination(loc);
    }
}