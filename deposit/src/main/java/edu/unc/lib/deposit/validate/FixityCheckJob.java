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
package edu.unc.lib.deposit.validate;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.deposit.work.AbstractDepositJob;
import edu.unc.lib.deposit.work.JobInterruptedException;
import edu.unc.lib.dl.event.PremisEventBuilder;
import edu.unc.lib.dl.event.PremisLogger;
import edu.unc.lib.dl.exceptions.InvalidChecksumException;
import edu.unc.lib.dl.exceptions.RepositoryException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.model.AgentPids;
import edu.unc.lib.dl.persist.services.deposit.DepositModelHelpers;
import edu.unc.lib.dl.rdf.Premis;
import edu.unc.lib.dl.util.DigestAlgorithm;
import edu.unc.lib.dl.util.MultiDigestInputStreamWrapper;
import edu.unc.lib.dl.util.SoftwareAgentConstants.SoftwareAgent;

/**
 * Calculates digests for staged files, performing a fixity check if existing
 * digests were provided with the deposit.
 *
 * The job will perform fixity checks concurrently using a thread pool. The results of
 * the checks are periodically flushed the deposit model and premis logs. The number of
 * fixity check jobs queued at one time is limited in order to avoid blocking other
 * deposit jobs for long periods of time.
 *
 * @author bbpennel
 */
public class FixityCheckJob extends AbstractDepositJob {
    private static final Logger log = LoggerFactory.getLogger(FixityCheckJob.class);

    private static final Collection<DigestAlgorithm> REQUIRED_ALGS = Collections.singleton(
            DigestAlgorithm.DEFAULT_ALGORITHM);

    private ExecutorService executorService;

    private AtomicBoolean donePerformingChecks;
    private AtomicBoolean isInterrupted;
    private Object flushingLock = new Object();

    private int flushRate = 5000;
    // Should be higher than the number of workers
    private int maxQueuedJobs = 10;

    private Queue<Future<?>> fixityFutures;
    private BlockingQueue<FixityCheckResult> fixityResults;

    public FixityCheckJob(String uuid, String depositUUID) {
        super(uuid, depositUUID);
        this.rollbackDatasetOnFailure = false;
        donePerformingChecks = new AtomicBoolean(false);
        isInterrupted = new AtomicBoolean(false);
        fixityResults = new LinkedBlockingQueue<>();
        fixityFutures = new LinkedList<>();
    }

    @Override
    public void runJob() {
        log.debug("Performing FixityCheckJob for deposit {}", depositUUID);
        Model model = getReadOnlyModel();

        List<Entry<PID, String>> stagingList = getOriginalStagingPairList(model);
        setTotalClicks(stagingList.size());

        startResultRegistrar();

        try {
            for (Entry<PID, String> stagingEntry : stagingList) {
                PID rescPid = stagingEntry.getKey();
                // Skip already checked files
                if (isObjectCompleted(rescPid)) {
                    log.debug("Skipping over already completed fixity check for {}", rescPid.getId());
                    continue;
                }

                interruptJobIfStopped();

                // Wait for some of the jobs to finish before queuing more to avoid blocking all other deposits
                waitForQueueCapacity();

                log.debug("Queuing fixity check for {}", rescPid.getId());

                String stagedPath = stagingEntry.getValue();
                URI stagedUri = URI.create(stagedPath);

                Resource objResc = model.getResource(rescPid.getRepositoryPath());
                Resource origResc = DepositModelHelpers.getDatastream(objResc);

                Map<DigestAlgorithm, String> existingDigests = getDigestsForResource(origResc);

                Future<?> future = executorService.submit(
                        new FixityCheckRunnable(rescPid, stagedUri, origResc, existingDigests));
                fixityFutures.add(future);
            }

            // Wait for the remaining jobs
            while (!fixityFutures.isEmpty()) {
                fixityFutures.poll().get();
            }

            // Wait for results
            while (!fixityResults.isEmpty()) {
                TimeUnit.MILLISECONDS.sleep(10);
            }
        } catch (InterruptedException e) {
            isInterrupted.set(true);
            throw new JobInterruptedException("Fixity check job interrupted", e);
        } catch (ExecutionException e) {
            isInterrupted.set(true);
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            } else {
                throw new RuntimeException(e.getCause());
            }
        } finally {
            donePerformingChecks.set(true);
        }
        // Wait if a flush of registrations is still active
        synchronized (flushingLock) {
        }
        log.debug("Completed FixityCheckJob {} in deposit {}", jobUUID, depositUUID);
    }

    private void waitForQueueCapacity() throws InterruptedException, ExecutionException {
        while (fixityFutures.size() >= maxQueuedJobs) {
            Iterator<Future<?>> it = fixityFutures.iterator();
            while (it.hasNext()) {
                Future<?> fixityFuture = it.next();
                if (fixityFuture.isDone()) {
                    it.remove();
                    return;
                }
            }
            Thread.sleep(10l);
        }
    }

    private Map<DigestAlgorithm, String> getDigestsForResource(Resource resc) {
        Map<DigestAlgorithm, String> digests = new HashMap<>();
        for (DigestAlgorithm algorithm : DigestAlgorithm.values()) {
            if (resc.hasProperty(algorithm.getDepositProperty())) {
                digests.put(algorithm, resc.getProperty(algorithm.getDepositProperty()).getString());
            }
        }
        return digests;
    }

    private void receiveFixityResult(FixityCheckResult result) {
        fixityResults.add(result);
    }

    private void startResultRegistrar() {
        Thread flushThread = new Thread(() -> {
            try {
                while (!isInterrupted.get()) {
                    registerResults();
                    if (donePerformingChecks.get() && fixityResults.isEmpty()) {
                        return;
                    }
                    TimeUnit.MILLISECONDS.sleep(flushRate);
                }
            } catch (InterruptedException e) {
                throw new JobInterruptedException("Interrupted fixity check result registrar", e);
            }
        });
        // Allow exceptions from the registrar thread to make it to the main thread
        flushThread.setUncaughtExceptionHandler( new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread th, Throwable ex) {
                isInterrupted.set(true);
                if (ex instanceof RuntimeException) {
                    throw (RuntimeException) ex;
                } else {
                    new RepositoryException(ex);
                }
            }
        });
        flushThread.start();
    }

    private void registerResults() {
        if (fixityResults.isEmpty()) {
            return;
        }
        // Start a flush lock so that the job will not end until it finishes
        synchronized (flushingLock) {
            // Capture the current set of results, in case it grows during registration
            List<FixityCheckResult> results = new ArrayList<>();
            fixityResults.drainTo(results);
            log.debug("Registering batch of {} fixity check results", results.size());
            // Commit any newly generated digests to the deposit model and record results
            commit(() -> {
                results.forEach(result -> {
                    result.digests.forEach((alg, digest) -> {
                        if (result.origResc.hasProperty(alg.getDepositProperty())) {
                            log.debug("{} fixity check for {} passed with value {}",
                                    alg.getName(), result.origResc, digest);
                        } else {
                            log.debug("Storing {} digest for {} with value {}",
                                    alg.getName(), result.origResc, digest);
                            result.origResc.addLiteral(alg.getDepositProperty(), digest);
                        }
                        result.details.add(alg.getName().toUpperCase() + " checksum calculated: " + digest);
                    });
                });
            });
            // Record events and progress state
            results.forEach(result -> {
                // Store event for calculation of checksums
                PremisLogger premisDepositLogger = getPremisLogger(result.rescPid);
                PremisEventBuilder builder = premisDepositLogger.buildEvent(Premis.MessageDigestCalculation)
                        .addSoftwareAgent(AgentPids.forSoftware(SoftwareAgent.depositService));
                result.details.forEach(builder::addEventDetail);
                builder.write();

                markObjectCompleted(result.rescPid);
                log.debug("Completed fixity recording for {}", result.stagedUri);
                addClicks(1);
            });
        }
    }

    private class FixityCheckRunnable implements Runnable {
        private URI stagedUri;
        private Resource origResc;
        private PID rescPid;
        private Map<DigestAlgorithm, String> existingDigests;

        public FixityCheckRunnable(PID rescPid, URI stagedUri, Resource origResc,
                Map<DigestAlgorithm, String> existingDigests) {
            this.stagedUri = stagedUri;
            this.origResc = origResc;
            this.rescPid = rescPid;
            this.existingDigests = existingDigests;
        }

        @Override
        public void run() {
            if (isInterrupted.get()) {
                return;
            }

            try (InputStream fStream = Files.newInputStream(Paths.get(stagedUri))) {
                log.debug("Calculating digests for {}", stagedUri);
                MultiDigestInputStreamWrapper digestWrapper = new MultiDigestInputStreamWrapper(
                        fStream, existingDigests, REQUIRED_ALGS);
                digestWrapper.checkFixity();
                log.debug("Verified fixity of {}", stagedUri);

                receiveFixityResult(new FixityCheckResult(rescPid, stagedUri, origResc, digestWrapper.getDigests()));
            } catch (InvalidChecksumException e) {
                failJob(String.format("Fixity check failed for %s belonging to %s",
                        stagedUri, origResc.getURI()), e.getMessage());
            } catch (IOException e) {
                failJob(e, "Failed to read file {0} for fixity check", stagedUri);
            }
        }
    }

    /**
     * Result from a fixity check job
     * @author bbpennel
     */
    private class FixityCheckResult {
        private PID rescPid;
        private URI stagedUri;
        private Resource origResc;
        private Map<DigestAlgorithm, String> digests;
        private List<String> details;

        public FixityCheckResult(PID rescPid, URI stagedUri, Resource origResc, Map<DigestAlgorithm, String> digests) {
            this.rescPid = rescPid;
            this.stagedUri = stagedUri;
            this.origResc = origResc;
            this.digests = digests;
            details = new ArrayList<>();
        }
    }

    public void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }

    public void setFlushRate(int flushRate) {
        this.flushRate = flushRate;
    }

    public void setMaxQueuedJobs(int maxQueuedJobs) {
        this.maxQueuedJobs = maxQueuedJobs;
    }
}
