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
    void executeUpdate(String uri, String updateString);

    void executeUpdate(String updateString);

}
