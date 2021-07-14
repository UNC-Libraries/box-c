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
package edu.unc.lib.deposit.work;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import java.util.List;
import java.util.Map;

import com.google.common.base.Objects;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.MethodMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.ids.PIDMinter;
import edu.unc.lib.deposit.work.DepositSupervisor.ActionMonitoringTask;
import edu.unc.lib.dl.persist.api.ingest.DepositData;
import edu.unc.lib.dl.persist.services.ingest.AbstractDepositHandler;
import edu.unc.lib.dl.util.DepositException;
import edu.unc.lib.dl.util.DepositPipelineStatusFactory;
import edu.unc.lib.dl.util.DepositStatusFactory;
import edu.unc.lib.dl.util.JobStatusFactory;
import edu.unc.lib.dl.util.PackagingType;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositAction;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositField;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositPipelineAction;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositPipelineState;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositState;
import edu.unc.lib.dl.util.RedisWorkerConstants.Priority;
import edu.unc.lib.dl.util.SpringJobFactory;
import net.greghaines.jesque.Config;
import net.greghaines.jesque.Job;
import net.greghaines.jesque.meta.dao.QueueInfoDAO;
import net.greghaines.jesque.worker.WorkerPool;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.embedded.RedisServer;

/**
 * @author bbpennel
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration( locations = { "/spring-test/cdr-client-container.xml",
    "/spring-test/deposit-supervisor-test-context.xml"} )
public class DepositSupervisorTest {

    private static final Logger log = getLogger(DepositSupervisorTest.class);

    private final long STATE_POLL_PERIOD = 25l;

    @Rule
    public final TemporaryFolder tmpFolder = new TemporaryFolder();

    @Autowired
    private Config jesqueConfig;

    @Autowired
    private PIDMinter pidMinter;

    @Autowired
    private JobStatusFactory jobStatusFactory;

    @Autowired
    private DepositStatusFactory depositStatusFactory;

    @Autowired
    private DepositPipelineStatusFactory pipelineStatusFactory;

    @Autowired
    private List<WorkerPool> depositWorkerPools;

    @Autowired
    private QueueInfoDAO queueDAO;

    private DepositSupervisor supervisor;

    @Autowired
    private SpringJobFactory jobFactory;

    @Autowired
    private JedisPool jedisPool;

    private PID depositDestination;

    private ActionMonitoringTask actionMonitor;

    private AgentPrincipals agent;

    private static final RedisServer redisServer;

    static {
        try {
            redisServer = new RedisServer(46380);
            redisServer.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @AfterClass
    public static void afterClass() throws Exception {
        redisServer.stop();
    }

    @Before
    public void setup() throws Exception {
        when(jobFactory.materializeJob(any())).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Job job = invocation.getArgumentAt(0, Job.class);
                if (job == null) {
                    return null;
                }
                String uuid = (String) job.getArgs()[0];
                String depositUUID = (String) job.getArgs()[1];
                return new TestDepositJob(uuid, depositUUID);
            }
        });

        depositDestination = pidMinter.mintContentPid();
        agent = new AgentPrincipalsImpl("user", new AccessGroupSetImpl());

        pipelineStatusFactory.setPipelineState(DepositPipelineState.active);
        supervisor = new DepositSupervisor();
        supervisor.setJesqueConfig(jesqueConfig);
        supervisor.setDepositStatusFactory(depositStatusFactory);
        supervisor.setJobStatusFactory(jobStatusFactory);
        setField(supervisor, "pipelineStatusFactory", pipelineStatusFactory);
        setField(supervisor, "depositWorkerPools", depositWorkerPools);
        setField(supervisor, "queueDAO", queueDAO);
        supervisor.setUnavailableDelaySeconds(60);
        supervisor.setCleanupDelaySeconds(60);
        // Turn up monitoring speed so tests are shorter
        supervisor.setActionMonitorDelay(25l);

        actionMonitor = supervisor.actionMonitoringTask;
    }

    @After
    public void cleanup() throws Exception {
        supervisor.stop(true);
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.flushDB();
        }
    }

    @Test
    public void queueNewDepositRequested() throws Exception {
        PID depositPid = queueDeposit();

        assertDepositStatus(DepositState.unregistered, depositPid);
        assertDepositAction(DepositAction.register, depositPid);

        // Run once to process the submitted deposit
        actionMonitor.run();

        assertDepositStatus(DepositState.queued, depositPid);
        assertDepositAction(null, depositPid);
    }

    @Test
    public void queueNewMigrationDepositRequested() throws Exception {
        PID depositPid = queueDeposit(PackagingType.BXC3_TO_5_MIGRATION, Priority.low);

        assertDepositStatus(DepositState.unregistered, depositPid);
        assertDepositAction(DepositAction.register, depositPid);

        // Run once to process the submitted deposit
        actionMonitor.run();

        assertDepositStatus(DepositState.queued, depositPid);
        assertDepositAction(null, depositPid);
    }

    @Test
    public void noNewActionsRequested() throws Exception {
        PID depositPid = queueDeposit(true, DepositState.queued);

        actionMonitor.run();

        assertDepositStatus(DepositState.queued, depositPid);
        assertPipelineStatus(DepositPipelineState.active);
    }

    @Test
    public void pauseAndResumeDeposit() throws Exception {
        PID depositPid = queueDeposit(true, DepositState.queued);

        requestDepositAction(depositPid, DepositAction.pause);

        actionMonitor.run();

        assertDepositStatus(DepositState.paused, depositPid);

        requestDepositAction(depositPid, DepositAction.resume);

        actionMonitor.run();

        assertDepositStatus(DepositState.queued, depositPid);
    }

    @Test
    public void quietPipelineInInvalidState() throws Exception {
        pipelineStatusFactory.setPipelineState(DepositPipelineState.shutdown);

        pipelineStatusFactory.requestPipelineAction(DepositPipelineAction.quiet);

        actionMonitor.run();

        assertPipelineStatus(DepositPipelineState.shutdown);
        assertPipelineAction(null);
    }

    @Test
    public void unquietPipelineInInvalidState() throws Exception {
        pipelineStatusFactory.setPipelineState(DepositPipelineState.active);

        pipelineStatusFactory.requestPipelineAction(DepositPipelineAction.unquiet);

        actionMonitor.run();

        assertPipelineStatus(DepositPipelineState.active);
        assertPipelineAction(null);
    }

    @Test
    public void quietAndUnquietWithRunningDeposit() throws Exception {
        PID depositPid = queueDeposit(true, DepositState.running);
        actionMonitor.run();

        assertWorkersPaused(false);

        // Quiet the pipeline
        pipelineStatusFactory.requestPipelineAction(DepositPipelineAction.quiet);

        actionMonitor.run();

        assertPipelineStatus(DepositPipelineState.quieted);
        assertPipelineAction(null);
        assertWorkersPaused(true);

        assertDepositStatus(DepositState.quieted, depositPid);

        // Unquiet pipeline
        pipelineStatusFactory.requestPipelineAction(DepositPipelineAction.unquiet);

        actionMonitor.run();

        assertPipelineStatus(DepositPipelineState.active);
        assertPipelineAction(null);

        assertDepositStatus(DepositState.quieted, depositPid);
        assertDepositAction(DepositAction.resume, depositPid);

        assertWorkersPaused(false);

        // One more pass to resume the deposits
        actionMonitor.run();

        assertPipelineStatus(DepositPipelineState.active);

        assertDepositStatus(DepositState.queued, depositPid);
        assertDepositAction(null, depositPid);
    }

    @Test
    public void quietAndUnquietWithQueuedDeposit() throws Exception {
        PID depositPid = queueDeposit(true, DepositState.queued);

        // Quiet the pipeline
        pipelineStatusFactory.requestPipelineAction(DepositPipelineAction.quiet);

        actionMonitor.run();

        assertPipelineStatus(DepositPipelineState.quieted);
        assertWorkersPaused(true);

        // Queued state should be unaffected
        assertDepositStatus(DepositState.queued, depositPid);

        // Unquiet pipeline
        pipelineStatusFactory.requestPipelineAction(DepositPipelineAction.unquiet);

        actionMonitor.run();

        assertPipelineStatus(DepositPipelineState.active);
        assertWorkersPaused(false);

        assertDepositStatus(DepositState.queued, depositPid);
        assertDepositAction(null, depositPid);
    }

    @Test
    public void quietAndUnquietWithPauseAction() throws Exception {
        PID depositPid = queueDeposit(true, DepositState.running);

        // Quiet the pipeline
        pipelineStatusFactory.requestPipelineAction(DepositPipelineAction.quiet);

        actionMonitor.run();

        assertPipelineStatus(DepositPipelineState.quieted);
        assertWorkersPaused(true);

        assertDepositStatus(DepositState.quieted, depositPid);

        // Attempt to pause deposit, which should have no effect currently
        requestDepositAction(depositPid, DepositAction.pause);

        actionMonitor.run();

        assertDepositStatus(DepositState.quieted, depositPid);
        assertDepositAction(DepositAction.pause, depositPid);

        // Unquiet pipeline
        pipelineStatusFactory.requestPipelineAction(DepositPipelineAction.unquiet);

        actionMonitor.run();

        assertPipelineStatus(DepositPipelineState.active);
        assertPipelineAction(null);
        assertWorkersPaused(false);

        // Requested pause action should survive the unquieting
        assertDepositStatus(DepositState.quieted, depositPid);
        assertDepositAction(DepositAction.pause, depositPid);

        // One more pass to trigger the pause action
        actionMonitor.run();

        assertDepositStatus(DepositState.paused, depositPid);
        assertDepositAction(null, depositPid);
    }

    @Test
    public void stopPipelineInInvalidState() throws Exception {
        pipelineStatusFactory.setPipelineState(DepositPipelineState.shutdown);

        pipelineStatusFactory.requestPipelineAction(DepositPipelineAction.stop);

        actionMonitor.run();

        assertPipelineStatus(DepositPipelineState.shutdown);
        assertPipelineAction(null);
    }

    @DirtiesContext(methodMode = MethodMode.BEFORE_METHOD)
    @Test
    public void stopPipeline() throws Exception {
        assertWorkersShutdown(false);

        pipelineStatusFactory.requestPipelineAction(DepositPipelineAction.stop);

        actionMonitor.run();

        assertPipelineStatus(DepositPipelineState.stopped);
        assertPipelineAction(null);

        assertWorkersShutdown(true);

        // Queue a deposit and show that the action is not performed while stopped
        PID depositPid = queueDeposit();

        actionMonitor.run();

        assertDepositStatus(DepositState.unregistered, depositPid);
        assertDepositAction(DepositAction.register, depositPid);

        // Attempt to unquiet in order to resume, which should have no affect
        pipelineStatusFactory.requestPipelineAction(DepositPipelineAction.unquiet);

        actionMonitor.run();

        assertPipelineStatus(DepositPipelineState.stopped);
        assertPipelineAction(null);

        assertWorkersShutdown(true);
    }

    @DirtiesContext(methodMode = MethodMode.BEFORE_METHOD)
    @Test
    public void startSupervisorProcessAndPauseDeposit() throws Exception {
        supervisor.setActionMonitorDelay(25l);

        supervisor.start();

        PID depositPid = queueDeposit();

        assertDepositQueuedOrRunning(depositPid);
        assertDepositAction(null, depositPid);

        requestDepositAction(depositPid, DepositAction.pause);

        assertDepositAction(null, depositPid);
        assertDepositStatus(DepositState.paused, depositPid);
    }

    @DirtiesContext(methodMode = MethodMode.BEFORE_METHOD)
    @Test
    public void startSupervisorWithRunningDeposit() throws Exception {
        PID depositPid = queueDeposit(true, DepositState.running);

        supervisor.start();

        assertDepositActionOrNull(DepositAction.resume, depositPid);

        assertDepositQueuedOrRunning(depositPid);
        assertDepositAction(null, depositPid);
    }

    @DirtiesContext(methodMode = MethodMode.BEFORE_METHOD)
    @Test
    public void startSupervisorWithQuietedDeposit() throws Exception {
        PID depositPid = queueDeposit(true, DepositState.quieted);

        supervisor.start();

        assertDepositActionOrNull(DepositAction.resume, depositPid);

        assertDepositQueuedOrRunning(depositPid);
        assertDepositAction(null, depositPid);
    }

    @DirtiesContext(methodMode = MethodMode.BEFORE_METHOD)
    @Test
    public void startSupervisorWithPausedDeposit() throws Exception {
        PID depositPid = queueDeposit(true, DepositState.paused);

        supervisor.start();

        assertDepositAction(null, depositPid);

        assertDepositStatus(DepositState.paused, depositPid);
        assertDepositAction(null, depositPid);
    }

    @DirtiesContext(methodMode = MethodMode.BEFORE_METHOD)
    @Test
    public void startSupervisorWithNewQueuedDeposit() throws Exception {
        PID depositPid = queueDeposit(true, DepositState.queued);

        supervisor.start();

        assertDepositActionOrNull(DepositAction.register, depositPid);

        assertDepositStatus(DepositState.queued, depositPid);
        assertDepositAction(null, depositPid);
    }

    private void assertWorkersPaused(boolean expectedValue) {
        for (WorkerPool workerPool : depositWorkerPools) {
            assertEquals("Expected worker to be " + (expectedValue ? "" : "un") + "paused, but it was not",
                    expectedValue, workerPool.isPaused());
        }
    }

    private void assertWorkersShutdown(boolean expectedValue) {
        for (WorkerPool workerPool : depositWorkerPools) {
            assertEquals("Expected worker to " + (expectedValue ? "not " : "") + " be shutdown, but it was",
                    expectedValue, workerPool.isShutdown());
        }
    }

    private void assertDepositStatus(DepositState expectedState, PID depositPid) throws Exception {
        assertDepositStatus(expectedState, depositPid, 1000l);
    }

    private void assertDepositStatus(DepositState expectedState, PID depositPid, long timeout) throws Exception {
        long endTime = System.currentTimeMillis() + timeout;
        DepositState liveState = null;
        while (endTime >= System.currentTimeMillis()) {
            liveState = depositStatusFactory.getState(depositPid.getId());
            if (expectedState.equals(liveState)) {
                return;
            }
            Thread.sleep(STATE_POLL_PERIOD);
        }
        fail("Expected deposit status to be " + expectedState + " but state was " + liveState);
    }

    private void assertDepositQueuedOrRunning(PID depositPid) throws Exception {
        long timeout = 1000l;
        long endTime = System.currentTimeMillis() + timeout;
        DepositState liveState = null;
        while (endTime >= System.currentTimeMillis()) {
            liveState = depositStatusFactory.getState(depositPid.getId());
            if (DepositState.queued.equals(liveState) || DepositState.running.equals(liveState)) {
                return;
            }
            Thread.sleep(STATE_POLL_PERIOD);
        }
        fail("Expected deposit status to be queued or running but state was " + liveState);
    }

    private void assertDepositAction(DepositAction expectedAction, PID depositPid) throws Exception {
        assertDepositAction(expectedAction, depositPid, false, 1000l);
    }

    private void assertDepositActionOrNull(DepositAction expectedAction, PID depositPid) throws Exception {
        assertDepositAction(expectedAction, depositPid, true, 1000l);
    }

    private void assertDepositAction(DepositAction expectedAction, PID depositPid, boolean allowNull, long timeout)
            throws Exception {
        long endTime = System.currentTimeMillis() + timeout;

        DepositAction action = null;
        while (endTime >= System.currentTimeMillis()) {
            Map<String, String> status = depositStatusFactory.get(depositPid.getId());

            if (status.containsKey(DepositField.actionRequest.name())) {
                action = DepositAction.valueOf(status.get(DepositField.actionRequest.name()));
            } else {
                action = null;
            }
            if (Objects.equal(expectedAction, action) || (allowNull && action == null)) {
                return;
            }
            Thread.sleep(STATE_POLL_PERIOD);
        }
        fail("Expected deposit action to be " + expectedAction + " but action was " + action);
    }

    private void assertPipelineStatus(DepositPipelineState expectedState) {
        assertEquals(expectedState, pipelineStatusFactory.getPipelineState());
    }

    private void assertPipelineAction(DepositPipelineAction expectedAction) {
        assertEquals(expectedAction, pipelineStatusFactory.getPipelineAction());
    }

    private void requestDepositAction(PID depositPid, DepositAction action) {
        depositStatusFactory.requestAction(depositPid.getId(), action);
        log.debug("Set deposit action {} for {} to {}",
                action, depositPid.getId(), depositStatusFactory.get(depositPid.getId()));
    }

    private PID queueDeposit() throws DepositException {
        return queueDeposit(false, null);
    }

    private PID queueDeposit(boolean clearAction, DepositState finalState) throws DepositException {
        PID depositPid = queueDeposit(PackagingType.DIRECTORY, Priority.normal);
        if (clearAction) {
            depositStatusFactory.clearActionRequest(depositPid.getId());
        }
        if (finalState != null) {
            depositStatusFactory.setState(depositPid.getId(), finalState);
        }
        return depositPid;
    }

    private PID queueDeposit(PackagingType packagingType, Priority priority) throws DepositException {
        AbstractDepositHandler depositHandler = new AbstractDepositHandler() {
            @Override
            public PID doDeposit(PID destination, DepositData deposit) throws DepositException {
                PID depositPID = pidMinter.mintDepositRecordPid();
                registerDeposit(depositPID, destination, deposit, null);
                return depositPID;
            }

        };
        depositHandler.setDepositStatusFactory(depositStatusFactory);
        depositHandler.setPidMinter(pidMinter);
        depositHandler.setDepositsDirectory(tmpFolder.getRoot());

        DepositData deposit = new DepositData(null, null, packagingType, null, agent);
        deposit.setPriority(priority);
        return depositHandler.doDeposit(depositDestination, deposit);
    }

    private static final long JOB_TIMEOUT = 5000l;
    private class TestDepositJob extends AbstractDepositJob {

        public TestDepositJob(String uuid, String depositUUID) {
            super(uuid, depositUUID);
            setDepositStatusFactory(depositStatusFactory);
            setJobStatusFactory(jobStatusFactory);
        }

        @Override
        public void runJob() {
            long end = System.currentTimeMillis() + JOB_TIMEOUT;
            while (true) {
                if (end < System.currentTimeMillis()) {
                    return;
                }
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    return;
                }
                interruptJobIfStopped();
            }
        }
    }
}
