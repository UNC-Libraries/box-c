package edu.unc.lib.dl.util;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Resource;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.PcdmModels;
import edu.unc.lib.dl.util.ContentModelHelper.Model;

/**
 * 
 * @author lfarrell
 *
 */
public class TripleStoreQueryServiceFusekiImpl implements TripleStoreQueryService {
    private static final Logger log = LoggerFactory.getLogger(TripleStoreQueryServiceFusekiImpl.class);

    private String fusekiEndpointURL;
    private String fusekiQueryURL;
    private CloseableHttpClient httpClient;
    private ObjectMapper mapper;
    private PID collections;

    private final HttpClientConnectionManager multiThreadedHttpConnectionManager;

    public TripleStoreQueryServiceFusekiImpl() {
        this.multiThreadedHttpConnectionManager = new PoolingHttpClientConnectionManager();
        this.httpClient = HttpClients.custom().setConnectionManager(multiThreadedHttpConnectionManager).build();
        this.mapper = new ObjectMapper();
        this.collections = null;
    }

    public void destroy() {
        this.httpClient = null;
        this.mapper = null;
        this.multiThreadedHttpConnectionManager.shutdown();
    }

    @Override
    public List<PID> fetchAllContents(PID key) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<PID> fetchChildren(PID key) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<String, PID> fetchChildSlugs(PID parent) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<PathInfo> fetchChildPathInfo(PID parent) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<PID> fetchChildContainers(PID key) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<PID> fetchByPredicateAndLiteral(String predicateURI, String literal) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<PID> fetchByPredicateAndLiteral(String predicateURI, PID pidLiteral) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PID fetchByRepositoryPath(String path) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PID fetchContainer(PID child, Resource membershipRelation) {
        Resource relationship = (membershipRelation != null) ? membershipRelation : PcdmModels.hasMember;

        String queryString = String.format("select ?pid where { ?pid <%1$s> <%2$s> }", relationship, child.getURI());

        Query query = QueryFactory.create(queryString);

        try (QueryExecution qexec = QueryExecutionFactory.sparqlService(fusekiQueryURL, query)) {
            ResultSet results = qexec.execSelect();

            for (; results.hasNext();) {
                QuerySolution soln = results.nextSolution();
                Resource res = soln.getResource("pid");

                if (res != null) {
                    return PIDs.get(res.getURI());
                }
            }
        }

        return null;
    }

    @Override
    public PID fetchContainer(PID child) {
        return fetchContainer(child, null);
    }

    @Override
    public boolean isOrphaned(PID key) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public PID fetchParentCollection(PID key) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PID fetchParentByModel(PID key, Model model) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<PID> fetchObjectReferences(PID pid) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getResourceIndexModelUri() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isContainer(PID key) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public List<URI> lookupContentModels(PID key) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String lookupRepositoryPath(PID key) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<PathInfo> lookupRepositoryPathInfo(PID key) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<PID, ParentBond> lookupRepositoryAncestorInheritance(PID pid) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<PID> lookupRepositoryAncestorPids(PID pid) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<String, List<String>> fetchAllTriples(PID pid) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<String, List<String>> lookupSinglePermission(PID pid, String permission) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<PID, String> fetchEmbargoes() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<PID> lookupAllContainersAbove(PID pid) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isSourceData(PID pid, String datastreamID) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean allowIndexing(PID pid) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public List<String> getSourceData(PID pid) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<String> listDisseminators(PID pid) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean hasDisseminator(PID pid, String dsName) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public List<String> getSurrogateData(PID pid) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<List<String>> queryResourceIndex(String query) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PID verify(PID key) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<String> fetchAllCollectionPaths() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String lookupLabel(PID pid) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String lookupLabel(String pid) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String lookupSlug(PID pid) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String lookupSourceMimeType(PID pid) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<PID> fetchPIDsSurrogatefor(PID pid) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map sendSPARQL(String query) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map sendSPARQL(String query, String format) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<String> fetchBySubjectAndPredicate(PID subject, String predicateURI) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String fetchFirstBySubjectAndPredicate(PID subject, String predicateURI) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String fetchState(PID pid) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<String, String> fetchDisseminatorMimetypes(PID pid) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<String, Map<String, String>> fetchVocabularyInfo() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<String, Map<String, Set<String>>> fetchVocabularyMapping() {
        // TODO Auto-generated method stub
        return null;
    }

    public void setFusekiEndpointURL(String fusekiEndpointURL) {
        this.fusekiEndpointURL = fusekiEndpointURL;
    }

    public String getFusekiEndpointURL() {
        return fusekiEndpointURL;
    }

    public void setFusekiQueryURL(String fusekiQueryURL) {
        this.fusekiQueryURL = fusekiQueryURL;
    }

    public String getFusekiQueryURL() {
        return fusekiQueryURL;
    }
}
