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
package edu.unc.lib.dl;

import java.io.File;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.ingest.IngestException;
import edu.unc.lib.dl.ingest.sip.METSPackageSIP;
import edu.unc.lib.dl.ingest.sip.SIPProcessorFactory;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/service-context.xml", "/remotes-context-test.xml" })
public class IngestProcessorFactoryTest extends Assert {
	private static final Log log = LogFactory.getLog(IngestProcessorFactoryTest.class);

	@Autowired
	private SIPProcessorFactory sipProcessorFactory = null;

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testFindMETSPackageProcessor() {
		try {
			File test = File.createTempFile("test", ".txt");
			METSPackageSIP foo = new METSPackageSIP(new PID("test:1"), test, null, false);
			this.getSipProcessorFactory().getSIPProcessor(foo);
		} catch (IOException e) {
			log.debug(e);
			fail(e.getMessage());
		} catch (IngestException e) {
			log.debug(e);
			fail(e.getMessage());
		}
	}

	public SIPProcessorFactory getSipProcessorFactory() {
		return sipProcessorFactory;
	}

	public void setSipProcessorFactory(SIPProcessorFactory sipProcessorFactory) {
		this.sipProcessorFactory = sipProcessorFactory;
	}
}
