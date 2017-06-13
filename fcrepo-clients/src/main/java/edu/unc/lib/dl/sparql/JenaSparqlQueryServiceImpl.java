package edu.unc.lib.dl.sparql;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;

/**
 * Service for initiating sparql queries against an in-memory jena model
 *
 * @author bbpennel
 *
 */
public class JenaSparqlQueryServiceImpl implements SparqlQueryService {

    private Model model;

    public JenaSparqlQueryServiceImpl(Model model) {
        this.model = model;
    }

    @Override
    public QueryExecution executeQuery(String queryString) {
        Query query = QueryFactory.create(queryString);

        return QueryExecutionFactory.create(query, model);
    }

}
