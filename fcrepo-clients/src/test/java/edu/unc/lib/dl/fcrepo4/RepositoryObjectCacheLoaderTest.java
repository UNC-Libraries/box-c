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

import static edu.unc.lib.dl.util.RDFModelUtil.streamModel;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.IOException;
import java.util.UUID;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoResponse;
import org.fcrepo.client.RequestBuilder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import edu.unc.lib.dl.fedora.ObjectTypeMismatchException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.rdf.Fcrepo4Repository;
import edu.unc.lib.dl.test.SelfReturningAnswer;

/**
 *
 * @author bbpennel
 *
 */
public class RepositoryObjectCacheLoaderTest {

    private static String ETAG = "etag";
    private static final String ETAG_HEADER =  "\"etag\"";

    private RepositoryObjectCacheLoader objectCacheLoader;

    @Mock
    private RepositoryObjectDriver driver;

    private FcrepoClient client;
    @Mock
    private FcrepoResponse response;

    @Mock
    private PID pid;

    @Before
    public void init() {
        initMocks(this);

        client = mock(FcrepoClient.class, new BuilderReturningAnswer());

        when(response.getHeaderValue(eq("ETag"))).thenReturn(ETAG_HEADER);

        objectCacheLoader = new RepositoryObjectCacheLoader();
        objectCacheLoader.setClient(client);
        objectCacheLoader.setRepositoryObjectDriver(driver);

        pid = PIDs.get(UUID.randomUUID().toString());
    }

    @Test
    public void loadDepositRecordTest() throws Exception {

        pid = PIDs.get(RepositoryPathConstants.DEPOSIT_RECORD_BASE + "/" + UUID.randomUUID().toString());

        mockResponseBodyWithType(pid, Cdr.DepositRecord);

        RepositoryObject obj = objectCacheLoader.load(pid);

        assertTrue(obj instanceof DepositRecord);
        assertEquals(pid, obj.getPid());
        assertEquals(ETAG, obj.getEtag());
        assertTrue(obj.getResource().hasProperty(RDF.type, Cdr.DepositRecord));
    }

    @Test
    public void loadWorkTest() throws Exception {

        mockResponseBodyWithType(pid, Cdr.Work);

        RepositoryObject obj = objectCacheLoader.load(pid);

        assertTrue(obj instanceof WorkObject);
        assertEquals(pid, obj.getPid());
        assertEquals(ETAG, obj.getEtag());
        assertTrue(obj.getResource().hasProperty(RDF.type, Cdr.Work));
    }

    @Test
    public void loadFileObjectTest() throws Exception {

        mockResponseBodyWithType(pid, Cdr.FileObject);

        RepositoryObject obj = objectCacheLoader.load(pid);

        assertTrue(obj instanceof FileObject);
        assertEquals(pid, obj.getPid());
        assertEquals(ETAG, obj.getEtag());
        assertTrue(obj.getResource().hasProperty(RDF.type, Cdr.FileObject));
    }

    @Test
    public void loadFolderObjectTest() throws Exception {

        mockResponseBodyWithType(pid, Cdr.Folder);

        RepositoryObject obj = objectCacheLoader.load(pid);

        assertTrue(obj instanceof FolderObject);
        assertEquals(pid, obj.getPid());
        assertEquals(ETAG, obj.getEtag());
        assertTrue(obj.getResource().hasProperty(RDF.type, Cdr.Folder));
    }

    @Test
    public void loadCollectionObjectTest() throws Exception {

        mockResponseBodyWithType(pid, Cdr.Collection);

        RepositoryObject obj = objectCacheLoader.load(pid);

        assertTrue(obj instanceof CollectionObject);
        assertEquals(pid, obj.getPid());
        assertEquals(ETAG, obj.getEtag());
        assertTrue(obj.getResource().hasProperty(RDF.type, Cdr.Collection));
    }

    @Test
    public void loadContentRootObjectTest() throws Exception {

        mockResponseBodyWithType(pid, Cdr.ContentRoot);

        RepositoryObject obj = objectCacheLoader.load(pid);

        assertTrue(obj instanceof ContentRootObject);
        assertEquals(pid, obj.getPid());
        assertEquals(ETAG, obj.getEtag());
        assertTrue(obj.getResource().hasProperty(RDF.type, Cdr.ContentRoot));
    }

    @Test
    public void loadAdminUnitTest() throws Exception {

        mockResponseBodyWithType(pid, Cdr.AdminUnit);

        RepositoryObject obj = objectCacheLoader.load(pid);

        assertTrue(obj instanceof AdminUnit);
        assertEquals(pid, obj.getPid());
        assertEquals(ETAG, obj.getEtag());
        assertTrue(obj.getResource().hasProperty(RDF.type, Cdr.AdminUnit));
    }

    @Test
    public void loadBinaryObjectTest() throws Exception {

        mockResponseBodyWithType(pid, Fcrepo4Repository.Binary);

        RepositoryObject obj = objectCacheLoader.load(pid);

        assertTrue(obj instanceof BinaryObject);
        assertEquals(pid, obj.getPid());
        assertEquals(ETAG, obj.getEtag());
        assertTrue(obj.getResource().hasProperty(RDF.type, Fcrepo4Repository.Binary));
    }

    @Test(expected = ObjectTypeMismatchException.class)
    public void loadUnsupportedTypeTest() throws Exception {
        mockResponseBodyWithType(pid, Fcrepo4Repository.Pairtree);

        objectCacheLoader.load(pid);
    }

    @Test(expected = ObjectTypeMismatchException.class)
    public void invalidContentPidTest() throws Exception {
        pid = PIDs.get(RepositoryPathConstants.DEPOSIT_RECORD_BASE + "/" + UUID.randomUUID().toString());

        mockResponseBodyWithType(pid, Cdr.Work);

        objectCacheLoader.load(pid);
    }

    private void mockResponseBodyWithType(PID pid, Resource rdfType) throws IOException {
        Model model = ModelFactory.createDefaultModel();
        model.add(createResource(pid.getRepositoryPath()), RDF.type, rdfType);

        when(response.getBody()).thenReturn(streamModel(model));
    }

    private class BuilderReturningAnswer implements Answer<Object> {

        @Override
        public Object answer(InvocationOnMock invocation) throws Throwable {
            RequestBuilder builder = (RequestBuilder) mock(invocation.getMethod().getReturnType(),
                    new SelfReturningAnswer());

            when(builder.perform()).thenReturn(response);

            return builder;
        }
    }

}
