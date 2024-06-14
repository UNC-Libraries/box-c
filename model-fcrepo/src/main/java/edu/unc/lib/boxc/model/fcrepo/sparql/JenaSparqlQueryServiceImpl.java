package edu.unc.lib.boxc.model.fcrepo.sparql;

import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;

import edu.unc.lib.boxc.model.api.sparql.SparqlQueryService;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;

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

    public Model getModel() {
        return model;
    }

    @Override
    public QueryExecution executeQuery(String queryString) {
        Query query = QueryFactory.create(queryString);

        return QueryExecutionFactory.create(query, model);
    }

    @Override
    public void executeUpdate(String queryString) {
        UpdateRequest updateRequest = UpdateFactory.create(queryString);
        UpdateProcessor updateProcessor = UpdateExecutionFactory.create(updateRequest, DatasetFactory.create(model));
        updateProcessor.execute();
    }
}
