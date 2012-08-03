/**
 * Copyright 2008 The University of North Carolina at Chapel Hill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.unc.lib.dl.cdr.services;

import static edu.unc.lib.dl.util.FileUtils.tempCopy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;
import javax.mail.internet.MimeMessage;

import org.apache.commons.io.FileUtils;
import org.jdom.Document;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.ws.client.WebServiceTransportException;

import edu.unc.lib.dl.agents.PersonAgent;
import edu.unc.lib.dl.fedora.AccessClient;
import edu.unc.lib.dl.fedora.FedoraTimeoutException;
import edu.unc.lib.dl.fedora.ManagementClient;
import edu.unc.lib.dl.fedora.ManagementClient.Format;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.fedora.types.MIMETypedStream;
import edu.unc.lib.dl.ingest.aip.DepositRecord;
import edu.unc.lib.dl.ingest.sip.METSPackageSIP;
import edu.unc.lib.dl.services.DigitalObjectManagerImpl;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.util.DepositMethod;
import edu.unc.lib.dl.util.TripleStoreQueryService;

/**
 * @author Gregory Jansen
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/bean-context-ingest-junit.xml" })
public class BatchIngestServiceTest {

	@Resource private DigitalObjectManagerImpl digitalObjectManagerImpl;

	@Resource private BatchIngestService batchIngestService;

	@Resource private TripleStoreQueryService tripleStoreQueryService;

	@Resource private ManagementClient managementClient;

	@Resource AccessClient accessClient;

	@Resource(name="javaMailSender") JavaMailSender mailSender;

	@Resource JmsTemplate jmsTemplate;

	private static final String MD_CONTENTS = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
		+ "<m:structMap xmlns:m=\"http://www.loc.gov/METS/\">" + "<m:div TYPE=\"Container\">"
		+ "<m:div ID=\"test:delete\" ORDER=\"0\"/>" + "</m:div>" + "</m:structMap>";

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	private static final String MD_EVENTS = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
		+ "<premis xmlns=\"info:lc/xmlns/premis-v2\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">"
		+ "<object xsi:type=\"representation\">" + "<objectIdentifier>"
		+ "<objectIdentifierType>PID</objectIdentifierType>"
		+ "<objectIdentifierValue>test:container</objectIdentifierValue>" + "</objectIdentifier>" + "</object>"
		+ "</premis>";

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		this.batchIngestService.getBatchIngestQueue().init();
		MIMETypedStream mts = mock(MIMETypedStream.class);
		when(mts.getStream()).thenReturn(MD_CONTENTS.getBytes());
		when(accessClient.getDatastreamDissemination(any(PID.class), eq("MD_CONTENTS"), anyString())).thenReturn(mts);

		// setup default MD_EVENTS stream
		MIMETypedStream mts2 = mock(MIMETypedStream.class);
		when(mts2.getStream()).thenReturn(MD_EVENTS.getBytes());
		when(accessClient.getDatastreamDissemination(any(PID.class), eq("MD_EVENTS"), any(String.class)))
				.thenReturn(mts2);
		
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		FileUtils.deleteQuietly(this.batchIngestService.getBatchIngestQueue().getServiceDirectory());
	}

	/**
	 * Test method for {@link edu.unc.lib.dl.services.BatchIngestService#pauseQueue()}.
	 */
	@Test
	public void testPauseResumeQueue() {
		try {
			reset(this.managementClient);
			File test = tempCopy(new File("src/test/resources/simple.zip"));
			PersonAgent user = new PersonAgent(new PID("test:person"), "TestyTess", "testonyen");
			DepositRecord record = new DepositRecord(user, user, DepositMethod.Unspecified);
			PID container = new PID("test:container");
			METSPackageSIP sip1 = new METSPackageSIP(container, test, true);

			test = tempCopy(new File("src/test/resources/simple.zip"));
			METSPackageSIP sip2 = new METSPackageSIP(container, test, true);

			when(this.managementClient.pollForObject(any(PID.class), Mockito.anyInt(), Mockito.anyInt())).thenReturn(true);
			List<String> personrow = new ArrayList<String>();
			personrow.add(user.getPID().getURI());
			personrow.add(user.getName());
			personrow.add(user.getOnyen());
			List<List<String>> answer = new ArrayList<List<String>>();
			answer.add(personrow);
			when(this.tripleStoreQueryService.queryResourceIndex(any(String.class))).thenReturn(answer);
			when(this.tripleStoreQueryService.lookupRepositoryPath(eq(container))).thenReturn("/test/container/path");
			when(this.tripleStoreQueryService.fetchByRepositoryPath(eq("/test/container/path"))).thenReturn(container);
			when(this.tripleStoreQueryService.verify(any(PID.class))).thenReturn(container);

			when(this.managementClient.upload(any(File.class))).thenReturn("upload:19238");
			when(this.managementClient.upload(any(Document.class))).thenReturn("upload:19238");

			ArrayList<URI> ans = new ArrayList<URI>();
			ans.add(ContentModelHelper.Model.CONTAINER.getURI());
			when(this.tripleStoreQueryService.lookupContentModels(eq(container))).thenReturn(ans);

			this.batchIngestService.pause();
			digitalObjectManagerImpl.addToIngestQueue(sip1, record);
			Thread.sleep(5*1000);
			verify(this.managementClient, never()).ingest(any(Document.class), any(Format.class), any(String.class));

			this.batchIngestService.resume();
			do {
				Thread.sleep(5*1000);
			} while(this.batchIngestService.executor.getAllRunningAndQueued().size() > 0);
			verify(this.managementClient, times(14)).ingest(any(Document.class), any(Format.class), any(String.class));

			digitalObjectManagerImpl.addToIngestQueue(sip2, record);
			do {
				Thread.sleep(5*1000);
			} while(this.batchIngestService.executor.getAllRunningAndQueued().size() > 0);
			verify(this.managementClient, times(28)).ingest(any(Document.class), any(Format.class), any(String.class));
		} catch (Exception e) {
			e.printStackTrace();
			throw new Error(e);
		}
	}

	/**
	 * Test method for {@link edu.unc.lib.dl.services.BatchIngestService#queueBatch(java.io.File)}.
	 */
	@Test
	public void testQueueBatch() {
		try {
			reset(this.managementClient);
			reset(this.jmsTemplate);
			reset(this.mailSender);
			File test = tempCopy(new File("src/test/resources/simple.zip"));
			PersonAgent user = new PersonAgent(new PID("test:person"), "TestyTess", "testonyen");
			DepositRecord record = new DepositRecord(user, user, DepositMethod.Unspecified);
			PID container = new PID("test:container");
			METSPackageSIP sip = new METSPackageSIP(container, test, true);

			when(this.managementClient.pollForObject(any(PID.class), Mockito.anyInt(), Mockito.anyInt())).thenReturn(true);
			List<String> personrow = new ArrayList<String>();
			personrow.add(user.getPID().getURI());
			personrow.add(user.getName());
			personrow.add(user.getOnyen());
			List<List<String>> answer = new ArrayList<List<String>>();
			answer.add(personrow);
			when(this.tripleStoreQueryService.queryResourceIndex(any(String.class))).thenReturn(answer);
			when(this.tripleStoreQueryService.lookupRepositoryPath(eq(container))).thenReturn("/test/container/path");
			when(this.tripleStoreQueryService.fetchByRepositoryPath(eq("/test/container/path"))).thenReturn(container);
			when(this.tripleStoreQueryService.verify(any(PID.class))).thenReturn(container);

			when(this.managementClient.upload(any(File.class))).thenReturn("upload:19238");
			when(this.managementClient.upload(any(Document.class))).thenReturn("upload:19238");

			ArrayList<URI> ans = new ArrayList<URI>();
			ans.add(ContentModelHelper.Model.CONTAINER.getURI());
			when(this.tripleStoreQueryService.lookupContentModels(eq(container))).thenReturn(ans);

			digitalObjectManagerImpl.addToIngestQueue(sip, record);

			do {
				Thread.sleep(10*1000);
			} while(this.batchIngestService.executor.getAllRunningAndQueued().size() > 0);

			// verify batch ingest called
			verify(this.managementClient, times(14)).ingest(any(Document.class), any(Format.class), any(String.class));
			verify(this.jmsTemplate, times(1)).send(any(MessageCreator.class));
			verify(this.mailSender, times(1)).send(any(MimeMessage.class));
		} catch (Exception e) {
			e.printStackTrace();
			throw new Error(e);
		}
	}

	@Test
	public void testIngestWithFedora503Timeout() {
		try {
			reset(this.managementClient);
			reset(this.jmsTemplate);
			reset(this.mailSender);
			when(this.managementClient.pollForObject(any(PID.class), Mockito.anyInt(), Mockito.anyInt())).thenAnswer(new Answer<Boolean>() {

				@Override
				public Boolean answer(InvocationOnMock invocation) throws Throwable {
					System.out.println("Polling Fedora, we will wait 2 secs");
					Thread.sleep(2*1000);
					return true;
				}

			});
			File test = tempCopy(new File("src/test/resources/simple.zip"));
			PersonAgent user = new PersonAgent(new PID("test:person"), "TestyTess", "testonyen");
			DepositRecord record = new DepositRecord(user, user, DepositMethod.Unspecified);
			PID container = new PID("test:container");
			METSPackageSIP sip = new METSPackageSIP(container, test, true);

			// define the fedora ingest behavior:
			when(this.managementClient.ingest(any(Document.class), any(Format.class), any(String.class)))
				.thenCallRealMethod() //Return(new PID("test:simple"))
				.thenThrow(new FedoraTimeoutException(new WebServiceTransportException("Service Temporarily Unavailable [503]")))
				.thenCallRealMethod();

			List<String> personrow = new ArrayList<String>();
			personrow.add(user.getPID().getURI());
			personrow.add(user.getName());
			personrow.add(user.getOnyen());
			List<List<String>> answer = new ArrayList<List<String>>();
			answer.add(personrow);
			when(this.tripleStoreQueryService.queryResourceIndex(any(String.class))).thenReturn(answer);
			when(this.tripleStoreQueryService.lookupRepositoryPath(eq(container))).thenReturn("/test/container/path");
			when(this.tripleStoreQueryService.fetchByRepositoryPath(eq("/test/container/path"))).thenReturn(container);
			when(this.tripleStoreQueryService.verify(any(PID.class))).thenReturn(container);

			when(this.managementClient.upload(any(File.class))).thenReturn("upload:19238");
			when(this.managementClient.upload(any(Document.class))).thenReturn("upload:19238");

			ArrayList<URI> ans = new ArrayList<URI>();
			ans.add(ContentModelHelper.Model.CONTAINER.getURI());
			when(this.tripleStoreQueryService.lookupContentModels(eq(container))).thenReturn(ans);

			digitalObjectManagerImpl.addToIngestQueue(sip, record);

			do {
				Thread.sleep(5*1000);
			} while(this.batchIngestService.executor.getAllRunningAndQueued().size() > 0);

			// verify batch ingest called
			verify(this.managementClient, times(14)).ingest(any(Document.class), any(Format.class), any(String.class));
			//verify(this.managementClient, times(1)).addObjectRelationship(container, anyString(), any(PID.class));
			//verify(this.managementClient, times(1)).modifyDatastreamByReference(container, "MD_EVENTS", any(Boolean.class), anyString(),
					//any(List<String>.class), anyString(), anyString(), anyString(), anyString(), anyString());
			verify(this.jmsTemplate, times(1)).send(any(MessageCreator.class));
			verify(this.mailSender, times(1)).send(any(MimeMessage.class));
		} catch (Exception e) {
			e.printStackTrace();
			throw new Error(e);
		}
	}

	// TODO verify mail and jms
	// TODO verify container operations

//	@Test
//	public void testWithFedoraUnavailable() {
//		fail("Not yet implemented"); // TODO
//	}
//
//	@Test
//	public void testWithMismatchedChecksum() {
//		fail("Not yet implemented"); // TODO
//	}
}
