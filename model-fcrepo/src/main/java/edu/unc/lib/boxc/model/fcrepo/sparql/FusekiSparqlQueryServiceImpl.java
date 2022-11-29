package edu.unc.lib.boxc.model.fcrepo.sparql;

import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;

import edu.unc.lib.boxc.model.api.sparql.SparqlQueryService;

import java.lang.reflect.Method;

/**
 * Service for executing sparql queries against a Fuseki backend
 *
 * @author bbpennel
 *
 */
public class FusekiSparqlQueryServiceImpl implements SparqlQueryService {

    private String fusekiQueryURL;

    @Override
    public QueryExecution executeQuery(String queryString) {
        Query query = QueryFactory.create(queryString);

        return QueryExecution.service(fusekiQueryURL)
                .query(query)
                .build();
    }

    public void setFusekiQueryURL(String fusekiQueryURL) {
        this.fusekiQueryURL = fusekiQueryURL;
    }
}
