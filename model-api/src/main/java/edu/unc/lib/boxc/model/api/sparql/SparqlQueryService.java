package edu.unc.lib.boxc.model.api.sparql;

import org.apache.jena.query.QueryExecution;

/**
 * Execute Sparql query
 * @author bbpennel
 *
 */
public interface SparqlQueryService {

    QueryExecution executeQuery(String queryString);
}
