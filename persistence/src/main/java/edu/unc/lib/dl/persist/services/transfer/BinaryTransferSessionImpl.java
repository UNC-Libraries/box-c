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

import static edu.unc.lib.dl.persist.api.storage.StorageType.FILESYSTEM;
import static edu.unc.lib.dl.persist.api.storage.StorageType.POSIX_FS;
import static org.springframework.util.Assert.notNull;

import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.NotImplementedException;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.persist.api.ingest.IngestSource;
import edu.unc.lib.dl.persist.api.ingest.IngestSourceManager;
import edu.unc.lib.dl.persist.api.storage.StorageLocation;
import edu.unc.lib.dl.persist.api.transfer.BinaryTransferClient;
import edu.unc.lib.dl.persist.api.transfer.BinaryTransferSession;
import edu.unc.lib.dl.persist.api.transfer.StreamTransferClient;

/**
 * Implementation of a session that can be used for single destination binary transfers
 *
 * @author bbpennel
 *
 */
public class BinaryTransferSessionImpl implements BinaryTransferSession {

    private IngestSourceManager sourceManager;
    private StorageLocation storageLocation;
    private Map<String, BinaryTransferClient> clientCache;
    private StreamTransferClient streamClient;

    /**
     * Constructor for session for a single destination
     *
     * @param storageLocation
     */
    public BinaryTransferSessionImpl(IngestSourceManager sourceManager, StorageLocation storageLocation) {
        notNull(storageLocation, "Must provide a storage location");
        this.sourceManager = sourceManager;
        this.storageLocation = storageLocation;
    }

    @Override
    public void close() {
        if (clientCache != null) {
            clientCache.values().forEach(BinaryTransferClient::shutdown);
        }
        if (streamClient != null) {
            streamClient.shutdown();
        }
    }

    @Override
    public URI transfer(PID binPid, URI sourceFileUri) {
        IngestSource source = sourceManager.getIngestSourceForUri(sourceFileUri);
        BinaryTransferClient client = getTransferClient(source);
        return client.transfer(binPid, sourceFileUri);
    }

    @Override
    public URI transferReplaceExisting(PID binPid, URI sourceFileUri) {
        IngestSource source = sourceManager.getIngestSourceForUri(sourceFileUri);
        BinaryTransferClient client = getTransferClient(source);
        return client.transferReplaceExisting(binPid, sourceFileUri);
    }

    @Override
    public URI transferVersion(PID binPid, URI sourceFileUri) {
        IngestSource source = sourceManager.getIngestSourceForUri(sourceFileUri);
        BinaryTransferClient client = getTransferClient(source);
        return client.transferVersion(binPid, sourceFileUri);
    }

    private BinaryTransferClient getTransferClient(IngestSource source) {
        if (clientCache == null) {
            clientCache = new HashMap<>();
        }

        String key = source.getId();
        if (clientCache.containsKey(key)) {
            return clientCache.get(key);
        }

        BinaryTransferClient client = null;
        if (FILESYSTEM.equals(source.getStorageType()) && FILESYSTEM.equals(storageLocation.getStorageType())) {
            client = new FSToFSTransferClient(source, storageLocation);
        } else if (FILESYSTEM.equals(source.getStorageType()) && POSIX_FS.equals(storageLocation.getStorageType())) {
            client = new FSToPosixTransferClient(source, storageLocation);
        } else {
            throw new NotImplementedException("Transfer from " + source.getId() + " to " + storageLocation.getId()
                + " is not currently supported.");
        }
        clientCache.put(key, client);

        return client;
    }

    @Override
    public URI transfer(PID binPid, InputStream sourceStream) {
        return getStreamClient().transfer(binPid, sourceStream);
    }

    @Override
    public URI transferReplaceExisting(PID binPid, InputStream sourceStream) {
        return getStreamClient().transferReplaceExisting(binPid, sourceStream);
    }

    @Override
    public URI transferVersion(PID binPid, InputStream sourceStream) {
        return getStreamClient().transferVersion(binPid, sourceStream);
    }

    private StreamTransferClient getStreamClient() {
        if (streamClient != null) {
            return streamClient;
        }

        if (FILESYSTEM.equals(storageLocation.getStorageType())) {
            streamClient = new StreamToFSTransferClient(storageLocation);
        } else if (POSIX_FS.equals(storageLocation.getStorageType())) {
            streamClient = new StreamToPosixTransferClient(storageLocation);
        } else {
            throw new NotImplementedException("Write stream to " + storageLocation.getId()
                + " is not currently supported.");
        }

        return streamClient;
    }

    @Override
    public void delete(URI fileUri) {
        getStreamClient().delete(fileUri);
    }
}
