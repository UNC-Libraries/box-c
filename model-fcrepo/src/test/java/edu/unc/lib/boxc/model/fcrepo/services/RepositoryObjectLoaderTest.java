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
package edu.unc.lib.boxc.model.fcrepo.services;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.net.URI;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import com.google.common.cache.LoadingCache;

import edu.unc.lib.boxc.common.util.URIUtil;
import edu.unc.lib.boxc.model.api.exceptions.FedoraException;
import edu.unc.lib.boxc.model.api.exceptions.NotFoundException;
import edu.unc.lib.boxc.model.api.exceptions.ObjectTypeMismatchException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.ids.PIDMinter;
import edu.unc.lib.boxc.model.api.objects.AdminUnit;
import edu.unc.lib.boxc.model.api.objects.BinaryObject;
import edu.unc.lib.boxc.model.api.objects.CollectionObject;
import edu.unc.lib.boxc.model.api.objects.DepositRecord;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.FolderObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObject;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.model.fcrepo.objects.RepositoryObjectCacheLoader;

/**
 *
 * @author harring
 *
 */
public class RepositoryObjectLoaderTest {

    @Mock
    private RepositoryObjectCacheLoader objectCacheLoader;
    @Mock
    private PIDMinter pidMinter;

    private RepositoryObjectLoaderImpl repoObjLoader;
    private PID contentPid;
    private PID depositRecordPid;
    private PID premisPid;

    @Before
    public void init() {
        initMocks(this);

        contentPid = PIDs.get("content/uuid:0311cf7e-9ac0-4ab0-8c24-ff367e8e77f5");
        depositRecordPid = PIDs.get("deposit/uuid:0411cf7e-9ac0-4ab0-8c24-ff367e8e77f6");
        premisPid = PIDs.get("premis/uuid:0511cf7e-9ac0-4ab0-8c24-ff367e8e77f7");

        repoObjLoader = new RepositoryObjectLoaderImpl();
        repoObjLoader.setRepositoryObjectCacheLoader(objectCacheLoader);
        repoObjLoader.setCacheMaxSize(10L);
        repoObjLoader.setCacheTimeToLive(1000L);
        repoObjLoader.init();

        when(pidMinter.mintContentPid()).thenReturn(contentPid);
        when(pidMinter.mintDepositRecordPid()).thenReturn(depositRecordPid);
        when(pidMinter.mintPremisEventPid(any(PID.class))).thenReturn(premisPid);
    }

    @Test
    public void getBinaryTest() {
        PID pid = getBinaryPid();

        when(objectCacheLoader.load(eq(pid))).thenReturn(mock(BinaryObject.class));

        assertNotNull(repoObjLoader.getBinaryObject(pid));
    }

    private PID getBinaryPid() {
        PID pid = pidMinter.mintContentPid();
        URI binaryUri = URI.create(URIUtil.join(pid.getRepositoryPath(), "file"));
        return PIDs.get(binaryUri);
    }

    @Test
    public void getFileObjectTest() {
        PID pid = pidMinter.mintContentPid();

        when(objectCacheLoader.load(eq(pid))).thenReturn(mock(FileObject.class));

        assertNotNull(repoObjLoader.getFileObject(pid));
    }

    @Test
    public void getWorkObjectTest() {
        PID pid = pidMinter.mintContentPid();

        when(objectCacheLoader.load(eq(pid))).thenReturn(mock(WorkObject.class));

        assertNotNull(repoObjLoader.getWorkObject(pid));
    }

    @Test
    public void getFolderObjectTest() {
        PID pid = pidMinter.mintContentPid();

        when(objectCacheLoader.load(eq(pid))).thenReturn(mock(FolderObject.class));

        assertNotNull(repoObjLoader.getFolderObject(pid));
    }

    @Test
    public void getCollectionObjectTest() {
        PID pid = pidMinter.mintContentPid();

        when(objectCacheLoader.load(eq(pid))).thenReturn(mock(CollectionObject.class));

        assertNotNull(repoObjLoader.getCollectionObject(pid));
    }

    @Test
    public void getAdminUnitTest() {
        PID pid = pidMinter.mintContentPid();

        when(objectCacheLoader.load(eq(pid))).thenReturn(mock(AdminUnit.class));

        assertNotNull(repoObjLoader.getAdminUnit(pid));
    }

    @Test
    public void getDepositRecordTest() {
        PID pid = pidMinter.mintDepositRecordPid();

        when(objectCacheLoader.load(eq(pid))).thenReturn(mock(DepositRecord.class));

        assertNotNull(repoObjLoader.getDepositRecord(pid));
    }

    @Test(expected = ObjectTypeMismatchException.class)
    public void getDepositRecordWrongTypeTest() {
        PID pid = pidMinter.mintDepositRecordPid();

        when(objectCacheLoader.load(eq(pid))).thenReturn(mock(WorkObject.class));

        repoObjLoader.getDepositRecord(pid);
    }

    @Test
    public void getRepositoryObjectFolderTest() throws Exception {
        PID pid = pidMinter.mintContentPid();

        when(objectCacheLoader.load(eq(pid))).thenReturn(mock(FolderObject.class));

        RepositoryObject resultObj = repoObjLoader.getRepositoryObject(pid);
        assertTrue(resultObj instanceof FolderObject);
    }

    @Test(expected = NotFoundException.class)
    public void getRepositoryObjectExecutionExceptionTest() throws Exception {
        PID pid = pidMinter.mintContentPid();

        FedoraException fedoraE = new NotFoundException("Not found");

        when(objectCacheLoader.load(eq(pid))).thenThrow(fedoraE);
        repoObjLoader.getRepositoryObject(pid);
    }

    @Test
    public void invalidateTest() throws Exception {
        PID pid = pidMinter.mintContentPid();

        when(objectCacheLoader.load(eq(pid))).thenReturn(mock(FolderObject.class));
        // Trigger population of cache
        repoObjLoader.getRepositoryObject(pid);

        LoadingCache<PID, RepositoryObject> repoObjCache = repoObjLoader.getRepositoryObjectCache();
        assertNotNull(repoObjCache.getIfPresent(pid));
        repoObjLoader.invalidate(pid);
        assertNull(repoObjCache.getIfPresent(pid));
    }
}
