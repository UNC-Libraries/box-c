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
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URI;

import org.apache.commons.io.FileUtils;
import org.apache.jena.vocabulary.RDF;
import org.apache.tika.io.IOUtils;
import org.fcrepo.client.FcrepoResponse;
import org.junit.Before;
import org.junit.Test;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Fcrepo4Repository;

/**
 *
 * @author bbpennel
 *
 */
public class BinaryObjectIT extends AbstractFedoraIT {

    private static final String BODY_STRING = "Test text";
    private static final String FILENAME = "test.txt";
    private static final String MIMETYPE = "text/plain";
    private static final String CHECKSUM = "82022e1782b92dce5461ee636a6c5bea8509ffee";

    private URI contentUri;

    @Before
    public void setup() throws Exception {
        File contentFile = File.createTempFile("test_file", ".txt");
        FileUtils.write(contentFile, BODY_STRING, "UTF-8");
        contentFile.deleteOnExit();
        contentUri = contentFile.toPath().toUri();
    }

    @Test
    public void retrieveExternalBinary() throws Exception {
        PID parentPid = pidMinter.mintContentPid();

        PID filePid = PIDs.get(parentPid.getId() + "/my_bin");
        BinaryObject obj = repoObjFactory.createOrUpdateBinary(filePid, contentUri,
                FILENAME, MIMETYPE, CHECKSUM, null, null);

        // Verify that the body of the binary is retrieved
        String respString = IOUtils.toString(obj.getBinaryStream());
        assertEquals("Binary content did not match submitted value", BODY_STRING, respString);

        // Check that metadata is retrieved
        assertEquals(FILENAME, obj.getFilename());
        assertEquals(MIMETYPE, obj.getMimetype());
        assertEquals(BODY_STRING.length(), obj.getFilesize().longValue());
        assertEquals("urn:sha1:" + CHECKSUM, obj.getSha1Checksum());

        assertTrue(obj.getResource().hasProperty(RDF.type, Fcrepo4Repository.Binary));
    }

    @Test
    public void retrieveInternalBinary() throws Exception {
        PID parentPid = pidMinter.mintContentPid();
        try (FcrepoResponse response = client.put(parentPid.getRepositoryUri()).perform()) {
        }

        InputStream contentStream = new ByteArrayInputStream(BODY_STRING.getBytes());

        BinaryObject obj = repoObjFactory.createBinary(parentPid.getRepositoryUri(), "binary_test",
                contentStream, FILENAME, MIMETYPE, CHECKSUM, null, null);

        // Verify that the body of the binary is retrieved
        String respString = IOUtils.toString(obj.getBinaryStream());
        assertEquals("Binary content did not match submitted value", BODY_STRING, respString);

        // Check that metadata is retrieved
        assertEquals(FILENAME, obj.getFilename());
        assertEquals(MIMETYPE, obj.getMimetype());
        assertEquals(BODY_STRING.length(), obj.getFilesize().longValue());
        assertEquals("urn:sha1:" + CHECKSUM, obj.getSha1Checksum());

        assertTrue(obj.getResource().hasProperty(RDF.type, Fcrepo4Repository.Binary));
    }

    @Test
    public void getParent() throws Exception {
        FileObject fileObj = repoObjFactory.createFileObject(null);

        BinaryObject binObj = fileObj.addOriginalFile(contentUri, FILENAME, MIMETYPE, null, null);

        treeIndexer.indexAll(baseAddress);

        RepositoryObject parent = binObj.getParent();
        assertEquals("Parent of the binary must match the file object which created it",
                parent.getPid(), fileObj.getPid());
    }
}