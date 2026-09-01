package edu.unc.lib.boxc.indexing.solr.test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoResponse;
import org.fcrepo.client.HeadBuilder;

import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.ContentObject;
import edu.unc.lib.boxc.model.api.rdf.PcdmModels;
import edu.unc.lib.boxc.model.api.services.MembershipService;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;

/**
 * Test helper which simulates fedora's inbound-references membership listing and rdf:type HEAD
 * lookups against an in-memory aggregate model of registered objects, for use in place of an
 * actual fedora instance in unit tests.
 *
 * @author bbpennel
 */
public class MockFedoraMembershipHelper {
    private final Model aggregateModel = ModelFactory.createDefaultModel();

    /**
     * Registers the resources of the given objects, so that they can be discovered via the
     * mocked membership service and fcrepo client produced by this helper.
     *
     * @param objs
     */
    public void addObjects(ContentObject... objs) {
        for (ContentObject obj : objs) {
            aggregateModel.add(obj.getResource().getModel());
        }
    }

    /**
     * @return a mocked MembershipService which lists the immediate pcdm:memberOf children of a
     *      parent PID, based on the objects previously registered via addObjects
     */
    public MembershipService mockMembershipService() {
        MembershipService membershipService = mock(MembershipService.class);
        when(membershipService.listMembers(any())).thenAnswer(invocation -> {
            PID parentPid = invocation.getArgument(0);
            Resource parentResc = aggregateModel.getResource(parentPid.getRepositoryPath());

            List<PID> memberPids = new ArrayList<>();
            aggregateModel.listResourcesWithProperty(PcdmModels.memberOf, parentResc)
                    .forEachRemaining(resc -> memberPids.add(PIDs.get(resc.getURI())));
            return memberPids;
        });
        return membershipService;
    }

    /**
     * @return a mocked FcrepoClient which responds to HEAD requests with Link "type" headers
     *      matching the rdf:type properties of objects previously registered via addObjects
     */
    public FcrepoClient mockFcrepoClient() {
        FcrepoClient fcrepoClient = mock(FcrepoClient.class);
        when(fcrepoClient.head(any())).thenAnswer(invocation -> {
            URI resourceUri = invocation.getArgument(0);
            Resource resc = aggregateModel.getResource(resourceUri.toString());

            List<URI> typeUris = new ArrayList<>();
            resc.listProperties(RDF.type).forEachRemaining(stmt ->
                    typeUris.add(URI.create(stmt.getResource().getURI())));

            HeadBuilder head = mock(HeadBuilder.class);
            FcrepoResponse response = mock(FcrepoResponse.class);
            when(head.perform()).thenReturn(response);
            when(response.getLinkHeaders("type")).thenReturn(typeUris);
            return head;
        });
        return fcrepoClient;
    }
}
