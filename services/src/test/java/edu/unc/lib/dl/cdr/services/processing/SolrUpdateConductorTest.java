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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.Mockito.*;

import org.jdom.Document;
import org.jdom.input.SAXBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;

import edu.unc.lib.dl.cdr.services.model.FedoraEventMessage;
import edu.unc.lib.dl.data.ingest.solr.BlockUntilTargetCompleteRequest;
import edu.unc.lib.dl.data.ingest.solr.CountDownUpdateRequest;
import edu.unc.lib.dl.data.ingest.solr.DeleteChildrenPriorToTimestampRequest;
import edu.unc.lib.dl.data.ingest.solr.SolrUpdateRequest;
import edu.unc.lib.dl.data.ingest.solr.SolrUpdateRunnableFactory;
import edu.unc.lib.dl.data.ingest.solr.UpdateDocTransformer;
import edu.unc.lib.dl.fedora.FedoraDataService;
import edu.unc.lib.dl.fedora.ManagementClient;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.search.solr.service.SolrSearchService;
import edu.unc.lib.dl.util.IndexingActionType;
import edu.unc.lib.dl.util.TripleStoreQueryService;

// TODO this test is going to fail until solrUpdateRunnableFactory is injected, which will require using a spring context
public class SolrUpdateConductorTest extends Assert {

	private SolrUpdateConductor solrUpdateConductor;
	private MessageDirector messageDirector;
	private TripleStoreQueryService tripleStoreQueryService;
	private Document simpleObject;
	private int numberTestMessages;
	private List<MessageFilter> filters;

	public SolrUpdateConductorTest() throws Exception {
		String simpleObjectXML = readFileAsString("fedoraObjectSimple.xml");
		SAXBuilder sb = new SAXBuilder();
		simpleObject = sb.build(new StringReader(simpleObjectXML));

		filters = new ArrayList<MessageFilter>();
		filters.add(new SolrUpdateMessageFilter());
	}

	@Before
	public void setUp() throws Exception {
		this.messageDirector = new MessageDirector();
		this.solrUpdateConductor = new SolrUpdateConductor();

		FedoraDataService fedoraDataService = mock(FedoraDataService.class);
		when(fedoraDataService.getObjectViewXML(startsWith("uuid:"))).thenReturn(simpleObject);
		when(fedoraDataService.getObjectViewXML(startsWith("uuid:"), anyBoolean())).thenReturn(simpleObject);

		tripleStoreQueryService = mock(TripleStoreQueryService.class);
		when(tripleStoreQueryService.isOrphaned(argThat(new IsMatchingPID("uuid:")))).thenReturn(false);
		when(tripleStoreQueryService.allowIndexing(argThat(new IsMatchingPID("uuid:")))).thenReturn(true);
		when(tripleStoreQueryService.fetchByRepositoryPath("/Collections")).thenReturn(new PID("collections"));

		when(fedoraDataService.getTripleStoreQueryService()).thenReturn(tripleStoreQueryService);

		//solrUpdateConductor.setUpdateDocTransformer(updateDocTransformer);
		solrUpdateConductor.setAutoCommit(false);
		solrUpdateConductor.setMaxThreads(3);

		List<MessageConductor> conductorsList = new ArrayList<MessageConductor>();
		conductorsList.add(solrUpdateConductor);

		messageDirector.setConductorsList(conductorsList);
		messageDirector.setFilters(filters);
		
		//SolrUpdateRunnableFactory solrUpdateRunnableFactory = ;

		solrUpdateConductor.init();
		solrUpdateConductor.clearState();

		numberTestMessages = 10;
	}

	//@Test
	public void modsModified() throws Exception {
		Document doc = readFileAsDocument("modifyMODSMessage.xml");
		FedoraEventMessage message = new FedoraEventMessage(doc);

		@SuppressWarnings("unchecked")
		BlockingQueue<SolrUpdateRequest> pidQueue = mock(BlockingQueue.class);
		solrUpdateConductor.setPidQueue(pidQueue);

		solrUpdateConductor.add(message);

		verify(pidQueue, times(1)).offer(any(SolrUpdateRequest.class));
	}

	//@Test
	public void ingestMessage() throws Exception {
		Document doc = readFileAsDocument("ingestMessage.xml");
		FedoraEventMessage message = new FedoraEventMessage(doc);

		@SuppressWarnings("unchecked")
		BlockingQueue<SolrUpdateRequest> pidQueue = mock(BlockingQueue.class);
		solrUpdateConductor.setPidQueue(pidQueue);

		solrUpdateConductor.add(message);

		verify(pidQueue, times(1)).offer(any(SolrUpdateRequest.class));
	}

	//@Test
	public void addRequests() throws Exception {
		SolrUpdateConductor solrUpdateConductor = this.solrUpdateConductor;
		// Add messages and check that they all ran
		for (int i = 0; i < numberTestMessages; i++) {
			SolrUpdateRequest message = new SolrUpdateRequest("uuid:" + i, IndexingActionType.ADD, null);
			messageDirector.direct(message);
		}
		while (!solrUpdateConductor.isEmpty())
			;

		assertEquals(solrUpdateConductor.getQueueSize(), 0);
		assertEquals(solrUpdateConductor.getLockedPids().size(), 0);
		//verify(solrUpdateConductor.getUpdateDocTransformer(), times(numberTestMessages)).addDocument(any(Document.class));
	}

	//@Test
	public synchronized void addCollisions() throws Exception {
		SolrUpdateConductor solrUpdateConductor = this.solrUpdateConductor;
		numberTestMessages = 4;

		BlockingManagementClient bmc = new BlockingManagementClient();

		// Check that collision list gets populated
		for (int i = 0; i < numberTestMessages; i++) {
			SolrUpdateRequest message = new SolrUpdateRequest("uuid:" + i, IndexingActionType.ADD, null);
			for (int j = 0; j < numberTestMessages; j++) {
				messageDirector.direct(message);
				assertTrue(solrUpdateConductor.getLockedPids().size() <= i + 1
						&& solrUpdateConductor.getLockedPids().size() <= solrUpdateConductor.getMaxThreads());
			}
		}

		while (solrUpdateConductor.getLockedPids().size() < solrUpdateConductor.getMaxThreads())
			;

		assertEquals(solrUpdateConductor.getLockedPids().size(), solrUpdateConductor.getMaxThreads());
		assertEquals(solrUpdateConductor.getCollisionList().size(), (solrUpdateConductor.getMaxThreads() - 1)
				* (numberTestMessages - 1));
		assertEquals(
				solrUpdateConductor.getPidQueue().size(),
				(numberTestMessages * numberTestMessages - (solrUpdateConductor.getMaxThreads() - 1) * numberTestMessages) - 1);
		assertEquals(solrUpdateConductor.getQueueSize(),
				(numberTestMessages * numberTestMessages) - solrUpdateConductor.getMaxThreads());

		while (bmc.count.get() < solrUpdateConductor.getMaxThreads())
			;

		// Process the remaining items to make sure all messages get processed.
		synchronized (bmc.blockingObject) {
			bmc.flag.set(false);
			bmc.blockingObject.notifyAll();
		}

		while (!solrUpdateConductor.isEmpty())
			;

		//verify(solrUpdateConductor.getUpdateDocTransformer(), times(numberTestMessages * numberTestMessages))
		//		.addDocument(any(Document.class));
	}

	//@Test
	public void clearState() throws InterruptedException {
		SolrUpdateConductor solrUpdateConductor = this.solrUpdateConductor;
		solrUpdateConductor.pause();
		for (int i = 0; i < numberTestMessages; i++) {
			SolrUpdateRequest message = new SolrUpdateRequest("uuid:" + i, IndexingActionType.ADD, null);
			messageDirector.direct(message);
		}
		solrUpdateConductor.resume();
		Thread.sleep(10L);
		solrUpdateConductor.pause();
		solrUpdateConductor.clearState();
		assertTrue(solrUpdateConductor.getPidQueue().size() == 0);
		assertTrue(solrUpdateConductor.getCollisionList().size() == 0);
		assertTrue(solrUpdateConductor.getLockedPids().size() == 0);
		assertTrue(solrUpdateConductor.getThreadPoolExecutor().getQueue().size() == 0);
	}

	// @Test
	public void stressBlockingRequests() throws Exception {
		for (int i = 0; i < 50; i++) {
			blockingRequests();
			while (!solrUpdateConductor.isEmpty())
				;
			setUp();
		}
	}

	//@Test
	public void blockingRequests() throws Exception {
		SolrUpdateConductor solrUpdateConductor = this.solrUpdateConductor;
		solrUpdateConductor.pause();

		// Create a blocked message and make sure that it doesn't get picked up until
		int numberTestMessages = 5;
		SolrUpdateRequest parentRequest = new SolrUpdateRequest("uuid:parent", IndexingActionType.ADD);
		
		SolrUpdateRequest blockedRequest = new BlockUntilTargetCompleteRequest("uuid:blocked",
				IndexingActionType.ADD, "urn:uuid:msg", null, parentRequest);

		for (int i = 0; i < numberTestMessages; i++) {
			SolrUpdateRequest childRequest = new SolrUpdateRequest("uuid:1", IndexingActionType.ADD, blockedRequest, null,
					null);
			solrUpdateConductor.offer(childRequest);
		}
		for (int i = 0; i < numberTestMessages; i++) {
			SolrUpdateRequest childRequest = new SolrUpdateRequest("uuid:2", IndexingActionType.ADD, blockedRequest, null,
					null);
			solrUpdateConductor.offer(childRequest);
		}

		solrUpdateConductor.offer(blockedRequest);

		// Add some post block non-blocked messages
		for (int i = 0; i < numberTestMessages; i++) {
			SolrUpdateRequest childRequest = new SolrUpdateRequest("uuid:3", IndexingActionType.ADD);
			solrUpdateConductor.offer(childRequest);
		}

		solrUpdateConductor.resume();
		while (((DeleteChildrenPriorToTimestampRequest) blockedRequest).isBlocked()) {
			synchronized (solrUpdateConductor.getLockedPids()) {
				if (((DeleteChildrenPriorToTimestampRequest) blockedRequest).isBlocked())
					assertFalse(solrUpdateConductor.getLockedPids().contains("uuid:blocked"));
			}
		}
		while (!solrUpdateConductor.isEmpty())
			;
		//verify(solrUpdateConductor.getUpdateDocTransformer(), times(numberTestMessages * 3 + 1)).addDocument(
		//		any(Document.class));
	}

	//@Test
	public void pauseExecutor() throws Exception {
		SolrUpdateConductor solrUpdateConductor = this.solrUpdateConductor;
		// Test that nothing processes while paused
		solrUpdateConductor.pause();
		for (int i = 0; i < numberTestMessages; i++) {
			SolrUpdateRequest message = new SolrUpdateRequest("uuid:" + i, IndexingActionType.ADD);
			messageDirector.direct(message);
		}
		assertEquals(solrUpdateConductor.getQueueSize(), numberTestMessages);
		assertEquals(solrUpdateConductor.getLockedPids().size(), 0);
		assertFalse(solrUpdateConductor.isEmpty());
		assertTrue(solrUpdateConductor.isReady());
		assertTrue(solrUpdateConductor.isIdle());
		//verify(solrUpdateConductor.getUpdateDocTransformer(), never()).addDocument(any(Document.class));

		// Ensure paused items process when unpaused
		solrUpdateConductor.resume();
		while (!solrUpdateConductor.isEmpty())
			;
		//verify(solrUpdateConductor.getUpdateDocTransformer(), times(numberTestMessages)).addDocument(any(Document.class));
		assertTrue(solrUpdateConductor.isEmpty());
	}

	//@Test
	public void executorShutdown() {
		SolrUpdateConductor solrUpdateConductor = this.solrUpdateConductor;
		solrUpdateConductor.pause();
		for (int i = 0; i < numberTestMessages; i++) {
			SolrUpdateRequest message = new SolrUpdateRequest("uuid:" + i, IndexingActionType.ADD);
			messageDirector.direct(message);
		}

		assertEquals(solrUpdateConductor.getThreadPoolExecutor().getQueue().size(), numberTestMessages
				- solrUpdateConductor.getMaxThreads());

		solrUpdateConductor.shutdownNow();
		while (solrUpdateConductor.getThreadPoolExecutor().isTerminating()
				&& !solrUpdateConductor.getThreadPoolExecutor().isShutdown())
			;
		assertEquals(solrUpdateConductor.getThreadPoolExecutor().getQueue().size(), 0);
		assertFalse(solrUpdateConductor.isReady());
	}

	// @Test
	public void stressAbortOperation() throws Exception {
		for (int i = 0; i < 50; i++) {
			abortOperation();
			setUp();
		}
	}

	//@Test
	public void abortOperation() {
		SolrUpdateConductor solrUpdateConductor = this.solrUpdateConductor;
		int numberTestMessages = 20;

		BlockingManagementClient bmc = new BlockingManagementClient();

		// Add messages and check that they all ran
		for (int i = 0; i < numberTestMessages; i++) {
			SolrUpdateRequest message = new SolrUpdateRequest("uuid:" + i, IndexingActionType.ADD);
			messageDirector.direct(message);
		}

		while (solrUpdateConductor.getLockedPids().size() < solrUpdateConductor.getMaxThreads()
				&& !solrUpdateConductor.isEmpty())
			;
		solrUpdateConductor.pause();
		solrUpdateConductor.abort();
		while (solrUpdateConductor.getThreadPoolExecutor().getQueue().size() == 0)
			;
		assertTrue(solrUpdateConductor.isIdle());
		assertEquals(solrUpdateConductor.getLockedPids().size(), 0);

		// Process the remaining items to make sure all messages get processed.
		synchronized (bmc.blockingObject) {
			bmc.flag.set(false);
			bmc.blockingObject.notifyAll();
		}

		solrUpdateConductor.resume();
		while (!solrUpdateConductor.isEmpty())
			;
		assertTrue(solrUpdateConductor.isIdle());
		// Abort for solr updates only are dropped from interruption if it happens during grabbing next request
	}

	class IsMatchingPID extends ArgumentMatcher<PID> {
		private String pid;

		public IsMatchingPID(String pid) {
			this.pid = pid;
		}

		public boolean matches(Object pid) {
			return ((PID) pid).getPid().startsWith(this.pid);
		}
	}

	protected String readFileAsString(String filePath) throws java.io.IOException {
		StringBuffer fileData = new StringBuffer(1000);
		java.io.InputStream inStream = this.getClass().getResourceAsStream(filePath);
		java.io.InputStreamReader inStreamReader = new InputStreamReader(inStream);
		BufferedReader reader = new BufferedReader(inStreamReader);
		// BufferedReader reader = new BufferedReader(new
		// InputStreamReader(this.getClass().getResourceAsStream(filePath)));
		char[] buf = new char[1024];
		int numRead = 0;
		while ((numRead = reader.read(buf)) != -1) {
			String readData = String.valueOf(buf, 0, numRead);
			fileData.append(readData);
			buf = new char[1024];
		}
		reader.close();
		return fileData.toString();
	}

	private Document readFileAsDocument(String filePath) throws Exception {
		return new SAXBuilder().build(new InputStreamReader(this.getClass().getResourceAsStream(filePath)));
	}

	public class BlockingManagementClient extends ManagementClient {
		public AtomicInteger count;
		public AtomicBoolean flag;
		public Object blockingObject;

		public BlockingManagementClient() {
			count = new AtomicInteger(0);
			flag = new AtomicBoolean(true);
			blockingObject = new Object();
		}

		@Override
		public Document getObjectXML(PID pid) {
			boolean first = true;
			while (flag.get()) {
				synchronized (blockingObject) {
					try {
						if (first) {
							count.incrementAndGet();
							first = false;
						}
						blockingObject.wait();
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						return null;
					}
				}
			}
			return simpleObject;
		}
	};
}
