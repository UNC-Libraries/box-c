package edu.unc.lib.boxc.model.fcrepo.services;

import static edu.unc.lib.boxc.model.api.rdf.RDFModelUtil.streamModel;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.io.IOException;
import java.util.UUID;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoResponse;
import org.fcrepo.client.RequestBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import edu.unc.lib.boxc.common.test.SelfReturningAnswer;
import edu.unc.lib.boxc.model.api.exceptions.ObjectTypeMismatchException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants;
import edu.unc.lib.boxc.model.api.objects.AdminUnit;
import edu.unc.lib.boxc.model.api.objects.BinaryObject;
import edu.unc.lib.boxc.model.api.objects.CollectionObject;
import edu.unc.lib.boxc.model.api.objects.ContentRootObject;
import edu.unc.lib.boxc.model.api.objects.DepositRecord;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.FolderObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObject;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.rdf.Fcrepo4Repository;
import edu.unc.lib.boxc.model.api.rdf.Ldp;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.model.fcrepo.objects.RepositoryObjectCacheLoader;

/**
 *
 * @author bbpennel
 *
 */
public class RepositoryObjectCacheLoaderTest {

    private static String ETAG = "etag";
    private static final String ETAG_HEADER =  "\"etag\"";

    private RepositoryObjectCacheLoader objectCacheLoader;
    private AutoCloseable closeable;

    @Mock
    private RepositoryObjectDriver driver;

    private FcrepoClient client;
    @Mock
    private FcrepoResponse response;

    @Mock
    private PID pid;

    @BeforeEach
    public void init() {
        closeable = openMocks(this);

        client = mock(FcrepoClient.class, new BuilderReturningAnswer());

        when(response.getHeaderValue(eq("ETag"))).thenReturn(ETAG_HEADER);

        objectCacheLoader = new RepositoryObjectCacheLoader();
        objectCacheLoader.setClient(client);
        objectCacheLoader.setRepositoryObjectDriver(driver);

        pid = PIDs.get(UUID.randomUUID().toString());
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
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

        mockResponseBodyWithType(pid, Ldp.NonRdfSource);

        RepositoryObject obj = objectCacheLoader.load(pid);

        assertTrue(obj instanceof BinaryObject);
        assertEquals(pid, obj.getPid());
        assertEquals(ETAG, obj.getEtag());
        assertTrue(obj.getResource().hasProperty(RDF.type, Ldp.NonRdfSource));
    }

    @Test
    public void loadUnsupportedTypeTest() throws Exception {
        Assertions.assertThrows(ObjectTypeMismatchException.class, () -> {
            mockResponseBodyWithType(pid, Fcrepo4Repository.Pairtree);

            objectCacheLoader.load(pid);
        });
    }

    @Test
    public void invalidContentPidTest() throws Exception {
        Assertions.assertThrows(ObjectTypeMismatchException.class, () -> {
            pid = PIDs.get(RepositoryPathConstants.DEPOSIT_RECORD_BASE + "/" + UUID.randomUUID().toString());

            mockResponseBodyWithType(pid, Cdr.Work);

            objectCacheLoader.load(pid);
        });
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
