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
package edu.unc.lib.dl.fedora;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Resource;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.rdf.PcdmModels;
import edu.unc.lib.dl.sparql.SparqlQueryService;

/**
 * Factory for retrieving path information for content objects
 *
 * @author bbpennel
 *
 */
public class ContentPathFactory {

    private LoadingCache<String, List<PID>> ancestorCache;
    private long cacheTimeToLive;
    private long cacheMaxSize;

    private SparqlQueryService queryService;

    private final static String ANCESTORS_QUERY = "SELECT ?start " +
            "WHERE " +
            "  {  ?mid (<%1$s>|<%2$s>)* <%3$s> . " +
            "   ?start (<%1$s>|<%2$s>)+ ?mid " +
            "  }" +
            "GROUP BY ?start " +
            "ORDER BY DESC(COUNT(?mid))";

    public void init() {
        ancestorCache = CacheBuilder.newBuilder()
                .maximumSize(cacheMaxSize)
                .expireAfterWrite(cacheTimeToLive, TimeUnit.MILLISECONDS)
                .build(new AncestorCacheLoader());
    }

    /**
     * Returns the list of PIDs for content objects which are parents of the provided
     * PID, ordered from the base of the hierarchy to the immediate parent of the PID.
     *
     * @param pid
     * @return
     */
    public List<PID> getAncestorPids(PID pid) {
        try {
            return ancestorCache.get(pid.getRepositoryPath());
        } catch (ExecutionException e) {
            throw new ServiceException(e);
        }
    }

    public void setCacheTimeToLive(long cacheTimeToLive) {
        this.cacheTimeToLive = cacheTimeToLive;
    }

    public void setCacheMaxSize(long cacheMaxSize) {
        this.cacheMaxSize = cacheMaxSize;
    }

    public void setQueryService(SparqlQueryService queryService) {
        this.queryService = queryService;
    }

    private class AncestorCacheLoader extends CacheLoader<String, List<PID>> {

        public List<PID> load(String key) {

            List<PID> results = new ArrayList<>();

            String queryString = String.format(ANCESTORS_QUERY, PcdmModels.hasFile.getURI(),
                    PcdmModels.hasMember.getURI(), key);

            try (QueryExecution qExecution = queryService.executeQuery(queryString)) {
                ResultSet resultSet = qExecution.execSelect();

                for (; resultSet.hasNext();) {
                    QuerySolution soln = resultSet.nextSolution();
                    Resource res = soln.getResource("start");

                    if (res != null) {
                        results.add(PIDs.get(res.getURI()));
                    }
                }
            }
            return results;
        }
    }
}
