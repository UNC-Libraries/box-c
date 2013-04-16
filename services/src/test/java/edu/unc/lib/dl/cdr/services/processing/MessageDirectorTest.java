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
import org.mockito.ArgumentMatcher;

import edu.unc.lib.dl.cdr.services.ObjectEnhancementService;
import edu.unc.lib.dl.cdr.services.imaging.ImageEnhancementService;
import edu.unc.lib.dl.cdr.services.imaging.ThumbnailEnhancementService;
import edu.unc.lib.dl.cdr.services.model.EnhancementMessage;
import edu.unc.lib.dl.cdr.services.model.FailedEnhancementMap;
import edu.unc.lib.dl.cdr.services.techmd.TechnicalMetadataEnhancementService;
import edu.unc.lib.dl.data.ingest.solr.SolrUpdateRequest;
import edu.unc.lib.dl.message.ActionMessage;
import edu.unc.lib.dl.util.IndexingActionType;
import edu.unc.lib.dl.util.JMSMessageUtil;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class MessageDirectorTest extends Assert {

	private MessageDirector messageDirector;
	private SolrUpdateConductor solrConductor;
	private EnhancementConductor enhancementConductor;
	private List<ObjectEnhancementService> services;

	public MessageDirectorTest(){
		services = new ArrayList<ObjectEnhancementService>();
		services.add(new TechnicalMetadataEnhancementService());
		services.add(new ThumbnailEnhancementService());
		services.add(new ImageEnhancementService());
	}

	@Before
	public void setup(){
		this.messageDirector = new MessageDirector();
		
		List<MessageConductor> conductors = new ArrayList<MessageConductor>();
		
		solrConductor = mock(SolrUpdateConductor.class);
		when(solrConductor.getIdentifier()).thenReturn(SolrUpdateConductor.identifier);
		enhancementConductor = mock(EnhancementConductor.class);
		when(enhancementConductor.getIdentifier()).thenReturn(EnhancementConductor.identifier);
		FailedEnhancementMap failedPids = mock(FailedEnhancementMap.class);
		when(failedPids.get(anyString())).thenReturn(null);
		when(enhancementConductor.getFailedPids()).thenReturn(failedPids);
		
		conductors.add(enhancementConductor);
		conductors.add(solrConductor);
		
		List<MessageFilter> filters = new ArrayList<MessageFilter>();
		filters.add(new SolrUpdateMessageFilter());
		ServicesQueueMessageFilter servicesFilter = new ServicesQueueMessageFilter();
		servicesFilter.setServices(services);
		servicesFilter.setenhancementConductor(enhancementConductor);
		filters.add(servicesFilter);
		messageDirector.setFilters(filters);
		
		messageDirector.setConductorsList(conductors);
	}
	
	class IsMatchingPID extends ArgumentMatcher<EnhancementMessage> {
		private String pid;
		
		public IsMatchingPID(String pid){
			this.pid = pid;
		}
		
      public boolean matches(Object pid) {
      	return ((EnhancementMessage) pid).getTargetID().startsWith(this.pid);
      }
   }
	
	@Test
	public void noServiceMessage(){
		EnhancementMessage message = new EnhancementMessage("cdr:test", JMSMessageUtil.servicesMessageNamespace, 
				JMSMessageUtil.ServicesActions.APPLY_SERVICE.getName(), "");
		messageDirector.direct(message);
		
		verify(solrConductor, never()).add(any(EnhancementMessage.class));
		verify(enhancementConductor, never()).add(any(EnhancementMessage.class));
	}
	
	@Test
	public void techmdServiceMessage(){
		EnhancementMessage message = new EnhancementMessage("cdr:test", JMSMessageUtil.servicesMessageNamespace, 
				JMSMessageUtil.ServicesActions.APPLY_SERVICE.getName(), TechnicalMetadataEnhancementService.class.getName());
		messageDirector.direct(message);
		
		verify(solrConductor, never()).add(any(EnhancementMessage.class));
		verify(enhancementConductor).add(any(EnhancementMessage.class));
	}
	
	@Test
	public void solrAddMessage(){
		ActionMessage message = new SolrUpdateRequest("cdr:test", IndexingActionType.ADD, null);
		messageDirector.direct(message);
		verify(solrConductor).add(any(ActionMessage.class));
		verify(enhancementConductor, never()).add(any(ActionMessage.class));
	}
	
	@Test
	public void nullMessage(){
		EnhancementMessage message = null;
		messageDirector.direct(message);
		verify(solrConductor, never()).add(any(EnhancementMessage.class));
		verify(enhancementConductor, never()).add(any(EnhancementMessage.class));
	}
}
