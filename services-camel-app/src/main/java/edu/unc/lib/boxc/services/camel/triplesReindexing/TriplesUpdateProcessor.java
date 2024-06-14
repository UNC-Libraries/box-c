package edu.unc.lib.boxc.services.camel.triplesReindexing;

import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.rdf.Fcrepo4Repository;
import edu.unc.lib.boxc.model.api.rdf.Ldp;
import edu.unc.lib.boxc.model.api.sparql.SparqlQueryService;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.commons.io.IOUtils;
import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoOperationFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;

import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;

/**
 * Processor which retrieves triples from fedora and updates the triples index with them.
 *
 * @author bbpennel
 */
public class TriplesUpdateProcessor implements Processor {
    private final static Logger log = LoggerFactory.getLogger(TriplesUpdateProcessor.class);
    private final static URI SERVER_MANAGED_URI = URI.create(Fcrepo4Repository.ServerManaged.getURI());
    private final static List<URI> OMIT_URIS = Collections.singletonList(SERVER_MANAGED_URI);
    private final static URI PREFER_CONTAINMENT_URI = URI.create("http://www.w3.org/ns/ldp#PreferContainment");
    private final static List<URI> INCLUDE_URIS = Collections.singletonList(PREFER_CONTAINMENT_URI);
    private static final URI BINARY_TYPE_URI = URI.create(Ldp.NonRdfSource.getURI());
    private FcrepoClient fcrepoClient;
    private SparqlQueryService sparqlQueryService;

    @Override
    public void process(Exchange exchange) throws Exception {
        final Message in = exchange.getIn();
        String fcrepoUri = (String) in.getHeader(FCREPO_URI);
        log.debug("Processing triples update for {}", fcrepoUri);
        String respNTriples;
        var rdfEndpointUri = getRdfEndpointUri(fcrepoUri);
        try (var resp = fcrepoClient.get(rdfEndpointUri)
                .accept("application/n-triples")
                .preferRepresentation(INCLUDE_URIS, OMIT_URIS)
                .perform()) {
            respNTriples = IOUtils.toString(resp.getBody());
        }
        String sparqlUpdate = "INSERT DATA { " + respNTriples + " }";
        sparqlQueryService.executeUpdate(sparqlUpdate);
    }

    private URI getRdfEndpointUri(String fcrepoUri) throws FcrepoOperationFailedException, IOException {
        if (fcrepoUri.endsWith("/fcr:metadata")) {
            return URI.create(fcrepoUri);
        }
        try (var resp = fcrepoClient.head(URI.create(fcrepoUri)).perform()) {
            if (resp.hasType(BINARY_TYPE_URI)) {
                return URI.create(fcrepoUri + "/fcr:metadata");
            }
        }
        return URI.create(fcrepoUri);
    }

    private String retrieveNTriples(String fcrepoUri) throws IOException, FcrepoOperationFailedException {
        try (var resp = fcrepoClient.get(URI.create(fcrepoUri))
                .accept("application/n-triples")
                .preferRepresentation(INCLUDE_URIS, OMIT_URIS)
                .perform()) {
            return IOUtils.toString(resp.getBody());
        }
    }

    public void setFcrepoClient(FcrepoClient fcrepoClient) {
        this.fcrepoClient = fcrepoClient;
    }

    public void setSparqlQueryService(SparqlQueryService sparqlQueryService) {
        this.sparqlQueryService = sparqlQueryService;
    }
}
