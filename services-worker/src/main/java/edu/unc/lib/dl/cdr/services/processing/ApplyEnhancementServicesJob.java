package edu.unc.lib.dl.cdr.services.processing;

import static edu.unc.lib.dl.util.JMSMessageUtil.servicesMessageNamespace;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ws.client.WebServiceTransportException;

import edu.unc.lib.dl.cdr.services.ObjectEnhancementService;
import edu.unc.lib.dl.cdr.services.exception.EnhancementException;
import edu.unc.lib.dl.cdr.services.model.EnhancementMessage;
import edu.unc.lib.dl.fedora.ManagementClient;
import edu.unc.lib.dl.reporting.ActivityMetricsClient;
import edu.unc.lib.dl.util.JMSMessageUtil.ServicesActions;

public class ApplyEnhancementServicesJob implements Runnable {

	private static final Logger LOG = LoggerFactory.getLogger(ApplyEnhancementServicesJob.class);

	private List<ObjectEnhancementService> services;
	private long recoverableDelay = 0;
	private final EnhancementMessage message;
	private ActivityMetricsClient metricsClient;
	private ManagementClient managementClient;

	public ApplyEnhancementServicesJob(String pidString, boolean force) {
		this.message = new EnhancementMessage(pidString, servicesMessageNamespace,
				ServicesActions.APPLY_SERVICE_STACK.getName());
		this.message.setCompletedServices(new ArrayList<String>());
		this.message.setForce(force);
	}

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

	public void setMetricsClient(ActivityMetricsClient operationMetricsClient) {
		this.metricsClient = operationMetricsClient;
	}

	@Override
	public void run() {
		long backoffDelay = 10000;
		int backoffAttempts = 1;
		int maxBackoffAttempts = 5;

		for (ObjectEnhancementService service : services) {
			if (message.getFilteredServices() != null
					&& !message.getFilteredServices().contains(service.getClass().getName())) {
				continue;
			}

			try {
				if (!service.isApplicable(message)) {
					continue;
				}
			} catch (EnhancementException e) {
				LOG.error("Error determining applicability for service " + service.getClass().getName() + " and object " + message.getTargetID(), e);
			}

			while (backoffAttempts <= maxBackoffAttempts) {
				try {
					if (managementClient.isRepositoryAvailable()) {
						processEnhancement(service);
						break;
					} else {
						throw new WebServiceTransportException("Unable to connect to Fedora");
					}
				} catch (WebServiceTransportException e) {
					LOG.warn("Unable to connect to fedora. Unable to run job for " + service.getClass().getName())
						+ ". Retry attempt" + backoffAttempt);
					
					try {
						Thread.sleep(backoffDelay * backoffAttempts);
					} catch (InterruptedException e1) {
						LOG.warn("Back off time was interrupted for job " + service.getClass().getName());
					}

					if (backoffAttempts == maxBackoffAttempts) {
						LOG.warn("Unable to connect to fedora. Unable to run job for " + service.getClass().getName());
					}

					backoffAttempts++;
				}
			}
		}
	}

	private void processEnhancement(ObjectEnhancementService service) {
		try {
			applyService(service);
			metricsClient.incrFinishedEnhancement(service.getClass().getName());
		} catch (EnhancementException e) {
			LOG.error("Error applying service " + service.getClass().getName() + " to object " + message.getTargetID(), e);
			metricsClient.incrFailedEnhancement(service.getClass().getName());
		} catch (Throwable t) {
			metricsClient.incrFailedEnhancement(service.getClass().getName());
			throw t;
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
