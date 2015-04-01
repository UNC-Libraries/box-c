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
package edu.unc.lib.deposit.fcrepo3;

import static edu.unc.lib.dl.test.TestHelpers.setField;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.internal.util.collections.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Bag;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.tdb.TDBFactory;

import edu.unc.lib.deposit.work.JobFailedException;
import edu.unc.lib.dl.fedora.AccessClient;
import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.fedora.FedoraTimeoutException;
import edu.unc.lib.dl.fedora.JobForwardingJMSListener;
import edu.unc.lib.dl.fedora.ListenerJob;
import edu.unc.lib.dl.fedora.ManagementClient;
import edu.unc.lib.dl.fedora.ManagementClient.Format;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.fedora.types.ObjectProfile;
import edu.unc.lib.dl.util.DepositStatusFactory;
import edu.unc.lib.dl.util.JobStatusFactory;
import edu.unc.lib.dl.util.PremisEventLogger;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositField;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositState;

/**
 * @author bbpennel
 * @date Mar 21, 2014
 */
public class IngestDepositTest {

	private static final Logger log = LoggerFactory.getLogger(IngestDepositTest.class);

	@Rule
	public final TemporaryFolder tmpFolder = new TemporaryFolder();

	private File depositsDirectory;

	@Mock
	private JobStatusFactory jobStatusFactory;
	@Mock
	private DepositStatusFactory depositStatusFactory;
	@Mock
	private ManagementClient client;
	@Mock
	private AccessClient accessClient;

	private Map<String, String> depositStatus;

	private FinishIngestsMockListener jmsListener;

	@Mock
	private Document messageDocument;
	@Mock
	private Element messageRoot;
	@Mock
	private Element messageSummary;
	@Mock
	Collection<String> ingestsAwaitingConfirmation;

	private IngestDeposit job;

	@Before
	public void setup() throws Exception {
		initMocks(this);

		when(messageDocument.getRootElement()).thenReturn(messageRoot);
		when(messageRoot.getChildTextTrim(eq("title"), any(Namespace.class))).thenReturn("ingest");
		when(messageRoot.getChild(eq("summary"), any(Namespace.class))).thenReturn(messageSummary);

		depositStatus = new HashMap<>();
		when(depositStatusFactory.get(anyString())).thenReturn(depositStatus);
		when(depositStatusFactory.getState(anyString())).thenReturn(DepositState.running);
		depositStatus.put(DepositField.permissionGroups.name(), "group");

		when(client.upload(any(Document.class))).thenReturn("uploadpath");
		when(client.upload(any(File.class))).thenReturn("uploadpath");

		depositsDirectory = tmpFolder.newFolder("deposits");

		createJob("bd5ff703-9c2e-466b-b4cc-15bbfd03c8ae", "/ingestDeposit");
	}

	private void createJob(String depositUuid, String depositDirectoryPath) throws Exception {

		// Clone the deposit directory
		File depositFolder = new File(depositsDirectory, depositUuid);
		File originalDepositDirectory = new File(getClass().getResource(depositDirectoryPath).toURI());
		FileUtils.copyDirectory(originalDepositDirectory, depositFolder);

		job = new IngestDeposit("jobuuid", depositUuid);
		jmsListener = new FinishIngestsMockListener(job);
		Dataset dataset = TDBFactory.createDataset();

		job.setListener(jmsListener);
		setField(job, "dataset", dataset);
		setField(job, "depositsDirectory", depositsDirectory);
		setField(job, "jobStatusFactory", jobStatusFactory);
		setField(job, "depositStatusFactory", depositStatusFactory);
		setField(job, "client", client);
		setField(job, "accessClient", accessClient);

		depositStatus.put(DepositField.containerId.name(), "uuid:destination");
		depositStatus.put(DepositField.excludeDepositRecord.name(), "false");

		job.init();
		
		Model model = job.getWritableModel();
		model.read(new File(depositFolder, "everything.n3").getAbsolutePath());
		job.closeModel();
		
	}
	
	@Test
	public void testDuplicateBagEntry() throws Exception {
		
		// Add a duplicate bag entry
		
		Model model = job.getWritableModel();
		Bag bag = model.getBag("info:fedora/uuid:bd5ff703-9c2e-466b-b4cc-15bbfd03c8ae");
		Resource resource = model.getResource("info:fedora/uuid:1faf9dbd-2c34-431b-a1e1-319437871b43");
		bag.add(resource);
		job.closeModel();
		
		Thread jobThread = new Thread(job);
		Thread finishThread = new Thread(jmsListener);

		jobThread.start();
		finishThread.start();

		// Start processing with a timelimit to prevent infinite wait in case of failure
		
		jobThread.join(5000L);
		finishThread.join(5000L);
		
		// Ensure that we counted and ingested the correct number of objects, even though one of the bags had a duplicate
		
		assertEquals("Incorrect number of objects counted", 9, job.getIngestObjectCount());
		verify(client, times(job.getIngestObjectCount() + 1)).ingestRaw(any(byte[].class), any(Format.class), anyString());
		
	}

	@Test
	public void testOnEventInvalidAction() {
		when(messageRoot.getChildTextTrim(eq("title"), any(Namespace.class))).thenReturn("remove");

		setField(job, "ingestsAwaitingConfirmation", ingestsAwaitingConfirmation);

		job.onEvent(messageDocument);

		verify(ingestsAwaitingConfirmation, never()).remove(any(String.class));
	}

	@Test
	public void testOnEventPIDNotRegistered() {

		setField(job, "ingestsAwaitingConfirmation", ingestsAwaitingConfirmation);
		when(ingestsAwaitingConfirmation.remove(any(String.class))).thenReturn(false);

		when(messageSummary.getText()).thenReturn("uuid:pid");

		job.onEvent(messageDocument);

		verify(ingestsAwaitingConfirmation).remove(any(String.class));

		verify(jobStatusFactory, never()).incrCompletion(anyString(), eq(1));
	}

	@Test
	public void testRunValidStructure() throws Exception {

		Thread jobThread = new Thread(job);
		Thread finishThread = new Thread(jmsListener);

		jobThread.start();
		finishThread.start();

		// Start processing with a timelimit to prevent infinite wait in case of failure
		jobThread.join(5000L);
		finishThread.join(5000L);

		assertTrue("Job must have been registered", jmsListener.registeredJob);
		assertTrue("Job must be unregistered", jmsListener.unregisteredJob);

		assertEquals("All ingest pids should have been removed", 0, job.getIngestPids().size());
		assertEquals("Top level pids should be present", 2, job.getTopLevelPids().size());

		assertEquals("Incorrect number of objects extracted", 9, job.getIngestObjectCount());

		// Clicks should have been registered
		verify(jobStatusFactory, times(job.getIngestObjectCount() + 1)).incrCompletion(eq(job.getJobUUID()), eq(1));

		verify(client, times(job.getTopLevelPids().size()))
				.addObjectRelationship(any(PID.class), anyString(), any(PID.class));

		verify(client, times(job.getIngestObjectCount() + 1))
				.ingestRaw(any(byte[].class), any(Format.class), anyString());

		// Two of the objects are containers with no data
		verify(client, times(job.getIngestObjectCount() - 2)).upload(any(File.class));

		// PREMIS was written
		verify(client, times(1)).writePremisEventsToFedoraObject(any(PremisEventLogger.class), eq(new PID(depositStatus.get(DepositField.containerId.name()))));

	}

	@Test
	public void testRunFailObjectIngest() throws Exception {

		when(client.ingestRaw(any(byte[].class), any(Format.class), anyString())).thenReturn(new PID("pid"))
				.thenReturn(new PID("pid")).thenThrow(new FedoraException(""));

		Thread jobThread = new Thread(job);
		final boolean[] exceptionCaught = new boolean[] { false };
		Thread.UncaughtExceptionHandler jobFailedHandler = new Thread.UncaughtExceptionHandler() {
			@Override
			public void uncaughtException(Thread th, Throwable ex) {
				if (ex instanceof JobFailedException)
					exceptionCaught[0] = true;
			}
		};

		jobThread.setUncaughtExceptionHandler(jobFailedHandler);
		jobThread.start();

		// Start processing with a timelimit to prevent infinite wait in case of failure
		jobThread.join();

		// Only the one successful top level pid added because of ordering
		verify(client).addObjectRelationship(any(PID.class), anyString(), any(PID.class));

		// Failing on third ingestRaw
		verify(client, times(3)).ingestRaw(any(byte[].class), any(Format.class), anyString());

		// Only one object with data should have been uploaded
		verify(client).upload(any(File.class));

		// PREMIS was written
		verify(client, times(0)).writePremisEventsToFedoraObject(any(PremisEventLogger.class), any(PID.class));

		assertTrue("Job must have been registered", jmsListener.registeredJob);
		assertTrue("Job must have been unregistered on failure", jmsListener.registeredJob);

		assertTrue("Exception must have been thrown by job", exceptionCaught[0]);

	}

	@Test
	public void testRunIngestTimeout() throws Exception {

		when(client.ingestRaw(any(byte[].class), any(Format.class), anyString())).thenReturn(new PID("pid"))
				.thenReturn(new PID("pid")).thenThrow(new FedoraTimeoutException(new Exception()))
				.thenReturn(new PID("pid"));

		Thread.UncaughtExceptionHandler jobFailedHandler = new Thread.UncaughtExceptionHandler() {
			@Override
			public void uncaughtException(Thread th, Throwable ex) {
				fail("Uncaught exception, job should have completed.");
			}
		};

		Thread jobThread = new Thread(job);
		Thread finishThread = new Thread(jmsListener);

		jobThread.setUncaughtExceptionHandler(jobFailedHandler);

		jobThread.start();
		finishThread.start();

		// Start processing with a timelimit to prevent infinite wait in case of failure
		jobThread.join(5000L);
		finishThread.join(5000L);

		// All ingests, including the timed out object, should have registered as a click
		verify(jobStatusFactory, times(job.getIngestObjectCount() + 1)).incrCompletion(eq(job.getJobUUID()), eq(1));

		// All objects should have been ingested despite the timeout
		verify(client, times(job.getIngestObjectCount() + 1))
				.ingestRaw(any(byte[].class), any(Format.class), anyString());

		assertTrue("Job must have been registered", jmsListener.registeredJob);
		assertTrue("Job must have been unregistered", jmsListener.registeredJob);

	}

	@Test
	public void testRunExcludeDepositRecord() throws Exception {

		depositStatus.put(DepositField.excludeDepositRecord.name(), "true");

		Thread jobThread = new Thread(job);
		Thread finishThread = new Thread(jmsListener);

		jobThread.start();
		finishThread.start();

		// Start processing with a timelimit to prevent infinite wait in case of failure
		jobThread.join(5000L);
		finishThread.join(5000L);

		assertTrue("Job must have been registered", jmsListener.registeredJob);
		assertTrue("Job must be unregistered", jmsListener.unregisteredJob);

		assertEquals("All ingest pids should have been removed", 0, job.getIngestPids().size());
		assertEquals("Top level pids should be present", 2, job.getTopLevelPids().size());

		assertEquals("Incorrect number of objects extracted", 9, job.getIngestObjectCount());

		// Clicks should have been registered
		verify(jobStatusFactory, times(job.getIngestObjectCount())).incrCompletion(eq(job.getJobUUID()), eq(1));

		verify(client, times(job.getTopLevelPids().size())).addObjectRelationship(any(PID.class), anyString(),
				any(PID.class));

		verify(client, times(job.getIngestObjectCount())).ingestRaw(any(byte[].class), any(Format.class), anyString());

		// Two of the objects are containers with no data
		verify(client, times(job.getIngestObjectCount() - 2)).upload(any(File.class));

		// PREMIS was written
		verify(client, times(1)).writePremisEventsToFedoraObject(any(PremisEventLogger.class), eq(new PID(depositStatus.get(DepositField.containerId.name()))));

	}

	@Test
	public void testResume() throws Exception {

		when(depositStatusFactory.isResumedDeposit(anyString())).thenReturn(true);
		when(depositStatusFactory.getUnconfirmedUploads(anyString())).thenReturn(new HashSet<String>());
		when(depositStatusFactory.getConfirmedUploads(anyString())).thenReturn(
				Sets.newSet("uuid:2a5d0363-899b-402d-981b-392a553e17a1", "uuid:7dd57979-084c-4b5a-acc0-a0eed25d2b33"));

		Thread jobThread = new Thread(job);
		Thread finishThread = new Thread(jmsListener);

		jobThread.start();
		finishThread.start();

		// Start processing with a timelimit to prevent infinite wait in case of failure
		jobThread.join(5000L);
		finishThread.join(5000L);

		assertTrue("Job must have been registered", jmsListener.registeredJob);
		assertTrue("Job must be unregistered", jmsListener.unregisteredJob);

		assertEquals("All ingest pids should have been removed", 0, job.getIngestPids().size());

		assertEquals("Ingest count must exclude the already completed items", 7, job.getIngestObjectCount());

		// Clicks should have been registered
		verify(jobStatusFactory, times(job.getIngestObjectCount() + 1)).incrCompletion(eq(job.getJobUUID()), eq(1));

		verify(client, times(job.getTopLevelPids().size())).addObjectRelationship(any(PID.class), anyString(),
				any(PID.class));

		verify(client, times(job.getIngestObjectCount() + 1))
				.ingestRaw(any(byte[].class), any(Format.class), anyString());

	}

	@Test
	public void testResumeUnconfirmed() throws Exception {

		when(accessClient.getObjectProfile(any(PID.class), anyString()))
				.thenReturn(mock(ObjectProfile.class)).thenThrow(new FedoraException(""));

		when(depositStatusFactory.isResumedDeposit(anyString())).thenReturn(true);
		when(depositStatusFactory.getUnconfirmedUploads(anyString())).thenReturn(
				Sets.newSet("uuid:3a8650f6-5de6-41b5-8ea1-42ac5e9a9687", "uuid:0a83ed37-6179-4fef-90cb-6e2374179d13"));
		when(depositStatusFactory.getConfirmedUploads(anyString())).thenReturn(
				Sets.newSet("uuid:2a5d0363-899b-402d-981b-392a553e17a1", "uuid:7dd57979-084c-4b5a-acc0-a0eed25d2b33"));

		Thread jobThread = new Thread(job);
		Thread finishThread = new Thread(jmsListener);

		jobThread.start();
		finishThread.start();

		// Start processing with a timelimit to prevent infinite wait in case of failure
		jobThread.join(5000L);
		finishThread.join(5000L);

		assertTrue("Job must have been registered", jmsListener.registeredJob);
		assertTrue("Job must be unregistered", jmsListener.unregisteredJob);

		assertEquals("All ingest pids should have been removed", 0, job.getIngestPids().size());

		assertEquals("Ingest count must exclude the already completed items", 6, job.getIngestObjectCount());

		// Click for newly run ingests and the one unregistered previously completed ingest
		verify(jobStatusFactory, times(job.getIngestObjectCount() + 2)).incrCompletion(eq(job.getJobUUID()), eq(1));

		verify(client, times(job.getTopLevelPids().size())).addObjectRelationship(any(PID.class), anyString(),
				any(PID.class));

	}

	/**
	 * Stub JMSListener which sends all of a jobs ingest pids back at it after it has registered
	 *
	 * @author bbpennel
	 * @date Mar 21, 2014
	 */
	protected class FinishIngestsMockListener extends JobForwardingJMSListener implements Runnable {

		private final IngestDeposit ingestJob;

		public boolean registeredJob = false;

		public boolean unregisteredJob = false;

		public FinishIngestsMockListener(IngestDeposit ingestJob) {
			this.ingestJob = ingestJob;
		}

		@Override
		public void registerListener(ListenerJob listener) {
			registeredJob = true;
		}

		@Override
		public void unregisterListener(ListenerJob listener) {
			unregisteredJob = true;
		}

		@Override
		public void run() {
			// Wait for the job to be registered
			while (!registeredJob) {
				try {
					Thread.sleep(50L);
				} catch (InterruptedException e) {
					log.error("Interrupted before ingest registered");
				}
			}

			log.debug("Job registered, beginning to process awaiting pids");
			String pid;
			Collection<String> pids = ingestJob.getIngestsAwaitingConfirmation();
			while (pids.size() > 0 || ingestJob.getIngestPids().size() > 0) {
				Iterator<String> pidIt = pids.iterator();
				if (!pidIt.hasNext())
					continue;
				pid = pidIt.next();
				when(messageSummary.getText()).thenReturn(pid);

				log.debug("Onevent {}", pid);

				job.onEvent(messageDocument);
				// Delay between attempts to prevent this from blowing up
				try {
					Thread.sleep(20L);
				} catch (InterruptedException e) {
					log.error("Interrupted while waiting for all objects to be confirmed");
				}
			}
		}
	}

}
