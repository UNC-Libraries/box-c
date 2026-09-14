package edu.unc.lib.boxc.model.fcrepo.services;

import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.fcrepo.client.FedoraHeaderConstants.LINK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import edu.unc.lib.boxc.model.api.rdf.Ebucore;
import edu.unc.lib.boxc.model.api.rdf.Fcrepo4Repository;
import edu.unc.lib.boxc.model.api.rdf.PcdmModels;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.update.UpdateAction;
import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoOperationFailedException;
import org.fcrepo.client.FcrepoResponse;
import org.fcrepo.client.HeadBuilder;
import org.fcrepo.client.PatchBuilder;
import org.fcrepo.client.PostBuilder;
import org.fcrepo.client.PutBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
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
import edu.unc.lib.boxc.model.api.objects.RepositoryObject;
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
    private PatchBuilder mockPatchBuilder;
    @Mock
    private FcrepoResponse mockResponse;
    @Mock
    private HeadBuilder mockHeadBuilder;
    @Mock
    private PID pid;
    @Captor
    private ArgumentCaptor<String> sparqlCaptor;

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

        mockPatchBuilder = mock(PatchBuilder.class, new SelfReturningAnswer());
        when(fcrepoClient.patch(any(URI.class))).thenReturn(mockPatchBuilder);
        when(mockPatchBuilder.perform()).thenReturn(mockResponse);
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
        verify(mockPutBuilder).addHeader(LINK, archivalGroupLink());
    }

    @Test
    public void createDepositRecordWithPidTest() throws Exception {
        DepositRecord expected = mock(DepositRecord.class);
        PID depositPid = pidMinter.mintDepositRecordPid();
        when(repoObjLoader.getDepositRecord(depositPid)).thenReturn(expected);
        when(mockResponse.getLocation()).thenReturn(depositPid.getRepositoryUri());

        assertSame(expected, repoObjFactory.createDepositRecord(depositPid, null));
        verify(ldpFactory, times(2)).createDirectContainer(eq(depositPid.getRepositoryUri()), any(), anyString());
    }

    @Test
    public void createAdminUnitTest() {
        when(repoObjLoader.getAdminUnit(any(PID.class))).thenReturn(mock(AdminUnit.class));
        AdminUnit obj = repoObjFactory.createAdminUnit(null);
        assertNotNull(obj);
        verify(mockPutBuilder).addHeader(LINK, archivalGroupLink());
    }

    @Test
    public void createAdminUnitWithPidTest() {
        PID contentPid = pidMinter.mintContentPid();
        AdminUnit expected = mock(AdminUnit.class);
        when(repoObjLoader.getAdminUnit(contentPid)).thenReturn(expected);

        assertSame(expected, repoObjFactory.createAdminUnit(contentPid, null));
    }

    @Test
    public void createContentRootObjectTest() throws Exception {
        URI path = pidMinter.mintContentPid().getRepositoryUri();
        when(mockResponse.getLocation()).thenReturn(path);

        assertEquals(path, repoObjFactory.createContentRootObject(path, null));
        verify(ldpFactory).createDirectContainer(eq(path), any(), anyString());
        verify(mockPutBuilder, never()).addHeader(eq(LINK), anyString());
    }

    @Test
    public void createCollectionObjectTest() throws Exception {
        when(repoObjLoader.getCollectionObject(any(PID.class))).thenReturn(mock(CollectionObject.class));
        CollectionObject obj = repoObjFactory.createCollectionObject(null);
        assertNotNull(obj);
        verify(mockPutBuilder).addHeader(LINK, archivalGroupLink());
    }

    @Test
    public void createCollectionObjectWithPidTest() {
        PID contentPid = pidMinter.mintContentPid();
        CollectionObject expected = mock(CollectionObject.class);
        when(repoObjLoader.getCollectionObject(contentPid)).thenReturn(expected);

        assertSame(expected, repoObjFactory.createCollectionObject(contentPid, null));
    }

    @Test
    public void createFolderObjectTest() throws Exception {
        when(repoObjLoader.getFolderObject(any(PID.class))).thenReturn(mock(FolderObject.class));
        FolderObject obj = repoObjFactory.createFolderObject(null);
        assertNotNull(obj);
        verify(mockPutBuilder).addHeader(LINK, archivalGroupLink());
    }

    @Test
    public void createFolderObjectWithPidTest() {
        PID contentPid = pidMinter.mintContentPid();
        FolderObject expected = mock(FolderObject.class);
        when(repoObjLoader.getFolderObject(contentPid)).thenReturn(expected);

        assertSame(expected, repoObjFactory.createFolderObject(contentPid, null));
    }

    @Test
    public void createWorkObjectTest() {
        when(repoObjLoader.getWorkObject(any(PID.class))).thenReturn(mock(WorkObject.class));
        WorkObject obj = repoObjFactory.createWorkObject(null);
        assertNotNull(obj);
        verify(mockPutBuilder).addHeader(LINK, archivalGroupLink());
    }

    @Test
    public void createWorkObjectWithPidTest() {
        PID contentPid = pidMinter.mintContentPid();
        WorkObject expected = mock(WorkObject.class);
        when(repoObjLoader.getWorkObject(contentPid)).thenReturn(expected);

        assertSame(expected, repoObjFactory.createWorkObject(contentPid, null));
    }

    @Test
    public void createFileObjectTest() {
        when(repoObjLoader.getFileObject(any(PID.class))).thenReturn(mock(FileObject.class));
        FileObject obj = repoObjFactory.createFileObject(null);
        assertNotNull(obj);
        verify(mockPutBuilder).addHeader(LINK, archivalGroupLink());
    }

    @Test
    public void createFileObjectWithPidTest() {
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
    public void createOrUpdateBinaryTest() {
        PID binaryPid = pidMinter.mintContentPid();
        BinaryObject expected = mock(BinaryObject.class);
        when(mockResponse.getLocation()).thenReturn(binaryPid.getRepositoryUri());
        when(repoObjLoader.getBinaryObject(binaryPid)).thenReturn(expected);

        assertSame(expected, repoObjFactory.createOrUpdateBinary(binaryPid, URI.create("file:///storage/file"),
                "file.txt", "text/plain", "sha1", "md5", null));
        verify(repoObjLoader).invalidate(binaryPid);
    }

    @Test
    public void createBinaryWithModelTest() throws FcrepoOperationFailedException {
        PID binaryPid = pidMinter.mintContentPid();
        BinaryObject expected = mock(BinaryObject.class);
        when(mockResponse.getLocation()).thenReturn(binaryPid.getRepositoryUri());
        when(repoObjLoader.getBinaryObject(binaryPid)).thenReturn(expected);

        assertSame(expected, repoObjFactory.createBinary(binaryPid.getRepositoryUri(), "file.txt",
                mock(InputStream.class), "file.txt", "text/plain", "sha1", "md5", modelWithProperty()));
        verify(mockPatchBuilder).perform();
    }

    @Test
    public void updateBinaryTest() {
        PID binaryPid = pidMinter.mintContentPid();
        BinaryObject expected = mock(BinaryObject.class);
        when(repoObjLoader.getBinaryObject(binaryPid)).thenReturn(expected);
        String binaryUri = binaryPid.getRepositoryUri().toString();
        int lastSlash = binaryUri.lastIndexOf('/');

        assertSame(expected, repoObjFactory.updateBinary(URI.create(binaryUri.substring(0, lastSlash)),
                binaryUri.substring(lastSlash + 1),
                mock(InputStream.class), "file.txt", "text/plain", "sha1", "md5", null));
        verify(repoObjLoader).invalidate(binaryPid);
    }

    @Test
    public void updateBinaryWithModelTest() throws FcrepoOperationFailedException {
        PID binaryPid = pidMinter.mintContentPid();
        BinaryObject expected = mock(BinaryObject.class);
        when(repoObjLoader.getBinaryObject(binaryPid)).thenReturn(expected);
        String binaryUri = binaryPid.getRepositoryUri().toString();
        int lastSlash = binaryUri.lastIndexOf('/');

        assertSame(expected, repoObjFactory.updateBinary(URI.create(binaryUri.substring(0, lastSlash)),
                binaryUri.substring(lastSlash + 1), mock(InputStream.class), "file.txt", "text/plain", "sha1", "md5",
                modelWithProperty()));
        verify(mockPatchBuilder).perform();
    }

    @Test
    public void binaryCreationRejectsNullContentTest() {
        URI path = pidMinter.mintContentPid().getRepositoryUri();

        assertThrows(IllegalArgumentException.class,
                () -> repoObjFactory.createBinary(path, "file", null, null, null, null, null, null));
        assertThrows(IllegalArgumentException.class,
                () -> repoObjFactory.updateBinary(path, "file", null, null, null, null, null, null));
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

    @Test
    public void createPropertyTest() {
        RepositoryObject object = repositoryObject();

        repoObjFactory.createProperty(object, Ebucore.filename, "file.txt");

        verify(sparqlUpdateService).executeUpdate(eq(object.getMetadataUri().toString()), anyString());
        verify(object).shouldRefresh();
    }

    @Test
    public void createExclusiveRelationshipNewTest() {
        var fileObject = mock(FileObject.class);
        var filePid = pidMinter.mintContentPid();
        when(fileObject.getPid()).thenReturn(filePid);
        Model objectModel = ModelFactory.createDefaultModel();
        var fileResc = objectModel.getResource(filePid.getRepositoryPath());
        when(fileObject.getModel(true)).thenReturn(objectModel);
        when(fileObject.getMetadataUri()).thenReturn(filePid.getRepositoryUri());

        repoObjFactory.createExclusiveRelationship(fileObject, Ebucore.filename, "afilename");

        verify(sparqlUpdateService).executeUpdate(eq(filePid.getRepositoryPath()), sparqlCaptor.capture());
        var sparql = sparqlCaptor.getValue();

        // Verify that the sparql update action can be parsed and executed
        UpdateAction.parseExecute(sparql, objectModel);
        assertTrue(objectModel.contains(fileResc, Ebucore.filename, "afilename"));
    }

    @Test
    public void createExclusiveRelationshipReplaceMultipleTest() {
        var fileObject = mock(FileObject.class);
        var filePid = pidMinter.mintContentPid();
        when(fileObject.getPid()).thenReturn(filePid);
        Model objectModel = ModelFactory.createDefaultModel();
        var fileResc = objectModel.getResource(filePid.getRepositoryPath());
        fileResc.addLiteral(Ebucore.filename, "oldfilename1");
        fileResc.addLiteral(Ebucore.filename, "oldfilename2");
        when(fileObject.getModel(true)).thenReturn(objectModel);
        when(fileObject.getMetadataUri()).thenReturn(filePid.getRepositoryUri());

        repoObjFactory.createExclusiveRelationship(fileObject, Ebucore.filename, "newfilename");

        verify(sparqlUpdateService).executeUpdate(eq(filePid.getRepositoryPath()), sparqlCaptor.capture());
        var sparql = sparqlCaptor.getValue();

        // Verify that the sparql update action can be parsed and executed
        UpdateAction.parseExecute(sparql, objectModel);
        assertTrue(objectModel.contains(fileResc, Ebucore.filename, "newfilename"));
        assertFalse(objectModel.contains(fileResc, Ebucore.filename, "oldfilename1"));
        assertFalse(objectModel.contains(fileResc, Ebucore.filename, "oldfilename2"));
    }

    @Test
    public void createExclusiveRelationshipReplaceContainingQuotesTest() {
        var fileObject = mock(FileObject.class);
        var filePid = pidMinter.mintContentPid();
        when(fileObject.getPid()).thenReturn(filePid);
        Model objectModel = ModelFactory.createDefaultModel();
        var fileResc = objectModel.getResource(filePid.getRepositoryPath());
        String oldName = "oldfilename1";
        fileResc.addLiteral(Ebucore.filename, "oldfilename1");
        when(fileObject.getModel(true)).thenReturn(objectModel);
        when(fileObject.getMetadataUri()).thenReturn(filePid.getRepositoryUri());

        String expectedName = "new\"file\"name";
        repoObjFactory.createExclusiveRelationship(fileObject, Ebucore.filename, expectedName);

        verify(sparqlUpdateService).executeUpdate(eq(filePid.getRepositoryPath()), sparqlCaptor.capture());
        var sparql = sparqlCaptor.getValue();

        // Verify that the sparql update action can be parsed and executed
        UpdateAction.parseExecute(sparql, objectModel);
        assertTrue(objectModel.contains(fileResc, Ebucore.filename, expectedName));
        assertFalse(objectModel.contains(fileResc, Ebucore.filename, oldName));
    }

    @Test
    public void deletePropertyTest() {
        RepositoryObject object = repositoryObject();

        repoObjFactory.deleteProperty(object, Ebucore.filename);

        verify(sparqlUpdateService).executeUpdate(eq(object.getMetadataUri().toString()), anyString());
        verify(object).shouldRefresh();
    }

    @Test
    public void createRelationshipTest() {
        RepositoryObject object = repositoryObject();
        Resource related = createResource("http://example.com/related");

        repoObjFactory.createRelationship(object, PcdmModels.memberOf, related);

        verify(sparqlUpdateService).executeUpdate(eq(object.getMetadataUri().toString()), anyString());
        verify(object).shouldRefresh();
    }

    @Test
    public void createRelationshipsTest() {
        RepositoryObject object = repositoryObject();
        Model model = ModelFactory.createDefaultModel();
        model.createResource("http://example.com/object").addProperty(Ebucore.filename, "file.txt");

        repoObjFactory.createRelationships(object, model);

        verify(sparqlUpdateService).executeUpdate(eq(object.getMetadataUri().toString()), anyString());
        verify(object).shouldRefresh();
    }

    @Test
    public void createOrTransformObjectTest() {
        URI path = pidMinter.mintContentPid().getRepositoryUri();
        URI created = URI.create(path + "/created");
        when(mockResponse.getLocation()).thenReturn(created);

        assertEquals(created, repoObjFactory.createOrTransformObject(path, ModelFactory.createDefaultModel()));
        verify(mockPutBuilder).preferLenient();
    }

    @Test
    public void objectExistsTest() throws FcrepoOperationFailedException {
        URI path = pidMinter.mintContentPid().getRepositoryUri();
        when(fcrepoClient.head(path)).thenReturn(mockHeadBuilder);
        when(mockHeadBuilder.perform()).thenReturn(mockResponse);

        assertTrue(repoObjFactory.objectExists(path));
    }

    @Test
    public void objectDoesNotExistTest() throws FcrepoOperationFailedException {
        URI path = pidMinter.mintContentPid().getRepositoryUri();
        when(fcrepoClient.head(path)).thenReturn(mockHeadBuilder);
        when(mockHeadBuilder.perform()).thenThrow(new FcrepoOperationFailedException(path, 404, "Not found"));

        assertFalse(repoObjFactory.objectExists(path));
    }

    @Test
    public void dependencyAccessorsTest() {
        assertSame(fcrepoClient, repoObjFactory.getClient());
        assertSame(ldpFactory, repoObjFactory.getLdpFactory());
    }

    private RepositoryObject repositoryObject() {
        PID objectPid = pidMinter.mintContentPid();
        RepositoryObject object = mock(RepositoryObject.class);
        when(object.getPid()).thenReturn(objectPid);
        when(object.getMetadataUri()).thenReturn(objectPid.getRepositoryUri());
        return object;
    }

    private Model modelWithProperty() {
        Model model = ModelFactory.createDefaultModel();
        model.createResource("http://example.com/binary").addProperty(Ebucore.filename, "file.txt");
        return model;
    }

    private String archivalGroupLink() {
        return "<" + Fcrepo4Repository.ArchivalGroup.getURI() + ">;rel=\"type\"";
    }
}
