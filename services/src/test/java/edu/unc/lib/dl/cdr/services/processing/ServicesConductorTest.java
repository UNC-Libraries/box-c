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

import javax.annotation.Resource;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import edu.unc.lib.dl.cdr.services.ObjectEnhancementService;
import edu.unc.lib.dl.cdr.services.imaging.ImageEnhancementService;
import edu.unc.lib.dl.cdr.services.imaging.ThumbnailEnhancementService;
import edu.unc.lib.dl.cdr.services.model.PIDMessage;
import edu.unc.lib.dl.cdr.services.techmd.TechnicalMetadataEnhancementService;
import edu.unc.lib.dl.cdr.services.test.ManagementClientMock;
import edu.unc.lib.dl.cdr.services.util.JMSMessageUtil;
import edu.unc.lib.dl.fedora.PID;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/service-context-unit-conductor.xml" })
public class ServicesConductorTest extends Assert {

	private static final Logger LOG = LoggerFactory.getLogger(ServicesConductorTest.class);
	
	@Resource
	private ServicesConductor servicesConductor;
	private ServicesThreadPoolExecutor executor; 
	
	@Before
	public void setUp() throws Exception {
		this.executor = servicesConductor.getExecutor();
	}
	
	/*@Test
	public void stressEnhancementFailureFailure(){
		for (int i=0; i<100; i++){
			enhancementFailure();
		}
	}*/
	
	@Test
	public void enhancementFailure(){
		for (ObjectEnhancementService s : servicesConductor.getServices()) {
			enhancementFailure(s.getClass().getName());
		}
	}
	
	
	public void enhancementFailure(String serviceName){
		LOG.debug("Service failure test of " + serviceName);
		
		PIDMessage message = new PIDMessage(ManagementClientMock.PID_FILE_SYSTEM, JMSMessageUtil.servicesMessageNamespace, 
				JMSMessageUtil.ServicesActions.APPLY_SERVICE.getName(), serviceName);
		
		servicesConductor.flushPids("yes");
		servicesConductor.clearFailedPids("yes");
		
		servicesConductor.setRecoverableDelay(500);
		servicesConductor.setUnexpectedExceptionDelay(0);
		
		assertFalse(servicesConductor.isPaused());
		
		//Test fatal exceptions
		servicesConductor.clearFailedPids("yes");
		servicesConductor.resume();
		
		servicesConductor.add(message);
		while (servicesConductor.getPidQueue().size() != 0 || servicesConductor.getLockedPids().size() != 0 
				|| executor.getActiveCount() != 0);
		assertTrue(servicesConductor.isPaused());
		assertTrue(servicesConductor.getFailedPids().containsKey(ManagementClientMock.PID_FILE_SYSTEM));
		
		
		assertTrue(executor.getActiveCount() == 0);
		
		//Test unrecoverable
		message = new PIDMessage(ManagementClientMock.PID_NOT_FOUND, JMSMessageUtil.servicesMessageNamespace, 
				JMSMessageUtil.ServicesActions.APPLY_SERVICE.getName(), serviceName);
		
		servicesConductor.getFailedPids().clear();
		servicesConductor.add(message);
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
		
		servicesConductor.add(message);
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
		
		servicesConductor.add(message);
		while (servicesConductor.getPidQueue().size() > 0 || executor.getActiveCount() > 0);
		assertTrue(servicesConductor.getFailedPids().containsKey(ManagementClientMock.PID_RUN_TIME));
		
	}

	public ServicesConductor getServicesConductor() {
		return servicesConductor;
	}

	public void setServicesConductor(ServicesConductor servicesConductor) {
		this.servicesConductor = servicesConductor;
	}
	
}
