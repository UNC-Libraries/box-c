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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.cdr.services.ObjectEnhancementService;
import edu.unc.lib.dl.cdr.services.model.PIDMessage;
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
	private ServicesConductor servicesConductor;
	// List of services to perform catchup processing on
	private List<ObjectEnhancementService> services = new ArrayList<ObjectEnhancementService>();
	// If this conductor is active
	private boolean isActive;
	// Number of items to get from each service per page.
	private int pageSize = 100;
	// Delay between checks to see if more candidates need to be retrieved.
	private long catchUpCheckDelay = 10000L;

	private long itemsProcessed = 0;
	private long itemsProcessedThisSession = 0;
	
	private String priorToDate = null;

	public void activate(){
		LOG.info("Activating CatchUp Service");
		isActive = true;
		catchUp();
	}
	
	public void deactivate(){
		LOG.info("Deactivating CatchUp Service");
		isActive = false;
	}
	
	/**
	 * Queues batches of candidates from each service for processing. New batches of candidates are retrieved when there
	 * until the conductor becomes inactive or there are no more candidates retrieved while the processing queue is
	 * empty.
	 */
	public void catchUp() {
		if (!isActive) {
			return;
		}
		itemsProcessedThisSession = 0;
		LOG.info("Catchup Services starting");

		boolean candidatesFound = false;
		boolean pidsQueued = false;

		do {
			pidsQueued = servicesConductor.getPidQueue().size() != 0 || servicesConductor.getCollisionList().size() != 0;
			if (!pidsQueued) {
				LOG.debug("Searching for new candidate batch");
				candidatesFound = addBatch();
			}
			try {
				Thread.sleep(catchUpCheckDelay);
			} catch (Exception e) {

			}
		} while (isActive && (candidatesFound || pidsQueued));
		LOG.info("Catchup Services ending.  " + itemsProcessedThisSession + " items were queued/processed.");
	}

	/**
	 * Adds a page of results to the processing queue from each service's findCandidates method.
	 * 
	 * @return true if any candidate items were queued.
	 */
	private boolean addBatch() {
		boolean candidatesFound = false;
		for (ObjectEnhancementService s : services) {
			try {
				if (s.isActive()) {
					int pageSize = this.pageSize + servicesConductor.getFailedPids().size();
	
					List<PID> candidates = s.findCandidateObjects(pageSize);
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
								PIDMessage pidMessage = new PIDMessage(candidate);
								messageDirector.direct(pidMessage);
							} else {
								PIDMessage pidMessage = new PIDMessage(candidate, null, s.getClass().getName());
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

	public ServicesConductor getServicesConductor() {
		return servicesConductor;
	}

	public void setServicesConductor(ServicesConductor servicesConductor) {
		this.servicesConductor = servicesConductor;
	}

	public List<ObjectEnhancementService> getServices() {
		return services;
	}

	public void setServices(List<ObjectEnhancementService> services) {
		this.services = services;
	}

	public boolean isActive() {
		return isActive;
	}

	public void setActive(boolean isActive) {
		this.isActive = isActive;
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
	
	public String getPriorToDate() {
		return priorToDate;
	}

	public void setPriorToDate(String priorToDate) {
		this.priorToDate = priorToDate;
	}
	
	public void clearPriorToDate(){
		this.priorToDate = null;
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
