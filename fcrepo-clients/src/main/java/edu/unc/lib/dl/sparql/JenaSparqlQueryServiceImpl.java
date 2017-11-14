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

    public Model getModel() {
        return model;
    }

    @Override
    public QueryExecution executeQuery(String queryString) {
        Query query = QueryFactory.create(queryString);

        return QueryExecutionFactory.create(query, model);
    }

}
