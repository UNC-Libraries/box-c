package edu.unc.lib.boxc.model.api.sparql;

import org.apache.jena.query.QueryExecution;

/**
 * Execute Sparql query
 * @author bbpennel
 *
 */
public interface SparqlQueryService {

    /**
     * Execute a select query
     * @param queryString
     * @return
     */
    public QueryExecution executeQuery(String queryString);

    /**
     * Execute an update query
     * @param queryString
     */
    public void executeUpdate(String queryString);
}
