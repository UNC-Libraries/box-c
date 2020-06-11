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
package edu.unc.lib.dl.fcrepo4;

import static org.slf4j.LoggerFactory.getLogger;

import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;

/**
 * Utility which keeps a transaction alive for as long as it is needed.
 *
 * @author bbpennel
 */
public class FedoraTransactionRefresher implements Runnable {
    private static final Logger log = getLogger(FedoraTransactionRefresher.class);

    // max transaction time, 1 day
    private static long maxTimeToLive = 24 * 60 * 60 * 1000;
    private static long refreshInterval = 60 * 1000;

    private Thread worker;

    private AtomicBoolean running = new AtomicBoolean(false);
    private AtomicBoolean stopped = new AtomicBoolean(false);

    private AtomicLong maxEndTime;

    private URI txUri;
    private TransactionManager txManager;

    /**
     * Construct a new refresher for the given transaction
     * @param tx
     */
    public FedoraTransactionRefresher(FedoraTransaction tx) {
        this.txUri = tx.getTxUri();
        this.txManager = tx.getTransactionManager();
    }

    /**
     * Start the refresher
     */
    public void start() {
        worker = new Thread(this);
        worker.start();
    }

    @Override
    public void run() {
        if (running.get() || stopped.get()) {
            log.warn("Refresher has already started for transaction {}", txUri);
            return;
        }

        log.debug("Starting transaction refresher for {}", txUri);
        running.set(true);
        maxEndTime = new AtomicLong(System.currentTimeMillis() + maxTimeToLive);

        while (running.get() && !stopped.get()) {
            // Check to see if the tx has been kept alive past the max time to live
            if (System.currentTimeMillis() >= maxEndTime.get()) {
                break;
            }
            // Wait the interval time
            try {
                Thread.sleep(refreshInterval);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.debug("Refresher interrupted for transaction {}", txUri);
            }
            // Refresh the transaction to keep it alive, unless the refresher is stopped
            if (!stopped.get()) {
                txManager.keepTransactionAlive(txUri);
            }
        }
        stopped.set(true);
        running.set(false);
        log.debug("Transaction refresher for {} finished after {}ms",
                txUri, (System.currentTimeMillis() - maxEndTime.get() + maxTimeToLive));
    }

    /**
     * Interrupt the refresher immediately
     */
    public void interrupt() {
        if (!running.get()) {
            log.warn("Cannot interrupt refresher for {}, it is not running", txUri);
            return;
        }
        stop();
        worker.interrupt();
    }

    /**
     * Stop the refresher at the next check
     */
    public void stop() {
        stopped.set(true);
    }

    public boolean isStopped() {
        return stopped.get();
    }

    public boolean isRunning() {
        return running.get();
    }

    public static void setMaxTimeToLive(long maxTimeToLive) {
        FedoraTransactionRefresher.maxTimeToLive = maxTimeToLive;
    }

    public static void setRefreshInterval(long refreshInterval) {
        FedoraTransactionRefresher.refreshInterval = refreshInterval;
    }

    public static long getMaxTimeToLive() {
        return maxTimeToLive;
    }

    public static long getRefreshInterval() {
        return refreshInterval;
    }
}
