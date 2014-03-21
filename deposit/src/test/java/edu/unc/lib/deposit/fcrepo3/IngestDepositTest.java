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
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.File;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Iterator;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;

import edu.unc.lib.deposit.work.JobStatusFactory;
import edu.unc.lib.dl.fedora.JobForwardingJMSListener;
import edu.unc.lib.dl.fedora.ListenerJob;
import edu.unc.lib.dl.util.DepositConstants;

/**
 * @author bbpennel
 * @date Mar 21, 2014
 */
public class IngestDepositTest {

	@Rule
	public final TemporaryFolder tmpFolder = new TemporaryFolder();

	private File depositsDirectory;

	private File modelFile;

	@Mock
	private JobStatusFactory jobStatusFactory;

	private FinishIngestsMockListener jmsListener;

	@Mock
	private Document messageDocument;
	@Mock
	private Element messageRoot;
	@Mock
	private Element messageSummary;

	private IngestDeposit job;

	@Before
	public void setup() throws Exception {
		initMocks(this);

		when(messageDocument.getRootElement()).thenReturn(messageRoot);
		when(messageRoot.getChildTextTrim(eq("title"), any(Namespace.class))).thenReturn("ingest");
		when(messageRoot.getChild(eq("summary"), any(Namespace.class))).thenReturn(messageSummary);

		depositsDirectory = tmpFolder.newFolder("deposits");

		createJob("bd5ff703-9c2e-466b-b4cc-15bbfd03c8ae", "/structure.n3");
	}

	private void createJob(String depositUuid, String modelFileName) throws Exception {

		File depositFolder = new File(depositsDirectory, depositUuid);
		depositFolder.mkdir();

		// Copy the target model file over into the deposit's model file.
		modelFile = new File(depositFolder, DepositConstants.MODEL_FILE);
		File originalFile = new File(getClass().getResource(modelFileName).toURI());
		Files.copy(originalFile.toPath(), modelFile.toPath());

		job = new IngestDeposit("jobuuid", depositUuid);
		jmsListener = new FinishIngestsMockListener(job);

		job.setListener(jmsListener);
		// job.setDepositDirectory(depositFolder);
		setField(job, "depositsDirectory", depositsDirectory);
		setField(job, "jobStatusFactory", jobStatusFactory);

		job.init();
	}

	@Test
	public void test() throws Exception {

		Thread jobThread = new Thread(job);
		Thread finishThread = new Thread(jmsListener);

		jobThread.start();
		finishThread.start();

		jobThread.join();
		finishThread.join();

		assertTrue("Job must have been registered", jmsListener.registeredJob);
		assertTrue("Job must be unregistered", jmsListener.unregisteredJob);
		assertEquals("All ingest pids should have been removed", 0, job.getIngestPids().size());
		assertEquals("Top level pids should be present", 2, job.getTopLevelPids().size());

		assertEquals("Incorrect number of objects extracted", 9, job.getIngestObjectCount());

		// Number of messages received should be equal to number of ingest objects plus deposit record
		verify(messageRoot, times(job.getIngestObjectCount() + 1)).getChild(eq("summary"), any(Namespace.class));

		// Clicks should have been registered
		verify(jobStatusFactory, times(job.getIngestObjectCount() + 1)).incrCompletion(eq(job), eq(1));

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
				}
			}

			String pid;
			Collection<String> ingestPids = ingestJob.getIngestPids();
			while (ingestJob.getIngestPids().size() > 0) {
				Iterator<String> pidIt = ingestPids.iterator();
				if (!pidIt.hasNext())
					break;
				pid = pidIt.next();
				when(messageSummary.getText()).thenReturn(pid);
				job.onEvent(messageDocument);
			}
		}
	}

}
