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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import edu.unc.lib.dl.cdr.services.model.PIDMessage;
import edu.unc.lib.dl.cdr.services.techmd.TechnicalMetadataEnhancementService;
import edu.unc.lib.dl.cdr.services.util.JMSMessageUtil;
import edu.unc.lib.dl.data.ingest.solr.SolrUpdateAction;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/service-context-unit.xml" })
public class MessageDirectorTest extends Assert {

	@Resource
	private MessageDirector messageDirector;
	@Resource
	private DummyMessageConductor solrDummyConductor;
	@Resource
	private DummyMessageConductor servicesDummyConductor;

	@Test
	public void directTest(){
		PIDMessage message = new PIDMessage("cdr:test", JMSMessageUtil.servicesMessageNamespace, 
				JMSMessageUtil.ServicesActions.APPLY_SERVICE.getName(), "");
		messageDirector.direct(message);
		
		assertTrue(solrDummyConductor.getMessageList().size() == 0);
		assertTrue(servicesDummyConductor.getMessageList().size() == 0);
		
		message.setServiceName(TechnicalMetadataEnhancementService.class.getName());
		messageDirector.direct(message);
		assertTrue(solrDummyConductor.getMessageList().size() == 0);
		assertTrue(servicesDummyConductor.getMessageList().size() == 1);
		
		message = new PIDMessage("cdr:test", SolrUpdateAction.namespace, SolrUpdateAction.ADD.getName());
		messageDirector.direct(message);
		assertTrue(solrDummyConductor.getMessageList().size() == 1);
		assertTrue(servicesDummyConductor.getMessageList().size() == 1);
		
		message = null;
		messageDirector.direct(message);
		assertTrue(solrDummyConductor.getMessageList().size() == 1);
		assertTrue(servicesDummyConductor.getMessageList().size() == 1);
	}
	
	public MessageDirector getMessageDirector() {
		return messageDirector;
	}

	public void setMessageDirector(MessageDirector messageDirector) {
		this.messageDirector = messageDirector;
	}

	public DummyMessageConductor getSolrDummyConductor() {
		return solrDummyConductor;
	}

	public void setSolrDummyConductor(DummyMessageConductor solrDummyConductor) {
		this.solrDummyConductor = solrDummyConductor;
	}

	public DummyMessageConductor getServicesDummyConductor() {
		return servicesDummyConductor;
	}

	public void setServicesDummyConductor(DummyMessageConductor servicesDummyConductor) {
		this.servicesDummyConductor = servicesDummyConductor;
	}
}
