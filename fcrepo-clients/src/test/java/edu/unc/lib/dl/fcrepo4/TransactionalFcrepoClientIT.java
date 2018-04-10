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
package edu.unc.lib.dl.fcrepo4;

import static edu.unc.lib.dl.util.RDFModelUtil.createModel;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.URI;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.fcrepo.client.FcrepoResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import edu.unc.lib.dl.test.TestHelper;

/**
 * @author bbpennel
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"/spring-test/test-fedora-container.xml"})
public class TransactionalFcrepoClientIT {

    private static final int BASE_PORT = 48085;
    private static final String BASE_PATH = "http://localhost:48085/rest/";
    private static final URI BASE_URI = URI.create(BASE_PATH);

    private static final String HOST_HEADER = "boxy.example.com";
    private static final String ALT_PATH = "http://boxy.example.com:123/rest/";

    private TransactionalFcrepoClient fcrepoClient;

    private TransactionManager txManager;

    @Before
    public void setup() {
        TestHelper.setContentBase(BASE_PATH);

        txManager = new TransactionManager();
    }

    @Test
    public void testRequestWithTx() throws Exception {
        fcrepoClient = TransactionalFcrepoClient.client(BASE_PATH)
                .build();

        txManager.setClient(fcrepoClient);

        // Create object in transaction, should return transaction location
        FedoraTransaction tx = txManager.startTransaction();
        URI objUri;
        try (FcrepoResponse response = fcrepoClient.post(BASE_URI).perform()) {
            objUri = response.getLocation();
        }

        assertTrue("Location must be within transaction",
                objUri.toString().startsWith(tx.getTxUri().toString()));

        // Verify that the response triples have non-tx uris
        try (FcrepoResponse response = fcrepoClient.get(objUri).perform()) {
            // Remove tx from obj uri
            String nonTxObjUri = objUri.toString().replaceFirst("/tx:[^/]+", "");

            Model model = createModel(response.getBody());

            Resource resc = model.getResource(nonTxObjUri);
            assertTrue("Subject must be non-tx uri", resc.hasProperty(RDF.type));
        } finally {
            tx.close();
        }
    }

    @Test
    public void testRequestWithHostParam() throws Exception {
        fcrepoClient = TransactionalFcrepoClient.client(BASE_PATH)
                .hostHeader(HOST_HEADER)
                .build();

        URI objUri;
        try (FcrepoResponse response = fcrepoClient.post(BASE_URI).perform()) {
            objUri = response.getLocation();
        }

        assertEquals("Response must use provided Host.",
                HOST_HEADER, objUri.getHost());
        assertEquals("Response must not specify port", -1, objUri.getPort());
    }

    @Test
    public void testRequestWithRebasedUri() throws Exception {
        fcrepoClient = TransactionalFcrepoClient.client(BASE_PATH)
                .build();

        URI offBaseUri = URI.create(ALT_PATH);

        URI objUri;
        try (FcrepoResponse response = fcrepoClient.post(offBaseUri).perform()) {
            objUri = response.getLocation();
        }

        assertEquals("Response must use real host", "localhost", objUri.getHost());
        assertEquals("Response must use real port", BASE_PORT, objUri.getPort());
    }

    @Test
    public void testRequestWithRebasedHostAndTx() throws Exception {
        fcrepoClient = TransactionalFcrepoClient.client(BASE_PATH)
                .hostHeader(HOST_HEADER)
                .build();

        txManager.setClient(fcrepoClient);

        FedoraTransaction tx = txManager.startTransaction();
        String txPath = tx.getTxUri().toString();

        String requestPath = txPath.replace(BASE_PATH, ALT_PATH);
        URI offBaseUri = URI.create(requestPath);

        URI objUri;
        try (FcrepoResponse response = fcrepoClient.post(offBaseUri).perform()) {
            objUri = response.getLocation();
        } finally {
            tx.close();
        }

        assertEquals("Response must use provided Host.",
                HOST_HEADER, objUri.getHost());
        assertEquals("Response must not specify port", -1, objUri.getPort());
    }
}
