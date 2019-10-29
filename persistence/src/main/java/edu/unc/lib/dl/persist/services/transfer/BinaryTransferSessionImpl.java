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

import static edu.unc.lib.dl.persist.services.storage.StorageType.FILESYSTEM;
import static org.springframework.util.Assert.notNull;

import java.net.URI;
import java.nio.file.FileAlreadyExistsException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.NotImplementedException;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.persist.services.ingest.IngestSource;
import edu.unc.lib.dl.persist.services.ingest.IngestSourceManager;
import edu.unc.lib.dl.persist.services.storage.StorageLocation;

/**
 * Implementation of a session that can be used for single or multi destination binary transfers
 *
 * @author bbpennel
 *
 */
public class BinaryTransferSessionImpl implements BinaryTransferSession, MultiDestinationTransferSession {

    private IngestSourceManager sourceManager;
    private StorageLocation storageLocation;
    private Map<String, BinaryTransferClient> clientCache;

    /**
     * Constructor for session operating in multi destination mode
     */
    public BinaryTransferSessionImpl(IngestSourceManager sourceManager) {
        clientCache = new HashMap<>();
        this.sourceManager = sourceManager;
    }

    /**
     * Constructor for session operating in single destination mode
     *
     * @param storageLocation
     */
    public BinaryTransferSessionImpl(IngestSourceManager sourceManager, StorageLocation storageLocation) {
        this(sourceManager);
        notNull(storageLocation, "Must provide a storage location");
        this.storageLocation = storageLocation;
    }

    @Override
    public void close() {
        clientCache.values().forEach(BinaryTransferClient::shutdown);
    }

    @Override
    public URI transfer(PID binPid, URI sourceFileUri) throws FileAlreadyExistsException {
        return transfer(binPid, sourceFileUri, storageLocation);
    }

    @Override
    public URI transferReplaceExisting(PID binPid, URI sourceFileUri) {
        return transferReplaceExisting(binPid, sourceFileUri, storageLocation);
    }

    @Override
    public URI transferVersion(PID binPid, URI sourceFileUri) {
        return transferVersion(binPid, sourceFileUri, storageLocation);
    }

    @Override
    public URI transfer(PID binPid, URI sourceFileUri, StorageLocation destination) {
        notNull(destination, "Must provide a storage location");
        IngestSource source = sourceManager.getIngestSourceForUri(sourceFileUri);
        BinaryTransferClient client = getTransferClient(source, destination);
        return client.transfer(binPid, sourceFileUri);
    }

    @Override
    public URI transferReplaceExisting(PID binPid, URI sourceFileUri, StorageLocation destination) {
        notNull(destination, "Must provide a storage location");
        IngestSource source = sourceManager.getIngestSourceForUri(sourceFileUri);
        BinaryTransferClient client = getTransferClient(source, destination);
        return client.transferReplaceExisting(binPid, sourceFileUri);
    }

    @Override
    public URI transferVersion(PID binPid, URI sourceFileUri, StorageLocation destination) {
        notNull(destination, "Must provide a storage location");
        IngestSource source = sourceManager.getIngestSourceForUri(sourceFileUri);
        BinaryTransferClient client = getTransferClient(source, destination);
        return client.transferVersion(binPid, sourceFileUri);
    }

    private BinaryTransferClient getTransferClient(IngestSource source, StorageLocation dest) {
        String key = source.getId() + "|" + dest.getId();
        if (clientCache.containsKey(key)) {
            return clientCache.get(key);
        }

        BinaryTransferClient client = null;
        if (FILESYSTEM.equals(source.getStorageType()) && FILESYSTEM.equals(dest.getStorageType())) {
            client = new FSToFSTransferClient(source, dest);
        } else {
            throw new NotImplementedException("Transfer from " + source.getId() + " to " + dest.getId()
                + " is not currently supported.");
        }
        clientCache.put(key, client);

        return client;
    }
}
