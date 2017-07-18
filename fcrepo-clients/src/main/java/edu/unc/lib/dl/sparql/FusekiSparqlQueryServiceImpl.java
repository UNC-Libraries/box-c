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

import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;

/**
 * Service for executing sparql queries against a Fuseki backend
 *
 * @author bbpennel
 *
 */
public class FusekiSparqlQueryServiceImpl implements SparqlQueryService {

    private String fusekiQueryURL;
    private CloseableHttpClient httpClient;
    private final HttpClientConnectionManager multiThreadedHttpConnectionManager;

    public FusekiSparqlQueryServiceImpl() {
        this.multiThreadedHttpConnectionManager = new PoolingHttpClientConnectionManager();
        this.httpClient = HttpClients.custom()
                .setConnectionManager(multiThreadedHttpConnectionManager)
                .build();
    }

    public void destroy() {
        this.httpClient = null;
        this.multiThreadedHttpConnectionManager.shutdown();
    }

    @Override
    public QueryExecution executeQuery(String queryString) {
        Query query = QueryFactory.create(queryString);

        return QueryExecutionFactory.sparqlService(fusekiQueryURL, query, httpClient);
    }

    public void setFusekiQueryURL(String fusekiQueryURL) {
        this.fusekiQueryURL = fusekiQueryURL;
    }
}
