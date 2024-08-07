package edu.unc.lib.boxc.fcrepo.utils;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoOperationFailedException;
import org.fcrepo.client.FcrepoResponse;

import edu.unc.lib.boxc.model.api.exceptions.FedoraException;
import edu.unc.lib.boxc.model.api.sparql.SparqlUpdateService;

/**
 * Execute sparql update queries against a fedora repository
 *
 * @author bbpennel
 *
 */
public class FedoraSparqlUpdateService implements SparqlUpdateService {

    private FcrepoClient fcrepoClient;

    public FedoraSparqlUpdateService() {
    }

    @Override
    public void executeUpdate(String uri, String updateString) {
        URI rescUri = URI.create(uri);

        try (InputStream sparqlStream = new ByteArrayInputStream(updateString.getBytes(UTF_8))) {
            try (FcrepoResponse ignored = fcrepoClient.patch(rescUri)
                    .body(sparqlStream)
                    .perform()) {
            }
        } catch (IOException e) {
            throw new FedoraException("Unable to perform update to object " + uri, e);
        } catch (FcrepoOperationFailedException e) {
            throw ClientFaultResolver.resolve(e);
        }
    }

    @Override
    public void executeUpdate(String updateString) {
        throw new UnsupportedOperationException("FedoraSparqlUpdateService requires a resource URI to update");
    }

    /**
     * @param fcrepoClient the fcrepoClient to set
     */
    public void setFcrepoClient(FcrepoClient fcrepoClient) {
        this.fcrepoClient = fcrepoClient;
    }
}
