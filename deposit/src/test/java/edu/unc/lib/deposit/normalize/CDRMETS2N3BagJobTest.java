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
package edu.unc.lib.deposit.normalize;

import static edu.unc.lib.dl.test.TestHelpers.setField;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.xml.validation.Schema;
import javax.xml.validation.Validator;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.core.io.ClassPathResource;

import com.google.common.io.Files;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.tdb.TDBFactory;

import edu.unc.lib.dl.event.PremisEventBuilder;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.RepositoryPathConstants;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.schematron.SchematronValidator;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositField;

/**
 * 
 * @author harring
 *
 */
public class CDRMETS2N3BagJobTest extends AbstractNormalizationJobTest {
	@Mock
	private Schema metsSipSchema;
	@Mock
	private Validator metsValidator;
	@Mock
	private PremisEventBuilder eventBuilder;
	private SchematronValidator schematronValidator;
	private PID destinationPid;

	private CDRMETS2N3BagJob job;

	private Map<String, String> status;

	@Before
	public void setup() throws Exception {
		initMocks(this);
		status = new HashMap<String, String>();
		when(depositStatusFactory.get(anyString())).thenReturn(status);
		when(metsSipSchema.newValidator()).thenReturn(metsValidator);
		Dataset dataset = TDBFactory.createDataset();
		destinationPid = makePid(RepositoryPathConstants.CONTENT_BASE);
		
		
		// equivalent to command line cp source dest
		File data = new File(depositDir, "data");
		data.mkdir();
		Files.copy(new File("src/test/resources/mets.xml"), new File(data, "mets.xml"));
		
		ClassPathResource test = new ClassPathResource("simple_mets_profile.sch", SchematronValidator.class);
		schematronValidator = new SchematronValidator();
    		schematronValidator.getSchemas().put("test", test);
    		schematronValidator.loadSchemas();
    		
		job = new CDRMETS2N3BagJob(jobUUID, depositUUID);
		setField(job, "dataset", dataset);
		job.setDepositDirectory(depositDir);
		setField(job, "depositsDirectory", depositsDirectory);
		setField(job, "depositStatusFactory", depositStatusFactory);
		setField(job, "metsSipSchema", metsSipSchema);
		job.setRepository(repository);
		job.init();
	}

	@Test
	public void testSimpleDeposit() throws Exception {
		String sourcePath = "src/test/resources/paths/valid-bag";
		status.put(DepositField.sourcePath.name(), sourcePath);
		status.put(DepositField.fileName.name(), "Test File");
		status.put(DepositField.extras.name(), "{\"accessionNumber\" : \"123456\", \"mediaId\" : \"789\"}");
		status.put(DepositField.containerId.name(), destinationPid.getQualifiedId());
		status.put(DepositField.metsType.name(), "src/test/resources/mets.xml");
		//String absoluteSourcePath = "file://" + Paths.get(sourcePath).toAbsolutePath().toString();
		job.run();
	}

}
