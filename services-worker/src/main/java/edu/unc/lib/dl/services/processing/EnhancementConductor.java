package edu.unc.lib.dl.cdr.services.processing;

import net.greghaines.jesque.Job;
import net.greghaines.jesque.worker.Worker;
import net.greghaines.jesque.worker.WorkerEvent;
import net.greghaines.jesque.worker.WorkerListener;
import net.greghaines.jesque.worker.WorkerPool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.cdr.services.model.EnhancementMessage;
import edu.unc.lib.dl.message.ActionMessage;

public class EnhancementConductor implements MessageConductor, WorkerListener {
	
	private static final Logger LOG = LoggerFactory.getLogger(EnhancementConductor.class);
	
	private net.greghaines.jesque.client.Client jesqueClient;
	private String queueName;
	
	public void setJesqueClient(net.greghaines.jesque.client.Client jesqueClient) {
		this.jesqueClient = jesqueClient;
	}
	
	public void setQueueName(String queueName) {
		this.queueName = queueName;
	}
	
	public void add(ActionMessage actionMessage) {
		EnhancementMessage message = (EnhancementMessage) actionMessage;
		
		// TODO? proper JSON serialization of enhancement messages so we just pass the message.
		Job job = new Job(ApplyEnhancementServicesJob.class.getName(),  message.getPid().getPid(), message.getNamespace(), message.getAction(), message.getServiceName(), message.getFilteredServices());
		jesqueClient.enqueue(queueName, job);
	}
	
	public void onEvent(WorkerEvent event, Worker worker, String queue, Job job, Object runner, Object result, Throwable t) {
		if (event == null || event == WorkerEvent.WORKER_POLL) {
			return;
		}
		
		LOG.debug("onEvent event={}, worker={}, queue={}, job={}, runner={}, result={}, t={}", new Object[] { event, worker, queue, job, runner, result, t });
	
		if (event == WorkerEvent.JOB_FAILURE) {
			LOG.error("Job failed: " + job, t);
		}
	}

	public static final String identifier = "JESQUE_ENHANCEMENT";

	/**
	 * Returns the identifier string for this conductor
	 * @return
	 */
	public String getIdentifier() {
		return identifier;
	}
	
}
