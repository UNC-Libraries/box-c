package edu.unc.lib.boxc.model.fcrepo.sparql;

import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.sparql.SparqlQueryService;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Resource;

import java.util.ArrayList;
import java.util.List;

/**
 * Helpers for common sparql listing operations
 * @author bbpennel
 */
public class SparqlListingHelper {
    private SparqlListingHelper() {
    }

    /**
     * Retrieve a list of PID results for a sparql query. Query should contain a select parameter named 'pid'
     * @param sparqlQueryService
     * @param queryString
     * @return
     */
    public static List<PID> listPids(SparqlQueryService sparqlQueryService, String queryString) {
        var pids = new ArrayList<PID>();

        try (QueryExecution qexec = sparqlQueryService.executeQuery(queryString)) {
            ResultSet results = qexec.execSelect();

            while (results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                Resource res = soln.getResource("pid");

                if (res != null) {
                    pids.add(PIDs.get(res.getURI()));
                }
            }
        }

        return pids;
    }
}
