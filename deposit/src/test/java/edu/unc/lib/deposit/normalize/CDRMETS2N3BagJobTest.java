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

import java.util.HashMap;
import java.util.Map;

import javax.xml.validation.Schema;

import org.junit.Before;
import org.springframework.core.io.ClassPathResource;

import edu.unc.lib.dl.schematron.SchematronValidator;

/**
 * 
 * @author harring
 *
 */
public class CDRMETS2N3BagJobTest extends AbstractNormalizationJobTest {
	
	private Schema metsSipSchema;
	private SchematronValidator schematronValidator;

	private CDRMETS2N3BagJob job;

	private Map<String, String> status;

	@Before
	public void setup() throws Exception {
		initMocks(this);
		status = new HashMap<String, String>();
		when(depositStatusFactory.get(anyString())).thenReturn(status);
		
		ClassPathResource test = new ClassPathResource("simple_mets_profile.sch", SchematronValidator.class);
		schematronValidator = new SchematronValidator();
    		schematronValidator.getSchemas().put("test", test);
    		schematronValidator.loadSchemas();

		job = new CDRMETS2N3BagJob();
		job.setDepositUUID(depositUUID);
		job.setDepositDirectory(depositDir);
		setField(job, "depositsDirectory", depositsDirectory);
		setField(job, "depositStatusFactory", depositStatusFactory);
		job.init();
	}


}
