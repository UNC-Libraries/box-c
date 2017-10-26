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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.stream.Collectors;

import org.apache.activemq.util.ByteArrayInputStream;
import org.apache.jena.vocabulary.RDF;
import org.fcrepo.client.FcrepoResponse;
import org.junit.Test;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Fcrepo4Repository;

/**
 *
 * @author bbpennel
 *
 */
public class BinaryObjectIT extends AbstractFedoraIT {

    @Test
    public void createBinaryTest() throws Exception {
        // Create a parent object to put the binary into
        URI contentBase = createBaseContainer(RepositoryPathConstants.CONTENT_BASE);
        PID parentPid;
        try (FcrepoResponse response = client.post(contentBase).perform()) {
            parentPid = PIDs.get(response.getLocation());
        }

        URI uri = parentPid.getRepositoryUri();

        String bodyString = "Test text";
        String filename = "test.txt";
        String mimetype = "text/plain";
        String checksum = "82022e1782b92dce5461ee636a6c5bea8509ffee";
        InputStream contentStream = new ByteArrayInputStream(bodyString.getBytes());

        BinaryObject obj = repoObjFactory.createBinary(uri, "binary_test", contentStream, filename, mimetype, checksum, null, null);

        // Verify that the body of the binary is retrieved
        InputStream resultStream = obj.getBinaryStream();
        String respString = new BufferedReader(new InputStreamReader(resultStream)).lines()
                .collect(Collectors.joining("\n"));
        assertEquals("Binary content did not match submitted value", bodyString, respString);

        // Check that metadata is retrieved
        assertEquals(filename, obj.getFilename());
        assertEquals(mimetype, obj.getMimetype());
        assertEquals(9L, obj.getFilesize().longValue());
        assertEquals("urn:sha1:" + checksum, obj.getSha1Checksum());

        assertTrue(obj.getResource().hasProperty(RDF.type, Fcrepo4Repository.Binary));
    }
}
