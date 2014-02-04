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

import java.util.Collection;
import java.util.concurrent.Callable;

import net.greghaines.jesque.Config;
import net.greghaines.jesque.worker.WorkerImpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 *
 * @author Alejandro Riveros Cruz <lariverosc@gmail.com>
 */
public class SpringWorkerFactory implements Callable<WorkerImpl>, ApplicationContextAware {

	private Logger logger = LoggerFactory.getLogger(SpringWorkerFactory.class);
	private final Config config;
	private final Collection<String> queues;
	private ApplicationContext applicationContext;
	private JobStatusFactory jobStatusFactory;

	public void setJobStatusFactory(JobStatusFactory jobStatusFactory) {
		this.jobStatusFactory = jobStatusFactory;
	}


	/**
	 * Creates a new factory for <code>SpringWorker</code> that use the provided arguments.
	 *
	 * @param config used to create a connection to Redis
	 * @param queues the list of queues to poll
	 */
	public SpringWorkerFactory(final Config config, final Collection<String> queues) {
		this.config = config;
		this.queues = queues;
	}


	/**
	 * Create a new <code>SpringWorker</code> using the arguments provided in the factory constructor.
	 */
	@Override
	public WorkerImpl call() {
		logger.info("Create new Spring Worker");
		WorkerImpl springWorker = new SpringWorker(this.config, this.queues);
		((SpringWorker) springWorker).setApplicationContext(this.applicationContext);
		((SpringWorker) springWorker).setJobStatusFactory(this.jobStatusFactory);
		return springWorker;
	}

	
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}
}
