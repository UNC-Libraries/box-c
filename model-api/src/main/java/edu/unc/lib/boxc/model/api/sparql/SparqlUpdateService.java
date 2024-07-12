package edu.unc.lib.boxc.model.api.sparql;

/**
 * Service for executing sparql update queries.
 *
 * @author bbpennel
 *
 */
public interface SparqlUpdateService {

    /**
     * Execute the provided sparql update query against the resource identified
     * by uri
     *
     * @param uri
     * @param updateString
     */
    public void executeUpdate(String uri, String updateString);

    public void executeUpdate(String updateString);

}
