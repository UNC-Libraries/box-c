package edu.unc.lib.boxc.deposit.pipeline;

import java.util.HashSet;
import java.util.Set;

/**
 * Service for tracking active deposits and limiting the number of concurrent deposits.
 *
 * @author bbpennel
 */
public class ActiveDepositsService {
    private int maxConcurrentDeposits = 3;
    private final Set<String> activeDepositIds = new HashSet<>();

    public synchronized void markInactive(String depositId) {
        activeDepositIds.remove(depositId);
    }

    public synchronized boolean markActive(String depositId) {
        if (activeDepositIds.contains(depositId) || activeDepositIds.size() >= maxConcurrentDeposits) {
            return false;
        }
        activeDepositIds.add(depositId);
        return true;
    }

    public synchronized boolean acceptingNewDeposits() {
        return activeDepositIds.size() < maxConcurrentDeposits;
    }

    public boolean isDepositActive(String depositId) {
        return activeDepositIds.contains(depositId);
    }

    public void setMaxConcurrentDeposits(int maxConcurrentDeposits) {
        this.maxConcurrentDeposits = maxConcurrentDeposits;
    }
}
