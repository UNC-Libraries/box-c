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
package edu.unc.lib.dl.cdr.services.test;

import java.io.InputStreamReader;

import org.jdom.Document;
import org.jdom.input.SAXBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import edu.unc.lib.dl.cdr.services.model.PIDMessage;
import edu.unc.lib.dl.cdr.services.util.JMSMessageUtil;
import edu.unc.lib.dl.util.ContentModelHelper;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/service-context-minimal.xml" })
public class MessageParseTest {
	private static final Logger LOG = LoggerFactory.getLogger(MessageParseTest.class);


	@Before
	public void setUp() throws Exception {

	}

	private Document readFileAsString(String filePath) throws Exception {
		return new SAXBuilder().build(new InputStreamReader(this.getClass().getResourceAsStream(filePath)));
	}

	//@Test
	public void cdrMessageTest(){

		try {
			Document doc = readFileAsString("cdrAddMessage.xml");
			PIDMessage message = new PIDMessage(doc, JMSMessageUtil.cdrMessageNamespace);
			message.generateCDRMessageContent();
			LOG.debug("");

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Test
	public void moveMessageTest(){

		try {
			Document doc = readFileAsString("moveMessage.xml");
			PIDMessage message = new PIDMessage(doc, JMSMessageUtil.cdrMessageNamespace);
			message.generateCDRMessageContent();
			LOG.debug("");

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Test
	public void datastreamNameTest() {
		Assert.assertTrue(ContentModelHelper.Datastream.DATA_FILE.equals("DATA_FILE"));
		Assert.assertTrue(ContentModelHelper.Datastream.DATA_FILE.getName().equals("DATA_FILE"));
		Assert.assertTrue(ContentModelHelper.Datastream.DATA_FILE.equals(ContentModelHelper.Datastream.DATA_FILE));
		Assert.assertFalse(ContentModelHelper.Datastream.DATA_FILE.equals("RELS-EXT"));
		Assert.assertFalse(ContentModelHelper.Datastream.DATA_FILE.equals(ContentModelHelper.Datastream.RELS_EXT));
	}
}
