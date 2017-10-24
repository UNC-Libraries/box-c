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
package edu.unc.lib.dl.sparql;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoOperationFailedException;
import org.fcrepo.client.FcrepoResponse;

import edu.unc.lib.dl.fcrepo4.ClientFaultResolver;
import edu.unc.lib.dl.fedora.FedoraException;

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
            try (FcrepoResponse response = fcrepoClient.patch(rescUri)
                    .body(sparqlStream)
                    .perform()) {
            }
        } catch (IOException e) {
            throw new FedoraException("Unable to perform update to object " + uri, e);
        } catch (FcrepoOperationFailedException e) {
            throw ClientFaultResolver.resolve(e);
        }
    }

    /**
     * @param fcrepoClient the fcrepoClient to set
     */
    public void setFcrepoClient(FcrepoClient fcrepoClient) {
        this.fcrepoClient = fcrepoClient;
    }
}
