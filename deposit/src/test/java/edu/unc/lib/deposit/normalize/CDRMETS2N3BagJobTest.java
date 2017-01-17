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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.Validator;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.xml.sax.SAXException;

import com.google.common.io.Files;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.tdb.TDBFactory;

import edu.unc.lib.deposit.work.JobFailedException;
import edu.unc.lib.dl.event.PremisEventBuilder;
import edu.unc.lib.dl.event.PremisLogger;
import edu.unc.lib.dl.event.PremisLoggerFactory;
import edu.unc.lib.dl.fcrepo4.Repository;
import edu.unc.lib.dl.fcrepo4.RepositoryPathConstants;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Premis;
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
	@Mock
	private SchematronValidator schematronValidator;
	@Mock
	private PremisLoggerFactory premisFactory;
	@Mock
	private PremisLogger premisLogger;
	@Mock
	private PremisEventBuilder premisEventBuilder;
	@Mock
	private Resource testResource;
	
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
		setField(job, "premisLoggerFactory", premisFactory);
		job.setRepository(repository);
		job.setSchematronValidator(schematronValidator);
		when(schematronValidator.validateReportErrors(any(StreamSource.class), eq(METSProfile.CDR_SIMPLE.name())))
			.thenReturn(new ArrayList<String>());
		
		when(premisFactory.createPremisLogger(any(PID.class), any(File.class), any(Repository.class)))
			.thenReturn(premisLogger);
		when(premisLogger.buildEvent(eq(Premis.Validation))).thenReturn(premisEventBuilder);
		when(premisLogger.buildEvent(eq(Premis.Normalization))).thenReturn(premisEventBuilder);
		when(premisEventBuilder.addEventDetail(anyString(), Matchers.<Object>anyVararg())).thenReturn(premisEventBuilder);
		when(premisEventBuilder.addEventDetail(anyString())).thenReturn(premisEventBuilder);
		when(premisEventBuilder.addSoftwareAgent(anyString())).thenReturn(premisEventBuilder);
		when(premisEventBuilder.create()).thenReturn(testResource);
		
		job.init();
	}

	@Test
	public void testSimpleDeposit() throws Exception {
		Files.copy(new File("src/test/resources/mets.xml"), new File(data, "mets.xml"));
		job.run();
	}
	
	@Test(expected = JobFailedException.class)
	public void testMissingFile() throws Exception {
		// checks case where no file is provided
		job.run();
	}

	@Test(expected = JobFailedException.class)
	public void testMETSInvalid() throws Exception {
		try {
			doThrow(new SAXException()).when(metsValidator).validate(any(StreamSource.class));
			Files.copy(new File("src/test/resources/mets.xml"), new File(data, "mets.xml"));
			job.run();
		} finally {
			verify(metsValidator).validate(any(StreamSource.class));
		}
	}
	
	@Test
	public void testPidsAssigned() throws Exception {
		try {
			Files.copy(new File("src/test/resources/mets.xml"), new File(data, "mets.xml"));
			job.run();
		} finally {
			// check that relevant events were created in AbstractMETS and CDRMETS jobs)
			verify(premisLogger).buildEvent(eq(Premis.Validation));
			verify(premisLogger, times(4)).buildEvent(eq(Premis.Normalization));
			verify(premisEventBuilder, times(5)).addEventDetail(anyString(), Matchers.<Object>anyVararg());
			verify(premisEventBuilder, times(4)).addSoftwareAgent(anyString());
			verify(premisEventBuilder, times(5)).create();
		}
	}
	
	@Test
	public void testObjectAdded() throws Exception {
		fail();
		// check object has right type
		// contains the file
		// verify props get set, e.g., checksum
		// verify correct acl
		// test line 58 of job, that file was created
		// maybe do another test case where only the object itself is present
	}
	
}
