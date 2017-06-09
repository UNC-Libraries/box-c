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

import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.stream.Collectors;

import org.apache.activemq.util.ByteArrayInputStream;
import org.fcrepo.client.FcrepoResponse;
import org.jgroups.util.UUID;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;

import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.rdf.Ebucore;
import edu.unc.lib.dl.rdf.Fcrepo4Repository;
import edu.unc.lib.dl.rdf.Ldp;
import edu.unc.lib.dl.rdf.PcdmModels;
import edu.unc.lib.dl.rdf.PcdmUse;
import edu.unc.lib.dl.util.RDFModelUtil;
import edu.unc.lib.dl.util.URIUtil;

/**
 *
 * @author bbpennel
 *
 */
public class RepositoryObjectFactoryIT extends AbstractFedoraIT {

    @Autowired
    private RepositoryObjectFactory factory;

    @Test
    public void createDepositRecordTest() throws Exception {
        String path = baseAddress + RepositoryPathConstants.DEPOSIT_RECORD_BASE
                + "/" + UUID.randomUUID().toString();
        URI uri = URI.create(path);

        URI resultUri = factory.createDepositRecord(uri, null);
        assertEquals("Requested URI did not match result", uri, resultUri);

        try (FcrepoResponse resp = client.get(uri).perform()) {
            Model respModel = RDFModelUtil.createModel(resp.getBody());

            Resource respResc = respModel.getResource(path);
            assertTrue("Did not have deposit record type", respResc.hasProperty(RDF.type, Cdr.DepositRecord));

            String manifestPath = path + "/" + RepositoryPathConstants.DEPOSIT_MANIFEST_CONTAINER;
            assertTrue("Manifest container not created",
                    respResc.hasProperty(Ldp.contains, createResource(manifestPath)));
            String eventPath = path + "/" + RepositoryPathConstants.EVENTS_CONTAINER;
            assertTrue("Event container not created", respResc.hasProperty(Ldp.contains, createResource(eventPath)));
        }
    }

    @Test
    public void createBinaryTest() throws Exception {
        String binarySlug = "binary_test";
        String binaryPath = URIUtil.join(baseAddress, binarySlug);
        URI serverUri = URI.create(baseAddress);

        Model model = ModelFactory.createDefaultModel();
        Resource resc = model.createResource(binaryPath);
        resc.addProperty(RDF.type, PcdmUse.OriginalFile);

        String bodyString = "Test text";
        String filename = "test.txt";
        String mimetype = "text/plain";
        InputStream contentStream = new ByteArrayInputStream(bodyString.getBytes());

        URI respUri = factory.createBinary(serverUri, binarySlug, contentStream, filename, mimetype, null, model);

        try (FcrepoResponse resp = client.get(respUri).perform()) {
            String respString = new BufferedReader(new InputStreamReader(resp.getBody())).lines()
                    .collect(Collectors.joining("\n"));

            assertEquals("Binary content did not match submitted value", bodyString, respString);
        }

        // Verify that triples were added
        URI metadataUri = URI.create(binaryPath + "/fcr:metadata");
        try (FcrepoResponse resp = client.get(metadataUri).perform()) {
            Model respModel = RDFModelUtil.createModel(resp.getBody());
            Resource respResc = respModel.getResource(binaryPath);

            assertTrue(respResc.hasProperty(RDF.type, Fcrepo4Repository.Binary));
            assertTrue(respResc.hasProperty(RDF.type, PcdmUse.OriginalFile));
            assertTrue(respResc.hasProperty(RDF.type, PcdmModels.File));

            assertEquals(respResc.getProperty(Ebucore.filename).getString(), filename);
            assertEquals(respResc.getProperty(Ebucore.hasMimeType).getString(), mimetype);
        }
    }

    @Test
    public void createFileObjectTest() throws Exception {
        String objectPath = URIUtil.join(baseAddress, UUID.randomUUID().toString());
        URI uri = URI.create(objectPath);

        URI resultUri = factory.createFileObject(uri, null);
        assertEquals("Requested URI did not match result", uri, resultUri);

        try (FcrepoResponse resp = client.get(uri).perform()) {
            Model respModel = RDFModelUtil.createModel(resp.getBody());

            Resource respResc = respModel.getResource(objectPath);
            // Verify that the correct RDF types were applied
            assertTrue(respResc.hasProperty(RDF.type, Cdr.FileObject));
            assertTrue(respResc.hasProperty(RDF.type, PcdmModels.Object));

            // Verify that subcontainers were created
            assertTrue(respResc.hasProperty(Ldp.contains,
                    createResource(URIUtil.join(objectPath, RepositoryPathConstants.EVENTS_CONTAINER))));
            assertTrue(respResc.hasProperty(Ldp.contains,
                    createResource(URIUtil.join(objectPath, RepositoryPathConstants.DATA_FILE_FILESET))));
        }
    }

    @Test
    public void createWorkObjectTest() throws Exception {
        String objectPath = URIUtil.join(baseAddress, UUID.randomUUID().toString());
        URI uri = URI.create(objectPath);

        URI resultUri = factory.createWorkObject(uri, null);
        assertEquals("Requested URI did not match result", uri, resultUri);

        try (FcrepoResponse resp = client.get(uri).perform()) {
            Model respModel = RDFModelUtil.createModel(resp.getBody());

            Resource respResc = respModel.getResource(objectPath);
            assertTrue(respResc.hasProperty(RDF.type, Cdr.Work));
            assertTrue(respResc.hasProperty(RDF.type, PcdmModels.Object));

            assertTrue(respResc.hasProperty(Ldp.contains,
                    createResource(URIUtil.join(objectPath, RepositoryPathConstants.EVENTS_CONTAINER))));
            assertTrue(respResc.hasProperty(Ldp.contains,
                    createResource(URIUtil.join(objectPath, RepositoryPathConstants.MEMBER_CONTAINER))));
        }
    }

    @Test
    public void createFolderObjectTest() throws Exception {
        String objectPath = URIUtil.join(baseAddress, UUID.randomUUID().toString());
        URI uri = URI.create(objectPath);

        URI resultUri = factory.createFolderObject(uri, null);
        assertEquals("Requested URI did not match result", uri, resultUri);

        try (FcrepoResponse resp = client.get(uri).perform()) {
            Model respModel = RDFModelUtil.createModel(resp.getBody());

            Resource respResc = respModel.getResource(objectPath);
            assertTrue(respResc.hasProperty(RDF.type, Cdr.Folder));
            assertTrue(respResc.hasProperty(RDF.type, PcdmModels.Object));

            assertTrue(respResc.hasProperty(Ldp.contains,
                    createResource(URIUtil.join(objectPath, RepositoryPathConstants.EVENTS_CONTAINER))));
            assertTrue(respResc.hasProperty(Ldp.contains,
                    createResource(URIUtil.join(objectPath, RepositoryPathConstants.MEMBER_CONTAINER))));
        }
    }

    @Test
    public void createAdminUnitTest() throws Exception {
        String objectPath = URIUtil.join(baseAddress, UUID.randomUUID().toString());
        URI uri = URI.create(objectPath);

        URI resultUri = factory.createAdminUnit(uri, null);
        assertEquals("Requested URI did not match result", uri, resultUri);

        try (FcrepoResponse resp = client.get(uri).perform()) {
            Model respModel = RDFModelUtil.createModel(resp.getBody());

            Resource respResc = respModel.getResource(objectPath);
            assertTrue(respResc.hasProperty(RDF.type, Cdr.AdminUnit));
            assertTrue(respResc.hasProperty(RDF.type, PcdmModels.Collection));

            assertTrue(respResc.hasProperty(Ldp.contains,
                    createResource(URIUtil.join(objectPath, RepositoryPathConstants.EVENTS_CONTAINER))));
            assertTrue(respResc.hasProperty(Ldp.contains,
                    createResource(URIUtil.join(objectPath, RepositoryPathConstants.MEMBER_CONTAINER))));
        }
    }

    @Test
    public void createCollectionObjectTest() throws Exception {
        String objectPath = URIUtil.join(baseAddress, UUID.randomUUID().toString());
        URI uri = URI.create(objectPath);

        URI resultUri = factory.createCollectionObject(uri, null);
        assertEquals("Requested URI did not match result", uri, resultUri);

        try (FcrepoResponse resp = client.get(uri).perform()) {
            Model respModel = RDFModelUtil.createModel(resp.getBody());

            Resource respResc = respModel.getResource(objectPath);
            assertTrue(respResc.hasProperty(RDF.type, Cdr.Collection));
            assertTrue(respResc.hasProperty(RDF.type, PcdmModels.Object));

            assertTrue(respResc.hasProperty(Ldp.contains,
                    createResource(URIUtil.join(objectPath, RepositoryPathConstants.EVENTS_CONTAINER))));
            assertTrue(respResc.hasProperty(Ldp.contains,
                    createResource(URIUtil.join(objectPath, RepositoryPathConstants.MEMBER_CONTAINER))));
        }
    }
}
