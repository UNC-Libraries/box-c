package edu.unc.lib.boxc.model.fcrepo.services;

import edu.unc.lib.boxc.fcrepo.utils.FedoraTransaction;
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
import edu.unc.lib.boxc.model.fcrepo.event.RepositoryPremisLog;
import edu.unc.lib.boxc.model.fcrepo.ids.DatastreamPids;
import edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPIDMinter;
import edu.unc.lib.boxc.model.fcrepo.objects.FileObjectImpl;
import org.apache.http.HttpStatus;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
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
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
        repositoryObjectDriver = new RepositoryObjectDriver();
        repositoryObjectDriver.setClient(fcrepoClient);
        repositoryObjectDriver.setPidMinter(pidMinter);
        repositoryObjectDriver.setRepositoryObjectLoader(repoObjLoader);
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
    public void loadModelNoModelTest() throws FcrepoOperationFailedException, IOException {
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
    public void loadModelHasModelCheckForUpdatesTrueTest() throws FcrepoOperationFailedException, IOException {
        var fileObject = mock(FileObjectImpl.class);
        var uri = URI.create("good/metadata");
        var response = mock(FcrepoResponse.class);
        var get = mock(GetBuilder.class);
        var model = ModelFactory.createDefaultModel();
        var inputStream = RDFModelUtil.streamModel(model);

        when(fileObject.getMetadataUri()).thenReturn(uri);
        when(fileObject.getPid()).thenReturn(pid);
        // object has been modified
        when(fileObject.isUnmodified()).thenReturn(false);
        when(get.perform()).thenReturn(response);
        when(get.accept(any())).thenReturn(get);
        when(fcrepoClient.get(any())).thenReturn(get);
        when(response.getBody()).thenReturn(inputStream);

        repositoryObjectDriver.loadModel(fileObject, true);
        verify(fileObject).storeModel(any());
        verify(fileObject).setEtag(any());
    }

    @Test
    public void loadModelHasModelCheckForUpdatesFalseTest() {
        var fileObject = mock(FileObjectImpl.class);
        when(fileObject.hasModel()).thenReturn(true);

        repositoryObjectDriver.loadModel(fileObject, false);
        verify(fileObject, never()).storeModel(any());
    }

    @Test
    public void loadModelNoCurrentTransactionTest() {
        try (MockedStatic<FedoraTransaction> mockedStatic = Mockito.mockStatic(FedoraTransaction.class)) {
            var fileObject = mock(FileObjectImpl.class);
            when(fileObject.hasModel()).thenReturn(true);
            when(fileObject.isUnmodified()).thenReturn(true);
            mockedStatic.when(FedoraTransaction::isStillAlive).thenReturn(false);

            repositoryObjectDriver.loadModel(fileObject, true);
            verify(fileObject, never()).storeModel(any());
        }
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
            doThrow(new IOException("something is wrong")).when(response).close();

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
    public void getParentObjectOriginalFileTest() {
        var object = mock(BinaryObject.class);
        PID binPid = DatastreamPids.getOriginalFilePid(pid);
        when(object.getPid()).thenReturn(binPid);

        repositoryObjectDriver.getParentObject(object);
        verify(repoObjLoader).getRepositoryObject(eq(pid));
    }

    @Test
    public void getParentObjectDepositManifestTest() {
        var object = mock(BinaryObject.class);
        PID binPid = DatastreamPids.getDepositManifestPid(pid, "manifest.xml");
        when(object.getPid()).thenReturn(binPid);

        repositoryObjectDriver.getParentObject(object);
        verify(repoObjLoader).getRepositoryObject(eq(pid));
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
    public void listRelatedTest() throws Exception {
        var object = mock(ContentObject.class);
        var uri = URI.create("good/metadata");
        when(object.getPid()).thenReturn(pid);
        when(object.getMetadataUri()).thenReturn(uri);

        var relatedObjectPid = pidMinter.mintContentPid();
        var relation = PcdmModels.hasRelatedObject;

        var model = ModelFactory.createDefaultModel();
        var relatedResource = model.getResource(relatedObjectPid.getRepositoryPath());
        var selfResource = model.getResource(pid.getRepositoryPath());
        relatedResource.addProperty(relation, selfResource);
        // Add a triple with a non-matching predicate, to ensure it is filtered out
        relatedResource.addProperty(PcdmModels.memberOf, selfResource);

        mockNTriplesResponse(uri, model);

        assertEquals(List.of(relatedObjectPid), repositoryObjectDriver.listRelated(object, relation));
    }

    @Test
    public void listMembersTest() throws Exception {
        var object = mock(ContentObject.class);
        var uri = URI.create("good/metadata");
        when(object.getPid()).thenReturn(pid);
        when(object.getMetadataUri()).thenReturn(uri);

        var memberObjectPid = pidMinter.mintContentPid();
        var relation = PcdmModels.memberOf;

        var model = ModelFactory.createDefaultModel();
        var memberResource = model.getResource(memberObjectPid.getRepositoryPath());
        var selfResource = model.getResource(pid.getRepositoryPath());
        memberResource.addProperty(relation, selfResource);

        mockNTriplesResponse(uri, model);

        assertEquals(List.of(memberObjectPid), repositoryObjectDriver.listMembers(object));
    }

    private void mockNTriplesResponse(URI uri, Model model) throws Exception {
        var response = mock(FcrepoResponse.class);
        var get = mock(GetBuilder.class);

        var out = new java.io.ByteArrayOutputStream();
        RDFDataMgr.write(out, model, Lang.NTRIPLES);
        var inputStream = new ByteArrayInputStream(out.toByteArray());

        when(fcrepoClient.get(eq(uri))).thenReturn(get);
        when(get.accept(any())).thenReturn(get);
        when(get.preferRepresentation(any(), any())).thenReturn(get);
        when(get.perform()).thenReturn(response);
        when(response.getBody()).thenReturn(inputStream);
    }
}

