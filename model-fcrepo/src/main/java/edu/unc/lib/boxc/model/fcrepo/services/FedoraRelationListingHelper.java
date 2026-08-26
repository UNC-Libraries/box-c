package edu.unc.lib.boxc.model.fcrepo.services;

import static edu.unc.lib.boxc.model.api.rdf.RDFModelUtil.NTRIPLES_MIMETYPE;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.riot.system.StreamRDFBase;
import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoOperationFailedException;
import org.fcrepo.client.FcrepoResponse;

import edu.unc.lib.boxc.fcrepo.utils.ClientFaultResolver;
import edu.unc.lib.boxc.model.api.exceptions.FedoraException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;

/**
 * Helpers for listing information about a resource's relationships directly from fedora.
 *
 * @author bbpennel
 */
public class FedoraRelationListingHelper {
    // URI for including references from other resources to the target resource
    private static final URI PREFER_INBOUND_REFERENCES = URI.create(
            "http://fedora.info/definitions/fcrepo#PreferInboundReferences");
    // URI for including/excluding server managed triples in fedora responses, including ldp:contains
    private static final URI PREFER_SERVER_MANAGED = URI.create(
            "http://fedora.info/definitions/fcrepo#ServerManaged");
    // URI for excluding triples that would be present when the container is empty, such as custom properties
    private static final URI PREFER_MINIMAL_CONTAINER = URI.create(
            "http://www.w3.org/ns/ldp#PreferMinimalContainer");

    private FedoraRelationListingHelper() {
    }

    /**
     * List the PIDs of resources that have relation referencing the selfUri resource
     * For example, to find the members of a container, selfUri would be the container's
     * URI and relation would be pcdm:memberOf.
     *
     * @param client fcrepo client to use to make the request
     * @param metadataUri URI to make the request against. Same as selfUri except for binary descriptions.
     * @param selfUri repository URI of the resource to find inbound relations for
     * @param relation relation predicate to match against
     * @return List of PIDs for subjects related to selfUri via the given predicate
     */
    public static List<PID> listSubjectsOfInboundRelations(FcrepoClient client,
                                                           URI metadataUri,
                                                           URI selfUri,
                                                           Property relation) {
        String selfUriString = selfUri.toString();

        // Retrieve the resource's triples from fedora, with inbound resources included and server managed excluded
        try (FcrepoResponse response = client.get(metadataUri)
                .accept(NTRIPLES_MIMETYPE)
                .preferRepresentation(List.of(PREFER_INBOUND_REFERENCES),
                        List.of(PREFER_SERVER_MANAGED, PREFER_MINIMAL_CONTAINER))
                .perform()) {
            return extractRelatedPids(response.getBody(), relation, selfUriString);
        } catch (IOException e) {
            throw new FedoraException("Failed to list objects related to " + selfUri + " by " + relation, e);
        } catch (FcrepoOperationFailedException e) {
            throw ClientFaultResolver.resolve(e);
        }
    }

    /**
     * Streams the n-triples response body, extracting the PIDs of subjects for any triples
     * which have the given predicate and whose object is the resource identified by selfUri.
     * The response is parsed as a stream rather than loaded into a Model, since it may include
     * a large number of inbound triples.
     */
    private static List<PID> extractRelatedPids(InputStream bodyStream, Property relation, String selfUri) {
        List<PID> relatedPids = new ArrayList<>();
        String relationUri = relation.getURI();

        RDFParser.source(bodyStream)
                .lang(Lang.NTRIPLES)
                .parse(new StreamRDFBase() {
                    @Override
                    public void triple(Triple triple) {
                        if (!triple.getObject().isURI() || !triple.getSubject().isURI()) {
                            return;
                        }
                        if (relationUri.equals(triple.getPredicate().getURI())
                                && selfUri.equals(triple.getObject().getURI())) {
                            relatedPids.add(PIDs.get(triple.getSubject().getURI()));
                        }
                    }
                });

        return relatedPids;
    }

    /**
     * Retrieves the set of rdf:type URIs for the resource identified by resourceUri, using a
     * lightweight HEAD request rather than retrieving and parsing the full resource body.
     *
     * @param client fcrepo client to use to make the request
     * @param resourceUri repository URI of the resource to retrieve types for
     * @return Set of rdf:type URIs, as strings, for the resource
     */
    public static Set<String> listTypes(FcrepoClient client, URI resourceUri) {
        try (FcrepoResponse response = client.head(resourceUri).perform()) {
            Set<String> types = new HashSet<>();
            for (URI typeUri : response.getLinkHeaders("type")) {
                types.add(typeUri.toString());
            }
            return types;
        } catch (IOException e) {
            throw new FedoraException("Failed to retrieve types for " + resourceUri, e);
        } catch (FcrepoOperationFailedException e) {
            throw ClientFaultResolver.resolve(e);
        }
    }
}
