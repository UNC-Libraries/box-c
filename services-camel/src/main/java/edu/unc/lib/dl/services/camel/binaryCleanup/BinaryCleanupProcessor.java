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
package edu.unc.lib.dl.services.camel.binaryCleanup;

import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;
import static org.slf4j.LoggerFactory.getLogger;

import java.net.URI;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;

import edu.unc.lib.dl.fcrepo4.BinaryObject;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.persist.api.storage.StorageLocation;
import edu.unc.lib.dl.persist.api.storage.StorageLocationManager;
import edu.unc.lib.dl.persist.api.transfer.BinaryTransferService;
import edu.unc.lib.dl.persist.api.transfer.BinaryTransferSession;

/**
 * Processor which cleans up old binaries leftover from transfers
 *
 * @author bbpennel
 */
public class BinaryCleanupProcessor implements Processor {
    private final Logger log = getLogger(BinaryCleanupProcessor.class);

    private StorageLocationManager storageLocationManager;
    private BinaryTransferService binaryTransferService;
    private RepositoryObjectLoader repositoryObjectLoaderNoCache;

    @Override
    public void process(Exchange exchange) throws Exception {
        String fcrepoUri = (String) exchange.getIn().getHeader(FCREPO_URI);
        PID dsPid = PIDs.get(fcrepoUri);
        BinaryObject binObj = repositoryObjectLoaderNoCache.getBinaryObject(dsPid);
        URI currentContentUri = binObj.getContentUri();
        String currentContentString = currentContentUri.toString();
        log.debug("Performing cleanup of out of date binaries for {}, retaining current: {}",
                fcrepoUri, currentContentString);
        StorageLocation storageLoc = storageLocationManager.getStorageLocation(binObj);
        // Delete all content files which are older than the current content file
        try (BinaryTransferSession session = binaryTransferService.getSession(storageLoc)) {
            List<URI> uris = storageLoc.getAllStorageUris(dsPid);
            if (!uris.contains(currentContentUri)) {
                log.error("Expected binary {} to have head content file {}, but file was not found",
                        dsPid, currentContentString);
                return;
            }
            uris.stream()
                .filter(storageUri -> storageUri.toString().compareTo(currentContentString) < 0)
                .forEach(storageUri -> {
                    log.debug("Cleaning up out of date binary URI {}", storageUri);
                    session.delete(storageUri);
                });
        }
    }

    public void setStorageLocationManager(StorageLocationManager storageLocationManager) {
        this.storageLocationManager = storageLocationManager;
    }

    public void setBinaryTransferService(BinaryTransferService binaryTransferService) {
        this.binaryTransferService = binaryTransferService;
    }

    public void setRepositoryObjectLoaderNoCache(RepositoryObjectLoader repositoryObjectLoader) {
        this.repositoryObjectLoaderNoCache = repositoryObjectLoader;
    }
}
