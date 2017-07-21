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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.URI;

import org.fcrepo.client.FcrepoResponse;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;

import edu.unc.lib.dl.rdf.PcdmModels;
import edu.unc.lib.dl.util.RDFModelUtil;

/**
 *
 * @author bbpennel
 *
 */
public class LdpContainerFactoryIT extends AbstractFedoraIT {

    @Autowired
    private LdpContainerFactory factory;

    @Test
    public void createDirectContainerTest() throws Exception {
        URI objUri;
        // create the base object
        try (FcrepoResponse resp = client.post(URI.create(baseAddress)).perform()) {
            objUri = resp.getLocation();
        }

        URI childUri;
        URI container = factory.createDirectContainer(objUri, PcdmModels.hasMember, "files");
        try (FcrepoResponse resp = client.post(container).perform()) {
            childUri = resp.getLocation();
        }

        Model model;
        try (FcrepoResponse resp = client.get(objUri).perform()) {
            model = RDFModelUtil.createModel(resp.getBody());
        }

        Resource objResc = model.getResource(objUri.toString());
        Statement member = objResc.getProperty(PcdmModels.hasMember);

        assertNotNull("No hasMember relationship", member);
        assertEquals("hasMember relation references the wrong object",
                childUri.toString(), member.getResource().toString());
    }

    @Test
    public void createIndirectContainerTest() throws Exception {
        URI objUri;
        // create the base object
        try (FcrepoResponse resp = client.post(URI.create(baseAddress)).perform()) {
            objUri = resp.getLocation();
        }
        // Create the indirect container for the base object
        URI container = factory.createIndirectContainer(objUri, PcdmModels.hasRelatedObject, "related");

        // Create another object, which will later be added the base object
        URI childUri;
        try (FcrepoResponse resp = client.post(URI.create(baseAddress)).perform()) {
            childUri = resp.getLocation();
        }

        // Add the proxy to the container
        factory.createIndirectProxy(container, objUri, childUri);

        Model model;
        try (FcrepoResponse resp = client.get(objUri).perform()) {
            model = RDFModelUtil.createModel(resp.getBody());
        }

        Resource objResc = model.getResource(objUri.toString());
        Statement member = objResc.getProperty(PcdmModels.hasRelatedObject);

        assertNotNull("No hasRelatedObject relationship", member);
        assertEquals("hasRelatedObject relation references the wrong object",
                childUri.toString(), member.getResource().toString());
    }
}
