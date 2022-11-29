package edu.unc.lib.boxc.model.fcrepo.ids;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.UUID;
import java.util.concurrent.locks.Lock;

import org.junit.Before;
import org.junit.Test;

import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.model.fcrepo.ids.PidLockManager;

/**
 * @author bbpennel
 */
public class PidLockManagerTest {

    private PID pid;

    private PidLockManager lockManager;

    @Before
    public void setup() {
        pid = PIDs.get(UUID.randomUUID().toString());
        lockManager = new PidLockManager();
    }

    @Test
    public void awaitReadLockThenWriteLock() throws Exception {
        Lock outerLock = lockManager.awaitReadLock(pid);

        Thread thread = new Thread(() -> {
            lockManager.awaitWriteLock(pid);
        });

        thread.start();

        Thread.sleep(25);

        assertTrue(thread.isAlive());

        outerLock.unlock();

        Thread.sleep(25);

        assertFalse(thread.isAlive());
    }

    @Test
    public void awaitWriteLockThenReadLock() throws Exception {
        Lock outerLock = lockManager.awaitWriteLock(pid);

        Thread thread = new Thread(() -> {
            lockManager.awaitReadLock(pid);
        });

        thread.start();

        Thread.sleep(25);

        assertTrue(thread.isAlive());

        outerLock.unlock();

        Thread.sleep(25);

        assertFalse(thread.isAlive());
    }

    @Test
    public void awaitWriteLockThenWriteLock() throws Exception {
        Lock outerLock = lockManager.awaitWriteLock(pid);

        Thread thread = new Thread(() -> {
            lockManager.awaitWriteLock(pid);
        });

        thread.start();

        Thread.sleep(25);

        assertTrue(thread.isAlive());

        outerLock.unlock();

        Thread.sleep(25);

        assertFalse(thread.isAlive());
    }

    @Test
    public void awaitReadLockThenReadLock() throws Exception {
        Lock outerLock = lockManager.awaitReadLock(pid);

        Thread thread = new Thread(() -> {
            lockManager.awaitReadLock(pid);
        });

        thread.start();

        Thread.sleep(25);

        assertFalse(thread.isAlive());

        outerLock.unlock();
    }
}
