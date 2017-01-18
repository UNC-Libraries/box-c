/**
 * Copyright 2016 The University of North Carolina at Chapel Hill
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
package edu.unc.lib.deposit.fcrepo4;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.File;
import java.util.UUID;

import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.mockito.Matchers;
import org.mockito.Mock;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.tdb.TDBFactory;

import edu.unc.lib.dl.event.PremisEventBuilder;
import edu.unc.lib.dl.event.PremisLogger;
import edu.unc.lib.dl.event.PremisLoggerFactory;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.Repository;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectDataLoader;
import edu.unc.lib.dl.fcrepo4.RepositoryPathConstants;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.reporting.ActivityMetricsClient;
import edu.unc.lib.dl.util.DepositStatusFactory;
import edu.unc.lib.dl.util.JobStatusFactory;

/**
 * 
 * @author bbpennel
 *
 */
public class AbstractDepositJobTest {

	protected static final String FEDORA_BASE = "http://example.com/";
	
	@Mock
	protected RepositoryObjectDataLoader dataLoader;
	@Mock
	public Repository repository;
	@Rule
	public final TemporaryFolder tmpFolder = new TemporaryFolder();
	
	protected File depositsDirectory;
	protected File depositDir;
	
	@Mock
	protected JobStatusFactory jobStatusFactory;
	@Mock
	protected DepositStatusFactory depositStatusFactory;
	@Mock
	protected PremisLoggerFactory premisLoggerFactory;
	@Mock
	protected PremisLogger premisLogger;
	@Mock
	protected PremisEventBuilder premisEventBuilder;
	@Mock
	protected ActivityMetricsClient metricsClient;
	@Mock
	protected Resource testResource;
	
	protected String jobUUID;
	
	protected String depositUUID;
	protected PID depositPid;
	
	protected Dataset dataset;

	@Before
	public void initBase() throws Exception {
		initMocks(this);
		
		PIDs.setRepository(repository);
		when(repository.getFedoraBase()).thenReturn(FEDORA_BASE);
		when(premisLoggerFactory.createPremisLogger(any(PID.class), any(File.class), any(Repository.class)))
		.thenReturn(premisLogger);
		when(premisLogger.buildEvent(any(Resource.class))).thenReturn(premisEventBuilder);
		when(premisEventBuilder.addEventDetail(anyString(), Matchers.<Object>anyVararg())).thenReturn(premisEventBuilder);
		when(premisEventBuilder.addSoftwareAgent(anyString())).thenReturn(premisEventBuilder);
		when(premisEventBuilder.create()).thenReturn(testResource);
		
		tmpFolder.create();
		depositsDirectory = tmpFolder.newFolder("deposits");
		
		jobUUID = UUID.randomUUID().toString();

		depositUUID = UUID.randomUUID().toString();
		depositDir = new File(depositsDirectory, depositUUID);
		depositDir.mkdir();
		depositPid = PIDs.get(RepositoryPathConstants.DEPOSIT_RECORD_BASE, depositUUID);
		
		dataset = TDBFactory.createDataset();
	}

	protected PID makePid(String qualifier) {
		String uuid = UUID.randomUUID().toString();
		return PIDs.get(qualifier + "/" + uuid);
	}
}
