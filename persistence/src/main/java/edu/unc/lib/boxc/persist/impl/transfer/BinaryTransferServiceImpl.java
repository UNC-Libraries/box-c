package edu.unc.lib.boxc.persist.impl.transfer;

import static org.slf4j.LoggerFactory.getLogger;

import java.net.URI;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;

import edu.unc.lib.boxc.fcrepo.utils.FedoraTransaction;
import edu.unc.lib.boxc.model.api.objects.RepositoryObject;
import edu.unc.lib.boxc.persist.api.sources.IngestSourceManager;
import edu.unc.lib.boxc.persist.api.storage.StorageLocation;
import edu.unc.lib.boxc.persist.api.storage.StorageLocationManager;
import edu.unc.lib.boxc.persist.api.transfer.BinaryTransferOutcome;
import edu.unc.lib.boxc.persist.api.transfer.BinaryTransferService;
import edu.unc.lib.boxc.persist.api.transfer.BinaryTransferSession;
import edu.unc.lib.boxc.persist.api.transfer.MultiDestinationTransferSession;

/**
 * Default implementation of a binary transfer service
 *
 * @author bbpennel
 *
 */
public class BinaryTransferServiceImpl implements BinaryTransferService {
    private static final Logger log = getLogger(BinaryTransferServiceImpl.class);

    private IngestSourceManager sourceManager;

    private StorageLocationManager storageLocationManager;

    private Map<String, Collection<TransferCacheEntry>> txTransferCache;

    public BinaryTransferServiceImpl() {
        txTransferCache = new ConcurrentHashMap<>();
    }

    @Override
    public MultiDestinationTransferSession getSession() {
        return new MultiDestinationTransferSessionImpl(sourceManager, storageLocationManager, this);
    }

    @Override
    public BinaryTransferSession getSession(StorageLocation destination) {
        return new BinaryTransferSessionImpl(sourceManager, destination, this);
    }

    @Override
    public BinaryTransferSession getSession(RepositoryObject repoObj) {
        StorageLocation loc = storageLocationManager.getStorageLocation(repoObj);
        return getSession(loc);
    }

    @Override
    public void rollbackTransaction(URI txUri) {
        String txId = txUri.toString();
        Collection<TransferCacheEntry> cache = txTransferCache.get(txId);
        if (cache == null) {
            return;
        }
        new Thread(() -> {
            try (MultiDestinationTransferSession mSession = getSession()) {
                for (TransferCacheEntry entry : cache) {
                    StorageLocation loc = storageLocationManager.getStorageLocationById(entry.newContentStorageId);
                    try (BinaryTransferSession session = mSession.forDestination(loc)) {
                        session.delete(entry.newContentUri);
                    } catch (Exception e) {
                        log.error("Rollback of transaction failed to cleanup new binary {}", entry.newContentUri, e);
                    }
                }
            } finally {
                txTransferCache.remove(txId);
            }
        }).start();
    }

    @Override
    public void commitTransaction(URI txUri) {
        txTransferCache.remove(txUri.toString());
    }

    @Override
    public BinaryTransferOutcome registerOutcome(BinaryTransferOutcome outcome) {
        FedoraTransaction tx = FedoraTransaction.getActiveTx();
        if (tx == null) {
            return outcome;
        }
        String txId = tx.getTxUri().toString();
        txTransferCache.computeIfAbsent(txId, k -> new ConcurrentLinkedQueue<TransferCacheEntry>())
                .add(new TransferCacheEntry(outcome));
        return outcome;
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

    private static class TransferCacheEntry {
        private URI newContentUri;
        private String newContentStorageId;

        private TransferCacheEntry(BinaryTransferOutcome outcome) {
            this.newContentUri = outcome.getDestinationUri();
            this.newContentStorageId = outcome.getDestinationId();
        }
    }
}
