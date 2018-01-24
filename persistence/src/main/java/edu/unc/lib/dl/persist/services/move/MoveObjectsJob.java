package edu.unc.lib.dl.persist.services.move;

import static edu.unc.lib.dl.fcrepo4.RepositoryPathConstants.FCR_TOMBSTONE;

import java.io.IOException;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.RepositoryObject;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;
import edu.unc.lib.dl.fcrepo4.TransactionManager;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.fedora.ServiceException;
import edu.unc.lib.dl.services.OperationsMessageSender;
import edu.unc.lib.dl.sparql.SparqlQueryService;

/**
 * Job which performs a single move operation to transfer a list of objects from
 * their source containers to a single destination content container
 *
 * @author bbpennel
 *
 */
public class MoveObjectsJob implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(MoveObjectsJob.class);

    private AccessControlService aclService;
    private RepositoryObjectLoader repositoryObjectLoader;
    private TransactionManager transactionManager;
    private SparqlQueryService sparqlQueryService;
    private FcrepoClient fcrepoClient;
    private OperationsMessageSender operationsMessageSender;

    private AgentPrincipals agent;
    private PID destination;
    private List<PID> pids;

    private ContentContainerObject destContainer;

    private Set<PID> sources;

    public MoveObjectsJob(AgentPrincipals agent, PID destination, List<PID> pids) {
        this.agent = agent;
        this.destination = destination;
        this.pids = pids;
        sources = new HashSet<>();
    }

    @Override
    public void run() {
        log.debug("Performing move for agent {} of {} objects to destination {}",
                agent.getUsername(), pids.size(), destination);

        // Check that agent has permission to add items to destination
        aclService.assertHasAccess("Agent " + agent.getUsername() + " does not have permission"
                + " to move objects into destination " + destination,
                destination, agent.getPrincipals(), Permission.move);

        retrieveDestinationContainer();

        FedoraTransaction tx = transactionManager.startTransaction();
        try {
            for (PID movePid : pids) {
                moveObject(movePid);
            }
        } catch (Exception e) {
            tx.cancel(e);
        } finally {
            tx.close();
        }

        operationsMessageSender.sendMoveOperation(agent.getUsername(), sources, destination, pids, null);
    }

    private void retrieveDestinationContainer() {
        // Verify that the destination is a content container
        RepositoryObject destObj = repositoryObjectLoader.getRepositoryObject(destination);
        if (!(destObj instanceof ContentContainerObject)) {
            throw new IllegalArgumentException("Destination " + destination + " was not a content container");
        }
        destContainer = (ContentContainerObject) destObj;
    }

    private void moveObject(PID objPid) {
        aclService.assertHasAccess("Agent " + agent.getUsername() + " does not have permission to move object "
                + objPid, objPid, agent.getPrincipals(), Permission.move);

        RepositoryObject moveObj = repositoryObjectLoader.getRepositoryObject(objPid);
        ContentObject moveContent = (ContentObject) moveObj;

        destroyProxy(objPid);

        destContainer.addMember(moveContent);
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

    private final static String PROXY_QUERY =
            "select ?proxyuri ?parent\n" +
            "where {\n" +
            "  ?proxyuri <http://www.openarchives.org/ore/terms/proxyFor> <$1> .\n" +
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
                sources.add(PIDs.get(parentResc.getURI()));

                return URI.create(proxyUri.getURI());
            }
        }

        return null;
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

    /**
     * @param operationsMessageSender the operationsMessageSender to set
     */
    public void setOperationsMessageSender(OperationsMessageSender operationsMessageSender) {
        this.operationsMessageSender = operationsMessageSender;
    }
}
