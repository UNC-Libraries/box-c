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
package edu.unc.lib.dl.persist.services.move;

import static edu.unc.lib.dl.fcrepo4.RepositoryPathConstants.FCR_TOMBSTONE;

import java.io.IOException;
import java.net.URI;
import java.security.SecureRandom;
import java.util.List;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Resource;
import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoOperationFailedException;
import org.fcrepo.client.FcrepoResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.acl.service.AccessControlService;
import edu.unc.lib.dl.acl.util.AgentPrincipals;
import edu.unc.lib.dl.acl.util.Permission;
import edu.unc.lib.dl.fcrepo4.ContentContainerObject;
import edu.unc.lib.dl.fcrepo4.ContentObject;
import edu.unc.lib.dl.fcrepo4.FedoraTransaction;
import edu.unc.lib.dl.fcrepo4.RepositoryObject;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;
import edu.unc.lib.dl.fcrepo4.TransactionManager;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.fedora.ServiceException;
import edu.unc.lib.dl.sparql.SparqlQueryService;

/**
 * Service which moves content objects between containers.
 *
 * @author bbpennel
 *
 */
public class MoveObjectsService {

    private static final Logger log = LoggerFactory.getLogger(MoveObjectsService.class);

    private AccessControlService aclService;
    private RepositoryObjectLoader repositoryObjectLoader;
    private TransactionManager transactionManager;
    private SparqlQueryService sparqlQueryService;
    private FcrepoClient fcrepoClient;

    public MoveObjectsService() {
    }

    /**
     * Move a list of objects to the destination container as the provided
     * agent.
     *
     * @param agent
     * @param destination
     * @param pids
     */
    public void moveObjects(AgentPrincipals agent, PID destination, List<PID> pids) {
        log.debug("Agent {} requesting move of {} objects to destination {}",
                agent.getUsername(), pids.size(), destination);

        // Check that agent has permission to add items to destination
        aclService.assertHasAccess("Agent " + agent.getUsername() + " does not have permission"
                + " to move objects into destination " + destination,
                destination, agent.getPrincipals(), Permission.move);

        // Verify that the destination is a content container
        RepositoryObject destObj = repositoryObjectLoader.getRepositoryObject(destination);
        if (!(destObj instanceof ContentContainerObject)) {
            throw new IllegalArgumentException("Destination " + destination + " was not a content container");
        }
        ContentContainerObject destContainer = (ContentContainerObject) destObj;

        FedoraTransaction tx = transactionManager.startTransaction();
        try {
            for (PID movePid : pids) {
                moveObject(agent, movePid, destContainer);
            }
        } catch (Exception e) {
            tx.cancel(e);
        } finally {
            tx.close();
        }

        System.out.println(new SecureRandom().nextLong());
    }

    private void moveObject(AgentPrincipals agent, PID objPid, ContentContainerObject destObj) {
        aclService.assertHasAccess("Agent " + agent.getUsername() + " does not have permission to move object "
                + objPid, objPid, agent.getPrincipals(), Permission.move);

        RepositoryObject moveObj = repositoryObjectLoader.getRepositoryObject(objPid);
        ContentObject moveContent = (ContentObject) moveObj;

        destroyProxy(objPid);

        destObj.addMember(moveContent);
    }

    private final static String PROXY_QUERY =
            "select ?proxyuri\n" +
            "where {\n" +
            "  ?proxyuri <http://www.openarchives.org/ore/terms/proxyFor> <$1> .\n" +
            "  ?proxyuri <http://fedora.info/definitions/v4/repository#hasParent> ?parent .\n" +
            "  FILTER regex(str(?parent), \"/member\")\n" +
            "}";

    private URI getProxyUri(PID pid) {
        String query = String.format(PROXY_QUERY, pid.getRepositoryPath());

        try (QueryExecution exec = sparqlQueryService.executeQuery(query)) {
            ResultSet resultSet = exec.execSelect();

            for (; resultSet.hasNext() ;) {
                QuerySolution soln = resultSet.nextSolution();
                Resource proxyUri = soln.getResource("proxyuri");

                return URI.create(proxyUri.getURI());
            }
        }

        return null;
    }

    private void destroyProxy(PID objPid) {
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

    /**
     * @param aclService the aclService to set
     */
    public void setAclService(AccessControlService aclService) {
        this.aclService = aclService;
    }

    /**
     * @param repositoryObjectLoader the repositoryObjectLoader to set
     */
    public void setRepositoryObjectLoader(RepositoryObjectLoader repositoryObjectLoader) {
        this.repositoryObjectLoader = repositoryObjectLoader;
    }

    /**
     * @param transactionManager the transactionManager to set
     */
    public void setTransactionManager(TransactionManager transactionManager) {
        this.transactionManager = transactionManager;
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
}
