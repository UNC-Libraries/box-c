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

import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

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
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.sparql.SparqlUpdateService;
import edu.unc.lib.dl.test.SelfReturningAnswer;
/**
 *
 * @author bbpennel
 * @author harring
 *
 */
public class RepositoryObjectFactoryTest {

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

    private RepositoryObjectFactory repoObjFactory;
    private RepositoryPIDMinter pidMinter;
    private List<URI> linkHeaders;

    @Before
    public void init() throws FcrepoOperationFailedException, URISyntaxException {
        initMocks(this);
        repoObjFactory = new RepositoryObjectFactory();
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
