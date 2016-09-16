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
package edu.unc.lib.dl.fcrepo4;

import java.io.IOException;
import java.net.URI;

import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoOperationFailedException;
import org.fcrepo.client.FcrepoResponse;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/spring-test/test-fedora-container.xml")
public class AbstractFedoraIT {

	protected static final int SERVER_PORT = Integer.parseInt(System.getProperty("fcrepo.dynamic.test.port", "8080"));

	protected static final String HOSTNAME = "localhost";

	protected static final String serverAddress = "http://" + HOSTNAME + ":" + SERVER_PORT + "/rest/";
	
	protected FcrepoClient client;
	
	protected URI createBaseContainer(String name) throws IOException, FcrepoOperationFailedException {
		URI baseUri = URI.create(serverAddress + "/" + RepositoryPathConstants.CONTENT_BASE);
		// Create a parent object to put the binary into
		try (FcrepoResponse response = client.put(baseUri).perform()) {
			return response.getLocation();
		}
	}
}
