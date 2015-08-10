package edu.unc.lib.dl.cdr.services.processing;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.cdr.services.ObjectEnhancementService;
import edu.unc.lib.dl.cdr.services.exception.EnhancementException;
import edu.unc.lib.dl.cdr.services.model.EnhancementMessage;

public class ApplyEnhancementServicesJob implements Runnable {

	private static final Logger LOG = LoggerFactory.getLogger(ApplyEnhancementServicesJob.class);

	private List<ObjectEnhancementService> services;
	private long recoverableDelay = 0;
	private EnhancementMessage message;
	
	public ApplyEnhancementServicesJob(String messagePid, String messageNamespace, String messageAction, String messageServiceName, List<String> filteredServices) {
		this.message = new EnhancementMessage(messagePid, messageNamespace, messageAction, messageServiceName);
		this.message.setFilteredServices(filteredServices);
		this.message.setCompletedServices(new ArrayList<String>());
	}
	
	public void setServices(List<ObjectEnhancementService> services) {
		this.services = services;
	}
	
	public void setRecoverableDelay(long recoverableDelay) {
		this.recoverableDelay = recoverableDelay;
	}
	
	@Override
	public void run() {
		for (ObjectEnhancementService service : services) {
			if (!message.getFilteredServices().contains(service.getClass().getName())) {
				continue;
			}
			
			try {
				if (!service.isApplicable(message)) {
					continue;
				}
			} catch (EnhancementException e) {
				LOG.error("Error determining applicability for service " + service.getClass().getName() + " and object " + message.getTargetID(), e);
			}
			
			try {
				applyService(service);
			} catch (EnhancementException e) {
				LOG.error("Error applying service " + service.getClass().getName() + " to object " + message.getTargetID(), e);
			}
		}
	}
	
	private void applyService(ObjectEnhancementService service) throws EnhancementException {
		while (true) {
			LOG.info("Applying service {} to object {}", service.getClass().getName(), message.getTargetID());

			try {
				service.getEnhancement(message).call();
				message.getCompletedServices().add(service.getClass().getName());
				
				break;
			} catch (EnhancementException e) {
				if (e.getSeverity() == EnhancementException.Severity.RECOVERABLE) {
					LOG.error("Retrying service for recoverable exception: " + service.getClass().getName(), e);
				} else {
					throw e;
				}
			}
			
			try {
				Thread.sleep(recoverableDelay);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
		}
	}
	
}
