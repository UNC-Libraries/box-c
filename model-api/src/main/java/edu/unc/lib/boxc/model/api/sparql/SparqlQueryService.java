package edu.unc.lib.boxc.model.api.sparql;

import org.apache.jena.query.QueryExecution;

/**
 * Execute Sparql query
 * @author bbpennel
 *
 */
public interface SparqlQueryService {

    public QueryExecution executeQuery(String queryString);
}
