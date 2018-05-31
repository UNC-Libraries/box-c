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
package edu.unc.lib.dl.persist.services.destroy;

import static edu.unc.lib.dl.fcrepo4.RepositoryPathConstants.FCR_TOMBSTONE;

import java.io.IOException;
import java.net.URI;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Resource;
import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoOperationFailedException;
import org.fcrepo.client.FcrepoResponse;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.fedora.ServiceException;
import edu.unc.lib.dl.sparql.SparqlQueryService;

/**
 * Service for destroying membership proxies from ldp IndirectContainers.
 *
 * @author bbpennel
 * @author harring
 *
 */
public class DestroyProxyService {

    private SparqlQueryService sparqlQueryService;
    private FcrepoClient fcrepoClient;

    public DestroyProxyService() {
    }

    /**
     * Destroys the membership proxy referencing objPid.
     *
     * @param objPid pid of the object whose proxy will be destroyed.
     * @return the path of the parent object the proxy was removed from.
     */
    public String destroyProxy(PID objPid) {
        ProxyInfo proxyInfo = getProxyInfo(objPid);
        URI proxyUri = proxyInfo.proxyUri;

        try (FcrepoResponse resp = fcrepoClient.delete(proxyUri).perform()) {
        } catch (FcrepoOperationFailedException | IOException e) {
            throw new ServiceException("Unable to clean up proxy for " + objPid, e);
        }

        URI tombstoneUri = URI.create(proxyUri.toString() + "/" + FCR_TOMBSTONE);
        try (FcrepoResponse resp = fcrepoClient.delete(tombstoneUri).perform()) {
        } catch (FcrepoOperationFailedException | IOException e) {
            throw new ServiceException("Unable to clean up proxy tombstone for " + objPid, e);
        }

        return proxyInfo.sourcePath;
    }

    private final static String PROXY_QUERY =
            "select ?proxyuri ?parent\n" +
            "where {\n" +
            "  ?proxyuri <http://www.openarchives.org/ore/terms/proxyFor> <%s> .\n" +
            "  ?proxyuri <http://www.openarchives.org/ore/terms/proxyIn> ?parent .\n" +
            "  FILTER regex(str(?proxyuri), \"/member\")\n" +
            "}";

    private ProxyInfo getProxyInfo(PID pid) {
        String query = String.format(PROXY_QUERY, pid.getRepositoryPath());

        try (QueryExecution exec = sparqlQueryService.executeQuery(query)) {
            ResultSet resultSet = exec.execSelect();

            for (; resultSet.hasNext() ;) {
                QuerySolution soln = resultSet.nextSolution();
                Resource proxyUri = soln.getResource("proxyuri");
                Resource parentResc = soln.getResource("parent");

                return new ProxyInfo(URI.create(proxyUri.getURI()), parentResc.getURI());
            }
        }
        return null;
    }

    /**
     * @param sparqlQueryService the sparqlQueryService to set
     */
    public void setSparqlQueryService(SparqlQueryService sparqlQueryService) {
        this.sparqlQueryService = sparqlQueryService;
    }

    /**
     * @param fcrepoClient the fcrepoClient to set
     */
    public void setFcrepoClient(FcrepoClient fcrepoClient) {
        this.fcrepoClient = fcrepoClient;
    }

    private static class ProxyInfo {
        public URI proxyUri;
        public String sourcePath;

        public ProxyInfo(URI proxyUri, String sourcePath) {
            this.proxyUri = proxyUri;
            this.sourcePath = sourcePath;
        }

    }
}
