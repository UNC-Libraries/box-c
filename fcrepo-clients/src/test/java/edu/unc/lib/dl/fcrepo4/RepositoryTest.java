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

import java.io.InputStream;
import java.net.URI;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.fcrepo.client.FcrepoClient;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.fedora.NotFoundException;
import edu.unc.lib.dl.fedora.ObjectTypeMismatchException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Premis;
import edu.unc.lib.dl.util.URIUtil;

/**
 *
 * @author bbpennel
 *
 */
public class RepositoryTest extends AbstractFedoraTest {

    private Repository repository;

    @Mock
    private RepositoryObjectFactory objFactory;

    @Mock
    private RepositoryObjectCacheLoader objectCacheLoader;

    @Mock
    private FcrepoClient fcrepoClient;

    @Before
    public void init() {
        initMocks(this);

        repository = new Repository();
        repository.setBaseUri(FEDORA_BASE);
        repository.setRepositoryObjectDataLoader(dataLoader);
        repository.setRepositoryObjectFactory(objFactory);
        repository.setRepositoryObjectCacheLoader(objectCacheLoader);
        repository.setClient(fcrepoClient);

        PIDs.setRepository(repository);

        repository.init();
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

        when(objectCacheLoader.load(eq(pid))).thenReturn(mock(DepositRecord.class));

        assertNotNull(repository.getDepositRecord(pid));
    }

    @Test(expected = ObjectTypeMismatchException.class)
    public void getDepositRecordWrongTypeTest() {
        PID pid = repository.mintDepositRecordPid();

        when(objectCacheLoader.load(eq(pid))).thenReturn(mock(WorkObject.class));

        repository.getDepositRecord(pid);
    }

    @Test
    public void getRepositoryObjectFolderTest() throws Exception {
        PID pid = repository.mintContentPid();

        when(objectCacheLoader.load(eq(pid))).thenReturn(mock(FolderObject.class));

        RepositoryObject resultObj = repository.getRepositoryObject(pid);
        assertTrue(resultObj instanceof FolderObject);
    }

    @Test(expected = NotFoundException.class)
    public void getRepositoryObjectExecutionExceptionTest() throws Exception {
        PID pid = repository.mintContentPid();

        FedoraException fedoraE = new NotFoundException("Not found");

        when(objectCacheLoader.load(eq(pid))).thenThrow(fedoraE);
        repository.getRepositoryObject(pid);
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

        when(objectCacheLoader.load(eq(pid))).thenReturn(mock(AdminUnit.class));

        assertNotNull(repository.getAdminUnit(pid));
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

        when(objectCacheLoader.load(eq(pid))).thenReturn(mock(CollectionObject.class));

        assertNotNull(repository.getCollectionObject(pid));
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

    @Test
    public void getFolderObjectTest() {
        PID pid = repository.mintContentPid();

        when(objectCacheLoader.load(eq(pid))).thenReturn(mock(FolderObject.class));

        assertNotNull(repository.getFolderObject(pid));
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

        when(objectCacheLoader.load(eq(pid))).thenReturn(mock(WorkObject.class));

        assertNotNull(repository.getWorkObject(pid));
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

        when(objectCacheLoader.load(eq(pid))).thenReturn(mock(FileObject.class));

        assertNotNull(repository.getFileObject(pid));
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

        when(objectCacheLoader.load(eq(pid))).thenReturn(mock(BinaryObject.class));

        assertNotNull(repository.getBinary(pid));
    }

    private PID getBinaryPid() {
        PID pid = repository.mintContentPid();
        URI binaryUri = URI.create(URIUtil.join(pid.getRepositoryPath(), "file"));
        return PIDs.get(binaryUri);
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
