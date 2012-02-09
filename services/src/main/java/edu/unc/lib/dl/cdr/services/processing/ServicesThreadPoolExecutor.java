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
package edu.unc.lib.dl.cdr.services.processing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

/**
 * Thread pool executor which allows for pausing the pool as well as expanding or
 * decreasing the thread pool size on the fly.
 * @author bbpennel
 *
 */
public class ServicesThreadPoolExecutor<T extends Runnable> extends ThreadPoolExecutor {
	private boolean isPaused;
	private ReentrantLock pauseLock = new ReentrantLock();
	private Condition unpaused = pauseLock.newCondition();
	//Delay before a new thread will begin processing, in milliseconds
	private long beforeExecuteDelay = 0;
	private Set<T> runningNow = new HashSet<T>();

	public ServicesThreadPoolExecutor(int nThreads, String serviceName){
		super(nThreads, nThreads, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue <Runnable>());
		CustomizableThreadFactory ctf = new CustomizableThreadFactory();
		ctf.setThreadGroupName(serviceName);
		ctf.setThreadNamePrefix("ServicesWorker-");
		//ctf.setDaemon(true);
		this.setThreadFactory(new CustomizableThreadFactory());
	}

	@Override
	protected void afterExecute(Runnable r, Throwable t) {
		super.afterExecute(r, t);
		this.runningNow.remove(r);
		//Check if there are too many threads running, if so then don't allow this one to run.
		if (this.getActiveCount() > this.getCorePoolSize()){
			throw new RuntimeException();
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void beforeExecute(Thread t, Runnable r) {
		super.beforeExecute(t, r);
		this.runningNow.add((T)r);
		//Pause the adding of new threads until the pause flag is off.
		pauseLock.lock();
		try {
			if (beforeExecuteDelay > 0)
				Thread.sleep(beforeExecuteDelay);
			while (isPaused)
				unpaused.await();
		} catch (InterruptedException ie) {
			t.interrupt();
		} finally {
			pauseLock.unlock();
		}
	}

	public void pause() {
		pauseLock.lock();
		try {
			isPaused = true;
		} finally {
			pauseLock.unlock();
		}
	}

	public void resume() {
		pauseLock.lock();
		try {
			isPaused = false;
			unpaused.signalAll();
		} finally {
			pauseLock.unlock();
		}
	}

	public boolean isPaused(){
		return this.isPaused;
	}

	public long getBeforeExecuteDelay() {
		return beforeExecuteDelay;
	}

	public void setBeforeExecuteDelay(long beforeExecuteDelay) {
		this.beforeExecuteDelay = beforeExecuteDelay;
	}

	/**
	 * @return the set of active runnables
	 */
	public Set<T> getRunningNow() {
		return Collections.unmodifiableSet(runningNow);
	}

	/**
	 * Gets the set of runnables that are active or pending. No synchronization.
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<T> getAllRunningAndQueued() {
		List<T> all = new ArrayList<T>();
		// adding queue first to ensure coverage without synchronizing
		for(Runnable r : getQueue()) {
			all.add((T)r);
		}
		all.addAll(runningNow);
		return all;
	}

	/**
	 * Gets the set of runnables that are active or pending. No synchronization.
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<T> getQueued() {
		List<T> all = new ArrayList<T>();
		// adding queue first to ensure coverage without synchronizing
		for(Runnable r : getQueue()) {
			all.add((T)r);
		}
		return all;
	}
}
