package edu.unc.lib.boxc.fcrepo.utils;

import java.net.URI;

import edu.unc.lib.boxc.fcrepo.exceptions.TransactionCancelledException;

/**
 * This class is responsible for storing a transaction id to a thread-local variable
 * NB: care must be taken to close a transaction when it is no longer needed; otherwise the old
 * txId will persist on the thread and may interfere with future transactions
 *
 * @author harring
 *
 */
public class FedoraTransaction {

    protected static ThreadLocal<URI> txUriThread = new ThreadLocal<>(); // initial value == null
    protected static ThreadLocal<FedoraTransaction> rootTxThread = new ThreadLocal<>(); // keeps track of root tx
    // whether tx is already underway on the current thread
    private boolean isSub = true;
    private boolean isCancelled = false;
    private URI txUri;
    private TransactionManager txManager;

    public FedoraTransaction(URI txUri, TransactionManager txManager) {
        // if tx is root
        if (rootTxThread.get() == null) {
            rootTxThread.set(this);
            txUriThread.set(txUri);
            isSub = false;
        }
        this.txUri = txUri;
        this.txManager = txManager;
    }

    public static boolean hasTxId() {
        return txUriThread.get() != null;
    }

    public static boolean isStillAlive() {
        return rootTxThread.get() != null;
    }

    public static FedoraTransaction getActiveTx() {
        return rootTxThread.get();
    }

    public URI getTxUri() {
        return txUri;
    }

    public void close() {
        if (!isSub && !isCancelled) {
            txUriThread.remove();
            rootTxThread.remove();
            txManager.commitTransaction(txUri);
        }
        txUri = null;
    }

    public void cancelAndIgnore() {
        try {
            cancelTx();
        } catch (TransactionCancelledException e) {
            // ignore
        }
    }

    public void cancel(Throwable t) {
        cancelTx();
        if (t instanceof TransactionCancelledException) {
            throw (TransactionCancelledException) t;
        } else {
            throw new TransactionCancelledException("The transaction with id " + txUri + " was rolled back", t);
        }
    }

    public void cancel() {
        cancel(null);
    }

    private void cancelTx() {
        if (isSub) {
            // sub tx defers to root
            FedoraTransaction.rootTxThread.get().cancel();
        } else if (!isCancelled) {
            isCancelled = true;
            txUriThread.remove();
            rootTxThread.remove();
            txManager.cancelTransaction(txUri);
        }
    }

    public TransactionManager getTransactionManager() {
        return txManager;
    }
}
