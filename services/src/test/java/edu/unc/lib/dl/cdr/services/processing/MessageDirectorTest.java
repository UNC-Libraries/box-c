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

import javax.annotation.Resource;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import edu.unc.lib.dl.cdr.services.ObjectEnhancementService;
import edu.unc.lib.dl.cdr.services.imaging.ImageEnhancementService;
import edu.unc.lib.dl.cdr.services.imaging.ThumbnailEnhancementService;
import edu.unc.lib.dl.cdr.services.model.PIDMessage;
import edu.unc.lib.dl.cdr.services.processing.SolrUpdateConductorTest.IsMatchingPID;
import edu.unc.lib.dl.cdr.services.techmd.TechnicalMetadataEnhancementService;
import edu.unc.lib.dl.cdr.services.util.JMSMessageUtil;
import edu.unc.lib.dl.data.ingest.solr.SolrUpdateAction;
import edu.unc.lib.dl.fedora.PID;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;

public class MessageDirectorTest extends Assert {

	private MessageDirector messageDirector;
	private SolrUpdateConductor solrConductor;
	private ServicesConductor servicesConductor;
	private List<ObjectEnhancementService> services;

	public MessageDirectorTest(){
		this.messageDirector = new MessageDirector();
		
		services = new ArrayList<ObjectEnhancementService>();
		services.add(new TechnicalMetadataEnhancementService());
		services.add(new ThumbnailEnhancementService());
		services.add(new ImageEnhancementService());
		
		List<MessageFilter> filters = new ArrayList<MessageFilter>();
		filters.add(new SolrUpdateMessageFilter());
		ServicesQueueMessageFilter servicesFilter = new ServicesQueueMessageFilter();
		servicesFilter.setServices(services);
		filters.add(servicesFilter);
		messageDirector.setFilters(filters);
	}

	@Before
	public void setup(){
		List<MessageConductor> conductors = new ArrayList<MessageConductor>();
		
		solrConductor = mock(SolrUpdateConductor.class);
		when(solrConductor.getIdentifier()).thenReturn(SolrUpdateConductor.identifier);
		servicesConductor = mock(ServicesConductor.class);
		when(servicesConductor.getIdentifier()).thenReturn(ServicesConductor.identifier);
		//servicesConductor.setServices(services);
		
		conductors.add(servicesConductor);
		conductors.add(solrConductor);
		
		messageDirector.setConductorsList(conductors);
	}
	
	class IsMatchingPID extends ArgumentMatcher<PIDMessage> {
		private String pid;
		
		public IsMatchingPID(String pid){
			this.pid = pid;
		}
		
      public boolean matches(Object pid) {
      	return ((PIDMessage) pid).getPIDString().startsWith(this.pid);
      }
   }
	
	@Test
	public void noServiceMessage(){
		PIDMessage message = new PIDMessage("cdr:test", JMSMessageUtil.servicesMessageNamespace, 
				JMSMessageUtil.ServicesActions.APPLY_SERVICE.getName(), "");
		messageDirector.direct(message);
		
		verify(solrConductor, never()).add(any(PIDMessage.class));
		verify(servicesConductor, never()).add(any(PIDMessage.class));
	}
	
	@Test
	public void techmdServiceMessage(){
		PIDMessage message = new PIDMessage("cdr:test", JMSMessageUtil.servicesMessageNamespace, 
				JMSMessageUtil.ServicesActions.APPLY_SERVICE.getName(), TechnicalMetadataEnhancementService.class.getName());
		messageDirector.direct(message);
		verify(solrConductor, never()).add(any(PIDMessage.class));
		verify(servicesConductor).add(any(PIDMessage.class));
	}
	
	@Test
	public void solrAddMessage(){
		PIDMessage message = new PIDMessage("cdr:test", SolrUpdateAction.namespace, SolrUpdateAction.ADD.getName());
		messageDirector.direct(message);
		verify(solrConductor).add(any(PIDMessage.class));
		verify(servicesConductor, never()).add(any(PIDMessage.class));
	}
	
	@Test
	public void nullMessage(){
		PIDMessage message = null;
		messageDirector.direct(message);
		verify(solrConductor, never()).add(any(PIDMessage.class));
		verify(servicesConductor, never()).add(any(PIDMessage.class));
	}
}
