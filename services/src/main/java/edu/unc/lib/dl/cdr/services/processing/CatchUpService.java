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
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.cdr.services.ObjectEnhancementService;
import edu.unc.lib.dl.cdr.services.exception.EnhancementException;
import edu.unc.lib.dl.cdr.services.model.EnhancementMessage;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.JMSMessageUtil;

/**
 * Performs services on candidate items after the fact. Processing takes place in pages of results so that the service
 * can be started and stopped partially through the catchup process.
 * 
 * @author bbpennel
 * 
 */
public class CatchUpService {
	private static final Logger LOG = LoggerFactory.getLogger(CatchUpService.class);

	private MessageDirector messageDirector;
	private EnhancementConductor enhancementConductor;
	// List of services to perform catchup processing on
	private List<ObjectEnhancementService> services = new ArrayList<ObjectEnhancementService>();
	// If the catchup service is allowed to run
	private boolean isEnabled;
	// If catchup is actively running
	private boolean isActive;
	// Indicates if the catchup method is currently running.
	private boolean inCatchUp;
	// Number of items to get from each service per page.
	private int pageSize = 100;
	// Delay between checks to see if more candidates need to be retrieved.
	private long catchUpCheckDelay = 10000L;

	private long itemsProcessed = 0;
	private long itemsProcessedThisSession = 0;
	
	private int candidateQueryRetries = 2;

	public CatchUpService() {
		inCatchUp = false;
	}

	/**
	 * Turns on catchup services if they are enabled. Return true if catchup ran, false if it was unable to.
	 */
	public boolean activate() {
		return activate(null);
	}

	/**
	 * Turns on catchup services if they are enabled. If a date is provided, then services run in stale mode. Return true
	 * if catchup ran, false if it was unable to.
	 * 
	 * @param priorToDate
	 */
	public boolean activate(String priorToDate) {
		if (!isEnabled) {
			LOG.info("Catchup services are disabled.");
			return false;
		}
		if (priorToDate != null) {
			// Validate that the date is actually a date.
			Pattern p = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}([.]\\d{3})?Z$");
			if (!p.matcher(priorToDate).matches()) {
				throw new IllegalArgumentException("Catchup activation failed, " + priorToDate
						+ " is not a valid date.  Please use iso8601 format, ie 2011-05-23T16:13:07.126Z.");
			}
		}
		LOG.info("Activating CatchUp Service");
		isActive = true;
		boolean result = catchUp(priorToDate);
		isActive = false;
		return result;
	}

	/**
	 * Sets isActive to false, deactivating the service so that no more candidates will be sought.
	 */
	public void deactivate() {
		LOG.info("Deactivating CatchUp Service");
		isActive = false;
	}

	/**
	 * Queues batches of candidates from each service for processing. New batches of candidates are retrieved when there
	 * until the conductor becomes inactive or there are no more candidates retrieved while the processing queue is
	 * empty.
	 * 
	 * @param priorToDate
	 *           - if supplied, then catchup services are run in stale mode, where all suitable objects with modification
	 *           dates prior to this parameter are selected as candidates.
	 * @return Return true if catchup ran, false if it was blocked.
	 */
	private boolean catchUp(String priorToDate) {
		if (!isActive || !isEnabled || inCatchUp) {
			return false;
		}
		try {
			inCatchUp = true;
			itemsProcessedThisSession = 0;
			LOG.info("Catchup Services starting");

			Iterator<ObjectEnhancementService> serviceIt = services.iterator();
			// Step through the service stack adding candidates, depth first per service
			while (serviceIt.hasNext() && isActive) {
				ObjectEnhancementService service = serviceIt.next();
				try {
					this.addServiceCandidates(service, priorToDate);
				} catch (EnhancementException e) {
					LOG.error("An exception occured while performing catchup on service " + service.getClass().getName(), e);
				}
			}

			LOG.info("Catchup Services ending.  " + itemsProcessedThisSession + " items were queued/processed.");
		} finally {
			inCatchUp = false;
		}
		return true;
	}
	
	private void addServiceCandidates(ObjectEnhancementService service, String priorToDate) throws EnhancementException {
		if (!service.isActive())
			return;
		
		boolean staleCatchup = (priorToDate != null);
		
		Set<String> failedPIDSet = this.enhancementConductor.getFailedPids().getOrCreateServicePIDSet(service);
		
		int resultLimit;
		List<PID> candidates = null;
		boolean candidatesAdded;
		int candidatesRetries;

		do {
			// Wait for the enhancement queue to be empty
			while (isActive && !enhancementConductor.isEmpty()) {
				try {
					Thread.sleep(catchUpCheckDelay);
				} catch (InterruptedException e) {
					Thread.currentThread().isInterrupted();
				}
			}
			// Make sure that catchup didn't turn off while we were waiting
			if (!isActive)
				break;

			candidatesRetries = this.candidateQueryRetries;
			
			// Have to grow the number of results by the number of failed pids per service since they are
			// generally at the beginning of the result set. Order is not guaranteed and sorting the results is
			// much slower than getting a larger page size.
			resultLimit = pageSize + failedPIDSet.size();
			// Retrieve candidates for the current service, with retries.
			while (candidatesRetries-- > 0) {
				try {
					if (staleCatchup)
						candidates = service.findStaleCandidateObjects(resultLimit, priorToDate);
					else candidates = service.findCandidateObjects(resultLimit, 0);
					break;
				} catch (Exception e) {
					LOG.error("An error occurred while attempting to get candidates for service " + service.getClass().getName(), e);
				}
			}
			
			candidatesAdded = addBatch(candidates, service, failedPIDSet, staleCatchup);
			// Done this service if no more candidates were added or there are no more results
		} while (isActive && service.isActive() && resultLimit == candidates.size() && candidatesAdded);
	}

	/**
	 * Directs a batch of candidates to the enhancement conductor if they have no failed previously for this particular
	 * service.
	 * 
	 * @param candidates
	 * @param service
	 * @param failedPIDSet
	 * @return True if any of the candidates were added to the enhancement conductor
	 */
	private boolean addBatch(List<PID> candidates, ObjectEnhancementService service, Set<String> failedPIDSet,
			boolean staleCatchup) {
		if (candidates == null || candidates.size() == 0)
			return false;
		boolean candidatesAdded = false;
		String serviceName = service.getClass().getName();
		for (PID candidate : candidates) {
			if (LOG.isDebugEnabled())
				LOG.debug("Failed pid cache for " + serviceName + " contains pid " + candidate.getPid() + ": " + failedPIDSet.contains(candidate.getPid()));
			if (failedPIDSet.contains(candidate.getPid()))
				continue;

			if (staleCatchup) {
				EnhancementMessage message = new EnhancementMessage(candidate, JMSMessageUtil.servicesMessageNamespace,
						JMSMessageUtil.ServicesActions.APPLY_SERVICE.getName(), serviceName);
				messageDirector.direct(message);
			} else {
				EnhancementMessage message = new EnhancementMessage(candidate, JMSMessageUtil.servicesMessageNamespace,
						JMSMessageUtil.ServicesActions.APPLY_SERVICE_STACK.getName(), serviceName);
				messageDirector.direct(message);
			}

			this.itemsProcessed++;
			this.itemsProcessedThisSession++;

			if (!candidatesAdded)
				candidatesAdded = true;
		}

		return candidatesAdded;
	}

	public EnhancementConductor getenhancementConductor() {
		return enhancementConductor;
	}

	public void setenhancementConductor(EnhancementConductor enhancementConductor) {
		this.enhancementConductor = enhancementConductor;
	}

	public List<ObjectEnhancementService> getServices() {
		return services;
	}

	public void setServices(List<ObjectEnhancementService> services) {
		this.services = services;
	}

	public boolean isInCatchUp() {
		return inCatchUp;
	}

	public void setInCatchUp(boolean inCatchUp) {
		this.inCatchUp = inCatchUp;
	}

	public boolean isActive() {
		return isActive;
	}

	public void setActive(boolean isActive) {
		this.isActive = isActive;
	}

	public boolean isEnabled() {
		return isEnabled;
	}

	public void setEnabled(boolean isEnabled) {
		this.isEnabled = isEnabled;
		if (!this.isEnabled)
			this.isActive = false;
	}

	public int getPageSize() {
		return pageSize;
	}

	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}

	public long getCatchUpCheckDelay() {
		return catchUpCheckDelay;
	}

	public void setCatchUpCheckDelay(long catchUpCheckDelay) {
		this.catchUpCheckDelay = catchUpCheckDelay;
	}

	public long getItemsProcessed() {
		return itemsProcessed;
	}

	public long getItemsProcessedThisSession() {
		return itemsProcessedThisSession;
	}

	public MessageDirector getMessageDirector() {
		return messageDirector;
	}

	public void setMessageDirector(MessageDirector messageDirector) {
		this.messageDirector = messageDirector;
	}
}
