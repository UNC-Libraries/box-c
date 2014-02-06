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

import static net.greghaines.jesque.utils.ResqueConstants.WORKER;
import static net.greghaines.jesque.worker.WorkerEvent.JOB_PROCESS;

import java.util.Collection;
import java.util.Collections;

import net.greghaines.jesque.Config;
import net.greghaines.jesque.Job;
import net.greghaines.jesque.worker.WorkerImpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * A worker class that finds and executes job beans, while updating status for known job types.
 * @author Alejandro Riveros Cruz <lariverosc@gmail.com>
 * @author Greg Jansen <count0@email.unc.edu>
 */
public class SpringWorker extends WorkerImpl implements ApplicationContextAware {

	private Logger log = LoggerFactory.getLogger(SpringWorker.class);
	private ApplicationContext applicationContext;
	
	private JobStatusFactory jobStatusFactory;

	public JobStatusFactory getJobStatusFactory() {
		return jobStatusFactory;
	}

	public void setJobStatusFactory(JobStatusFactory jobStatusFactory) {
		this.jobStatusFactory = jobStatusFactory;
	}

	/**
	 *
	 * @param config used to create a connection to Redis
	 * @param queues the list of queues to poll
	 */
	public SpringWorker(final Config config, final Collection<String> queues) {
		super(config, queues, Collections.EMPTY_MAP);
	}

	@Override
	protected void process(final Job job, final String curQueue) {
		log.info("Process new Job {} from queue {}", job.getClassName(), curQueue);
		Runnable runnableJob = null;
		try {
			if (applicationContext.containsBeanDefinition(job.getClassName())) {//Lookup by bean Id
				runnableJob = (Runnable) applicationContext.getBean(job.getClassName(), job.getArgs());
			} else {
				try {
					Class clazz = Class.forName(job.getClassName());//Lookup by Class type
					String[] beanNames = applicationContext.getBeanNamesForType(clazz, true, false);
					if (applicationContext.containsBeanDefinition(job.getClassName())) {
						runnableJob = (Runnable) applicationContext.getBean(beanNames[0], job.getArgs());
					} else {
						if (beanNames != null && beanNames.length == 1) {
							runnableJob = (Runnable) applicationContext.getBean(beanNames[0], job.getArgs());
						}
					}
				} catch (ClassNotFoundException cnfe) {
					log.error("Not bean Id or class definition found {}", job.getClassName());
					throw new Exception("Not bean Id or class definition found " + job.getClassName());
				}
			}
			if (runnableJob != null) {
				log.info("Prepared the Job {}", runnableJob);
				this.listenerDelegate.fireEvent(JOB_PROCESS, this, curQueue, job, null, null, null);
	            this.jedis.set(key(WORKER, this.getName()), statusMsg(curQueue, job));
				if (isThreadNameChangingEnabled()) {
					renameThread("Processing " + curQueue + " since " + System.currentTimeMillis());
				}
				if(runnableJob instanceof AbstractBagJob) getJobStatusFactory().started((AbstractBagJob)runnableJob);
				Object result = execute(job, curQueue, runnableJob);
	            success(job, runnableJob, result, curQueue);
				if(runnableJob instanceof AbstractBagJob) getJobStatusFactory().completed((AbstractBagJob)runnableJob);
			}
		} catch (Exception e) {
			failure(e, job, curQueue);
			if(runnableJob != null && runnableJob instanceof AbstractBagJob) {
				getJobStatusFactory().failed((AbstractBagJob)runnableJob, e.getMessage());
				log.error("Exception while running job", e);
			}
		} finally {
            this.jedis.del(key(WORKER, getName()));
        }
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}
	
	/**
	 * Convenient initialization method for the Spring container
	 */
	public void init(){
		log.info("Start a new thread for SpringWorker");
		new Thread(this).start();
	}
	
	/**
	 * Convenient destroy method for the Spring container
	 */
	public void destroy(){
		log.info("End the SpringWorker thread");
		end(true);
	}
}
