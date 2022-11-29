package edu.unc.lib.boxc.fcrepo.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.net.URI;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import edu.unc.lib.boxc.fcrepo.utils.FedoraTransaction;
import edu.unc.lib.boxc.fcrepo.utils.FedoraTransactionRefresher;
import edu.unc.lib.boxc.fcrepo.utils.TransactionManager;

/**
 * @author bbpennel
 */
public class FedoraTransactionRefresherTest {

    @Mock
    private TransactionManager txManager;
    @Mock
    private FedoraTransaction tx;

    private URI txUri;

    private static long originalMaxTimeToLive = FedoraTransactionRefresher.getMaxTimeToLive();
    private static long originalRefreshInterval = FedoraTransactionRefresher.getRefreshInterval();

    @Before
    public void setup() {
        initMocks(this);
        txUri = URI.create("http://example.com/tx");

        when(tx.getTransactionManager()).thenReturn(txManager);
        when(tx.getTxUri()).thenReturn(txUri);
    }

    @After
    public void cleanup() {
        FedoraTransactionRefresher.setMaxTimeToLive(originalMaxTimeToLive);
        FedoraTransactionRefresher.setRefreshInterval(originalRefreshInterval);
    }

    @Test
    public void stopBeforeRefresh() throws Exception {
        FedoraTransactionRefresher.setRefreshInterval(75l);

        FedoraTransactionRefresher refresher = new FedoraTransactionRefresher(tx);

        refresher.start();
        Thread.sleep(25);
        refresher.stop();
        Thread.sleep(100l);

        assertTrue(refresher.isStopped());
        assertFalse(refresher.isRunning());
        verify(txManager, never()).keepTransactionAlive(any(URI.class));
    }

    @Test
    public void refreshMultipleTimes() throws Exception {
        FedoraTransactionRefresher.setRefreshInterval(25l);

        FedoraTransactionRefresher refresher = new FedoraTransactionRefresher(tx);

        refresher.start();
        Thread.sleep(100l);
        refresher.stop();
        Thread.sleep(50l);

        assertTrue(refresher.isStopped());
        assertFalse(refresher.isRunning());
        verify(txManager, atLeast(3)).keepTransactionAlive(any(URI.class));
    }

    @Test
    public void exceedMaxTimeToLive() throws Exception {
        FedoraTransactionRefresher.setRefreshInterval(25l);
        FedoraTransactionRefresher.setMaxTimeToLive(100l);

        FedoraTransactionRefresher refresher = new FedoraTransactionRefresher(tx);

        refresher.start();
        Thread.sleep(125l);

        assertTrue(refresher.isStopped());
        assertFalse(refresher.isRunning());
        verify(txManager, atLeast(3)).keepTransactionAlive(any(URI.class));
    }

    @Test
    public void interruptRunning() throws Exception {
        FedoraTransactionRefresher refresher = new FedoraTransactionRefresher(tx);

        refresher.start();
        Thread.sleep(25);
        refresher.interrupt();
        Thread.sleep(25);

        assertTrue(refresher.isStopped());
        assertFalse(refresher.isRunning());
    }

    @Test
    public void interruptNotRunning() throws Exception {
        FedoraTransactionRefresher refresher = new FedoraTransactionRefresher(tx);

        refresher.interrupt();

        assertFalse(refresher.isStopped());
        assertFalse(refresher.isRunning());
    }
}
