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
import static org.junit.Assert.*;

import java.net.URI;

import org.fcrepo.server.errors.ServerException;
import org.fcrepo.server.storage.ContentManagerParams;
import org.fcrepo.server.storage.types.MIMETypedStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import edu.unc.lib.staging.Stages;
import edu.unc.lib.staging.StagingArea;
import fedorax.server.module.storage.IrodsExternalContentManager;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/spring-context.xml" })
public class StagingExternalContentManagerTest {

	@Autowired
	IrodsExternalContentManager externalContentManager;
	
	@Test
	public void testTagLocally() throws ServerException {
		Stages stages = externalContentManager.getStages();
		StagingArea shc = stages.getAllAreas().get(URI.create("tag:cdr.lib.unc.edu,2013:/storhouse_shc/"));
		assertTrue("The SHC test stage must be connected: "+shc.getStatus(), shc.isConnected());
		String testURL = "tag:joey@cdr.lib.unc.edu,2013:/storhouse_shc/my+project/_-1/test+file.txt";
		ContentManagerParams params = new ContentManagerParams(testURL);
		MIMETypedStream mts = externalContentManager.getExternalContent(params);
		assertNotNull("Stream result must not be null", mts);
	}

}
