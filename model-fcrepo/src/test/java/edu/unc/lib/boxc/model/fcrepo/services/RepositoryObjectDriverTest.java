package edu.unc.lib.boxc.model.fcrepo.services;

import edu.unc.lib.boxc.model.api.exceptions.FedoraException;
import edu.unc.lib.boxc.model.api.exceptions.NotFoundException;
import edu.unc.lib.boxc.model.api.exceptions.ObjectTypeMismatchException;
import edu.unc.lib.boxc.model.api.exceptions.OrphanedObjectException;
import edu.unc.lib.boxc.model.api.exceptions.TombstoneFoundException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.ids.PIDMinter;
import edu.unc.lib.boxc.model.api.objects.BinaryObject;
import edu.unc.lib.boxc.model.api.objects.ContentObject;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.objects.Tombstone;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.rdf.PcdmModels;
import edu.unc.lib.boxc.model.api.rdf.RDFModelUtil;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.model.api.sparql.SparqlQueryService;
import edu.unc.lib.boxc.model.fcrepo.event.RepositoryPremisLog;
import edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPIDMinter;
import edu.unc.lib.boxc.model.fcrepo.objects.FileObjectImpl;
import edu.unc.lib.boxc.model.fcrepo.sparql.JenaSparqlQueryServiceImpl;
import org.apache.http.HttpStatus;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoOperationFailedException;
import org.fcrepo.client.FcrepoResponse;
import org.fcrepo.client.GetBuilder;
import org.fcrepo.client.HeadBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;

import static edu.unc.lib.boxc.model.api.rdf.RDFModelUtil.TURTLE_MIMETYPE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

public class RepositoryObjectDriverTest {
    private AutoCloseable closeable;
    private RepositoryObjectDriver repositoryObjectDriver;
    private PIDMinter pidMinter;
    private PID pid;
    private PID parentPid;
    protected Model sparqlModel;
    protected SparqlQueryService sparqlQueryService;
    @Mock
    private RepositoryObjectLoader repoObjLoader;
    @Mock
    private FcrepoClient fcrepoClient;
    @Captor
    private ArgumentCaptor<List<String>> typesCaptor;

    @BeforeEach
    public void init() {
        closeable = openMocks(this);
        pidMinter = new RepositoryPIDMinter();
        pid = pidMinter.mintContentPid();
        parentPid = pidMinter.mintContentPid();
        sparqlModel = ModelFactory.createDefaultModel();
        sparqlQueryService = new JenaSparqlQueryServiceImpl(sparqlModel);
        repositoryObjectDriver = new RepositoryObjectDriver();
        repositoryObjectDriver.setClient(fcrepoClient);
        repositoryObjectDriver.setPidMinter(pidMinter);
        repositoryObjectDriver.setRepositoryObjectLoader(repoObjLoader);
        repositoryObjectDriver.setSparqlQueryService(sparqlQueryService);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void loadTypesTest() {
        var model = ModelFactory.createDefaultModel();
        var resc = model.getResource(pid.getRepositoryPath());
        resc.addProperty(RDF.type, PcdmModels.Object);
        resc.addProperty(RDF.type, Cdr.FileObject);
        var fileObject = mock(FileObjectImpl.class);

        when(fileObject.getModel()).thenReturn(model);
        when(fileObject.getPid()).thenReturn(pid);

        repositoryObjectDriver.loadTypes(fileObject);
        verify(fileObject).setTypes(typesCaptor.capture());
        assertTrue(typesCaptor.getValue().contains(Cdr.FileObject.getURI()));
    }

    @Test
    public void loadModelTest() throws FcrepoOperationFailedException, IOException {
        var fileObject = mock(FileObjectImpl.class);
        var uri = URI.create("good/metadata");
        var response = mock(FcrepoResponse.class);
        var get = mock(GetBuilder.class);
        var model = ModelFactory.createDefaultModel();
        var inputStream = RDFModelUtil.streamModel(model);

        when(fileObject.getMetadataUri()).thenReturn(uri);
        when(fileObject.getPid()).thenReturn(pid);
        when(get.perform()).thenReturn(response);
        when(get.accept(any())).thenReturn(get);
        when(fcrepoClient.get(any())).thenReturn(get);
        when(response.getBody()).thenReturn(inputStream);

        repositoryObjectDriver.loadModel(fileObject, false);
        verify(fileObject).storeModel(any());
        verify(fileObject).setEtag(any());
    }

    @Test
    public void loadModelAlreadyHasModelTest() {
        var fileObject = mock(FileObjectImpl.class);
        when(fileObject.hasModel()).thenReturn(true);

        repositoryObjectDriver.loadModel(fileObject, true);
        verify(fileObject, never()).storeModel(any());
    }

    @Test
    public void loadModelCheckForUpdatesFalseTest() {
        var fileObject = mock(FileObjectImpl.class);
        when(fileObject.hasModel()).thenReturn(true);

        repositoryObjectDriver.loadModel(fileObject, false);
        verify(fileObject, never()).storeModel(any());
    }

    @Test
    public void loadModelNoCurrentTransactionTest() {
        var fileObject = mock(FileObjectImpl.class);
        when(fileObject.hasModel()).thenReturn(true);

        repositoryObjectDriver.loadModel(fileObject, true);
        verify(fileObject, never()).storeModel(any());
    }

    @Test
    public void loadModelObjectNotModifiedTest() {
        var fileObject = mock(FileObjectImpl.class);
        when(fileObject.hasModel()).thenReturn(true);
        when(fileObject.isUnmodified()).thenReturn(true);

        repositoryObjectDriver.loadModel(fileObject, true);
        verify(fileObject, never()).storeModel(any());
    }

    @Test
    public void loadModelIOExceptionTest() {
        Assertions.assertThrows(FedoraException.class, () -> {
            var fileObject = mock(FileObjectImpl.class);
            var uri = URI.create("good/metadata");
            var get = mock(GetBuilder.class);

            when(fileObject.getMetadataUri()).thenReturn(uri);
            when(fileObject.getPid()).thenReturn(pid);
            when(get.accept(any())).thenReturn(get);
            when(fcrepoClient.get(any())).thenReturn(get);
            doThrow(new IOException("something is wrong")).when(get).perform();

            repositoryObjectDriver.loadModel(fileObject, true);
        });
    }

    @Test
    public void loadModelFcrepoErrorTest() {
        Assertions.assertThrows(NotFoundException.class, () -> {
            var fileObject = mock(FileObjectImpl.class);
            var uri = URI.create("good/metadata");
            var get = mock(GetBuilder.class);

            when(fileObject.getMetadataUri()).thenReturn(uri);
            when(fileObject.getPid()).thenReturn(pid);
            when(get.accept(any())).thenReturn(get);
            when(fcrepoClient.get(any())).thenReturn(get);
            doThrow(new FcrepoOperationFailedException(uri, 404, "error")).when(get).perform();

            repositoryObjectDriver.loadModel(fileObject, true);
        });
    }

    @Test
    public void getRepositoryObjectTest() {
        var fileObject = mock(FileObject.class);
        when(repoObjLoader.getRepositoryObject(eq(pid))).thenReturn(fileObject);

        assertEquals(fileObject, repositoryObjectDriver.getRepositoryObject(pid));
    }

    @Test
    public void getRepositoryObjectWithTypeTest() {
        var binaryObject = mock(BinaryObject.class);
        when(repoObjLoader.getRepositoryObject(eq(pid))).thenReturn(binaryObject);

        assertEquals(binaryObject, repositoryObjectDriver.getRepositoryObject(pid, BinaryObject.class));
    }

    @Test
    public void getRepositoryObjectWithTombstoneTest() {
        Assertions.assertThrows(TombstoneFoundException.class, () -> {
            var tombstone = mock(Tombstone.class);
            when(repoObjLoader.getRepositoryObject(eq(pid))).thenReturn(tombstone);
            repositoryObjectDriver.getRepositoryObject(pid, BinaryObject.class);
        });
    }

    @Test
    public void getRepositoryObjectWithInvalidTypeTest() {
        Assertions.assertThrows(ObjectTypeMismatchException.class, () -> {
            var fileObject = mock(FileObject.class);
            when(repoObjLoader.getRepositoryObject(eq(pid))).thenReturn(fileObject);
            repositoryObjectDriver.getRepositoryObject(pid, BinaryObject.class);
        });
    }

    @Test
    public void getPremisLogTest() {
        var repoObject = mock(RepositoryObject.class);
        var result = repositoryObjectDriver.getPremisLog(repoObject);
        assertInstanceOf(RepositoryPremisLog.class, result);
    }

    @Test
    public void getETagTest() throws FcrepoOperationFailedException {
        var object = mock(RepositoryObject.class);
        var response = mock(FcrepoResponse.class);
        var head = mock(HeadBuilder.class);
        var uri = URI.create("good/metadata");

        when(object.getMetadataUri()).thenReturn(uri);
        when(object.getPid()).thenReturn(pid);
        when(head.perform()).thenReturn(response);
        when(fcrepoClient.head(any())).thenReturn(head);
        when(response.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        when(response.getHeaderValue(eq("ETag"))).thenReturn("amiddlez");

        assertEquals("middle", repositoryObjectDriver.getEtag(object));
    }

    @Test
    public void getNullETagTest() throws FcrepoOperationFailedException {
        var object = mock(RepositoryObject.class);
        var response = mock(FcrepoResponse.class);
        var head = mock(HeadBuilder.class);
        var uri = URI.create("good/metadata");

        when(object.getMetadataUri()).thenReturn(uri);
        when(object.getPid()).thenReturn(pid);
        when(head.perform()).thenReturn(response);
        when(fcrepoClient.head(any())).thenReturn(head);
        when(response.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        when(response.getHeaderValue(eq("ETag"))).thenReturn(null);

        assertNull(repositoryObjectDriver.getEtag(object));
    }

    @Test
    public void getETagBadResponseTest() {
        Assertions.assertThrows(FedoraException.class, () -> {
            var object = mock(RepositoryObject.class);
            var response = mock(FcrepoResponse.class);
            var head = mock(HeadBuilder.class);
            var uri = URI.create("good/metadata");

            when(object.getMetadataUri()).thenReturn(uri);
            when(object.getPid()).thenReturn(pid);
            when(head.perform()).thenReturn(response);
            when(fcrepoClient.head(any())).thenReturn(head);
            when(response.getStatusCode()).thenReturn(HttpStatus.SC_INTERNAL_SERVER_ERROR);

            repositoryObjectDriver.getEtag(object);
        });
    }

    @Test
    public void getParentPidBinaryObjectTest() {
        var object = mock(BinaryObject.class);
        var resource = sparqlModel.getResource(pid.getRepositoryPath());

        when(object.getPid()).thenReturn(pid);
        var parentResource = sparqlModel.getResource(parentPid.getRepositoryPath());
        parentResource.addProperty(PcdmModels.hasFile, resource);

        assertEquals(parentPid, repositoryObjectDriver.getParentPid(object));
    }

    @Test
    public void getParentPidContentObjectTest() {
        var object = mock(ContentObject.class);
        var model = ModelFactory.createDefaultModel();
        var resource = model.getResource(pid.getRepositoryPath());
        var parentResc = model.getResource(parentPid.getRepositoryPath());
        resource.addProperty(PcdmModels.memberOf, parentResc);

        when(object.getResource()).thenReturn(resource);
        when(object.getPid()).thenReturn(pid);

        assertEquals(parentPid, repositoryObjectDriver.getParentPid(object));
    }
    @Test
    public void getParentPidContentObjectNoParentTest() {
        Assertions.assertThrows(OrphanedObjectException.class, () -> {
            var object = mock(ContentObject.class);
            var resource = mock(Resource.class);
            when(object.getResource()).thenReturn(resource);
            when(object.getPid()).thenReturn(pid);
            repositoryObjectDriver.getParentPid(object);
        });
    }

    @Test
    public void getParentPidInvalidObjectTest() {
        Assertions.assertThrows(ObjectTypeMismatchException.class, () -> {
            var object = mock(RepositoryObject.class);
            repositoryObjectDriver.getParentPid(object);
        });
    }

    @Test
    public void getParentObjectTest() {
        var object = mock(BinaryObject.class);
        var resource = sparqlModel.getResource(pid.getRepositoryPath());

        when(object.getPid()).thenReturn(pid);
        var parentResource = sparqlModel.getResource(parentPid.getRepositoryPath());
        parentResource.addProperty(PcdmModels.hasFile, resource);

        repositoryObjectDriver.getParentObject(object);
        verify(repoObjLoader).getRepositoryObject(eq(parentPid));
    }

    @Test
    public void fetchContainerTest() {
        var object = mock(BinaryObject.class);
        var resource = sparqlModel.getResource(pid.getRepositoryPath());

        when(object.getPid()).thenReturn(pid);
        var parentResource = sparqlModel.getResource(parentPid.getRepositoryPath());
        parentResource.addProperty(PcdmModels.hasFile, resource);

        assertEquals(parentPid, repositoryObjectDriver.fetchContainer(object, PcdmModels.hasFile));
    }

    @Test
    public void fetchContainerNullTest() {
        var object = mock(BinaryObject.class);
        when(object.getPid()).thenReturn(pid);

        assertNull(repositoryObjectDriver.fetchContainer(object, PcdmModels.hasFile));
    }

    @Test
    public void getBinaryStreamTest() throws FcrepoOperationFailedException {
        var object = mock(BinaryObject.class);
        var response = mock(FcrepoResponse.class);
        var get = mock(GetBuilder.class);
        var inputStream = mock(InputStream.class);

        when(object.getPid()).thenReturn(pid);
        when(get.perform()).thenReturn(response);
        when(fcrepoClient.get(any())).thenReturn(get);
        when(response.getBody()).thenReturn(inputStream);

        assertEquals(inputStream, repositoryObjectDriver.getBinaryStream(object));
    }

    @Test
    public void getBinaryStreamErrorTest() {
        Assertions.assertThrows(NotFoundException.class, () -> {
            var object = mock(BinaryObject.class);
            var response = mock(FcrepoResponse.class);
            var get = mock(GetBuilder.class);
            var uri = URI.create("bad/error");

            when(object.getPid()).thenReturn(pid);
            when(get.perform()).thenReturn(response);
            when(fcrepoClient.get(any())).thenReturn(get);
            doThrow(new FcrepoOperationFailedException(uri, 404, "error")).when(get).perform();

            repositoryObjectDriver.getBinaryStream(object);
        });
    }

    @Test
    public void listRelatedTest() {
        var object = mock(ContentObject.class);
        var resource = sparqlModel.getResource(pid.getRepositoryPath());
        when(object.getPid()).thenReturn(pid);
        when(object.getResource()).thenReturn(resource);

        var relatedObjectPid = pidMinter.mintContentPid();
        var relatedResource = sparqlModel.getResource(relatedObjectPid.getRepositoryPath());

        var relation = PcdmModels.hasRelatedObject;
        relatedResource.addProperty(relation, resource);

        assertEquals(List.of(relatedObjectPid), repositoryObjectDriver.listRelated(object, relation));
    }

    @Test
    public void listMembersTest() {
        var object = mock(ContentObject.class);
        var resource = sparqlModel.getResource(pid.getRepositoryPath());
        when(object.getPid()).thenReturn(pid);
        when(object.getResource()).thenReturn(resource);

        var memberObjectPid = pidMinter.mintContentPid();
        var memberResource = sparqlModel.getResource(memberObjectPid.getRepositoryPath());

        var relation = PcdmModels.memberOf;
        memberResource.addProperty(relation, resource);

        assertEquals(List.of(memberObjectPid), repositoryObjectDriver.listMembers(object));
    }
}

