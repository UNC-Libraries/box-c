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
package edu.unc.lib.dl.acl.fcrepo4;

import java.text.ParseException;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import edu.unc.lib.dl.acl.service.PatronAccess;
import edu.unc.lib.dl.acl.util.UserRole;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.CdrAcl;
import edu.unc.lib.dl.sparql.SparqlQueryService;
import edu.unc.lib.dl.util.DateTimeUtil;

/**
 * Factory which provides access control details which are directly represented
 * on objects, but does not take into account inheritance.
 *
 * @author bbpennel
 *
 */
public class ObjectAclFactory implements AclFactory {

    private static final Logger log = LoggerFactory.getLogger(ObjectAclFactory.class);

    private LoadingCache<String, List<Entry<String, String>>> objAclCache;
    private long cacheTimeToLive;
    private long cacheMaxSize;

    private SparqlQueryService queryService;

    private final List<String> roleUris;

    public ObjectAclFactory() {
        List<String> roleUris = new ArrayList<>(UserRole.values().length);

        for (UserRole role : UserRole.values()) {
            roleUris.add(role.getPropertyString());
        }

        this.roleUris = Collections.unmodifiableList(roleUris);
    }

    public void init() {
        objAclCache = CacheBuilder.newBuilder()
                .maximumSize(cacheMaxSize)
                .expireAfterWrite(cacheTimeToLive, TimeUnit.MILLISECONDS)
                .build(new ObjectAclCacheLoader());
    }

    @Override
    public Map<String, Set<String>> getPrincipalRoles(PID pid) {

        Map<String, Set<String>> result = getObjectAcls(pid).stream()
                // Filter to only role assignments
                .filter(p -> roleUris.contains(p.getKey()))
                // Group up roles by principal
                .collect(Collectors.groupingBy(
                        Entry<String, String>::getValue,
                        Collectors.mapping(Entry<String, String>::getKey, Collectors.toSet())
                    ));

        return result;
    }

    @Override
    public PatronAccess getPatronAccess(PID pid) {

        String patronPropertyUri = CdrAcl.patronAccess.getURI();
        return getObjectAcls(pid).stream()
                .filter(p -> patronPropertyUri.equals(p.getKey()))
                .findFirst()
                .map(p -> PatronAccess.valueOf(p.getValue()))
                .orElse(PatronAccess.parent);
    }

    @Override
    public Date getEmbargoUntil(PID pid) {

        String embargoPropertyUri = CdrAcl.embargoUntil.getURI();
        return getObjectAcls(pid).stream()
                .filter(p -> embargoPropertyUri.equals(p.getKey()))
                .findFirst()
                .map(p -> {
                    try {
                        return DateTimeUtil.parseUTCToDate(p.getValue());
                    } catch (IllegalArgumentException | ParseException e) {
                        log.warn("Failed to parse embargo {} for {} while retrieving ACLs",
                                new Object[] {p.getValue(), pid}, e);
                        return null;
                    }
                })
                .orElse(null);
    }

    @Override
    public boolean isMarkedForDeletion(PID pid) {

        String deletedPropertyUri = CdrAcl.markedForDeletion.getURI();
        return getObjectAcls(pid).stream()
                .filter(p -> deletedPropertyUri.equals(p.getKey()))
                .findFirst()
                .isPresent();
    }

    private List<Entry<String, String>> getObjectAcls(PID pid) {
        String pidString = pid.getRepositoryPath();
        return objAclCache.getUnchecked(pidString);
    }

    public long getCacheTimeToLive() {
        return cacheTimeToLive;
    }

    public void setCacheTimeToLive(long cacheTimeToLive) {
        this.cacheTimeToLive = cacheTimeToLive;
    }

    public long getCacheMaxSize() {
        return cacheMaxSize;
    }

    public void setCacheMaxSize(long cacheMaxSize) {
        this.cacheMaxSize = cacheMaxSize;
    }

    public SparqlQueryService getQueryService() {
        return queryService;
    }

    public void setQueryService(SparqlQueryService queryService) {
        this.queryService = queryService;
    }

    /**
     * Loader for cache of ACL information about individual objects. Retrieves
     * ACL properties from a SPARQL endpoint which are directly present on
     * objects
     *
     * @author bbpennel
     *
     */
    private class ObjectAclCacheLoader extends CacheLoader<String, List<Entry<String, String>>> {

        private static final String ACL_QUERY = "SELECT ?pred ?obj"
                + " WHERE { <%1$s> ?pred ?obj ."
                + "   filter(strstarts(str(?pred), \"http://cdr.unc.edu/definitions/acl#\")) . }";

        public List<Entry<String, String>> load(String key) {

            String query = String.format(ACL_QUERY, key);

            try (QueryExecution qExecution = queryService.executeQuery(query)) {
                ResultSet resultSet = qExecution.execSelect();
                List<Entry<String, String>> valueResults = new ArrayList<>();

                // Read all results into a list of predicate object pairs
                for (; resultSet.hasNext() ;) {
                    QuerySolution soln = resultSet.nextSolution();
                    Resource predicateRes = soln.getResource("pred");
                    RDFNode valueNode = soln.get("obj");

                    if (predicateRes != null && valueNode != null) {
                        String predicateString = predicateRes.getURI();
                        String valueString;
                        if (valueNode.isLiteral()) {
                            valueString = valueNode.asLiteral().getLexicalForm();
                        } else {
                            valueString = valueNode.asResource().getURI();
                        }

                        valueResults.add(new SimpleEntry<String, String>
                                (predicateString, valueString));
                    }
                }

                return valueResults;
            }
        }
    }
}
