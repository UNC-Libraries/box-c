/**
 * Copyright 2011 The University of North Carolina at Chapel Hill
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
package edu.unc.lib.dl.services;

import static edu.unc.lib.dl.util.FileUtils.tempCopy;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

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
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.ws.client.WebServiceTransportException;

import edu.unc.lib.dl.agents.MockPersonAgent;
import edu.unc.lib.dl.agents.PersonAgent;
import edu.unc.lib.dl.fedora.AccessClient;
import edu.unc.lib.dl.fedora.FedoraTimeoutException;
import edu.unc.lib.dl.fedora.ManagementClient;
import edu.unc.lib.dl.fedora.ManagementClient.Format;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.fedora.types.MIMETypedStream;
import edu.unc.lib.dl.ingest.sip.METSPackageSIP;
import edu.unc.lib.dl.ingest.sip.SingleFolderSIP;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.util.TripleStoreQueryService;

/**
 * @author Gregory Jansen
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/service-context.xml" })
public class BatchIngestServiceTest {
	public static final String batchDir = "/tmp/batch-ingest";

	@Resource
	private DigitalObjectManagerImpl digitalObjectManagerImpl = null;

	@Resource(name = "realBatchIngestService")
	private BatchIngestService realBatchIngestService = null;

	private BatchIngestService originalMockService = null;

	@Resource
	private TripleStoreQueryService tripleStoreQueryService;

	@Resource
	private ManagementClient managementClient = null;

	@Resource
	private String propertiesURI = null;

	@Resource AccessClient accessClient = null;

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
		originalMockService = digitalObjectManagerImpl.getBatchIngestService();
		digitalObjectManagerImpl.setBatchIngestService(realBatchIngestService);

		MIMETypedStream mts = mock(MIMETypedStream.class);
		when(mts.getStream()).thenReturn(MD_CONTENTS.getBytes());
		when(accessClient.getDatastreamDissemination(any(PID.class), eq("MD_CONTENTS"), anyString())).thenReturn(mts);

		// setup default MD_EVENTS stream
		MIMETypedStream mts2 = mock(MIMETypedStream.class);
		when(mts2.getStream()).thenReturn(MD_EVENTS.getBytes());
		when(accessClient.getDatastreamDissemination(any(PID.class), eq("MD_EVENTS"), any(String.class)))
				.thenReturn(mts2);
		//FileUtils.deleteDir(new File(batchDir));
		//new File(batchDir).mkdir();
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		if (originalMockService != null) {
			digitalObjectManagerImpl.setBatchIngestService(originalMockService);
		}
	}

	/**
	 * Test method for {@link edu.unc.lib.dl.services.BatchIngestServiceImpl#pauseQueue()}.
	 */
	@Test
	public void testPauseResumeQueue() {
		try {
			reset(this.managementClient);
			File test = tempCopy(new File("src/test/resources/simple.zip"));
			PersonAgent user = new MockPersonAgent("TestyTess", "testonyen", new PID("test:person"));
			PID container = new PID("test:container");
			METSPackageSIP sip1 = new METSPackageSIP(container, test, user, true);

			test = tempCopy(new File("src/test/resources/simple.zip"));
			METSPackageSIP sip2 = new METSPackageSIP(container, test, user, true);

			test = tempCopy(new File("src/test/resources/simple.zip"));
			METSPackageSIP sip3 = new METSPackageSIP(container, test, user, true);

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

			ArrayList<URI> ans = new ArrayList<URI>();
			ans.add(ContentModelHelper.Model.CONTAINER.getURI());
			when(this.tripleStoreQueryService.lookupContentModels(eq(container))).thenReturn(ans);

			this.realBatchIngestService.pauseQueue();
			digitalObjectManagerImpl.addBatch(sip1, user, "testAdd1 for a good METS SIP");
			Thread.sleep(5*1000);
			verify(this.managementClient, never()).ingest(any(Document.class), any(Format.class), any(String.class));

			this.realBatchIngestService.startQueue();
			this.realBatchIngestService.waitUntilActive();
			this.realBatchIngestService.waitUntilIdle();
			verify(this.managementClient, times(14)).ingest(any(Document.class), any(Format.class), any(String.class));

			digitalObjectManagerImpl.addBatch(sip2, user, "testAdd2 for a good METS SIP");
			this.realBatchIngestService.waitUntilActive();
			this.realBatchIngestService.waitUntilIdle();
			verify(this.managementClient, times(28)).ingest(any(Document.class), any(Format.class), any(String.class));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new Error(e);
		}
	}

	/**
	 * Test method for {@link edu.unc.lib.dl.services.BatchIngestServiceImpl#queueBatch(java.io.File)}.
	 */
	@Test
	public void testQueueBatch() {
		try {
			reset(this.managementClient);
			File test = tempCopy(new File("src/test/resources/simple.zip"));
			PersonAgent user = new MockPersonAgent("TestyTess", "testonyen", new PID("test:person"));
			PID container = new PID("test:container");
			METSPackageSIP sip = new METSPackageSIP(container, test, user, true);

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

			ArrayList<URI> ans = new ArrayList<URI>();
			ans.add(ContentModelHelper.Model.CONTAINER.getURI());
			when(this.tripleStoreQueryService.lookupContentModels(eq(container))).thenReturn(ans);

			digitalObjectManagerImpl.addBatch(sip, user, "testAdd for a good METS SIP");

			this.realBatchIngestService.waitUntilActive();
			this.realBatchIngestService.waitUntilIdle();

			// verify batch ingest called
			verify(this.managementClient, times(14)).ingest(any(Document.class), any(Format.class), any(String.class));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new Error(e);
		}
	}

	/**
	 * Test method for {@link edu.unc.lib.dl.services.BatchIngestServiceImpl#ingestBatchNow(java.io.File)}.
	 */
	@Test
	public void testIngestBatchNow() {
		try {
			reset(this.managementClient);
			PersonAgent user = new MockPersonAgent("Testy Tess", "testonyen", new PID("test:person"));
			PID container = new PID("test:container");
			SingleFolderSIP sip = new SingleFolderSIP();
			sip.setContainerPID(container);
			sip.setOwner(user);
			sip.setSlug("testslug");

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

			ArrayList<URI> ans = new ArrayList<URI>();
			ans.add(ContentModelHelper.Model.CONTAINER.getURI());
			when(this.tripleStoreQueryService.lookupContentModels(eq(container))).thenReturn(ans);

			digitalObjectManagerImpl.addSingleObject(sip, user, "testing add single object (now)");

			// verify batch ingest called
			verify(this.managementClient, times(1)).ingest(any(Document.class), any(Format.class), any(String.class));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new Error(e);
		}
	}

	@Test
	public void testIngestWithFedora503Timeout() {
		try {
			reset(this.managementClient);
			when(this.managementClient.pollForObject(any(PID.class), Mockito.anyInt(), Mockito.anyInt())).thenAnswer(new Answer<Boolean>() {

				@Override
				public Boolean answer(InvocationOnMock invocation) throws Throwable {
					System.out.println("Polling Fedora, we will wait 2 secs");
					Thread.sleep(2*1000);
					return true;
				}

			});
			File test = tempCopy(new File("src/test/resources/simple.zip"));
			PersonAgent user = new MockPersonAgent("TestyTess", "testonyen", new PID("test:person"));
			PID container = new PID("test:container");
			METSPackageSIP sip = new METSPackageSIP(container, test, user, true);

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

			ArrayList<URI> ans = new ArrayList<URI>();
			ans.add(ContentModelHelper.Model.CONTAINER.getURI());
			when(this.tripleStoreQueryService.lookupContentModels(eq(container))).thenReturn(ans);

			digitalObjectManagerImpl.addBatch(sip, user, "testAdd for a good METS SIP");

			this.realBatchIngestService.waitUntilActive();
			this.realBatchIngestService.waitUntilIdle();

			// verify batch ingest called
			verify(this.managementClient, times(14)).ingest(any(Document.class), any(Format.class), any(String.class));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new Error(e);
		}
	}

	@Test
	public void testWithFedoraUnavailable() {
		fail("Not yet implemented"); // TODO
	}

	@Test
	public void testWithMismatchedChecksum() {
		fail("Not yet implemented"); // TODO
	}
}
