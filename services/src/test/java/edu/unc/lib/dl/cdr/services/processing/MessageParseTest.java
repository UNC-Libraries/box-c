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
public class MessageParseTest extends Assert {
	private static final Logger LOG = LoggerFactory.getLogger(MessageParseTest.class);


	@Before
	public void setUp() throws Exception {

	}

	private Document readFileAsString(String filePath) throws Exception {
		return new SAXBuilder().build(new InputStreamReader(this.getClass().getResourceAsStream(filePath)));
	}

	@Test
	public void cdrAddMessageTest(){

		try {
			Document doc = readFileAsString("cdrAddMessage.xml");
			PIDMessage message = new PIDMessage(doc, JMSMessageUtil.cdrMessageNamespace);
			assertTrue(message.getPIDString().equals("uuid:7c740ac5-5685-4be1-9008-9a8be5f54744"));
			assertTrue(JMSMessageUtil.CDRActions.ADD.equals(message.getQualifiedAction()));
			assertTrue("2011-04-28T18:55:32.220Z".equals(message.getTimestamp()));
			assertTrue("".equals(message.getDatastream()));
			assertTrue("".equals(message.getRelation()));
			assertNull(message.getServiceName());
			assertNull(message.getCDRMessageContent());
			
			message.generateCDRMessageContent();
			assertTrue(message.getCDRMessageContent().getParent().equals("uuid:7c740ac5-5685-4be1-9008-9a8be5f54744"));
			assertTrue(message.getCDRMessageContent().getSubjects().size() == 3);
			assertNull(message.getCDRMessageContent().getOldParents());
			assertTrue(JMSMessageUtil.CDRActions.ADD.getName().equals(message.getCDRMessageContent().getOperation()));
			assertTrue(message.getCDRMessageContent().getReordered().size() == 0);
			assertNull(message.getCDRMessageContent().getMode());

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Test
	public void moveMessageTest(){

		try {
			Document doc = readFileAsString("cdrMoveMessage.xml");
			PIDMessage message = new PIDMessage(doc, JMSMessageUtil.cdrMessageNamespace);
			assertTrue(JMSMessageUtil.CDRActions.MOVE.equals(message.getQualifiedAction()));
			message.generateCDRMessageContent();
			assertTrue(message.getCDRMessageContent().getParent().equals("uuid:a7ac047d-7991-462c-a024-897c36280b83"));
			assertTrue(message.getCDRMessageContent().getOldParents().size() == 2);
			assertTrue(message.getCDRMessageContent().getSubjects().size() == 2);
			assertTrue(message.getCDRMessageContent().getReordered().size() == 1);
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
