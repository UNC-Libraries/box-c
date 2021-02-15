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

import static org.slf4j.LoggerFactory.getLogger;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.slf4j.Logger;

import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.persist.api.storage.StorageLocation;
import edu.unc.lib.dl.persist.api.storage.StorageLocationManager;
import edu.unc.lib.dl.persist.api.transfer.BinaryTransferService;
import edu.unc.lib.dl.persist.api.transfer.BinaryTransferSession;
import edu.unc.lib.dl.persist.api.transfer.MultiDestinationTransferSession;

/**
 * Processor which cleans up old binaries leftover from transfers
 *
 * @author bbpennel
 */
public class BinaryCleanupProcessor implements Processor {
    private final Logger log = getLogger(BinaryCleanupProcessor.class);

    private StorageLocationManager storageLocationManager;
    private BinaryTransferService binaryTransferService;

    @SuppressWarnings("unchecked")
    @Override
    public void process(Exchange exchange) throws Exception {
        Message aggrMsg = exchange.getIn();
        Map<String, String> messages = aggrMsg.getBody(Map.class);
        try (MultiDestinationTransferSession mSession = binaryTransferService.getSession()) {
            for (Entry<String, String> entry : messages.entrySet()) {
                try {
                    cleanupForBinary(entry.getKey(), entry.getValue(), mSession);
                } catch (Exception e) {
                    log.error("Failed to cleanup old binaries for {}: {}", entry.getKey(), e.getMessage());
                }
            }
        }
    }

    private void cleanupForBinary(String fcrepoUri, String contentUriString,
            MultiDestinationTransferSession mSession) {
        URI contentUri = URI.create(contentUriString);
        PID dsPid = PIDs.get(fcrepoUri);
        log.debug("Performing cleanup of out of date binaries for {}, retaining current: {}",
                fcrepoUri, contentUriString);
        StorageLocation storageLoc = storageLocationManager.getStorageLocationForUri(contentUri);

        try (BinaryTransferSession session = binaryTransferService.getSession(storageLoc)) {
            List<URI> uris = storageLoc.getAllStorageUris(dsPid);
            if (!uris.contains(contentUri)) {
                log.error("Expected binary {} to have head content file {}, but file was not found",
                        dsPid, contentUriString);
                return;
            }
            uris.stream()
                .filter(storageUri -> storageUri.toString().compareTo(contentUriString) < 0)
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
}
