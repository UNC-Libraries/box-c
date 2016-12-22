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
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.Validator;

import org.junit.Before;
import org.junit.Test;
import org.junit.internal.runners.statements.Fail;
import org.mockito.Mock;
import org.xml.sax.SAXException;

import com.google.common.io.Files;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.tdb.TDBFactory;

import edu.unc.lib.deposit.work.JobFailedException;
import edu.unc.lib.dl.event.PremisEventBuilder;
import edu.unc.lib.dl.fcrepo4.RepositoryPathConstants;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.schematron.SchematronValidator;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositField;
import edu.unc.lib.dl.xml.METSProfile;

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
	//@Mock
	//private PremisEventBuilder eventBuilder;
	@Mock
	private SchematronValidator schematronValidator;
	private CDRMETS2N3BagJob job;

	private Map<String, String> status;
	
	private File data;

	@Before
	public void setup() throws Exception {
		status = new HashMap<String, String>();
		status.put(DepositField.fileName.name(), "src/test/resources/mets.xml");
		
		when(depositStatusFactory.get(anyString())).thenReturn(status);
		when(metsSipSchema.newValidator()).thenReturn(metsValidator);
		Dataset dataset = TDBFactory.createDataset();
		makePid(RepositoryPathConstants.CONTENT_BASE);
		
		data = new File(depositDir, "data");
		data.mkdir();
    		
		job = new CDRMETS2N3BagJob(jobUUID, depositUUID);
		setField(job, "dataset", dataset);
		job.setDepositDirectory(depositDir);
		setField(job, "depositsDirectory", depositsDirectory);
		setField(job, "depositStatusFactory", depositStatusFactory);
		setField(job, "metsSipSchema", metsSipSchema);
		job.setRepository(repository);
		job.setSchematronValidator(schematronValidator);
		when(schematronValidator.validateReportErrors(any(StreamSource.class), eq(METSProfile.CDR_SIMPLE.name())))
			.thenReturn(new ArrayList<String>());
		job.init();
	}

	@Test
	public void testSimpleDeposit() throws Exception {
		Files.copy(new File("src/test/resources/mets.xml"), new File(data, "mets.xml"));
		job.run();
	}
	
	@Test(expected = JobFailedException.class)
	public void testMissingFile() throws Exception {
		job.run();
	}

	@Test
	public void testMETSInvalid() throws Exception {
		doThrow(new SAXException()).when(metsValidator).validate(any(StreamSource.class));
		//test validator (is it valid mets?) returns false; test schematron (is it valid cdr mets?) returns false
	}
	
}
