package edu.unc.lib.boxc.operations.impl.destroy;

import static edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants.FCR_TOMBSTONE;

import java.io.IOException;
import java.net.URI;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Resource;
import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoOperationFailedException;
import org.fcrepo.client.FcrepoResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.boxc.fcrepo.exceptions.ServiceException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.sparql.SparqlQueryService;

/**
 * Service for destroying membership proxies from ldp IndirectContainers.
 *
 * @author bbpennel
 * @author harring
 *
 */
public class DestroyProxyService {

    private static final Logger log = LoggerFactory.getLogger(DestroyProxyService.class);

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
        if (proxyInfo == null) {
            log.debug("No proxy found for object {}", objPid);
            return null;
        }
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
