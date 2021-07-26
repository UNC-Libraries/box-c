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
package edu.unc.lib.boxc.model.fcrepo.ids;

import com.google.common.util.concurrent.Striped;

import edu.unc.lib.boxc.model.api.exceptions.InterruptedLockException;
import edu.unc.lib.boxc.model.api.ids.PID;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * Manager for getting locks on PID objects
 *
 * @author bbpennel
 */
public class PidLockManager {
    private static final int DEFAULT_MAX_LOCKS = 1000;
    private static final PidLockManager DEFAULT_LOCK_MANAGER = new PidLockManager();

    private Striped<ReadWriteLock> locks;

    public PidLockManager() {
        this(DEFAULT_MAX_LOCKS);
    }

    public PidLockManager(int numLocks) {
        locks = Striped.lazyWeakReadWriteLock(numLocks);
    }

    public static PidLockManager getDefaultPidLockManager() {
        return DEFAULT_LOCK_MANAGER;
    }

    /**
     * Get a read write lock for the provided pid
     *
     * @param pid
     * @return
     */
    public ReadWriteLock lockPid(PID pid) {
        return locks.get(pid.getQualifiedId());
    }

    /**
     * Locks and returns the write lock to a pid once it is available
     *
     * @param pid
     */
    public Lock awaitWriteLock(PID pid) {
        try {
            Lock lock = lockPid(pid).writeLock();
            lock.lockInterruptibly();
            return lock;
        } catch (InterruptedException e) {
            throw new InterruptedLockException("Interrupted while waiting for lock on " + pid.getQualifiedId());
        }
    }

    /**
     * Locks and returns the read lock to a pid once it is available
     *
     * @param pid
     */
    public Lock awaitReadLock(PID pid) {
        try {
            Lock lock = lockPid(pid).readLock();
            lock.lockInterruptibly();
            return lock;
        } catch (InterruptedException e) {
            throw new InterruptedLockException("Interrupted while waiting for lock on " + pid.getQualifiedId());
        }
    }
}
