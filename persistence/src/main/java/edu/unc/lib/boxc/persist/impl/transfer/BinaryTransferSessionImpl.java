package edu.unc.lib.boxc.persist.impl.transfer;

import static edu.unc.lib.boxc.persist.api.storage.StorageType.FILESYSTEM;
import static edu.unc.lib.boxc.persist.api.storage.StorageType.POSIX_FS;
import static org.springframework.util.Assert.notNull;

import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.NotImplementedException;

import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.persist.api.sources.IngestSource;
import edu.unc.lib.boxc.persist.api.sources.IngestSourceManager;
import edu.unc.lib.boxc.persist.api.storage.BinaryDetails;
import edu.unc.lib.boxc.persist.api.storage.StorageLocation;
import edu.unc.lib.boxc.persist.api.transfer.BinaryTransferClient;
import edu.unc.lib.boxc.persist.api.transfer.BinaryTransferOutcome;
import edu.unc.lib.boxc.persist.api.transfer.BinaryTransferService;
import edu.unc.lib.boxc.persist.api.transfer.BinaryTransferSession;
import edu.unc.lib.boxc.persist.api.transfer.StreamTransferClient;

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
    private BinaryTransferService binaryTransferService;

    /**
     * Constructor for session for a single destination
     *
     * @param storageLocation
     */
    public BinaryTransferSessionImpl(IngestSourceManager sourceManager, StorageLocation storageLocation,
            BinaryTransferService binaryTransferService) {
        notNull(storageLocation, "Must provide a storage location");
        this.sourceManager = sourceManager;
        this.storageLocation = storageLocation;
        this.binaryTransferService = binaryTransferService;
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
    public BinaryTransferOutcome transfer(PID binPid, URI sourceFileUri) {
        IngestSource source = sourceManager.getIngestSourceForUri(sourceFileUri);
        BinaryTransferClient client = getTransferClient(source);
        return binaryTransferService.registerOutcome(
                client.transfer(binPid, sourceFileUri));
    }

    @Override
    public BinaryTransferOutcome transferReplaceExisting(PID binPid, URI sourceFileUri) {
        IngestSource source = sourceManager.getIngestSourceForUri(sourceFileUri);
        BinaryTransferClient client = getTransferClient(source);
        return binaryTransferService.registerOutcome(
                client.transferReplaceExisting(binPid, sourceFileUri));
    }

    @Override
    public BinaryTransferOutcome transferVersion(PID binPid, URI sourceFileUri) {
        IngestSource source = sourceManager.getIngestSourceForUri(sourceFileUri);
        BinaryTransferClient client = getTransferClient(source);
        return binaryTransferService.registerOutcome(
                client.transferVersion(binPid, sourceFileUri));
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
    public BinaryTransferOutcome transfer(PID binPid, InputStream sourceStream) {
        return binaryTransferService.registerOutcome(
                getStreamClient().transfer(binPid, sourceStream));
    }

    @Override
    public BinaryTransferOutcome transferReplaceExisting(PID binPid, InputStream sourceStream) {
        return binaryTransferService.registerOutcome(
                getStreamClient().transferReplaceExisting(binPid, sourceStream));
    }

    @Override
    public BinaryTransferOutcome transferVersion(PID binPid, InputStream sourceStream) {
        return binaryTransferService.registerOutcome(
                getStreamClient().transferVersion(binPid, sourceStream));
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

    @Override
    public BinaryDetails getStoredBinaryDetails(PID binPid) {
        return getStreamClient().getStoredBinaryDetails(binPid);
    }

    @Override
    public boolean isTransferred(PID binPid, URI sourceUri) {
        IngestSource source = sourceManager.getIngestSourceForUri(sourceUri);
        BinaryTransferClient client = getTransferClient(source);
        return client.isTransferred(binPid, sourceUri);
    }
}
