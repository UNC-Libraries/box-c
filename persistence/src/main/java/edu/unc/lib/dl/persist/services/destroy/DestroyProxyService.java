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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Resource;
import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoOperationFailedException;
import org.fcrepo.client.FcrepoResponse;

import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.fedora.ServiceException;
import edu.unc.lib.dl.sparql.SparqlQueryService;

/**
 *
 * @author bbpennel
 * @author harring
 *
 */
public class DestroyProxyService {

    private Map<String, Collection<PID>> sourceToPid;
    private SparqlQueryService sparqlQueryService;
    private FcrepoClient fcrepoClient;

    public DestroyProxyService(Map<String, Collection<PID>> sourceToPid, SparqlQueryService sparqlQueryService,
            FcrepoClient fcrepoClient) {
        this.sourceToPid = sourceToPid;
        this.sparqlQueryService = sparqlQueryService;
        this.fcrepoClient = fcrepoClient;
    }

    public void destroyProxy(PID objPid) {
        URI proxyUri = getProxyUri(objPid);

        try (FcrepoResponse resp = fcrepoClient.delete(proxyUri).perform()) {
        } catch (FcrepoOperationFailedException | IOException e) {
            throw new ServiceException("Unable to clean up proxy for " + objPid, e);
        }

        URI tombstoneUri = URI.create(proxyUri.toString() + "/" + FCR_TOMBSTONE);
        try (FcrepoResponse resp = fcrepoClient.delete(tombstoneUri).perform()) {
        } catch (FcrepoOperationFailedException | IOException e) {
            throw new ServiceException("Unable to clean up proxy tombstone for " + objPid, e);
        }
    }

    private final static String PROXY_QUERY =
            "select ?proxyuri ?parent\n" +
            "where {\n" +
            "  ?proxyuri <http://www.openarchives.org/ore/terms/proxyFor> <%s> .\n" +
            "  ?proxyuri <http://www.openarchives.org/ore/terms/proxyIn> ?parent .\n" +
            "  FILTER regex(str(?proxyuri), \"/member\")\n" +
            "}";

    private URI getProxyUri(PID pid) {
        String query = String.format(PROXY_QUERY, pid.getRepositoryPath());

        try (QueryExecution exec = sparqlQueryService.executeQuery(query)) {
            ResultSet resultSet = exec.execSelect();

            for (; resultSet.hasNext() ;) {
                QuerySolution soln = resultSet.nextSolution();
                Resource proxyUri = soln.getResource("proxyuri");
                Resource parentResc = soln.getResource("parent");

                // Store the pid of the content container owning this proxy as a move source
                addPidToSource(pid, parentResc.getURI());

                return URI.create(proxyUri.getURI());
            }
        }
        return null;
    }

        private void addPidToSource(PID pid, String sourcePath) {
            String sourceId = PIDs.get(sourcePath).getId();
            Collection<PID> pidsForSource = sourceToPid.get(sourceId);
            if (pidsForSource == null) {
                pidsForSource = new ArrayList<>();
                sourceToPid.put(sourceId, pidsForSource);
            }
            pidsForSource.add(pid);
        }

}
