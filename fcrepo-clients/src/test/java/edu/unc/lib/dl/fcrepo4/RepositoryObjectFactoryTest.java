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

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Premis;

/**
 *
 * @author bbpennel
 * @author harring
 *
 */
public class RepositoryObjectFactoryTest {

    @Mock
    private RepositoryObjectCacheLoader objectCacheLoader;
    @Mock
    private RepositoryObjectDataLoader dataLoader;
    @Mock
    private FcrepoClient fcrepoClient;

    private RepositoryObjectFactory repoObjFactory;
    private RepositoryPIDMinter pidMinter;

    @Before
    public void init() {
        initMocks(this);

        repoObjFactory = new RepositoryObjectFactory();
        pidMinter = new RepositoryPIDMinter();
    }

    @Test
    public void mintDepositRecordPidTest() {
        PID pid = pidMinter.mintDepositRecordPid();

        assertEquals(pid.getQualifier(), RepositoryPathConstants.DEPOSIT_RECORD_BASE);
        assertTrue(pid.getQualifiedId().startsWith(RepositoryPathConstants.DEPOSIT_RECORD_BASE));
    }

    @Test
    public void createDepositRecordTest() {
        PID pid = pidMinter.mintDepositRecordPid();

        DepositRecord obj = repoObjFactory.createDepositRecord(pid, null);
        assertNotNull(obj);
        assertEquals(pid, obj.getPid());

        verify(repoObjFactory).createDepositRecord(eq(pid), (Model) isNull());
    }

    @Test
    public void createAdminUnitTest() {
        PID pid = pidMinter.mintContentPid();

        AdminUnit obj = repoObjFactory.createAdminUnit(pid);
        assertNotNull(obj);
        assertEquals(pid, obj.getPid());

        verify(repoObjFactory).createAdminUnit(eq(pid), (Model) isNull());
    }

    @Test
    public void createCollectionObjectTest() {
        PID pid = pidMinter.mintContentPid();

        CollectionObject obj = repoObjFactory.createCollectionObject(pid);
        assertNotNull(obj);
        assertEquals(pid, obj.getPid());

        verify(repoObjFactory).createCollectionObject(eq(pid), (Model) isNull());
    }

    @Test
    public void createFolderObjectTest() {
        PID pid = pidMinter.mintContentPid();

        FolderObject obj = repoObjFactory.createFolderObject(pid);
        assertNotNull(obj);
        assertEquals(pid, obj.getPid());

        verify(repoObjFactory).createFolderObject(eq(pid), (Model) isNull());
    }

    @Test
    public void createWorkObjectTest() {
        PID pid = pidMinter.mintContentPid();

        WorkObject obj = repoObjFactory.createWorkObject(pid);
        assertNotNull(obj);
        assertEquals(pid, obj.getPid());

        verify(repoObjFactory).createWorkObject(eq(pid), (Model) isNull());
    }

    @Test
    public void createFileObjectTest() {
        PID pid = pidMinter.mintContentPid();

        FileObject obj = repoObjFactory.createFileObject(pid);
        assertEquals(pid, obj.getPid());

        verify(repoObjFactory).createFileObject(eq(pid), (Model) isNull());
    }

    @Test
    public void createBinaryTest() {
        PID pid = pidMinter.mintContentPid();
        URI binaryUri = pid.getRepositoryUri();

        String slug = "slug";
        InputStream content = mock(InputStream.class);
        String filename = "file.ext";
        String mimetype = "application/octet-stream";
        String checksum = "checksum";
        Model model = mock(Model.class);

        BinaryObject obj = repoObjFactory.createBinary(binaryUri, slug, content, filename,
                mimetype, checksum, pid, model);

        assertEquals(binaryUri, obj.getPid().getRepositoryUri());
        verify(repoObjFactory).createBinary(eq(binaryUri), eq(slug), eq(content), eq(filename),
                eq(mimetype), eq(checksum), eq(pid), eq(model));
    }

    @Test
    public void createPremisEventTest() {
        PID parentPid = pidMinter.mintContentPid();
        PID eventPid = pidMinter.mintPremisEventPid(parentPid);
        URI eventUri = eventPid.getRepositoryUri();

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

        PremisEventObject obj = repoObjFactory.createPremisEventObject(eventPid, model);
        assertEquals(eventPid, obj.getPid());
        assertTrue(obj.getResource().hasProperty(Premis.hasEventType, Premis.Ingestion));

        verify(repoObjFactory).createObject(eq(eventUri), any(Model.class));
    }

    @Test
    public void addMemberTest() {
        PID parentPid = pidMinter.mintContentPid();
        ContentObject parent = mock(ContentObject.class);
        when(parent.getPid()).thenReturn(parentPid);

        PID memberPid = pidMinter.mintContentPid();
        ContentObject member = mock(ContentObject.class);
        when(member.getPid()).thenReturn(memberPid);

        repoObjFactory.addMember(parent, member);

        verify(repoObjFactory).createMemberLink(parentPid.getRepositoryUri(),
                memberPid.getRepositoryUri());
    }
}
