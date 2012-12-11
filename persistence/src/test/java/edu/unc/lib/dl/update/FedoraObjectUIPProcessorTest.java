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
package edu.unc.lib.dl.update;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import edu.unc.lib.dl.agents.Agent;
import edu.unc.lib.dl.agents.PersonAgent;
import edu.unc.lib.dl.fedora.AccessClient;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.services.DigitalObjectManager;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.util.ContentModelHelper.Datastream;

public class FedoraObjectUIPProcessorTest extends Assert {

	@Test
	public void invalidDatastreamAndMissingContent() throws Exception {
		AccessClient accessClient = mock(AccessClient.class);
		when(accessClient.getDatastreamDissemination(any(PID.class), anyString(), anyString())).thenReturn(null);
		
		UIPUpdatePipeline pipeline = mock(UIPUpdatePipeline.class);
		DigitalObjectManager digitalObjectManager = mock(DigitalObjectManager.class);
		
		FedoraObjectUIPProcessor uipProcessor = new FedoraObjectUIPProcessor();
		uipProcessor.setAccessClient(accessClient);
		uipProcessor.setDigitalObjectManager(digitalObjectManager);
		uipProcessor.setPipeline(pipeline);
		
		
		PID pid = new PID("uuid:test");
		PersonAgent user = new PersonAgent("testuser", "testuser");
		
		Map<String,File> modifiedFiles = new HashMap<String,File>();
		modifiedFiles.put(ContentModelHelper.Datastream.MD_DESCRIPTIVE.getName(), mock(File.class));
		modifiedFiles.put(ContentModelHelper.Datastream.MD_TECHNICAL.getName(), null);
		modifiedFiles.put(ContentModelHelper.Datastream.AUDIT.getName(), mock(File.class));
		modifiedFiles.put("INVALID", mock(File.class));
		
		FedoraObjectUIP uip = mock(FedoraObjectUIP.class);
		when(uip.getModifiedFiles()).thenReturn(modifiedFiles);
		when(uip.getPID()).thenReturn(pid);
		when(uip.getUser()).thenReturn(user);
		when(uip.getOperation()).thenReturn(UpdateOperation.ADD);
		
		when(pipeline.processUIP(uip)).thenReturn(uip);
		
		uipProcessor.process(uip);
		
		verify(uip, times(1)).getModifiedFiles();
		verify(uip, times(1)).storeOriginalDatastreams(any(AccessClient.class));
		verify(digitalObjectManager, times(2)).addOrReplaceDatastream(any(PID.class), any(Datastream.class), any(File.class), anyString(), any(Agent.class), anyString());
		verify(digitalObjectManager, times(1)).addOrReplaceDatastream(any(PID.class), eq(Datastream.AUDIT), any(File.class), anyString(), any(Agent.class), anyString());
		verify(digitalObjectManager, times(1)).addOrReplaceDatastream(any(PID.class), eq(Datastream.MD_DESCRIPTIVE), any(File.class), anyString(), any(Agent.class), anyString());
		
		//Check reaction to null modified files, shouldn't do any updates
		when(uip.getModifiedFiles()).thenReturn(null);
		uipProcessor.process(uip);
		verify(digitalObjectManager, times(2)).addOrReplaceDatastream(any(PID.class), any(Datastream.class), any(File.class), anyString(), any(Agent.class), anyString());
	}
}
