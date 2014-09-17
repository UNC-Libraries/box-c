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
package edu.unc.lib.dl.util;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.fedora.PID;

/**
 * Lock which allows for locking on individual PIDs. Implemented as a stripe lock
 *
 * @author bbpennel
 * @date Sep 15, 2014
 */
public class PIDLocker {
	private static final Logger log = LoggerFactory.getLogger(PIDLocker.class);

	private final Lock[] pidLocks;

	public PIDLocker() {
		pidLocks = new Lock[128];
		for (int i = 0; i < pidLocks.length; i++) {
			pidLocks[i] = new ReentrantLock();
		}
	}

	/**
	 * Obtain a lock on a particular pid. If a lock can't be obtained the thread will wait.
	 *
	 * @param pid
	 *           pid of the object to lock
	 */
	public void lock(PID pid) {
		log.debug("Acquiring lock on {} for thread {}", pid.getPid(), Thread.currentThread().getName());
		getLock(pid).lock();
		log.debug("Acquired lock on {} for thread {}", pid.getPid(), Thread.currentThread().getName());
	}

	/**
	 * Release a lock for the specified pid
	 *
	 * @param pid
	 */
	public void unlock(PID pid) {
		log.debug("Releasing lock on {} from thread {}", pid.getPid(), Thread.currentThread().getName());
		getLock(pid).unlock();
		log.debug("Released lock on {} from thread {}", pid.getPid(), Thread.currentThread().getName());
	}

	private Lock getLock(PID pid) {
		return pidLocks[Math.abs(pid.getPid().hashCode()) % pidLocks.length];
	}
}
