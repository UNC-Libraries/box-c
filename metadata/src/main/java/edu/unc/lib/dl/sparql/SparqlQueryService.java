package edu.unc.lib.dl.sparql;

import org.apache.jena.query.QueryExecution;

public interface SparqlQueryService {

	public QueryExecution executeQuery(String queryString);
}
