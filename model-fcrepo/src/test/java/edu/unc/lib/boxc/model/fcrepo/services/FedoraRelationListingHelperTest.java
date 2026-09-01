package edu.unc.lib.boxc.model.fcrepo.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.List;
import java.util.Set;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoOperationFailedException;
import org.fcrepo.client.FcrepoResponse;
import org.fcrepo.client.GetBuilder;
import org.fcrepo.client.HeadBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import edu.unc.lib.boxc.model.api.exceptions.FedoraException;
import edu.unc.lib.boxc.model.api.exceptions.NotFoundException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.rdf.PcdmModels;
import edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPIDMinter;

/**
 * @author bbpennel
 */
public class FedoraRelationListingHelperTest {
    private AutoCloseable closeable;

    @Mock
    private FcrepoClient client;

    private RepositoryPIDMinter pidMinter;
    private PID selfPid;
    private URI selfUri;
    private URI metadataUri;

    @BeforeEach
    public void setup() {
        closeable = openMocks(this);
        pidMinter = new RepositoryPIDMinter();
        selfPid = pidMinter.mintContentPid();
        selfUri = selfPid.getRepositoryUri();
        metadataUri = selfUri;
    }

    @AfterEach
    public void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void listSubjectsOfInboundRelationsTest() throws Exception {
        var relatedPid = pidMinter.mintContentPid();
        var relation = PcdmModels.memberOf;

        var model = ModelFactory.createDefaultModel();
        var relatedResc = model.getResource(relatedPid.getRepositoryPath());
        var selfResc = model.getResource(selfPid.getRepositoryPath());
        relatedResc.addProperty(relation, selfResc);

        mockNTriplesResponse(metadataUri, model);

        var result = FedoraRelationListingHelper.listSubjectsOfInboundRelations(
                client, metadataUri, selfUri, relation);

        assertEquals(List.of(relatedPid), result);
    }

    @Test
    public void listSubjectsOfInboundRelationsFiltersNonMatchingPredicateTest() throws Exception {
        var relatedPid = pidMinter.mintContentPid();

        var model = ModelFactory.createDefaultModel();
        var relatedResc = model.getResource(relatedPid.getRepositoryPath());
        var selfResc = model.getResource(selfPid.getRepositoryPath());
        // Triple present, but with a different predicate than the one being queried for
        relatedResc.addProperty(PcdmModels.hasMember, selfResc);

        mockNTriplesResponse(metadataUri, model);

        var result = FedoraRelationListingHelper.listSubjectsOfInboundRelations(
                client, metadataUri, selfUri, PcdmModels.memberOf);

        assertTrue(result.isEmpty());
    }

    @Test
    public void listSubjectsOfInboundRelationsFiltersNonMatchingObjectTest() throws Exception {
        var relatedPid = pidMinter.mintContentPid();
        var otherPid = pidMinter.mintContentPid();
        var relation = PcdmModels.memberOf;

        var model = ModelFactory.createDefaultModel();
        var relatedResc = model.getResource(relatedPid.getRepositoryPath());
        var otherResc = model.getResource(otherPid.getRepositoryPath());
        // Triple present with the matching predicate, but object is a different resource
        relatedResc.addProperty(relation, otherResc);

        mockNTriplesResponse(metadataUri, model);

        var result = FedoraRelationListingHelper.listSubjectsOfInboundRelations(
                client, metadataUri, selfUri, relation);

        assertTrue(result.isEmpty());
    }

    @Test
    public void listSubjectsOfInboundRelationsEmptyTest() throws Exception {
        var model = ModelFactory.createDefaultModel();

        mockNTriplesResponse(metadataUri, model);

        var result = FedoraRelationListingHelper.listSubjectsOfInboundRelations(
                client, metadataUri, selfUri, PcdmModels.memberOf);

        assertTrue(result.isEmpty());
    }

    @Test
    public void listSubjectsOfInboundRelationsClientErrorTest() throws Exception {
        var get = mock(GetBuilder.class);
        when(client.get(eq(metadataUri))).thenReturn(get);
        when(get.accept(any())).thenReturn(get);
        when(get.preferRepresentation(any(), any())).thenReturn(get);
        doThrow(new FcrepoOperationFailedException(metadataUri, 404, "Not Found")).when(get).perform();

        assertThrows(NotFoundException.class, () -> FedoraRelationListingHelper.listSubjectsOfInboundRelations(
                client, metadataUri, selfUri, PcdmModels.memberOf));
    }

    @Test
    public void listTypesTest() throws Exception {
        var typeUri1 = URI.create("http://example.com/model#Work");
        var typeUri2 = URI.create("http://www.w3.org/ns/ldp#RDFSource");

        mockHeadResponse(selfUri, List.of(typeUri1, typeUri2));

        var result = FedoraRelationListingHelper.listTypes(client, selfUri);

        assertEquals(Set.of(typeUri1.toString(), typeUri2.toString()), result);
    }

    @Test
    public void listTypesEmptyTest() throws Exception {
        mockHeadResponse(selfUri, List.of());

        var result = FedoraRelationListingHelper.listTypes(client, selfUri);

        assertTrue(result.isEmpty());
    }

    @Test
    public void listTypesClientErrorTest() throws Exception {
        var head = mock(HeadBuilder.class);
        when(client.head(eq(selfUri))).thenReturn(head);
        doThrow(new FcrepoOperationFailedException(selfUri, 404, "Not Found")).when(head).perform();

        assertThrows(NotFoundException.class, () -> FedoraRelationListingHelper.listTypes(client, selfUri));
    }

    @Test
    public void listTypesIOErrorTest() throws Exception {
        var head = mock(HeadBuilder.class);
        var response = mock(FcrepoResponse.class);
        when(client.head(eq(selfUri))).thenReturn(head);
        when(head.perform()).thenReturn(response);
        when(response.getLinkHeaders(eq("type"))).thenReturn(List.of());
        doThrow(new java.io.IOException("boom")).when(response).close();

        assertThrows(FedoraException.class, () -> FedoraRelationListingHelper.listTypes(client, selfUri));
    }

    private void mockNTriplesResponse(URI uri, Model model) throws Exception {
        var response = mock(FcrepoResponse.class);
        var get = mock(GetBuilder.class);

        var out = new ByteArrayOutputStream();
        RDFDataMgr.write(out, model, Lang.NTRIPLES);
        var inputStream = new ByteArrayInputStream(out.toByteArray());

        when(client.get(eq(uri))).thenReturn(get);
        when(get.accept(any())).thenReturn(get);
        when(get.preferRepresentation(any(), any())).thenReturn(get);
        when(get.perform()).thenReturn(response);
        when(response.getBody()).thenReturn(inputStream);
    }

    private void mockHeadResponse(URI uri, List<URI> typeUris) throws Exception {
        var response = mock(FcrepoResponse.class);
        var head = mock(HeadBuilder.class);

        when(client.head(eq(uri))).thenReturn(head);
        when(head.perform()).thenReturn(response);
        when(response.getLinkHeaders(eq("type"))).thenReturn(typeUris);
    }
}
