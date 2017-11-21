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
package edu.unc.lib.dl.data.ingest.solr.action;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.ResultSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import edu.unc.lib.dl.data.ingest.solr.SolrUpdateRequest;
import edu.unc.lib.dl.data.ingest.solr.exception.IndexingException;
import edu.unc.lib.dl.fcrepo4.RepositoryObject;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.PcdmModels;
import edu.unc.lib.dl.sparql.SparqlQueryService;

/**
 * Updates an object and all of its descendants using the pipeline provided. No
 * cleanup is performed on any of the updated objects.
 *
 * @author bbpennel
 *
 */
public class UpdateTreeAction extends AbstractIndexingAction {
    private static final Logger log = LoggerFactory.getLogger(UpdateTreeAction.class);

    @Autowired
    protected SparqlQueryService sparqlQueryService;

    protected RepositoryObjectLoader repositoryObjectLoader;

    private final static String DESCENDANT_COUNT_SPARQL =
            "select (count(?child) as ?count) " +
            "where {" +
            "  <%1$s> <" + PcdmModels.hasMember.getURI() + "> ?child . " +
            "}";

    private long updateDelay;

    @Override
    public void performAction(SolrUpdateRequest updateRequest) throws IndexingException {
        log.debug("Starting update tree of {}", updateRequest.getPid());

        // Perform updates
        index(updateRequest);

        if (log.isDebugEnabled()) {
            log.debug("Finished updating tree of " + updateRequest.getPid() + ".  "
                    + updateRequest.getChildrenPending() + " objects updated in "
                    + (System.currentTimeMillis() - updateRequest.getTimeStarted()) + " ms");
        }
    }

    protected void index(SolrUpdateRequest updateRequest) throws IndexingException {
        PID startingPid = updateRequest.getPid();

        // Get the number of objects in the tree being indexed
        int totalObjects = countDescendants(startingPid) + 1;
        updateRequest.setChildrenPending(totalObjects);

        // Start indexing
        RecursiveTreeIndexer treeIndexer = new RecursiveTreeIndexer(updateRequest, this, addDocumentMode);
        RepositoryObject startingObj = repositoryObjectLoader.getRepositoryObject(startingPid);
        treeIndexer.index(startingObj, null);
    }

    /**
     * Count the number of children objects belonging to the pid provided
     *
     * @param pid
     * @return
     */
    protected int countDescendants(PID pid) {
        String query = String.format(DESCENDANT_COUNT_SPARQL, pid.getURI());
        try (QueryExecution qExecution = sparqlQueryService.executeQuery(query)) {
            ResultSet resultSet = qExecution.execSelect();
            return resultSet.next().getLiteral("count").getInt();
        }
    }

    public long getUpdateDelay() {
        return updateDelay;
    }

    public void setUpdateDelay(long updateDelay) {
        this.updateDelay = updateDelay;
    }

    /**
     * @param sparqlQueryService the sparqlQueryService to set
     */
    public void setSparqlQueryService(SparqlQueryService sparqlQueryService) {
        this.sparqlQueryService = sparqlQueryService;
    }

    /**
     * @param repositoryObjectLoader the repositoryObjectLoader to set
     */
    public void setRepositoryObjectLoader(RepositoryObjectLoader repositoryObjectLoader) {
        this.repositoryObjectLoader = repositoryObjectLoader;
    }
}