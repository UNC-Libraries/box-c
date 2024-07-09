package edu.unc.lib.boxc.model.fcrepo.sparql;

import edu.unc.lib.boxc.model.api.sparql.SparqlUpdateService;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;

/**
 * Query service for updating triples in a triple store
 *
 * @author bbpennel
 */
public class TripleStoreSparqlUpdateService implements SparqlUpdateService {
    private String fusekiQueryURL;

    @Override
    public void executeUpdate(String uri, String updateString) {
        executeUpdate(updateString);
    }

    @Override
    public void executeUpdate(String queryString) {
        UpdateRequest updateRequest = UpdateFactory.create(queryString);
        UpdateProcessor updateProcessor = UpdateExecutionFactory.createRemote(updateRequest, fusekiQueryURL);
        updateProcessor.execute();
    }

    public void setFusekiQueryURL(String fusekiQueryURL) {
        this.fusekiQueryURL = fusekiQueryURL;
    }
}
