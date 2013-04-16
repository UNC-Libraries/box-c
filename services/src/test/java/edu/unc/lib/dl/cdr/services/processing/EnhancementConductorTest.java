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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.cdr.services.ObjectEnhancementService;
import edu.unc.lib.dl.cdr.services.imaging.ImageEnhancementService;
import edu.unc.lib.dl.cdr.services.imaging.ThumbnailEnhancementService;
import edu.unc.lib.dl.cdr.services.model.EnhancementMessage;
import edu.unc.lib.dl.cdr.services.model.FailedEnhancementMap;
import edu.unc.lib.dl.cdr.services.techmd.TechnicalMetadataEnhancementService;
import edu.unc.lib.dl.util.JMSMessageUtil;

public class EnhancementConductorTest extends Assert {
	protected static final Logger LOG = LoggerFactory.getLogger(EnhancementConductorTest.class);

	protected EnhancementConductor enhancementConductor;
	protected MessageDirector messageDirector;
	@SuppressWarnings("rawtypes")
	protected ServicesThreadPoolExecutor executor;
	protected List<ObjectEnhancementService> servicesList = null;
	protected List<ObjectEnhancementService> delayServices;
	protected ServicesQueueMessageFilter servicesMessageFilter;

	protected int numberTestMessages;

	public EnhancementConductorTest() {
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
		enhancementConductor.setFailedPids(new FailedEnhancementMap());
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
		DelayEnhancement.init();
		
		numberTestMessages = 10;
	}

	// @Test
	public void stressQueueOperations() throws Exception {
		while (this.executor.isShutdown() || this.executor.isTerminated() || this.executor.isTerminating())
			;

		for (int i = 0; i < 500; i++) {
			addMessages();
			while (!enhancementConductor.isEmpty())
				;
			setUp();
		}
		while (!enhancementConductor.isIdle())
			;
	}

	@Test
	public void addMessages() throws InterruptedException {
		enhancementConductor.setServices(delayServices);
		servicesMessageFilter.setServices(delayServices);

		DelayEnhancement.flag.set(false);

		// Add messages and check that they all ran
		for (int i = 0; i < numberTestMessages; i++) {
			EnhancementMessage message = new EnhancementMessage("uuid:" + i, JMSMessageUtil.servicesMessageNamespace,
					JMSMessageUtil.ServicesActions.APPLY_SERVICE_STACK.getName());
			messageDirector.direct(message);
			message = new EnhancementMessage("uuid:" + i + "d", JMSMessageUtil.servicesMessageNamespace,
					JMSMessageUtil.ServicesActions.APPLY_SERVICE.getName(), DelayService.class.getName());
			messageDirector.direct(message);
		}

		while (!enhancementConductor.isEmpty())
			;

		if (DelayEnhancement.servicesCompleted.get() != numberTestMessages * 2) {
			LOG.warn("Number of services completed (" + DelayEnhancement.servicesCompleted.get()
					+ ") does not match number of test messages (" + (numberTestMessages * 2) + ")");
		}
		assertEquals(DelayEnhancement.servicesCompleted.get(), numberTestMessages * 2);
	}

	@Test
	public void addCollisions() {
		enhancementConductor.setServices(delayServices);
		servicesMessageFilter.setServices(delayServices);

		numberTestMessages = 5;

		// Add messages which contain a lot of duplicates
		for (int i = 0; i < numberTestMessages; i++) {
			EnhancementMessage message = new EnhancementMessage("uuid:" + i, JMSMessageUtil.servicesMessageNamespace,
					JMSMessageUtil.ServicesActions.APPLY_SERVICE_STACK.getName());
			for (int j = 0; j < numberTestMessages; j++) {
				messageDirector.direct(message);
			}
		}

		while (enhancementConductor.getLockedPids().size() < enhancementConductor.getMaxThreads())
			;

		assertEquals(enhancementConductor.getLockedPids().size(), enhancementConductor.getMaxThreads());
		LOG.debug("Add collisions waiting");
		LOG.debug("Queue" + enhancementConductor.getPidQueue());
		LOG.debug("Collisions" + enhancementConductor.getCollisionList());
		assertEquals(enhancementConductor.getCollisionList().size(), (enhancementConductor.getMaxThreads() - 1)
				* (numberTestMessages - 1));
		assertEquals(
				enhancementConductor.getPidQueue().size(),
				(numberTestMessages * numberTestMessages - (enhancementConductor.getMaxThreads() - 1) * numberTestMessages) - 1);
		assertEquals(enhancementConductor.getQueueSize(), (numberTestMessages * numberTestMessages)
				- enhancementConductor.getMaxThreads());

		// Process the remaining items to make sure all messages get processed.
		synchronized (DelayEnhancement.blockingObject) {
			DelayEnhancement.flag.set(false);
			DelayEnhancement.blockingObject.notifyAll();
		}

		while (!enhancementConductor.isEmpty())
			;
		assertEquals(DelayEnhancement.servicesCompleted.get(), numberTestMessages * numberTestMessages);
	}

	@Test
	public void clearState() {
		enhancementConductor.setServices(delayServices);
		servicesMessageFilter.setServices(delayServices);

		enhancementConductor.pause();

		// Add messages then clear the conductors state
		for (int i = 0; i < numberTestMessages; i++) {
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

	// @Test
	public void addToShutdownExecutor() {
		enhancementConductor.shutdownNow();
		assertFalse(enhancementConductor.isReady());

		// Try to direct a pid with conductor shutdown
		DelayEnhancement.servicesCompleted.set(0);
		EnhancementMessage message = new EnhancementMessage("uuid:fail", JMSMessageUtil.servicesMessageNamespace,
				JMSMessageUtil.ServicesActions.APPLY_SERVICE_STACK.getName());
		messageDirector.direct(message);

		assertTrue(DelayEnhancement.servicesCompleted.get() == 0);
		assertTrue(enhancementConductor.getQueueSize() == 0);
		assertTrue(enhancementConductor.getLockedPids().size() == 0);
	}

	@Test
	public void finishedWindowingTest() {
		EnhancementConductor.LimitedWindowList<Integer> limited = new EnhancementConductor.LimitedWindowList<Integer>(10);
		List<Integer> tooBigToFail = new ArrayList<Integer>();
		
		for (int i=0; i<12; i++) {
			limited.add(i);
			tooBigToFail.add(i);
		}
		assertEquals("[2, 3, 4, 5, 6, 7, 8, 9, 10, 11]", limited.toString());
		
		limited.clear();
		assertEquals(0, limited.size());
		
		
		limited.addAll(tooBigToFail);
		assertEquals("[2, 3, 4, 5, 6, 7, 8, 9, 10, 11]", limited.toString());
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
}
