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
package edu.unc.lib.dl.cdr.sword.managers;

import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import org.swordapp.server.AuthCredentials;
import org.swordapp.server.DepositReceipt;
import org.swordapp.server.SwordAuthException;

import edu.unc.lib.dl.agents.AgentFactory;
import edu.unc.lib.dl.agents.PersonAgent;
import edu.unc.lib.dl.cdr.sword.server.SwordConfigurationImpl;
import edu.unc.lib.dl.cdr.sword.server.managers.ContainerManagerImpl;
import edu.unc.lib.dl.cdr.sword.server.util.DepositReportingUtil;
import edu.unc.lib.dl.fedora.AccessClient;
import edu.unc.lib.dl.fedora.AccessControlUtils;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.fedora.types.MIMETypedStream;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.util.TripleStoreQueryService;

import static org.mockito.Mockito.*;

public class ContainerManagerTest extends Assert {

	private SwordConfigurationImpl config;
	
	public ContainerManagerTest(){
		config = new SwordConfigurationImpl();
		config.setBasePath("http://localhost");
		config.setSwordPath("http://localhost/sword");
	}
	
	
	@SuppressWarnings("unchecked")
	@Test
	public void getEntryCredentials() throws Exception {
		DepositReceipt resultReceipt = mock(DepositReceipt.class);
		
		DepositReportingUtil depositReportingUtil = mock(DepositReportingUtil.class);
		when(depositReportingUtil.retrieveDepositReceipt(any(PID.class), any(SwordConfigurationImpl.class))).thenReturn(resultReceipt);
		
		PID pid = new PID("uuid:test");
		
		TripleStoreQueryService tripleStoreQueryService = mock(TripleStoreQueryService.class);
		
		Map<String,String> disseminations = new HashMap<String,String>();
		disseminations.put(pid.getURI() + "/" + ContentModelHelper.Datastream.MD_DESCRIPTIVE.getName(), "text/xml");
		disseminations.put(pid.getURI() + "/" + ContentModelHelper.Datastream.DATA_FILE.getName(), "image/jpg");
		
		when(tripleStoreQueryService.fetchDisseminatorMimetypes(any(PID.class))).thenReturn(disseminations);
		
		RandomAccessFile modsFile = new RandomAccessFile("src/test/resources/modsDocument.xml", "r");
		byte[] modsBytes = new byte[(int)modsFile.length()];
		modsFile.read(modsBytes);
		MIMETypedStream mimeStream = new MIMETypedStream();
		mimeStream.setStream(modsBytes);
		
		AgentFactory agentFactory = mock(AgentFactory.class);
		when(agentFactory.findPersonByOnyen(anyString(), anyBoolean())).thenReturn(new PersonAgent("testuser", "testuser"));
		
		AccessControlUtils accessControlUtils = mock(AccessControlUtils.class);
		when(accessControlUtils.hasAccess(any(PID.class), anyCollection(),anyString())).thenReturn(true);
		
		AccessClient accessClient = mock(AccessClient.class);
		when(accessClient.getDatastreamDissemination(any(PID.class), eq(ContentModelHelper.Datastream.MD_DESCRIPTIVE.getName()), anyString())).thenReturn(mimeStream);
		
		ContainerManagerImpl containerManager = new ContainerManagerImpl();
		containerManager.setDepositReportingUtil(depositReportingUtil);
		containerManager.setTripleStoreQueryService(tripleStoreQueryService);
		containerManager.setAccessClient(accessClient);
		containerManager.setAgentFactory(agentFactory);
		containerManager.setAccessControlUtils(accessControlUtils);
		
		String editIRI = "http://localhost"  + SwordConfigurationImpl.EDIT_PATH + "/" + pid.getPid();
		
		AuthCredentials auth = new AuthCredentials("testuser", "", null);
		
		DepositReceipt receipt = containerManager.getEntry(editIRI, null, auth, config);
		
		assertNotNull(receipt);
		
		when(agentFactory.findPersonByOnyen(anyString(), anyBoolean())).thenReturn(null);
		
		try {
			receipt = containerManager.getEntry(editIRI, null, auth, config);
			fail();
		} catch (SwordAuthException e){
			//pass
		}
		
		when(agentFactory.findPersonByOnyen(anyString(), anyBoolean())).thenReturn(new PersonAgent("testuser", "testuser"));
		when(accessControlUtils.hasAccess(any(PID.class), anyCollection(),anyString())).thenReturn(false);
		try {
			receipt = containerManager.getEntry(editIRI, null, auth, config);
			fail();
		} catch (SwordAuthException e){
			//pass
		}
	}
}
