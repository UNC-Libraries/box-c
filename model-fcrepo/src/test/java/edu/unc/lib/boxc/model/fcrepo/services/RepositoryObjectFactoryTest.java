package edu.unc.lib.boxc.model.fcrepo.services;

import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoOperationFailedException;
import org.fcrepo.client.FcrepoResponse;
import org.fcrepo.client.PostBuilder;
import org.fcrepo.client.PutBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import edu.unc.lib.boxc.common.test.SelfReturningAnswer;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.ids.PIDMinter;
import edu.unc.lib.boxc.model.api.objects.AdminUnit;
import edu.unc.lib.boxc.model.api.objects.BinaryObject;
import edu.unc.lib.boxc.model.api.objects.CollectionObject;
import edu.unc.lib.boxc.model.api.objects.ContentObject;
import edu.unc.lib.boxc.model.api.objects.DepositRecord;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.FolderObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.api.sparql.SparqlUpdateService;
import edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPIDMinter;
/**
 *
 * @author bbpennel
 * @author harring
 *
 */
public class RepositoryObjectFactoryTest {
    private AutoCloseable closeable;

    @Mock
    private LdpContainerFactory ldpFactory;
    @Mock
    private RepositoryObjectLoader repoObjLoader;
    @Mock
    private SparqlUpdateService sparqlUpdateService;
    @Mock
    private FcrepoClient fcrepoClient;
    @Mock
    private PutBuilder mockPutBuilder;
    @Mock
    private PostBuilder mockPostBuilder;
    @Mock
    private FcrepoResponse mockResponse;
    @Mock
    private PID pid;

    private RepositoryObjectFactoryImpl repoObjFactory;
    private PIDMinter pidMinter;
    private List<URI> linkHeaders;

    @BeforeEach
    public void init() throws FcrepoOperationFailedException, URISyntaxException {
        closeable = openMocks(this);
        repoObjFactory = new RepositoryObjectFactoryImpl();
        repoObjFactory.setClient(fcrepoClient);
        repoObjFactory.setLdpFactory(ldpFactory);
        repoObjFactory.setSparqlUpdateService(sparqlUpdateService);
        repoObjFactory.setRepositoryObjectLoader(repoObjLoader);
        pidMinter = new RepositoryPIDMinter();
        repoObjFactory.setPidMinter(pidMinter);
        linkHeaders = new ArrayList<>();
        URI testHeader = new URI("/path/to/resource");
        linkHeaders.add(testHeader);

        mockPutBuilder = mock(PutBuilder.class, new SelfReturningAnswer());
        when(fcrepoClient.put(any(URI.class))).thenReturn(mockPutBuilder);
        when(mockPutBuilder.perform()).thenReturn(mockResponse);

        mockPostBuilder = mock(PostBuilder.class, new SelfReturningAnswer());
        when(fcrepoClient.post(any(URI.class))).thenReturn(mockPostBuilder);
        when(mockPostBuilder.perform()).thenReturn(mockResponse);
        when(mockResponse.getLinkHeaders(any(String.class))).thenReturn(linkHeaders);

    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void createDepositRecordTest() {
        when(repoObjLoader.getDepositRecord(any(PID.class))).thenReturn(mock(DepositRecord.class));
        DepositRecord obj = repoObjFactory.createDepositRecord(null);
        assertNotNull(obj);
    }

    @Test
    public void createAdminUnitTest() {
        when(repoObjLoader.getAdminUnit(any(PID.class))).thenReturn(mock(AdminUnit.class));
        AdminUnit obj = repoObjFactory.createAdminUnit(null);
        assertNotNull(obj);
    }

    @Test
    public void createCollectionObjectTest() throws Exception {
        when(repoObjLoader.getCollectionObject(any(PID.class))).thenReturn(mock(CollectionObject.class));
        CollectionObject obj = repoObjFactory.createCollectionObject(null);
        assertNotNull(obj);
    }

    @Test
    public void createFolderObjectTest() throws Exception {
        when(repoObjLoader.getFolderObject(any(PID.class))).thenReturn(mock(FolderObject.class));
        FolderObject obj = repoObjFactory.createFolderObject(null);
        assertNotNull(obj);
    }

    @Test
    public void createWorkObjectTest() {
        when(repoObjLoader.getWorkObject(any(PID.class))).thenReturn(mock(WorkObject.class));
        WorkObject obj = repoObjFactory.createWorkObject(null);
        assertNotNull(obj);
    }

    @Test
    public void createFileObjectTest() {
        when(repoObjLoader.getFileObject(any(PID.class))).thenReturn(mock(FileObject.class));
        FileObject obj = repoObjFactory.createFileObject(null);
        assertNotNull(obj);
    }

    @Test
    public void createFolderWithPidTest() {
        PID pid = pidMinter.mintContentPid();
        FileObject mockFile = mock(FileObject.class);
        when(mockFile.getPid()).thenReturn(pid);
        when(repoObjLoader.getFileObject(pid)).thenReturn(mockFile);
        FileObject obj = repoObjFactory.createFileObject(pid, null);
        assertNotNull(obj);
        assertEquals(pid, obj.getPid());
    }

    @Test
    public void createBinaryTest() throws FcrepoOperationFailedException {
        PID pid = pidMinter.mintContentPid();
        URI binaryUri = pid.getRepositoryUri();
        when(mockResponse.getLocation()).thenReturn(binaryUri);
        BinaryObject mockBinary = mock(BinaryObject.class);
        when(mockBinary.getPid()).thenReturn(pid);
        when(repoObjLoader.getBinaryObject(any(PID.class))).thenReturn(mockBinary);

        String slug = "slug";
        InputStream content = mock(InputStream.class);
        String filename = "file.ext";
        String mimetype = "application/octet-stream";
        String sha1Checksum = "checksum";

        BinaryObject obj = repoObjFactory.createBinary(binaryUri, slug, content, filename,
                mimetype, sha1Checksum, null, null);

        assertTrue(obj.getPid().getRepositoryPath().startsWith(binaryUri.toString()));
        // check to see that client creates FcrepoResponse
        verify(mockPostBuilder).perform();
    }

    @Test
    public void addMemberTest() {
        PID parentPid = pidMinter.mintContentPid();
        ContentObject parent = mock(ContentObject.class);
        when(parent.getPid()).thenReturn(parentPid);
        when(parent.getUri()).thenReturn(parentPid.getRepositoryUri());
        when(parent.getResource()).thenReturn(createResource(parentPid.getRepositoryPath()));

        Model memberModel = ModelFactory.createDefaultModel();
        PID memberPid = pidMinter.mintContentPid();
        ContentObject member = mock(ContentObject.class);
        when(member.getPid()).thenReturn(memberPid);
        when(member.getModel(true)).thenReturn(memberModel);
        when(member.getMetadataUri()).thenReturn(memberPid.getRepositoryUri());

        repoObjFactory.addMember(parent, member);

        verify(sparqlUpdateService).executeUpdate(eq(memberPid.getRepositoryPath()), anyString());
    }
}
