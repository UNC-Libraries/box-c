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
import java.util.List;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.cdr.services.ObjectEnhancementService;
import edu.unc.lib.dl.cdr.services.model.PIDMessage;
import edu.unc.lib.dl.cdr.services.util.JMSMessageUtil;
import edu.unc.lib.dl.fedora.PID;

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

	public CatchUpService(){
		inCatchUp = false;
	}
	
	/**
	 * Turns on catchup services if they are enabled.  Return true if catchup ran, false if it was unable to.
	 */
	public boolean activate(){
		return activate(null);
	}
	
	/**
	 * Turns on catchup services if they are enabled.  If a date is provided, then services run in stale mode.
	 * Return true if catchup ran, false if it was unable to.
	 * @param priorToDate
	 */
	public boolean activate(String priorToDate){
		if (!isEnabled){
			LOG.info("Catchup services are disabled.");
			return false;
		}
		if (priorToDate != null){
			// Validate that the date is actually a date.
			Pattern p = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}([.]\\d{3})?Z$");
			if (!p.matcher(priorToDate).matches()){
				throw new IllegalArgumentException("Catchup activation failed, " + priorToDate 
						+ " is not a valid date.  Please use iso8601 format, ie 2011-05-23T16:13:07.126Z.");
			}
		}
		LOG.info("Activating CatchUp Service");
		isActive = true;
		return catchUp(priorToDate);
	}
	
	/**
	 * Sets isActive to false, deactivating the service so that no more candidates will be sought.
	 */
	public void deactivate(){
		LOG.info("Deactivating CatchUp Service");
		isActive = false;
	}
	
	/**
	 * Queues batches of candidates from each service for processing. New batches of candidates are retrieved when there
	 * until the conductor becomes inactive or there are no more candidates retrieved while the processing queue is
	 * empty.  
	 * @param priorToDate - if supplied, then catchup services are run in stale mode, where all suitable objects
	 * 	with modification dates prior to this parameter are selected as candidates.
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
	
			boolean candidatesFound = false;
			boolean pidsQueued = false;
	
			do {
				pidsQueued = !enhancementConductor.isEmpty();
				if (!pidsQueued) {
					LOG.debug("Searching for new candidate batch");
					candidatesFound = addBatch(priorToDate);
				}
				try {
					Thread.sleep(catchUpCheckDelay);
				} catch (InterruptedException e) {
					Thread.currentThread().isInterrupted();
				}
			} while (isActive && (candidatesFound || pidsQueued));
			LOG.info("Catchup Services ending.  " + itemsProcessedThisSession + " items were queued/processed.");
		} finally {
			inCatchUp = false;
		}
		return true;
	}

	/**
	 * Adds a page of results to the processing queue from each service's findCandidates method.
	 * 
	 * @return true if any candidate items were queued.
	 */
	private boolean addBatch(String priorToDate) {
		boolean candidatesFound = false;
		for (ObjectEnhancementService s : services) {
			try {
				if (s.isActive()) {
					int pageSize = this.pageSize + enhancementConductor.getFailedPids().size();
	
					List<PID> candidates = null;
					if (priorToDate == null){
						candidates = s.findCandidateObjects(pageSize);
					} else {
						candidates = s.findStaleCandidateObjects(pageSize, priorToDate);
					}
					LOG.debug("Searched for " + pageSize + " candidates, found " + candidates.size() + " for "
							+ s.getClass().getName());
					if (candidates.size() > 0) {
						candidatesFound = true;
						for (PID candidate : candidates) {
							if (priorToDate == null){
								PIDMessage pidMessage = new PIDMessage(candidate, JMSMessageUtil.servicesMessageNamespace, 
										JMSMessageUtil.ServicesActions.APPLY_SERVICE_STACK.getName(), s.getClass().getName());
								messageDirector.direct(pidMessage);
							} else {
								PIDMessage pidMessage = new PIDMessage(candidate, JMSMessageUtil.servicesMessageNamespace, 
										JMSMessageUtil.ServicesActions.APPLY_SERVICE.getName(), s.getClass().getName());
								messageDirector.direct(pidMessage);
							}
							this.itemsProcessed++;
							this.itemsProcessedThisSession++;
						}
					}
				}
			} catch (Exception e){
				LOG.error("Failed to add batch for " + s.getClass().getName(), e);
			}
		}
		return candidatesFound;
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

	public boolean isInCatchUp(){
		return inCatchUp;
	}
	
	public void setInCatchUp(boolean inCatchUp){
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
