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

import org.apache.commons.lang3.NotImplementedException;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import edu.unc.lib.dl.acl.util.RoleAssignment;
import edu.unc.lib.dl.acl.util.UserRole;
import edu.unc.lib.dl.fcrepo4.RepositoryObject;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.rdf.CdrAcl;
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

    private static String DELETED_PROPERTY_URI = CdrAcl.markedForDeletion.getURI();
    private static String TOMBSTONE_URI = Cdr.Tombstone.getURI();

    private LoadingCache<PID, List<Entry<String, String>>> objAclCache;
    private long cacheTimeToLive;
    private long cacheMaxSize;

    private RepositoryObjectLoader repoObjLoader;

    private final List<String> roleUris;

    public ObjectAclFactory() {
        List<String> uris = new ArrayList<>(UserRole.values().length);

        for (UserRole role : UserRole.values()) {
            uris.add(role.getPropertyString());
        }

        this.roleUris = Collections.unmodifiableList(uris);
    }

    public void init() {
        objAclCache = CacheBuilder.newBuilder()
                .maximumSize(cacheMaxSize)
                .expireAfterWrite(cacheTimeToLive, TimeUnit.MILLISECONDS)
                .build(new ObjectAclCacheLoader());
    }

    @Override
    public Map<String, Set<String>> getPrincipalRoles(PID pid) {

        return getObjectAcls(pid).stream()
                // Filter to only role assignments
                .filter(p -> roleUris.contains(p.getKey()))
                // Group up roles by principal
                .collect(Collectors.groupingBy(
                        Entry<String, String>::getValue,
                        Collectors.mapping(Entry<String, String>::getKey, Collectors.toSet())
                    ));
    }

    @Override
    public List<RoleAssignment> getStaffRoleAssignments(PID pid) {
        return getRoleAssignments(pid, true);
    }

    @Override
    public List<RoleAssignment> getPatronRoleAssignments(PID pid) {
        return getRoleAssignments(pid, false);
    }

    private List<RoleAssignment> getRoleAssignments(PID pid, boolean retrieveStaffRoles) {
        Map<String, Set<String>> princToRoles = getPrincipalRoles(pid);

        List<RoleAssignment> result = new ArrayList<>();
        princToRoles.forEach((princ, roles) -> {
            for (String roleString: roles) {
                UserRole role = UserRole.getRoleByProperty(roleString);
                // Skip over either staff or patrons roles, depending on what is being requested
                if (retrieveStaffRoles == role.isStaffRole()) {
                    result.add(new RoleAssignment(princ, role, pid));
                }
            }
        });

        return result;
    }

    @Override
    public List<RoleAssignment> getPatronAccess(PID pid) {
        throw new NotImplementedException("Not implemented for ObjectAclFactory at this time");
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
                    } catch (IllegalArgumentException e) {
                        log.warn("Failed to parse embargo {} for {} while retrieving ACLs",
                                new Object[] {p.getValue(), pid}, e);
                        return null;
                    }
                })
                .orElse(null);
    }

    @Override
    public boolean isMarkedForDeletion(PID pid) {

        return getObjectAcls(pid).stream()
                .anyMatch(p -> DELETED_PROPERTY_URI.equals(p.getKey())
                        || TOMBSTONE_URI.equals(p.getValue()));
    }

    private List<Entry<String, String>> getObjectAcls(PID pid) {
        return objAclCache.getUnchecked(pid);
    }

    /**
     * Invalidates cached data for the provided pid
     * @param pid
     */
    public void invalidate(PID pid) {
        objAclCache.invalidate(pid);
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

    /**
     * @param repositoryObjectLoader the repositoryObjectLoader to set
     */
    public void setRepositoryObjectLoader(RepositoryObjectLoader repositoryObjectLoader) {
        this.repoObjLoader = repositoryObjectLoader;
    }

    /**
     * Loader for cache of ACL information about individual objects. Retrieves
     * directly present ACLs according to fcrepo.
     *
     * @author bbpennel
     *
     */
    private class ObjectAclCacheLoader extends CacheLoader<PID, List<Entry<String, String>>> {
        @Override
        public List<Entry<String, String>> load(PID pid) {
            List<Entry<String, String>> valueResults = new ArrayList<>();

            RepositoryObject repoObj = repoObjLoader.getRepositoryObject(pid);
            Model model = repoObj.getModel();
            StmtIterator it = model.listStatements();
            while (it.hasNext()) {
                Statement stmt = it.next();
                Property property = stmt.getPredicate();
                if (CdrAcl.NS.equals(property.getNameSpace())
                        || (RDF.type.equals(property) && Cdr.Tombstone.equals(stmt.getResource()))) {
                    RDFNode valueNode = stmt.getObject();
                    String valueString;
                    if (valueNode.isLiteral()) {
                        valueString = valueNode.asLiteral().getLexicalForm();
                    } else {
                        valueString = valueNode.asResource().getURI();
                    }

                    valueResults.add(new SimpleEntry<> (property.getURI(), valueString));
                }
            }

            return valueResults;
        }
    }
}
