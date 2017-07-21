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
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoResponse;
import org.fcrepo.client.GetBuilder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import edu.unc.lib.dl.fedora.ObjectTypeMismatchException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.rdf.Fcrepo4Repository;
import edu.unc.lib.dl.rdf.PcdmModels;
import edu.unc.lib.dl.rdf.Premis;
import edu.unc.lib.dl.util.URIUtil;

/**
 *
 * @author bbpennel
 *
 */
public class RepositoryTest extends AbstractFedoraTest {

    private static String ETAG = "etag";
    private static final String ETAG_HEADER =  "\"etag\"";

    private Repository repository;

    @Mock
    private RepositoryObjectFactory objFactory;

    @Mock
    private FcrepoClient fcrepoClient;

    @Before
    public void init() {
        initMocks(this);

        repository = new Repository();
        repository.setBaseUri(FEDORA_BASE);
        repository.setRepositoryObjectDataLoader(dataLoader);
        repository.setRepositoryObjectFactory(objFactory);
        repository.setClient(fcrepoClient);

        PIDs.setRepository(repository);
    }

    @Test
    public void mintDepositRecordPidTest() {
        PID pid = repository.mintDepositRecordPid();

        assertEquals(pid.getQualifier(), RepositoryPathConstants.DEPOSIT_RECORD_BASE);
        assertTrue(pid.getQualifiedId().startsWith(RepositoryPathConstants.DEPOSIT_RECORD_BASE));
    }

    @Test
    public void createDepositRecordTest() {
        PID pid = repository.mintDepositRecordPid();

        when(objFactory.createDepositRecord(eq(pid.getRepositoryUri()), (Model) isNull()))
                .thenReturn(pid.getRepositoryUri());

        DepositRecord obj = repository.createDepositRecord(pid, null);
        assertNotNull(obj);
        assertEquals(pid, obj.getPid());

        verify(objFactory).createDepositRecord(eq(pid.getRepositoryUri()), (Model) isNull());
    }

    @Test
    public void getDepositRecordTest() {
        PID pid = repository.mintDepositRecordPid();

        mockLoadTypes(Arrays.asList(Cdr.DepositRecord.getURI()));

        DepositRecord obj = repository.getDepositRecord(pid);
        assertEquals(pid, obj.getPid());
    }

    @Test
    public void getContentObjectFolderTest() throws Exception {
        PID pid = repository.mintContentPid();

        // Fake the fcrepo client and its response to return a model with the Folder type
        mockFcrepoResponse(pid, ETAG_HEADER, Cdr.Folder);
        // Fake the dataloader to also give back the same types
        mockLoadTypes(Arrays.asList(Cdr.Folder.getURI()));

        ContentObject obj = repository.getContentObject(pid);

        assertTrue("Incorrect type of object returned", obj instanceof FolderObject);
        assertEquals(pid, obj.getPid());
        assertEquals("Etag was not present or didn't match", ETAG, obj.getEtag());
        assertTrue(obj.getResource().hasProperty(RDF.type, Cdr.Folder));
    }

    @Test
    public void getContentObjectWorkTest() throws Exception {
        PID pid = repository.mintContentPid();

        // Fake the fcrepo client and its response to return a model with the Work type
        mockFcrepoResponse(pid, ETAG_HEADER, Cdr.Work);
        mockLoadTypes(Arrays.asList(Cdr.Work.getURI()));

        ContentObject obj = repository.getContentObject(pid);

        assertTrue("Incorrect type of object returned", obj instanceof WorkObject);
        assertEquals(pid, obj.getPid());
        assertEquals("Etag was not present or didn't match", ETAG, obj.getEtag());
        assertTrue(obj.getResource().hasProperty(RDF.type, Cdr.Work));
    }

    @Test
    public void getContentObjectFileTest() throws Exception {
        PID pid = repository.mintContentPid();

        mockFcrepoResponse(pid, ETAG_HEADER, Cdr.FileObject);

        mockLoadTypes(Arrays.asList(Cdr.FileObject.getURI()));

        ContentObject obj = repository.getContentObject(pid);

        assertTrue("Incorrect type of object returned", obj instanceof FileObject);
        assertEquals(pid, obj.getPid());
        assertEquals("Etag was not present or didn't match", ETAG, obj.getEtag());
        assertTrue(obj.getResource().hasProperty(RDF.type, Cdr.FileObject));
    }

    @Test(expected = ObjectTypeMismatchException.class)
    public void getContentObjectInvalidTypeTest() throws Exception {
        PID pid = repository.mintContentPid();

        // Response returns DepositRecord type, which isn't a ContentObject
        mockFcrepoResponse(pid, ETAG_HEADER, Cdr.DepositRecord);

        // Make the dataloader agree
        mockLoadTypes(Arrays.asList(Cdr.DepositRecord.getURI()));

        repository.getContentObject(pid);
    }

    /**
     * Mocks a fcrepoResponse for a get request to retrieve a model containing a
     * particular rdf type
     *
     * @param pid
     * @param etag
     * @param type
     * @throws Exception
     */
    private void mockFcrepoResponse(PID pid, String etag, Resource type) throws Exception {
        FcrepoResponse mockResp = mock(FcrepoResponse.class);
        String turtle = "<" + pid.getRepositoryPath() + "> a <"
                + type.getURI() + "> .";
        when(mockResp.getBody()).thenReturn(new ByteArrayInputStream(turtle.getBytes()));
        when(mockResp.getHeaderValue(eq("ETag"))).thenReturn(etag);

        GetBuilder mockGet = mock(GetBuilder.class);
        when(fcrepoClient.get(any(URI.class))).thenReturn(mockGet);
        when(mockGet.accept(anyString())).thenReturn(mockGet);
        when(mockGet.perform()).thenReturn(mockResp);
    }

    @Test(expected = ObjectTypeMismatchException.class)
    public void getContentObjectInvalidBaseTest() {
        PID pid = PIDs.get("invalid/43f3016b-9cb0-4d7b-92a4-0e2921362a66");

        repository.getContentObject(pid);
    }

    @Test(expected = ObjectTypeMismatchException.class)
    public void getContentObjectInvalidComponentTest() {
        PID pid = getBinaryPid();

        repository.getContentObject(pid);
    }

    @Test
    public void createAdminUnitTest() {
        PID pid = repository.mintContentPid();

        when(objFactory.createAdminUnit(eq(pid.getRepositoryUri()), (Model) isNull()))
                .thenReturn(pid.getRepositoryUri());

        AdminUnit obj = repository.createAdminUnit(pid);
        assertNotNull(obj);
        assertEquals(pid, obj.getPid());

        verify(objFactory).createAdminUnit(eq(pid.getRepositoryUri()), (Model) isNull());
    }

    @Test
    public void getAdminUnitTest() {
        PID pid = repository.mintContentPid();

        mockLoadTypes(Arrays.asList(Cdr.AdminUnit.getURI(), PcdmModels.Collection.getURI()));

        AdminUnit obj = repository.getAdminUnit(pid);
        assertEquals(pid, obj.getPid());
    }

    @Test(expected = ObjectTypeMismatchException.class)
    public void createAdminUnitAtInvalidPathTest() {
        PID pid = PIDs.get("invalid/43f3016b-9cb0-4d7b-92a4-0e2921362a66");

        repository.createAdminUnit(pid);
    }

    @Test
    public void createCollectionObjectTest() {
        PID pid = repository.mintContentPid();

        when(objFactory.createCollectionObject(eq(pid.getRepositoryUri()), (Model) isNull()))
                .thenReturn(pid.getRepositoryUri());

        CollectionObject obj = repository.createCollectionObject(pid);
        assertNotNull(obj);
        assertEquals(pid, obj.getPid());

        verify(objFactory).createCollectionObject(eq(pid.getRepositoryUri()), (Model) isNull());
    }

    @Test
    public void getCollectionObjectTest() {
        PID pid = repository.mintContentPid();

        mockLoadTypes(Arrays.asList(Cdr.Collection.getURI(), PcdmModels.Object.getURI()));

        CollectionObject obj = repository.getCollectionObject(pid);
        assertEquals(pid, obj.getPid());
    }

    @Test(expected = ObjectTypeMismatchException.class)
    public void createCollectionObjectAtInvalidPathTest() {
        PID pid = PIDs.get("invalid/43f3016b-9cb0-4d7b-92a4-0e2921362a66");

        repository.createCollectionObject(pid);
    }

    @Test
    public void createFolderObjectTest() {
        PID pid = repository.mintContentPid();

        when(objFactory.createFolderObject(eq(pid.getRepositoryUri()), (Model) isNull()))
                .thenReturn(pid.getRepositoryUri());

        FolderObject obj = repository.createFolderObject(pid);
        assertNotNull(obj);
        assertEquals(pid, obj.getPid());

        verify(objFactory).createFolderObject(eq(pid.getRepositoryUri()), (Model) isNull());
    }

    @Test(expected = ObjectTypeMismatchException.class)
    public void createFolderObjectAtInvalidPathTest() {
        PID pid = PIDs.get("invalid/43f3016b-9cb0-4d7b-92a4-0e2921362a66");

        repository.createFolderObject(pid);
    }

    @Test
    public void getFolderObjectTest() {
        PID pid = repository.mintContentPid();

        mockLoadTypes(Arrays.asList(Cdr.Folder.getURI()));

        FolderObject obj = repository.getFolderObject(pid);
        assertEquals(pid, obj.getPid());
    }

    @Test
    public void createWorkObjectTest() {
        PID pid = repository.mintContentPid();

        when(objFactory.createWorkObject(eq(pid.getRepositoryUri()), (Model) isNull()))
                .thenReturn(pid.getRepositoryUri());

        WorkObject obj = repository.createWorkObject(pid);
        assertNotNull(obj);
        assertEquals(pid, obj.getPid());

        verify(objFactory).createWorkObject(eq(pid.getRepositoryUri()), (Model) isNull());
    }

    @Test
    public void getWorkObjectTest() {
        PID pid = repository.mintContentPid();

        mockLoadTypes(Arrays.asList(Cdr.Work.getURI()));

        WorkObject obj = repository.getWorkObject(pid);
        assertEquals(pid, obj.getPid());
    }

    @Test(expected = ObjectTypeMismatchException.class)
    public void createWorkObjectAtInvalidPathTest() {
        PID pid = PIDs.get("invalid/43f3016b-9cb0-4d7b-92a4-0e2921362a66");

        repository.createWorkObject(pid);
    }

    @Test
    public void createFileObjectTest() {
        PID pid = repository.mintContentPid();

        when(objFactory.createFileObject(eq(pid.getRepositoryUri()), (Model) isNull()))
                .thenReturn(pid.getRepositoryUri());

        FileObject obj = repository.createFileObject(pid);
        assertEquals(pid, obj.getPid());

        verify(objFactory).createFileObject(eq(pid.getRepositoryUri()), (Model) isNull());
    }

    @Test
    public void getFileObjectTest() {
        PID pid = repository.mintContentPid();

        mockLoadTypes(Arrays.asList(Cdr.FileObject.getURI()));

        FileObject obj = repository.getFileObject(pid);
        assertEquals(pid, obj.getPid());
    }

    @Test(expected = ObjectTypeMismatchException.class)
    public void createFileObjectAtInvalidPathTest() {
        PID pid = PIDs.get("invalid/43f3016b-9cb0-4d7b-92a4-0e2921362a66");

        repository.createFileObject(pid);
    }

    @Test
    public void createBinaryTest() {
        URI binaryUri = getBinaryPid().getRepositoryUri();

        String slug = "slug";
        InputStream content = mock(InputStream.class);
        String filename = "file.ext";
        String mimetype = "application/octet-stream";
        String checksum = "checksum";
        Model model = mock(Model.class);

        when(objFactory.createBinary(any(URI.class), anyString(), any(InputStream.class),
                anyString(), anyString(), anyString(), any(Model.class)))
                .thenReturn(binaryUri);

        BinaryObject obj = repository.createBinary(binaryUri, slug, content, filename,
                mimetype, checksum, model);

        assertEquals(binaryUri, obj.getPid().getRepositoryUri());
        verify(objFactory).createBinary(eq(binaryUri), eq(slug), eq(content), eq(filename),
                eq(mimetype), eq(checksum), eq(model));
    }

    @Test
    public void getBinaryTest() {
        PID pid = getBinaryPid();

        mockLoadTypes(Arrays.asList(Fcrepo4Repository.Binary.getURI()));

        BinaryObject obj = repository.getBinary(pid);

        assertEquals(pid, obj.getPid());
    }

    private PID getBinaryPid() {
        PID pid = repository.mintContentPid();
        URI binaryUri = URI.create(URIUtil.join(pid.getRepositoryPath(), "file"));
        return PIDs.get(binaryUri);
    }

    private void mockLoadTypes(List<String> types) {
        when(dataLoader.loadTypes(any(RepositoryObject.class))).thenAnswer(new Answer<RepositoryObjectDataLoader>() {
            @Override
            public RepositoryObjectDataLoader answer(InvocationOnMock invocation) throws Throwable {
                RepositoryObject obj = invocation.getArgumentAt(0, RepositoryObject.class);
                obj.setTypes(types);
                return dataLoader;
            }
        });
    }

    @Test
    public void createPremisEventTest() {
        PID parentPid = repository.mintContentPid();
        PID eventPid = repository.mintPremisEventPid(parentPid);
        URI eventUri = eventPid.getRepositoryUri();

        when(objFactory.createObject(eq(eventUri), any(Model.class)))
                .thenReturn(eventUri);

        final Model model = ModelFactory.createDefaultModel();
        Resource resc = model.getResource(eventPid.getRepositoryPath());
        resc.addProperty(Premis.hasEventType, Premis.Ingestion);

        when(dataLoader.loadModel(any(RepositoryObject.class))).thenAnswer(new Answer<RepositoryObjectDataLoader>() {
            @Override
            public RepositoryObjectDataLoader answer(InvocationOnMock invocation) throws Throwable {
                RepositoryObject premisObj = invocation.getArgumentAt(0, RepositoryObject.class);
                premisObj.storeModel(model);
                return dataLoader;
            }
        });

        PremisEventObject obj = repository.createPremisEvent(eventPid, model);
        assertEquals(eventPid, obj.getPid());
        assertTrue(obj.getResource().hasProperty(Premis.hasEventType, Premis.Ingestion));

        verify(objFactory).createObject(eq(eventUri), any(Model.class));
    }

    @Test
    public void getPremisEventTest() {
        PID parentPid = repository.mintContentPid();
        PID eventPid = repository.mintPremisEventPid(parentPid);

        PremisEventObject obj = repository.getPremisEvent(eventPid);
        assertEquals(eventPid, obj.getPid());
    }

    @Test
    public void addMemberTest() {
        PID parentPid = repository.mintContentPid();
        ContentObject parent = mock(ContentObject.class);
        when(parent.getPid()).thenReturn(parentPid);

        PID memberPid = repository.mintContentPid();
        ContentObject member = mock(ContentObject.class);
        when(member.getPid()).thenReturn(memberPid);

        repository.addMember(parent, member);

        verify(objFactory).createMemberLink(parentPid.getRepositoryUri(),
                memberPid.getRepositoryUri());
    }
}
