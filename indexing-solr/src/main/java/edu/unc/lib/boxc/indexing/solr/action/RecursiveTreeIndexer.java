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
package edu.unc.lib.boxc.indexing.solr.action;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.boxc.indexing.solr.exception.IndexingException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.RepositoryObject;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.rdf.PcdmModels;
import edu.unc.lib.boxc.model.api.sparql.SparqlQueryService;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingActionType;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingMessageSender;

/**
 * Performs depth first indexing of a tree of repository objects, starting at the PID of the provided update request.
 *
 * @author bbpennel
 *
 */
public class RecursiveTreeIndexer {
    private static final Logger log = LoggerFactory.getLogger(RecursiveTreeIndexer.class);

    private IndexingMessageSender messageSender;

    private SparqlQueryService queryService;

    private Set<String> CONTAINER_TYPES = new HashSet<>(Arrays.asList(Cdr.AdminUnit.getURI(),
            Cdr.Collection.getURI(),
            Cdr.ContentRoot.getURI(),
            Cdr.Folder.getURI(),
            Cdr.Work.getURI()));

    public RecursiveTreeIndexer() {
    }

    /**
     * Index the provided repoObj and all of its children
     * @param repoObj
     * @param actionType Type of indexing action to perform
     * @param userid
     * @throws IndexingException
     */
    public void index(RepositoryObject repoObj, IndexingActionType actionType, String userid)
            throws IndexingException {
        PID pid = repoObj.getPid();
        Set<String> types = repoObj.getResource().listProperties(RDF.type).toList().stream()
                .map(Statement::getResource)
                .map(Resource::getURI)
                .collect(Collectors.toSet());

        index(pid, types, actionType, userid);
    }

    private void index(PID pid, Set<String> types, IndexingActionType actionType, String userid)
            throws IndexingException {
        if (types.contains(Cdr.Tombstone.getURI())) {
            log.debug("Skipping indexing tombstone object {}", pid.getQualifiedId());
            return;
        }

        messageSender.sendIndexingOperation(userid, pid, actionType);

        if (types.stream().anyMatch(CONTAINER_TYPES::contains)) {
            // Start indexing the children
            indexChildren(pid, actionType, userid);
        }
    }

    /**
     * Index all the children of the provided parentPid
     * @param parentPid
     * @param actionType Type of indexing action to perform
     * @param userid
     * @throws IndexingException
     */
    public void indexChildren(PID parentPid, IndexingActionType actionType, String userid)
            throws IndexingException {
        Map<String, Set<String>> childToTypes = getMembers(parentPid);

        if (childToTypes.size() == 0) {
            return;
        }
        log.debug("Queuing {} children of {} for indexing", childToTypes.size(), parentPid);
        childToTypes.forEach((childPid, types) -> {
            index(PIDs.get(childPid), types, actionType, userid);
        });
    }

    private final static String CHILDREN_QUERY =
            "select ?pid ?rdfType"
            + " where {"
                + " ?pid <%1$s> <%2$s> ."
                + " ?pid <%3$s> ?rdfType . }";

    private Map<String, Set<String>> getMembers(PID parentPid) {
        String queryString = String.format(CHILDREN_QUERY,
                PcdmModels.memberOf, parentPid.getURI(), RDF.type, Cdr.NS);

        log.debug("Performing member query:\n{}", queryString);

        Map<String, Set<String>> childToTypes = new HashMap<>();

        try (QueryExecution qexec = queryService.executeQuery(queryString)) {
            ResultSet results = qexec.execSelect();

            while (results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                Resource pidResc = soln.getResource("pid");
                Resource type = soln.getResource("rdfType");

                if (pidResc == null || type == null) {
                    continue;
                }

                String pid = pidResc.getURI();

                Set<String> types = childToTypes.get(pid);
                if (types == null) {
                    types = new HashSet<>();
                    childToTypes.put(pid, types);
                }
                types.add(type.getURI());
            }
        }

        return childToTypes;
    }

    /**
     * @param messageSender the messageSender to set
     */
    public void setIndexingMessageSender(IndexingMessageSender messageSender) {
        this.messageSender = messageSender;
    }

    public void setSparqlQueryService(SparqlQueryService queryService) {
        this.queryService = queryService;
    }
}