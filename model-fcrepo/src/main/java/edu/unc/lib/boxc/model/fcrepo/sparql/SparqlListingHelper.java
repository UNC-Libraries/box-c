/**
 * Copyright 2008 The University of North Carolina at Chapel Hill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
