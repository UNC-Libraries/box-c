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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.jdom.Element;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.cdr.services.AbstractFedoraEnhancementService;
import edu.unc.lib.dl.cdr.services.Enhancement;
import edu.unc.lib.dl.cdr.services.ObjectEnhancementService;
import edu.unc.lib.dl.cdr.services.exception.EnhancementException;
import edu.unc.lib.dl.cdr.services.model.EnhancementMessage;
import edu.unc.lib.dl.cdr.services.model.FailedObjectHashMap;
import edu.unc.lib.dl.cdr.services.util.JMSMessageUtil;
import edu.unc.lib.dl.fedora.PID;

public class EnhancementConductorInterruptTest extends Assert {
	
	protected static final Logger LOG = LoggerFactory.getLogger(EnhancementConductorInterruptTest.class);
	
	protected EnhancementConductor enhancementConductor;
	@SuppressWarnings("rawtypes")
	protected ServicesThreadPoolExecutor executor;
	protected List<ObjectEnhancementService> delayServices;
	
	public static AtomicInteger inIsApplicable;
	public static AtomicInteger incompleteServices;
	public static AtomicInteger betweenApplicableAndEnhancement;
	public static AtomicInteger servicesCompleted;
	public static AtomicInteger inService;
	
	protected int numberTestMessages;
	
	public AtomicBoolean flag;
	public Object blockingObject;
	
	public EnhancementConductorInterruptTest(){
		
	}
	
	@Before
	public void setUp() throws Exception {
		delayServices = new ArrayList<ObjectEnhancementService>();
		DelayService delayService = new DelayService();
		delayServices.add(delayService);
		
		enhancementConductor = new EnhancementConductor();
		enhancementConductor.setRecoverableDelay(0);
		enhancementConductor.setUnexpectedExceptionDelay(0);
		enhancementConductor.setMaxThreads(3);
		enhancementConductor.setFailedPids(new FailedObjectHashMap());
		enhancementConductor.setServices(delayServices);
		enhancementConductor.init();
		
		this.executor = enhancementConductor.getExecutor();
		inIsApplicable = new AtomicInteger(0);
		incompleteServices = new AtomicInteger(0);
		betweenApplicableAndEnhancement = new AtomicInteger(0);
		servicesCompleted = new AtomicInteger(0);
		inService = new AtomicInteger(0);
		numberTestMessages = 10;
		
		this.blockingObject = new Object();
		this.flag = new AtomicBoolean(true);
	}
	
	@Test
	public void abortPause() throws InterruptedException {
		assertTrue(enhancementConductor.isReady());
		
		assertTrue(enhancementConductor.isReady());
		assertTrue(enhancementConductor.isIdle());
		int numberTestMessages = 10;
		//queue items while paused, make sure they aren't moving
		for (int i=0; i<numberTestMessages; i++){
			EnhancementMessage message = new EnhancementMessage("uuid:" + i, JMSMessageUtil.servicesMessageNamespace, 
					JMSMessageUtil.ServicesActions.APPLY_SERVICE_STACK.getName());
			message.setFilteredServices(delayServices);
			enhancementConductor.add(message);
		}
		
		while (inService.get() != enhancementConductor.getMaxThreads());

		assertTrue(enhancementConductor.getLockedPids().size() == enhancementConductor.getMaxThreads());
		
		//assertEquals(incompleteServices.get(), enhancementConductor.getMaxThreads());
		assertEquals(incompleteServices.get(), numberTestMessages - enhancementConductor.getQueueSize());
		//Abort the currently active threads
		enhancementConductor.abort();
		
		while (enhancementConductor.getExecutor().isTerminating() || enhancementConductor.getExecutor().isShutdown());
		
		executor = enhancementConductor.getExecutor();
		while (executor.getQueue().size() < numberTestMessages - enhancementConductor.getQueueSize());
		//Verify that current threads died but that the remaining items are still ready to go
		assertTrue(enhancementConductor.getLockedPids().size() == 0);
		assertTrue(enhancementConductor.getQueueSize() == numberTestMessages - enhancementConductor.getMaxThreads());
		
		//Process remaining message queue, then shut down conductor
		synchronized(blockingObject){
			flag.set(false);
			blockingObject.notifyAll();
		}
		enhancementConductor.resume();
		while (enhancementConductor.getLockedPids().size() > 0 || enhancementConductor.getQueueSize() > 0);
		
		//assertEquals(servicesCompleted.get(), numberTestMessages - enhancementConductor.getMaxThreads());
	}
	
	public class DelayService extends AbstractFedoraEnhancementService {
		
		public DelayService(){
			this.active = true;
		}
		
		@Override
		public List<PID> findCandidateObjects(int maxResults) throws EnhancementException {
			return null;
		}

		@Override
		public List<PID> findStaleCandidateObjects(int maxResults, String priorToDate) throws EnhancementException {
			return null;
		}

		@Override
		public Enhancement<Element> getEnhancement(EnhancementMessage pid) throws EnhancementException {
			return new DelayEnhancement(this, pid);
		}

		@Override
		public boolean isApplicable(EnhancementMessage pid) throws EnhancementException {
			incompleteServices.incrementAndGet();
			betweenApplicableAndEnhancement.incrementAndGet();
			LOG.debug("Completed isApplicable for " + pid.getTargetID());	
			return true;
		}

		@Override
		public boolean prefilterMessage(EnhancementMessage pid) throws EnhancementException {
			return true;
		}

		@Override
		public boolean isStale(PID pid) throws EnhancementException {
			return false;
		}
	}
	
	public class DelayEnhancement extends Enhancement<Element> {
		public DelayEnhancement(ObjectEnhancementService service, EnhancementMessage pid) {
			super(pid);
		}
		
		@Override
		public Element call() throws EnhancementException {
			LOG.debug("Call invoked for " + this.pid.getTargetID());
			betweenApplicableAndEnhancement.decrementAndGet();
			inService.incrementAndGet();
			while (flag.get()){
				synchronized(blockingObject){
					try {
						blockingObject.wait();
					} catch (InterruptedException e){
						Thread.currentThread().interrupt();
						return null;
					}
				}
			}
			incompleteServices.decrementAndGet();
			servicesCompleted.incrementAndGet();
			return null;
		}
		
	}
}
