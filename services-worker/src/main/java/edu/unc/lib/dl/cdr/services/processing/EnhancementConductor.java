package edu.unc.lib.dl.cdr.services.processing;

import net.greghaines.jesque.Job;
import edu.unc.lib.dl.cdr.services.model.EnhancementMessage;
import edu.unc.lib.dl.message.ActionMessage;

public class EnhancementConductor implements MessageConductor {
	
	private net.greghaines.jesque.client.Client jesqueClient;
	private String queueName;
	
	public void setJesqueClient(net.greghaines.jesque.client.Client jesqueClient) {
		this.jesqueClient = jesqueClient;
	}
	
	public void setQueueName(String queueName) {
		this.queueName = queueName;
	}
	
	@Override
	public void add(ActionMessage actionMessage) {
		EnhancementMessage message = (EnhancementMessage) actionMessage;
		
		// TODO? proper JSON serialization of enhancement messages so we just pass the message.
		Job job = new Job(ApplyEnhancementServicesJob.class.getName(),  message.getPid().getPidAsString(), message.getNamespace(), message.getAction(), message.getServiceName(), message.getFilteredServices());
		jesqueClient.enqueue(queueName, job);
	}

	public static final String identifier = "JESQUE_ENHANCEMENT";

	/**
	 * Returns the identifier string for this conductor
	 * @return
	 */
	@Override
	public String getIdentifier() {
		return identifier;
	}
	
}
