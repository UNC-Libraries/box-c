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
import edu.unc.lib.dl.cdr.services.imaging.ImageEnhancementService;
import edu.unc.lib.dl.cdr.services.imaging.ThumbnailEnhancementService;
import edu.unc.lib.dl.cdr.services.model.EnhancementApplication;
import edu.unc.lib.dl.cdr.services.model.EnhancementMessage;
import edu.unc.lib.dl.cdr.services.model.FailedObjectHashMap;
import edu.unc.lib.dl.cdr.services.techmd.TechnicalMetadataEnhancementService;
import edu.unc.lib.dl.cdr.services.util.JMSMessageUtil;
import edu.unc.lib.dl.fedora.PID;

public class EnhancementConductorTest extends Assert {

	protected static final Logger LOG = LoggerFactory.getLogger(EnhancementConductorTest.class);
	
	protected EnhancementConductor enhancementConductor;
	protected MessageDirector messageDirector;
	@SuppressWarnings("rawtypes")
	protected ServicesThreadPoolExecutor executor;
	protected List<ObjectEnhancementService> servicesList = null;
	protected List<ObjectEnhancementService> delayServices;
	protected ServicesQueueMessageFilter servicesMessageFilter;
	
	public static AtomicInteger inIsApplicable;
	public static AtomicInteger incompleteServices;
	public static AtomicInteger betweenApplicableAndEnhancement;
	public static AtomicInteger servicesCompleted;
	
	protected int numberTestMessages;
	
	public AtomicBoolean flag;
	public Object blockingObject;
	
	public EnhancementConductorTest(){
		delayServices = new ArrayList<ObjectEnhancementService>();
		DelayService delayService = new DelayService();
		delayServices.add(delayService);
		
		servicesList = new ArrayList<ObjectEnhancementService>();
		servicesList.add(new TechnicalMetadataEnhancementService());
		servicesList.add(new ImageEnhancementService());
		servicesList.add(new ThumbnailEnhancementService());
		
	}
	
	@Before
	public void setUp() throws Exception {
		enhancementConductor = new EnhancementConductor();
		enhancementConductor.setRecoverableDelay(0);
		enhancementConductor.setUnexpectedExceptionDelay(0);
		enhancementConductor.setMaxThreads(3);
		enhancementConductor.setFailedPids(new FailedObjectHashMap());
		enhancementConductor.init();
		
		List<MessageConductor> conductors = new ArrayList<MessageConductor>(1);
		conductors.add(enhancementConductor);
		
		messageDirector = new MessageDirector();
		messageDirector.setConductorsList(conductors);
		
		servicesMessageFilter = new ServicesQueueMessageFilter();
		servicesMessageFilter.setenhancementConductor(enhancementConductor);
		List<MessageFilter> filters = new ArrayList<MessageFilter>();
		filters.add(servicesMessageFilter);
		messageDirector.setFilters(filters);
		
		this.executor = enhancementConductor.getExecutor();
		inIsApplicable = new AtomicInteger(0);
		incompleteServices = new AtomicInteger(0);
		betweenApplicableAndEnhancement = new AtomicInteger(0);
		servicesCompleted = new AtomicInteger(0);
		numberTestMessages = 10;
		
		this.blockingObject = new Object();
		this.flag = new AtomicBoolean(true);
	}
	
	
	//@Test
	public void stressQueueOperations() throws Exception {
		while (this.executor.isShutdown() || this.executor.isTerminated() || this.executor.isTerminating());
		
		for (int i=0; i<500; i++){
			addMessages();
			while (!enhancementConductor.isEmpty());
			setUp();
		}
		while (!enhancementConductor.isIdle());
	}
	
	@Test
	public void addMessages() throws InterruptedException{
		enhancementConductor.setServices(delayServices);
		servicesMessageFilter.setServices(delayServices);
		
		flag.set(false);

		//Add messages and check that they all ran
		for (int i=0; i<numberTestMessages; i++){
			EnhancementMessage message = new EnhancementMessage("uuid:" + i, JMSMessageUtil.servicesMessageNamespace, 
					JMSMessageUtil.ServicesActions.APPLY_SERVICE_STACK.getName());
			messageDirector.direct(message);
			message = new EnhancementMessage("uuid:" + i + "d", JMSMessageUtil.servicesMessageNamespace, 
					JMSMessageUtil.ServicesActions.APPLY_SERVICE.getName(), DelayService.class.getName());
			messageDirector.direct(message);
		}
		
		while (!enhancementConductor.isEmpty());

		if (servicesCompleted.get() != numberTestMessages * 2){
			LOG.warn("Number of services completed (" + servicesCompleted.get() + 
					") does not match number of test messages (" + (numberTestMessages * 2) + ")");
		}
		assertEquals(servicesCompleted.get(), numberTestMessages * 2);
	}
	
	@Test
	public void addCollisions(){
		enhancementConductor.setServices(delayServices);
		servicesMessageFilter.setServices(delayServices);
		
		numberTestMessages = 5;
		
		//Add messages which contain a lot of duplicates
		for (int i=0; i<numberTestMessages; i++){
			EnhancementMessage message = new EnhancementMessage("uuid:" + i, JMSMessageUtil.servicesMessageNamespace, 
					JMSMessageUtil.ServicesActions.APPLY_SERVICE_STACK.getName());
			for (int j=0; j<numberTestMessages; j++){
				messageDirector.direct(message);
			}
		}
		
		while (enhancementConductor.getLockedPids().size() < enhancementConductor.getMaxThreads());
		
		assertEquals(enhancementConductor.getLockedPids().size(), enhancementConductor.getMaxThreads());
		assertEquals(enhancementConductor.getCollisionList().size(), 
				(enhancementConductor.getMaxThreads() - 1) * (numberTestMessages - 1));
		assertEquals(enhancementConductor.getPidQueue().size(), 
				(numberTestMessages * numberTestMessages - (enhancementConductor.getMaxThreads() - 1) * numberTestMessages) - 1);
		assertEquals(enhancementConductor.getQueueSize(), (numberTestMessages * numberTestMessages) - enhancementConductor.getMaxThreads());
		
		//Process the remaining items to make sure all messages get processed.
		synchronized(blockingObject){
			flag.set(false);
			blockingObject.notifyAll();
		}
		
		while (!enhancementConductor.isEmpty());
		assertEquals(servicesCompleted.get(), numberTestMessages * numberTestMessages);
	}
	
	@Test
	public void clearState(){
		enhancementConductor.setServices(delayServices);
		servicesMessageFilter.setServices(delayServices);
		
		enhancementConductor.pause();
		
		//Add messages then clear the conductors state
		for (int i=0; i<numberTestMessages; i++){
			EnhancementMessage message = new EnhancementMessage("uuid:" + i, JMSMessageUtil.servicesMessageNamespace, 
					JMSMessageUtil.ServicesActions.APPLY_SERVICE_STACK.getName());
			messageDirector.direct(message);
		}
		
		enhancementConductor.clearState();
		assertTrue(enhancementConductor.getPidQueue().size() == 0);
		assertTrue(enhancementConductor.getCollisionList().size() == 0);
		assertTrue(enhancementConductor.getFailedPids().size() == 0);
		assertTrue(enhancementConductor.getLockedPids().size() == 0);
		assertTrue(executor.getQueue().size() == 0);
		enhancementConductor.resume();
	}
	
	
	
	//@Test
	public void addToShutdownExecutor(){
		enhancementConductor.shutdownNow();
		assertFalse(enhancementConductor.isReady());
		
		//Try to direct a pid with conductor shutdown
		servicesCompleted.set(0);
		EnhancementMessage message = new EnhancementMessage("uuid:fail", JMSMessageUtil.servicesMessageNamespace, 
				JMSMessageUtil.ServicesActions.APPLY_SERVICE_STACK.getName());
		messageDirector.direct(message);
		
		assertTrue(servicesCompleted.get() == 0);
		assertTrue(enhancementConductor.getQueueSize() == 0);
		assertTrue(enhancementConductor.getLockedPids().size() == 0);
	}

	public EnhancementConductor getenhancementConductor() {
		return enhancementConductor;
	}

	public void setenhancementConductor(EnhancementConductor enhancementConductor) {
		this.enhancementConductor = enhancementConductor;
	}

	public MessageDirector getMessageDirector() {
		return messageDirector;
	}

	public void setMessageDirector(MessageDirector messageDirector) {
		this.messageDirector = messageDirector;
	}

	public ServicesQueueMessageFilter getServicesMessageFilter() {
		return servicesMessageFilter;
	}

	public void setServicesMessageFilter(ServicesQueueMessageFilter servicesMessageFilter) {
		this.servicesMessageFilter = servicesMessageFilter;
	}

	public List<ObjectEnhancementService> getServicesList() {
		return servicesList;
	}

	public void setServicesList(List<ObjectEnhancementService> servicesList) {
		this.servicesList = servicesList;
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
			return new DelayEnhancement(this, pid.getPid());
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

		@Override
		public EnhancementApplication getLastApplied(PID pid) throws EnhancementException {
			// TODO Auto-generated method stub
			return null;
		}
		
	}
	
	public class DelayEnhancement extends Enhancement<Element> {
		public DelayEnhancement(ObjectEnhancementService service, PID pid) {
			super(pid);
		}
		
		@Override
		public Element call() throws EnhancementException {
			LOG.debug("Call invoked for " + this.pid.getPid());
			betweenApplicableAndEnhancement.decrementAndGet();
			//inService.incrementAndGet();
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
