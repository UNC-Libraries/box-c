package edu.unc.lib.boxc.persist.impl.transfer;

import static org.springframework.util.Assert.notNull;

import java.util.HashMap;
import java.util.Map;

import edu.unc.lib.boxc.model.api.objects.RepositoryObject;
import edu.unc.lib.boxc.persist.api.sources.IngestSourceManager;
import edu.unc.lib.boxc.persist.api.storage.StorageLocation;
import edu.unc.lib.boxc.persist.api.storage.StorageLocationManager;
import edu.unc.lib.boxc.persist.api.transfer.BinaryTransferService;
import edu.unc.lib.boxc.persist.api.transfer.BinaryTransferSession;
import edu.unc.lib.boxc.persist.api.transfer.MultiDestinationTransferSession;

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
    private BinaryTransferService binaryTransferService;

    /**
     *
     * @param sourceManager
     * @param storageLocationManager
     */
    public MultiDestinationTransferSessionImpl(IngestSourceManager sourceManager,
            StorageLocationManager storageLocationManager, BinaryTransferService binaryTransferService) {
        sessionMap = new HashMap<>();
        this.sourceManager = sourceManager;
        this.storageLocationManager = storageLocationManager;
        this.binaryTransferService = binaryTransferService;
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
            session = new BinaryTransferSessionImpl(sourceManager, dest, binaryTransferService);
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
