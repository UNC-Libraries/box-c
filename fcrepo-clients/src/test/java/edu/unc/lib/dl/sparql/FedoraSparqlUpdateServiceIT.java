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
package edu.unc.lib.dl.sparql;

import static org.junit.Assert.assertTrue;

import java.net.URI;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.DC;
import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import edu.unc.lib.dl.test.TestHelper;
import edu.unc.lib.dl.util.RDFModelUtil;

/**
 *
 * @author bbpennel
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"/spring-test/test-fedora-container.xml", "/spring-test/cdr-client-container.xml"})
public class FedoraSparqlUpdateServiceIT {

    @Autowired
    protected String baseAddress;
    private URI baseUri;

    @Autowired
    protected FcrepoClient client;

    private FedoraSparqlUpdateService updateService;

    @Before
    public void init_() {
        TestHelper.setContentBase(baseAddress);
        baseUri = URI.create(baseAddress);

        updateService = new FedoraSparqlUpdateService();
        updateService.setFcrepoClient(client);
    }

    @Test
    public void updateTriplesTest() throws Exception {
        URI containerUri;
        try (FcrepoResponse response = client.post(baseUri).perform()) {
            containerUri = response.getLocation();
        }

        String updateString = "INSERT { <> <" + DC.title.getURI() + "> \"title\" . } WHERE {}";
        updateService.executeUpdate(containerUri.toString(), updateString);

        try (FcrepoResponse response = client.get(containerUri).perform()) {
            Model model = RDFModelUtil.createModel(response.getBody());
            Resource resc = model.getResource(containerUri.toString());
            assertTrue(resc.hasProperty(DC.title, "title"));
        }
    }

}
