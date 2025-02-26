package edu.unc.lib.boxc.model.fcrepo.services;

import edu.unc.lib.boxc.model.api.exceptions.FedoraException;
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
import edu.unc.lib.boxc.model.api.rdf.PcdmModels;
import edu.unc.lib.boxc.model.api.sparql.SparqlQueryService;
import edu.unc.lib.boxc.model.fcrepo.event.RepositoryPremisLog;
import edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPIDMinter;
import org.apache.http.HttpStatus;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoOperationFailedException;
import org.fcrepo.client.FcrepoResponse;
import org.fcrepo.client.HeadBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
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
    private SparqlQueryService sparqlQueryService;
    @Mock
    private FcrepoClient fcrepoClient;
    @Mock
    private QueryExecution mockQueryExec;
    @Mock
    private ResultSet mockResultSet;
    @Mock
    private QuerySolution mockQuerySolution;
    @Mock
    private Resource mockParentResource;

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
        repositoryObjectDriver.setSparqlQueryService(sparqlQueryService);

        when(mockQueryExec.execSelect()).thenReturn(mockResultSet);
        when(mockResultSet.nextSolution()).thenReturn(mockQuerySolution);
        when(mockQuerySolution.getResource("pid")).thenReturn(mockParentResource);
        when(mockParentResource.getURI()).thenReturn(parentPid.getRepositoryPath());
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
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

        when(object.getPid()).thenReturn(pid);
        when(sparqlQueryService.executeQuery(any())).thenReturn(mockQueryExec);
        when(mockResultSet.hasNext()).thenReturn(true).thenReturn(false);

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
}
