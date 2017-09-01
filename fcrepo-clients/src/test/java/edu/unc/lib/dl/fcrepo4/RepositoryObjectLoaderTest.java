package edu.unc.lib.dl.fcrepo4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.net.URI;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.fedora.NotFoundException;
import edu.unc.lib.dl.fedora.ObjectTypeMismatchException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.URIUtil;

public class RepositoryObjectLoaderTest extends AbstractFedoraTest {

    @Mock
    RepositoryObjectCacheLoader objectCacheLoader;

    private RepositoryPIDMinter pidMinter;

    @Before
    public void init() {
        initMocks(this);

        pidMinter = new RepositoryPIDMinter();
    }

    @Test
    public void getPremisEventTest() {
        PID parentPid = pidMinter.mintContentPid();
        PID eventPid = pidMinter.mintPremisEventPid(parentPid);

        PremisEventObject obj = repoObjLoader.getPremisEventObject(eventPid);
        assertEquals(eventPid, obj.getPid());
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


}
