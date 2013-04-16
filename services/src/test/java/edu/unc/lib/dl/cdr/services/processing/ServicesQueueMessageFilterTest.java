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

import static org.mockito.Mockito.*;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jdom.Document;
import org.jdom.input.SAXBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import edu.unc.lib.dl.cdr.services.ObjectEnhancementService;
import edu.unc.lib.dl.cdr.services.imaging.ImageEnhancementService;
import edu.unc.lib.dl.cdr.services.imaging.ThumbnailEnhancementService;
import edu.unc.lib.dl.cdr.services.model.EnhancementMessage;
import edu.unc.lib.dl.cdr.services.model.FailedEnhancementMap;
import edu.unc.lib.dl.cdr.services.model.FedoraEventMessage;
import edu.unc.lib.dl.cdr.services.solr.SolrUpdateEnhancementService;
import edu.unc.lib.dl.cdr.services.techmd.TechnicalMetadataEnhancementService;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.util.JMSMessageUtil;

public class ServicesQueueMessageFilterTest extends Assert {

	private ServicesQueueMessageFilter servicesMessageFilter;
	private List<ObjectEnhancementService> services;
	
	public ServicesQueueMessageFilterTest(){
		services = new ArrayList<ObjectEnhancementService>();
		services.add(new TechnicalMetadataEnhancementService());
		services.add(new ThumbnailEnhancementService());
		services.add(new ImageEnhancementService());
		services.add(new SolrUpdateEnhancementService());
	}
	
	@Before
   public void setUp() throws Exception {
		servicesMessageFilter = new ServicesQueueMessageFilter();
		servicesMessageFilter.setServices(services);
		
		FailedEnhancementMap failedPids = mock(FailedEnhancementMap.class);
		when(failedPids.get(anyString())).thenReturn(null);
		
		EnhancementConductor enhancementConductor = mock(EnhancementConductor.class);
		when(enhancementConductor.getFailedPids()).thenReturn(failedPids);
		servicesMessageFilter.setenhancementConductor(enhancementConductor);
	}
	
	@Test
	public void serviceMessage(){
		EnhancementMessage message = new EnhancementMessage("cdr:test", JMSMessageUtil.servicesMessageNamespace, 
				JMSMessageUtil.ServicesActions.APPLY_SERVICE.getName(), "");
		assertFalse(servicesMessageFilter.filter(message));
		message.setServiceName(TechnicalMetadataEnhancementService.class.getName());
		assertTrue(servicesMessageFilter.filter(message));
		message.setServiceName(null);
		assertFalse(servicesMessageFilter.filter(message));
		message.setServiceName("does.not.exist.Service");
		assertFalse(servicesMessageFilter.filter(message));
		message.setServiceName("");
		assertFalse(servicesMessageFilter.filter(message));
		//Full stack run
		message = new EnhancementMessage("cdr:test", JMSMessageUtil.servicesMessageNamespace, 
				JMSMessageUtil.ServicesActions.APPLY_SERVICE_STACK.getName());
		assertTrue(servicesMessageFilter.filter(message));
	}
	
	@Test
	public void nullMessage(){
		assertFalse(servicesMessageFilter.filter(null));
		String pid = null;
		try {
			servicesMessageFilter.filter(new EnhancementMessage(pid, null, null));
			assertTrue(false);
		} catch (IllegalArgumentException e){
			assertTrue(true);
		}
		EnhancementMessage emptyMessage = new EnhancementMessage("", "", "");
		assertFalse(servicesMessageFilter.filter(emptyMessage));
	}
	
	@Test
	public void fedoraObjectMessages() throws Exception {
		
		//Ingest object message, should partially pass, not pass solr
		Document doc = readFileAsString("ingestMessage.xml");
		EnhancementMessage message = new FedoraEventMessage(doc);
		assertTrue(servicesMessageFilter.filter(message));
		assertTrue(message.getFilteredServices().size() > 0);
		assertTrue(message.filteredServicesContains(SolrUpdateEnhancementService.class));
		assertTrue(message.filteredServicesContains(TechnicalMetadataEnhancementService.class));
		assertTrue(message.filteredServicesContains(ImageEnhancementService.class));
		assertTrue(message.filteredServicesContains(ThumbnailEnhancementService.class));
		
		//Purge object message, fail
		message.setAction(JMSMessageUtil.FedoraActions.PURGE_OBJECT.getName());
		assertFalse(servicesMessageFilter.filter(message));
	}
	
	@Test
	public void fedoraDatastreamMessages() throws Exception {
		//Change md descript datastream, should not pass filters
		Document doc = readFileAsString("modifyDSMDDescriptive.xml");
		EnhancementMessage message = new FedoraEventMessage(doc);
		assertFalse(servicesMessageFilter.filter(message));
		assertNull(message.getFilteredServices());
		
		message.setAction(JMSMessageUtil.FedoraActions.PURGE_DATASTREAM.getName());
		assertFalse(servicesMessageFilter.filter(message));
		
		message.setAction(JMSMessageUtil.FedoraActions.ADD_DATASTREAM.getName());
		assertFalse(servicesMessageFilter.filter(message));
		
		//Change data file, should pass
		doc = readFileAsString("modifyDSDataFile.xml");
		message = new FedoraEventMessage(doc);
		assertTrue(servicesMessageFilter.filter(message));
		assertTrue(message.getFilteredServices().size() > 0);
		assertTrue(message.filteredServicesContains(SolrUpdateEnhancementService.class));
		assertTrue(message.filteredServicesContains(TechnicalMetadataEnhancementService.class));
		assertTrue(message.filteredServicesContains(ImageEnhancementService.class));
		assertTrue(message.filteredServicesContains(ThumbnailEnhancementService.class));
		
		message.setAction(JMSMessageUtil.FedoraActions.PURGE_DATASTREAM.getName());
		assertFalse(servicesMessageFilter.filter(message));
		
		message.setAction(JMSMessageUtil.FedoraActions.ADD_DATASTREAM.getName());
		assertTrue(servicesMessageFilter.filter(message));
		assertTrue(message.filteredServicesContains(SolrUpdateEnhancementService.class));
		assertTrue(message.filteredServicesContains(TechnicalMetadataEnhancementService.class));
		assertTrue(message.filteredServicesContains(ImageEnhancementService.class));
		assertTrue(message.filteredServicesContains(ThumbnailEnhancementService.class));
	}
	
	@Test
	public void fedoraRelationMessages() throws Exception {
		//Add relation tests
		Document doc = readFileAsString("addRelSourceData.xml");
		FedoraEventMessage message = new FedoraEventMessage(doc);
		assertTrue(servicesMessageFilter.filter(message));
		assertTrue(message.filteredServicesContains(SolrUpdateEnhancementService.class));
		assertFalse(message.filteredServicesContains(TechnicalMetadataEnhancementService.class));
		assertFalse(message.filteredServicesContains(ImageEnhancementService.class));
		assertTrue(message.filteredServicesContains(ThumbnailEnhancementService.class));
		
		message.setAction(JMSMessageUtil.FedoraActions.PURGE_RELATIONSHIP.getName());
		assertTrue(servicesMessageFilter.filter(message));
		assertTrue(message.filteredServicesContains(SolrUpdateEnhancementService.class));
		assertFalse(message.filteredServicesContains(TechnicalMetadataEnhancementService.class));
		assertFalse(message.filteredServicesContains(ImageEnhancementService.class));
		assertTrue(message.filteredServicesContains(ThumbnailEnhancementService.class));
		
		message.setRelationPredicate(ContentModelHelper.CDRProperty.hasSurrogate.getURI().toString());
		message.setAction(JMSMessageUtil.FedoraActions.ADD_RELATIONSHIP.getName());
		assertTrue(servicesMessageFilter.filter(message));
		assertTrue(message.filteredServicesContains(SolrUpdateEnhancementService.class));
		assertFalse(message.filteredServicesContains(TechnicalMetadataEnhancementService.class));
		assertFalse(message.filteredServicesContains(ImageEnhancementService.class));
		assertTrue(message.filteredServicesContains(ThumbnailEnhancementService.class));
		
		message.setAction(JMSMessageUtil.FedoraActions.PURGE_RELATIONSHIP.getName());
		assertTrue(servicesMessageFilter.filter(message));
		assertTrue(message.filteredServicesContains(SolrUpdateEnhancementService.class));
		assertFalse(message.filteredServicesContains(TechnicalMetadataEnhancementService.class));
		assertFalse(message.filteredServicesContains(ImageEnhancementService.class));
		assertTrue(message.filteredServicesContains(ThumbnailEnhancementService.class));
		
		message.setRelationPredicate(ContentModelHelper.CDRProperty.techData.getURI().toString());
		message.setAction(JMSMessageUtil.FedoraActions.ADD_RELATIONSHIP.getName());
		assertFalse(servicesMessageFilter.filter(message));
		
		message.setAction(JMSMessageUtil.FedoraActions.PURGE_RELATIONSHIP.getName());
		assertFalse(servicesMessageFilter.filter(message));
		
		message.setRelationPredicate(ContentModelHelper.CDRProperty.thumb.getURI().toString());
		message.setAction(JMSMessageUtil.FedoraActions.ADD_RELATIONSHIP.getName());
		assertFalse(servicesMessageFilter.filter(message));
		
		message.setAction(JMSMessageUtil.FedoraActions.PURGE_RELATIONSHIP.getName());
		assertFalse(servicesMessageFilter.filter(message));
	}
	
	@Test
	public void applyStackStartingServicesFailures(){
		Set<String> failedServices = new HashSet<String>();
		failedServices.add(TechnicalMetadataEnhancementService.class.getName());
		
		FailedEnhancementMap failedPids = mock(FailedEnhancementMap.class);
		when(failedPids.getFailedServices(anyString())).thenReturn(failedServices);
		
		EnhancementConductor enhancementConductor = mock(EnhancementConductor.class);
		when(enhancementConductor.getFailedPids()).thenReturn(failedPids);
		servicesMessageFilter.setenhancementConductor(enhancementConductor);
		
		//Full stack run with the first service failing but no starting service
		EnhancementMessage message = new EnhancementMessage("cdr:test", JMSMessageUtil.servicesMessageNamespace, 
				JMSMessageUtil.ServicesActions.APPLY_SERVICE_STACK.getName());
		
		assertTrue(servicesMessageFilter.filter(message));
		assertFalse(message.filteredServicesContains(TechnicalMetadataEnhancementService.class));
		assertTrue(message.filteredServicesContains(ThumbnailEnhancementService.class));
		assertTrue(message.filteredServicesContains(SolrUpdateEnhancementService.class));
		
		//Invalid starting service
		message = new EnhancementMessage("cdr:test", JMSMessageUtil.servicesMessageNamespace, 
				JMSMessageUtil.ServicesActions.APPLY_SERVICE_STACK.getName(), "");
		assertFalse(servicesMessageFilter.filter(message));
		assertNull(message.getFilteredServices());
		
		//Fail on the starting point
		message = new EnhancementMessage("cdr:test", JMSMessageUtil.servicesMessageNamespace, 
				JMSMessageUtil.ServicesActions.APPLY_SERVICE_STACK.getName(), TechnicalMetadataEnhancementService.class.getName());
		assertFalse(servicesMessageFilter.filter(message));
		assertNull(message.getFilteredServices());
		
		//Fail before the starting point
		message = new EnhancementMessage("cdr:test", JMSMessageUtil.servicesMessageNamespace, 
				JMSMessageUtil.ServicesActions.APPLY_SERVICE_STACK.getName(), ThumbnailEnhancementService.class.getName());
		assertTrue(servicesMessageFilter.filter(message));
		assertFalse(message.filteredServicesContains(TechnicalMetadataEnhancementService.class));
		assertTrue(message.filteredServicesContains(ThumbnailEnhancementService.class));
		assertTrue(message.filteredServicesContains(SolrUpdateEnhancementService.class));
		
		//Fail after the starting point
		message = new EnhancementMessage("cdr:test", JMSMessageUtil.servicesMessageNamespace, 
				JMSMessageUtil.ServicesActions.APPLY_SERVICE_STACK.getName(), TechnicalMetadataEnhancementService.class.getName());
		failedServices.clear();
		failedServices.add(ThumbnailEnhancementService.class.getName());
		assertTrue(servicesMessageFilter.filter(message));
		assertTrue(message.filteredServicesContains(TechnicalMetadataEnhancementService.class));
		assertFalse(message.filteredServicesContains(ThumbnailEnhancementService.class));
		assertTrue(message.filteredServicesContains(SolrUpdateEnhancementService.class));
	}
	
	@Test
	public void applyServicesFailure(){
		Set<String> failedServices = new HashSet<String>();
		failedServices.add(TechnicalMetadataEnhancementService.class.getName());
		failedServices.add(ThumbnailEnhancementService.class.getName());
		failedServices.add(ImageEnhancementService.class.getName());
		failedServices.add(SolrUpdateEnhancementService.class.getName());
		
		FailedEnhancementMap failedPids = mock(FailedEnhancementMap.class);
		when(failedPids.getFailedServices(anyString())).thenReturn(failedServices);
		
		EnhancementConductor enhancementConductor = mock(EnhancementConductor.class);
		when(enhancementConductor.getFailedPids()).thenReturn(failedPids);
		servicesMessageFilter.setenhancementConductor(enhancementConductor);
		
		//fail techmd call
		EnhancementMessage message = new EnhancementMessage("cdr:test", JMSMessageUtil.servicesMessageNamespace, 
				JMSMessageUtil.ServicesActions.APPLY_SERVICE.getName(), TechnicalMetadataEnhancementService.class.getName());
		
		assertFalse(servicesMessageFilter.filter(message));
		assertNull(message.getFilteredServices());
		
		//fail full stack
		message = new EnhancementMessage("cdr:test", JMSMessageUtil.servicesMessageNamespace, 
				JMSMessageUtil.ServicesActions.APPLY_SERVICE_STACK.getName());
		
		assertFalse(servicesMessageFilter.filter(message));
		assertNull(message.getFilteredServices());
		
		//fail full stack, without solr being present in fail list.
		message = new EnhancementMessage("cdr:test", JMSMessageUtil.servicesMessageNamespace, 
				JMSMessageUtil.ServicesActions.APPLY_SERVICE_STACK.getName());
		
		failedServices.clear();
		failedServices.add(TechnicalMetadataEnhancementService.class.getName());
		failedServices.add(ThumbnailEnhancementService.class.getName());
		failedServices.add(ImageEnhancementService.class.getName());
		
		assertFalse(servicesMessageFilter.filter(message));
		assertNull(message.getFilteredServices());
		
		//pass techmd call
		failedServices.clear();
		failedServices.add(ThumbnailEnhancementService.class.getName());
		failedServices.add(ImageEnhancementService.class.getName());
		
		message = new EnhancementMessage("cdr:test", JMSMessageUtil.servicesMessageNamespace, 
				JMSMessageUtil.ServicesActions.APPLY_SERVICE.getName(), TechnicalMetadataEnhancementService.class.getName());
		
		assertTrue(servicesMessageFilter.filter(message));
		assertTrue(message.filteredServicesContains(TechnicalMetadataEnhancementService.class));
		assertFalse(message.filteredServicesContains(ThumbnailEnhancementService.class));
		assertFalse(message.filteredServicesContains(SolrUpdateEnhancementService.class));
		
		//pass only techmd and solr from full stack
		message = new EnhancementMessage("cdr:test", JMSMessageUtil.servicesMessageNamespace, 
				JMSMessageUtil.ServicesActions.APPLY_SERVICE_STACK.getName());
		assertTrue(servicesMessageFilter.filter(message));
		assertTrue(message.filteredServicesContains(TechnicalMetadataEnhancementService.class));
		assertFalse(message.filteredServicesContains(ThumbnailEnhancementService.class));
		assertTrue(message.filteredServicesContains(SolrUpdateEnhancementService.class));
	}

	public ServicesQueueMessageFilter getServicesMessageFilter() {
		return servicesMessageFilter;
	}

	public void setServicesMessageFilter(ServicesQueueMessageFilter servicesMessageFilter) {
		this.servicesMessageFilter = servicesMessageFilter;
	}

	private Document readFileAsString(String filePath) throws Exception {
		return new SAXBuilder().build(new InputStreamReader(this.getClass().getResourceAsStream(filePath)));
	}
}
