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

import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.stream.Collectors;

import org.apache.activemq.util.ByteArrayInputStream;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.fcrepo.client.FcrepoResponse;
import org.junit.Test;

import edu.unc.lib.dl.fedora.PID;
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

    @Test
    public void createDepositRecordTest() throws Exception {
        PID pid = pidMinter.mintDepositRecordPid();
        URI uri = pid.getRepositoryUri();
        String path = uri.getPath();

        DepositRecord depRec = repoObjFactory.createDepositRecord(null);
        assertEquals("Requested URI did not match result", uri, depRec.getUri());

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

        BinaryObject binObj = repoObjFactory.createBinary(serverUri, binarySlug, contentStream, filename, mimetype, null, model);

        try (FcrepoResponse resp = client.get(binObj.getUri()).perform()) {
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
        FileObject fileObj = repoObjFactory.createFileObject();
        PID pid = fileObj.getPid();
        URI uri = pid.getRepositoryUri();
        String objPath = uri.getPath();

        assertEquals("Requested URI did not match result", uri, fileObj.getUri());

        try (FcrepoResponse resp = client.get(uri).perform()) {
            Model respModel = RDFModelUtil.createModel(resp.getBody());

            Resource respResc = respModel.getResource(uri.getPath());
            // Verify that the correct RDF types were applied
            assertTrue(respResc.hasProperty(RDF.type, Cdr.FileObject));
            assertTrue(respResc.hasProperty(RDF.type, PcdmModels.Object));

            // Verify that subcontainers were created
            assertTrue(respResc.hasProperty(Ldp.contains,
                    createResource(URIUtil.join(objPath, RepositoryPathConstants.EVENTS_CONTAINER))));
            assertTrue(respResc.hasProperty(Ldp.contains,
                    createResource(URIUtil.join(objPath, RepositoryPathConstants.DATA_FILE_FILESET))));
        }
    }

    @Test
    public void createWorkObjectTest() throws Exception {
        WorkObject workObj = repoObjFactory.createWorkObject();
        PID pid = workObj.getPid();
        URI uri = pid.getRepositoryUri();
        String objPath = uri.getPath();

        assertEquals("Requested URI did not match result", uri, workObj.getUri());

        try (FcrepoResponse resp = client.get(uri).perform()) {
            Model respModel = RDFModelUtil.createModel(resp.getBody());

            Resource respResc = respModel.getResource(objPath);
            assertTrue(respResc.hasProperty(RDF.type, Cdr.Work));
            assertTrue(respResc.hasProperty(RDF.type, PcdmModels.Object));

            assertTrue(respResc.hasProperty(Ldp.contains,
                    createResource(URIUtil.join(objPath, RepositoryPathConstants.EVENTS_CONTAINER))));
            assertTrue(respResc.hasProperty(Ldp.contains,
                    createResource(URIUtil.join(objPath, RepositoryPathConstants.MEMBER_CONTAINER))));
        }
    }

    @Test
    public void createFolderObjectTest() throws Exception {

        FolderObject folderObj = repoObjFactory.createFolderObject();
        PID pid = folderObj.getPid();
        URI uri = pid.getRepositoryUri();
        String objPath = uri.getPath();
        assertEquals("Requested URI did not match result", uri, folderObj.getUri());

        try (FcrepoResponse resp = client.get(uri).perform()) {
            Model respModel = RDFModelUtil.createModel(resp.getBody());

            Resource respResc = respModel.getResource(objPath);
            assertTrue(respResc.hasProperty(RDF.type, Cdr.Folder));
            assertTrue(respResc.hasProperty(RDF.type, PcdmModels.Object));

            assertTrue(respResc.hasProperty(Ldp.contains,
                    createResource(URIUtil.join(objPath, RepositoryPathConstants.EVENTS_CONTAINER))));
            assertTrue(respResc.hasProperty(Ldp.contains,
                    createResource(URIUtil.join(objPath, RepositoryPathConstants.MEMBER_CONTAINER))));
        }
    }

    @Test
    public void createAdminUnitTest() throws Exception {

        AdminUnit adminUnit = repoObjFactory.createAdminUnit();
        PID pid = adminUnit.getPid();
        URI uri = pid.getRepositoryUri();
        String objPath = uri.getPath();

        assertEquals("Requested URI did not match result", uri, adminUnit.getUri());

        try (FcrepoResponse resp = client.get(uri).perform()) {
            Model respModel = RDFModelUtil.createModel(resp.getBody());

            Resource respResc = respModel.getResource(objPath);
            assertTrue(respResc.hasProperty(RDF.type, Cdr.AdminUnit));
            assertTrue(respResc.hasProperty(RDF.type, PcdmModels.Collection));

            assertTrue(respResc.hasProperty(Ldp.contains,
                    createResource(URIUtil.join(objPath, RepositoryPathConstants.EVENTS_CONTAINER))));
            assertTrue(respResc.hasProperty(Ldp.contains,
                    createResource(URIUtil.join(objPath, RepositoryPathConstants.MEMBER_CONTAINER))));
        }
    }

    @Test
    public void createCollectionObjectTest() throws Exception {

        CollectionObject collObj = repoObjFactory.createCollectionObject();
        PID pid = collObj.getPid();
        URI uri = pid.getRepositoryUri();
        String objPath = uri.getPath();
        assertEquals("Requested URI did not match result", uri, collObj.getUri());

        try (FcrepoResponse resp = client.get(uri).perform()) {
            Model respModel = RDFModelUtil.createModel(resp.getBody());
            Resource respResc = respModel.getResource(objPath);
            assertTrue(respResc.hasProperty(RDF.type, Cdr.Collection));
            assertTrue(respResc.hasProperty(RDF.type, PcdmModels.Object));

            assertTrue(respResc.hasProperty(Ldp.contains,
                    createResource(URIUtil.join(objPath, RepositoryPathConstants.EVENTS_CONTAINER))));
            assertTrue(respResc.hasProperty(Ldp.contains,
                    createResource(URIUtil.join(objPath, RepositoryPathConstants.MEMBER_CONTAINER))));
        }
    }
}
