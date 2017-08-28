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
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import edu.unc.lib.dl.fedora.ObjectTypeMismatchException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Ebucore;
import edu.unc.lib.dl.rdf.Fcrepo4Repository;
import edu.unc.lib.dl.rdf.Premis;
import edu.unc.lib.dl.util.URIUtil;

public class BinaryObjectTest extends AbstractFedoraTest {
    @Mock
    private PID mockPid;

    private BinaryObject binObj;
    private Model model;
    private Resource resource;

    private ByteArrayInputStream stream;

    @Before
    public void init() throws URISyntaxException {
        MockitoAnnotations.initMocks(this);

        byte[] buf = new byte[10];
        stream = new ByteArrayInputStream(buf);

        URI repoUri = new URI(FEDORA_BASE);
        when(mockPid.getRepositoryUri()).thenReturn(repoUri);
        when(repoPaths.getMetadataUri(any(PID.class))).thenReturn(
                URI.create(URIUtil.join(repoUri, RepositoryPathConstants.FCR_METADATA)));

        binObj = new BinaryObject(mockPid, repoObjLoader, dataLoader, repoObjFactory);

        when(dataLoader.loadModel(binObj)).thenReturn(dataLoader);

        setupModel();
    }

    private void setupModel() {
        model = ModelFactory.createDefaultModel();
        binObj.storeModel(model);
        resource = binObj.getResource();
    }

    @Test
    public void testGetMetadataUri() {
        assertEquals(FEDORA_BASE + RepositoryPathConstants.FCR_METADATA, binObj.getMetadataUri().toString());
    }

    @Test
    public void testValidateType() {
        // Return the correct RDF types
        List<String> types = Arrays.asList(Fcrepo4Repository.Binary.toString());
         when(dataLoader.loadTypes(eq(binObj))).thenAnswer(new Answer<RepositoryObjectDataLoader>() {
            @Override
            public RepositoryObjectDataLoader answer(InvocationOnMock invocation) throws Throwable {
                 binObj.setTypes(types);
                 return dataLoader;
             }
         });

        binObj.validateType();
    }

    @Test(expected = ObjectTypeMismatchException.class)
    public void invalidTypeTest() {
        when(dataLoader.loadTypes(eq(binObj))).thenAnswer(new Answer<RepositoryObjectDataLoader>() {
            @Override
             public RepositoryObjectDataLoader answer(InvocationOnMock invocation) throws Throwable {
                binObj.setTypes(Arrays.asList());
                return dataLoader;
             }
        });

         binObj.validateType();
     }

    @Test
    public void testGetBinaryStream() {
        when(dataLoader.getBinaryStream(binObj)).thenReturn(stream);
        assertEquals(binObj.getBinaryStream(), stream);
    }

    @Test
    public void testGetFilename() {
        resource.addProperty(Ebucore.filename, "example.txt");
        assertEquals("example.txt", binObj.getFilename());

        binObj.setFilename("sample.txt");
        assertEquals("sample.txt", binObj.getFilename());
    }


    @Test
    public void testGetMimetype() {
        binObj.setMimetype("text/plain");
        assertEquals("text/plain", binObj.getMimetype());

        binObj.setMimetype(null);
        resource.addProperty(Ebucore.hasMimeType, "application/json");
        assertEquals("application/json", binObj.getMimetype());
    }

    @Test
    public void testGetChecksum() {
        binObj.setChecksum("abcd1234");
        assertEquals("abcd1234", binObj.getChecksum());

        binObj.setChecksum(null);
        resource.addProperty(Premis.hasMessageDigest, "12345-67890");
        assertEquals("12345-67890", binObj.getChecksum());
    }

    @Test
    public void testGetFilesize() {
        long size = 42;
        binObj.setFilesize(size);
        assertTrue(size == binObj.getFilesize());

        binObj.setFilesize(null);
        resource.addProperty(Premis.hasSize, Long.toString(99));
        assertTrue(binObj.getFilesize() == 99L);
    }
}