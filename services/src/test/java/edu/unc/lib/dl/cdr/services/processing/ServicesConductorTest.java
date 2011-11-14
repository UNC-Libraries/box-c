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
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Resource;

import org.jdom.Element;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import edu.unc.lib.dl.cdr.services.AbstractFedoraEnhancementService;
import edu.unc.lib.dl.cdr.services.Enhancement;
import edu.unc.lib.dl.cdr.services.ObjectEnhancementService;
import edu.unc.lib.dl.cdr.services.exception.EnhancementException;
import edu.unc.lib.dl.cdr.services.model.PIDMessage;
import edu.unc.lib.dl.cdr.services.test.ManagementClientMock;
import edu.unc.lib.dl.cdr.services.util.JMSMessageUtil;
import edu.unc.lib.dl.fedora.PID;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/service-context-unit-conductor.xml" })
public class ServicesConductorTest extends Assert {

	private static final Logger LOG = LoggerFactory.getLogger(ServicesConductorTest.class);
	
	@Resource
	private ServicesConductor servicesConductor;
	@Resource
	private MessageDirector messageDirector;
	private ServicesThreadPoolExecutor executor;
	@Resource
	private List<ObjectEnhancementService> servicesList = null;
	private List<ObjectEnhancementService> delayServices;
	@Resource
	private ServicesQueueMessageFilter servicesMessageFilter;
	
	public static AtomicInteger inIsApplicable;
	public static AtomicInteger incompleteServices;
	public static AtomicInteger betweenApplicableAndEnhancement;
	public static AtomicInteger servicesCompleted;
	
	public long delayServiceTime = 0;
	
	public ServicesConductorTest(){
		delayServices = new ArrayList<ObjectEnhancementService>();
		DelayService delayService = new DelayService();
		delayServices.add(delayService);
	}
	
	@Before
	public void setUp() throws Exception {
		servicesConductor.clearState("yes");
		servicesConductor.shutdownNow("yes");
		while (servicesConductor.getExecutor().isTerminating() && !servicesConductor.getExecutor().isShutdown());
		servicesConductor.restart();
		this.executor = servicesConductor.getExecutor();
		inIsApplicable = new AtomicInteger(0);
		incompleteServices = new AtomicInteger(0);
		betweenApplicableAndEnhancement = new AtomicInteger(0);
		servicesCompleted = new AtomicInteger(0);
	}
	
	
	/*@Test
	public void stressQueueOperations() throws Exception {
		for (int i=0; i<20; i++){
			setUp();
			queueOperations();
		}
		while (!servicesConductor.isIdle());
	}*/
	
	@Test
	public void queueOperations() {
		servicesConductor.setServices(delayServices);
		servicesMessageFilter.setServices(delayServices);
		
		delayServiceTime = 0;
		
		int numberTestMessages = 10;
		
		//Add messages and check that they all ran
		for (int i=0; i<numberTestMessages; i++){
			PIDMessage message = new PIDMessage("uuid:" + i, JMSMessageUtil.servicesMessageNamespace, 
					JMSMessageUtil.ServicesActions.APPLY_SERVICE_STACK.getName());
			messageDirector.direct(message);
		}
		while (!servicesConductor.isEmpty());
		
		assertTrue(servicesCompleted.get() == numberTestMessages);
		
		
		//Add messages which contain a lot of duplicates
		delayServiceTime = 300;
		servicesConductor.pause();
		for (int i=0; i<numberTestMessages; i++){
			PIDMessage message = new PIDMessage("uuid:" + i, JMSMessageUtil.servicesMessageNamespace, 
					JMSMessageUtil.ServicesActions.APPLY_SERVICE_STACK.getName());
			messageDirector.direct(message);
			messageDirector.direct(message);
			messageDirector.direct(message);
		}
		
		assertTrue(servicesConductor.getPidQueue().size() == numberTestMessages * 3);
		
		//Let max threads number of threads start, make sure that collision list is properly populated
		servicesConductor.resume();
		while (servicesConductor.getLockedPids().size() < servicesConductor.getMaxThreads());
		servicesConductor.pause();
		
		assertTrue(servicesConductor.getCollisionList().size() == (servicesConductor.getMaxThreads() - 1) * 2);
		
		//Add messages then clear the conductors state
		for (int i=0; i<numberTestMessages; i++){
			PIDMessage message = new PIDMessage("uuid:" + i, JMSMessageUtil.servicesMessageNamespace, 
					JMSMessageUtil.ServicesActions.APPLY_SERVICE_STACK.getName());
			messageDirector.direct(message);
		}
		
		servicesConductor.clearState("yes");
		assertTrue(servicesConductor.getPidQueue().size() == 0);
		assertTrue(servicesConductor.getCollisionList().size() == 0);
		assertTrue(servicesConductor.getFailedPids().size() == 0);
		assertTrue(servicesConductor.getLockedPids().size() == 0);
		assertTrue(executor.getQueue().size() == 0);
	}
	
	/*@Test
	public void stressExecutorOperations() throws Exception {
		for (int i=0; i<50; i++){
			setUp();
			executorOperations();
		}
	}*/
	
	@Test
	public void executorOperations() throws InterruptedException {
		servicesConductor.setServices(delayServices);
		servicesMessageFilter.setServices(delayServices);
		
		delayServiceTime = 500;
		
		assertTrue(servicesConductor.isReady());
		servicesConductor.pause();
		assertTrue(servicesConductor.isReady());
		assertTrue(servicesConductor.isIdle());
		int numberTestMessages = 10;
		//queue items while paused, make sure they aren't moving
		for (int i=0; i<numberTestMessages; i++){
			PIDMessage message = new PIDMessage("uuid:" + i, JMSMessageUtil.servicesMessageNamespace, 
					JMSMessageUtil.ServicesActions.APPLY_SERVICE_STACK.getName());
			messageDirector.direct(message);
		}
		
		Thread.sleep(100);
		
		assertTrue(servicesConductor.getQueueSize() == numberTestMessages);
		assertTrue(servicesConductor.getLockedPids().size() == 0);
		
		//Unpause and let the first max threads number of messages start processing, then pause mid-way
		servicesConductor.resume();
		while (servicesConductor.getLockedPids().size() < servicesConductor.getMaxThreads() 
				&& servicesConductor.getQueueSize() > 0);
		synchronized(servicesConductor.getPidQueue()){
			servicesConductor.pause();
		}
		
		//Wait for isApplicables to finish so that services are paused midway
		while (betweenApplicableAndEnhancement.get() < servicesConductor.getMaxThreads());
		
		assertTrue(servicesConductor.getLockedPids().size() == servicesConductor.getMaxThreads());
		LOG.debug("Incomplete: " + incompleteServices.get());
		assertEquals(incompleteServices.get(), servicesConductor.getMaxThreads());
		
		//Abort the currently active threads
		servicesConductor.abort("yes");
		
		while (servicesConductor.getExecutor().isTerminating() || servicesConductor.getExecutor().isShutdown());
		
		executor = servicesConductor.getExecutor();
		//Verify that current threads died but that the remaining items are still ready to go
		assertTrue(servicesConductor.getLockedPids().size() == 0);
		assertTrue(servicesConductor.getQueueSize() == numberTestMessages - servicesConductor.getMaxThreads());
		LOG.debug("Queue: " + executor.getQueue().size() + " Active:" + executor.getActiveCount());
		assertTrue(executor.getQueue().size() + executor.getActiveCount() == numberTestMessages - servicesConductor.getMaxThreads());
		
		//Process remaining message queue, then shut down conductor
		servicesConductor.resume();
		while (servicesConductor.getLockedPids().size() > 0 || servicesConductor.getQueueSize() > 0);
		
		assertTrue(servicesCompleted.get() == numberTestMessages - servicesConductor.getMaxThreads());
		
		servicesConductor.shutdownNow("yes");
		assertFalse(servicesConductor.isReady());
		
		//Try to direct a pid with conductor shutdown
		servicesCompleted.set(0);
		PIDMessage message = new PIDMessage("uuid:fail", JMSMessageUtil.servicesMessageNamespace, 
				JMSMessageUtil.ServicesActions.APPLY_SERVICE_STACK.getName());
		messageDirector.direct(message);
		
		assertTrue(servicesCompleted.get() == 0);
		assertTrue(servicesConductor.getQueueSize() == 0);
		assertTrue(servicesConductor.getLockedPids().size() == 0);
	}
	
	/*@Test
	public void stressEnhancementFailure(){
		for (int i=0; i<50; i++){
			enhancementFailure();
		}
	}*/
	
	@Test
	public void enhancementFailure(){
		servicesConductor.setServices(servicesList);
		servicesMessageFilter.setServices(servicesList);
		
		for (ObjectEnhancementService s : servicesConductor.getServices()) {
			enhancementFailure(s);
		}
	}
	
	
	public void enhancementFailure(ObjectEnhancementService s){
		String serviceName = s.getClass().getName();
		
		LOG.debug("Service failure test of " + serviceName);
		
		PIDMessage message = new PIDMessage(ManagementClientMock.PID_FILE_SYSTEM, JMSMessageUtil.servicesMessageNamespace, 
				JMSMessageUtil.ServicesActions.APPLY_SERVICE.getName(), serviceName);
		
		servicesConductor.clearState("yes");
		servicesConductor.setRecoverableDelay(500);
		servicesConductor.setUnexpectedExceptionDelay(0);
		
		assertFalse(servicesConductor.isPaused());
		
		//Test fatal exceptions
		servicesConductor.resume();
		
		messageDirector.direct(message);
		while (servicesConductor.getPidQueue().size() != 0 || servicesConductor.getLockedPids().size() != 0 
				|| executor.getActiveCount() != 0);
		assertTrue(servicesConductor.isPaused());
		assertTrue(servicesConductor.getFailedPids().containsKey(ManagementClientMock.PID_FILE_SYSTEM));
		
		
		assertTrue(executor.getActiveCount() == 0);
		
		//Test unrecoverable
		message = new PIDMessage(ManagementClientMock.PID_NOT_FOUND, JMSMessageUtil.servicesMessageNamespace, 
				JMSMessageUtil.ServicesActions.APPLY_SERVICE.getName(), serviceName);
		
		servicesConductor.getFailedPids().clear();
		messageDirector.direct(message);
		while (servicesConductor.getFailedPids().size() == 0 
				&& (servicesConductor.getPidQueue().size() == 0 || executor.getActiveCount() == 0));
		assertTrue(servicesConductor.isPaused());
		assertTrue(servicesConductor.getPidQueue().size() == 1 && executor.getActiveCount() == 1);
		assertFalse(servicesConductor.getFailedPids().containsKey(ManagementClientMock.PID_NOT_FOUND));
		
		servicesConductor.resume();
		assertFalse(servicesConductor.isPaused());
		
		while (servicesConductor.getPidQueue().size() != 0 || servicesConductor.getLockedPids().size() != 0 
				|| executor.getActiveCount() != 0);
		assertTrue(servicesConductor.getFailedPids().containsKey(ManagementClientMock.PID_NOT_FOUND));
		assertFalse(servicesConductor.isPaused());
		
		servicesConductor.pause();
		
		//Test recoverable
		message = new PIDMessage(ManagementClientMock.PID_FEDORA, JMSMessageUtil.servicesMessageNamespace, 
				JMSMessageUtil.ServicesActions.APPLY_SERVICE.getName(), serviceName);
		
		messageDirector.direct(message);
		while (servicesConductor.getPidQueue().size() == 0);
		servicesConductor.resume();
		while (servicesConductor.getLockedPids().size() == 0);
		assertTrue(servicesConductor.getLockedPids().contains(ManagementClientMock.PID_FEDORA));
		assertFalse(servicesConductor.getFailedPids().containsKey(ManagementClientMock.PID_FEDORA));
		while (servicesConductor.getLockedPids().size() == 1);
		assertTrue(servicesConductor.getFailedPids().containsKey(ManagementClientMock.PID_FEDORA));
		
		//Test run time
		message = new PIDMessage(ManagementClientMock.PID_RUN_TIME, JMSMessageUtil.servicesMessageNamespace, 
				JMSMessageUtil.ServicesActions.APPLY_SERVICE.getName(), serviceName);
		
		messageDirector.direct(message);
		while (servicesConductor.getPidQueue().size() > 0 || executor.getActiveCount() > 0);
		assertTrue(servicesConductor.getFailedPids().containsKey(ManagementClientMock.PID_RUN_TIME));
		
	}

	public ServicesConductor getServicesConductor() {
		return servicesConductor;
	}

	public void setServicesConductor(ServicesConductor servicesConductor) {
		this.servicesConductor = servicesConductor;
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
		public Enhancement<Element> getEnhancement(PIDMessage pid) throws EnhancementException {
			return new DelayEnhancement(this, pid);
		}

		@Override
		public boolean isApplicable(PIDMessage pid) throws EnhancementException {
			incompleteServices.incrementAndGet();
			inIsApplicable.incrementAndGet();
			try {
				Thread.sleep(delayServiceTime);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			} finally {
				inIsApplicable.decrementAndGet();
				betweenApplicableAndEnhancement.incrementAndGet();
				LOG.debug("Completed isApplicable for " + pid.getPIDString());	
			}
			return true;
		}

		@Override
		public boolean prefilterMessage(PIDMessage pid) throws EnhancementException {
			return true;
		}

		@Override
		public boolean isStale(PID pid) throws EnhancementException {
			return false;
		}
		
	}
	
	public class DelayEnhancement extends Enhancement<Element> {

		public DelayEnhancement(ObjectEnhancementService service, PIDMessage pid) {
			super(pid);
		}
		
		@Override
		public Element call() throws EnhancementException {
			try {
				betweenApplicableAndEnhancement.decrementAndGet();
				try {
					Thread.sleep(delayServiceTime);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			} finally {
				incompleteServices.decrementAndGet();
				servicesCompleted.incrementAndGet();
			}
			return null;
		}
		
	}
	
}
