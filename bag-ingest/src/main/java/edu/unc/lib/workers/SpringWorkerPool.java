/*
 * Copyright 2012 Alejandro Riveros Cruz <lariverosc@gmail.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.unc.lib.workers;

import java.util.concurrent.Callable;
import java.util.concurrent.ThreadFactory;
import net.greghaines.jesque.worker.Worker;
import net.greghaines.jesque.worker.WorkerPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Alejandro Riveros Cruz <lariverosc@gmail.com>
 */
public class SpringWorkerPool extends WorkerPool {

	private Logger logger = LoggerFactory.getLogger(SpringWorkerPool.class);

	/**
	 * Constructor for SpringWorkerPool
	 * @param workerFactory
	 * @param numWorkers
	 */
	public SpringWorkerPool(Callable<? extends Worker> workerFactory, int numWorkers) {
		super(workerFactory, numWorkers);
	}

	/**
	 * Constructor for SpringWorkerPool
	 * @param workerFactory
	 * @param numWorkers
	 * @param threadFactory
	 */
	public SpringWorkerPool(Callable<? extends Worker> workerFactory, int numWorkers, ThreadFactory threadFactory) {
		super(workerFactory, numWorkers, threadFactory);
	}

	/**
	 * Convenient initialization method for the Spring container
	 */
	public void init() {
		logger.info("Start a new thread for SpringWorkerPool");
		new Thread(this).start();
	}

	/**
	 *Convenient destroy method for the Spring container
	 */
	public void destroy() {
		logger.info("End the SpringWorkerPool thread");
		end(true);
	}
}
