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

import java.io.File;
import java.util.Collection;

import net.greghaines.jesque.Job;
import net.greghaines.jesque.worker.Worker;
import net.greghaines.jesque.worker.WorkerEvent;
import net.greghaines.jesque.worker.WorkerListener;
import net.greghaines.jesque.worker.WorkerPool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author bbpennel
 * @date Jul 31, 2015
 */
public class BulkMetadataUpdateConductor implements WorkerListener {
	private static final Logger log = LoggerFactory.getLogger(BulkMetadataUpdateConductor.class);
	
	private net.greghaines.jesque.client.Client jesqueClient;
	private WorkerPool workerPool;
	private String queueName;

	public void add(String email, String username, Collection<String> groups, File importFile) {
		Job job = new Job(BulkMetadataUpdateJob.class.getName(), email, username, groups,
				importFile.getAbsolutePath());
		jesqueClient.enqueue(queueName, job);
	}
	
	@Override
	public void onEvent(WorkerEvent event, Worker worker, String queue, Job job, Object runner, Object result, Throwable t) {
		if (event == null || event == WorkerEvent.WORKER_POLL) {
			return;
		}
		
		log.debug("onEvent event={}, worker={}, queue={}, job={}, runner={}, result={}, t={}", new Object[] { event, worker, queue, job, runner, result, t });
	
		if (event == WorkerEvent.JOB_FAILURE) {
			log.error("Job failed: " + job, t);
		}
	}
	
	public void setJesqueClient(net.greghaines.jesque.client.Client jesqueClient) {
		this.jesqueClient = jesqueClient;
	}

	public void setWorkerPool(WorkerPool workerPool) {
		this.workerPool = workerPool;
	}
	
	public void setQueueName(String queueName) {
		this.queueName = queueName;
	}
	
	public void init() {
		workerPool.getWorkerEventEmitter().addListener(this);
		workerPool.run();
	}
	
	public void destroy() {
		workerPool.end(true);
	}
}
