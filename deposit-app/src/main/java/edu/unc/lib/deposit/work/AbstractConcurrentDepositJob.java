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

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;

import edu.unc.lib.boxc.model.api.exceptions.RepositoryException;

/**
 * An abstract deposit job which performs work concurrently
 *
 * @author bbpennel
 */
public abstract class AbstractConcurrentDepositJob extends AbstractDepositJob {

    private static final Logger log = getLogger(AbstractConcurrentDepositJob.class);

    protected AtomicBoolean isInterrupted = new AtomicBoolean(false);
    protected AtomicBoolean doneWork = new AtomicBoolean(false);
    protected Object flushingLock = new Object();
    private Thread flushThread;

    protected ExecutorService executorService;
    protected Queue<Future<?>> futuresQueue = new LinkedBlockingQueue<>();
    protected BlockingQueue<Object> resultsQueue = new LinkedBlockingQueue<>();

    private long MAX_REGISTRAR_SHUTDOWN_MS = 1000 * 10;
    private int flushRate = 5000;
    // Should be higher than the number of workers
    private int maxQueuedJobs = 10;

    public AbstractConcurrentDepositJob() {
        super();
    }

    public AbstractConcurrentDepositJob(String uuid, String depositUUID) {
        super(uuid, depositUUID);
    }

    /**
     * Wait for all queued or executing tasks to complete and for all of the results
     * from these tasks to be registered
     */
    protected void waitForCompletion() {
        try {
            // Wait for the remaining jobs
            while (!futuresQueue.isEmpty()) {
                futuresQueue.poll().get();
            }

            // Wait for results
            while (!resultsQueue.isEmpty()) {
                TimeUnit.MILLISECONDS.sleep(10l);
            }

            // Wait if a flush of registrations is still active
            synchronized (flushingLock) {
            }

            doneWork.set(true);
        } catch (InterruptedException e) {
            isInterrupted.set(true);
            throw new JobInterruptedException("Interrupted while waiting for completion", e);
        } catch (ExecutionException e) {
            isInterrupted.set(true);
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            } else {
                throw new RuntimeException(e.getCause());
            }
        }
    }

    @Override
    protected void interruptJobIfStopped() throws JobInterruptedException {
        try {
            super.interruptJobIfStopped();
        } catch (JobInterruptedException e) {
            isInterrupted.set(true);
            throw e;
        }
    }

    /**
     * Submit a task to the thread pool for execution
     * @param task
     */
    protected void submitTask(Runnable task) {
        Future<?> future = executorService.submit(task);
        futuresQueue.add(future);
    }

    /**
     * Wait for the queue of task futures to drop below the max number of allowed queued jobs
     */
    protected void waitForQueueCapacity() {
        try {
            while (futuresQueue.size() >= maxQueuedJobs) {
                Iterator<Future<?>> it = futuresQueue.iterator();
                while (it.hasNext()) {
                    Future<?> future = it.next();
                    if (future.isDone()) {
                        future.get();
                        it.remove();
                        return;
                    }
                }
                Thread.sleep(10l);
            }
        } catch (InterruptedException e) {
            isInterrupted.set(true);
            throw new JobInterruptedException("Interrupted while waiting for queue capacity");
        } catch (ExecutionException e) {
            isInterrupted.set(true);
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            } else {
                throw new RuntimeException(e.getCause());
            }
        }
    }

    /**
     * Registers results from tasks executed by this job every flushRate milliseconds
     *
     * @throws InterruptedException
     */
    protected void startResultRegistrar() {
        flushThread = new Thread(() -> {
            try {
                while (true) {
                    registerResults();
                    if ((doneWork.get() || isInterrupted.get()) && resultsQueue.isEmpty()) {
                        log.debug("Shutting down registrar for {}", jobUUID);
                        return;
                    }
                    TimeUnit.MILLISECONDS.sleep(flushRate);
                }
            } catch (InterruptedException e) {
                throw new JobInterruptedException("Interrupted result registrar", e);
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

    protected void awaitRegistrarShutdown() {
        try {
            flushThread.join(MAX_REGISTRAR_SHUTDOWN_MS);
        } catch (InterruptedException e) {
            isInterrupted.set(true);
            throw new JobInterruptedException("Interrupted while waiting for registrar", e);
        }
    }

    private void registerResults() {
        if (resultsQueue.isEmpty()) {
            return;
        }
        // Start a flush lock so that the job will not end until it finishes
        synchronized (flushingLock) {
            registrationAction();
        }
    }

    /**
     * The action that takes place when registering a batch of results from completed tasks
     */
    protected abstract void registrationAction();

    /**
     * Receive the result object from an executed task and add it to the queue for processing
     * @param result
     */
    protected void receiveResult(Object result) {
        resultsQueue.add(result);
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
