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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import edu.unc.lib.dl.acl.util.Permission;
import edu.unc.lib.dl.acl.util.UserRole;

/**
 * Utility which evaluates the permissions of agents against globally defined
 * roles.
 *
 * @author bbpennel
 *
 */
public class GlobalPermissionEvaluator {

    protected static final String GLOBAL_PROP_PREFIX = "cdr.acl.globalRoles.";
    private Map<String, Set<String>> globalPrincipalToPermissions;
    private Map<String, UserRole> globalPrincipalToRole;
    private Set<String> globalPrincipals;

    public GlobalPermissionEvaluator(Properties properties) {
        storeGlobalPrincipals(properties);
        globalPrincipals = Collections.unmodifiableSet(globalPrincipalToPermissions.keySet());
    }

    private void storeGlobalPrincipals(Properties properties) {
        globalPrincipalToRole = new HashMap<>();

        // Transform properties defining global role assignments into a lookup
        // table of global principals to permissions.
        // A principal cannot be assigned to more than one global role
        globalPrincipalToPermissions = properties.entrySet().stream()
                .filter(p -> p.getKey().toString().startsWith(GLOBAL_PROP_PREFIX))
                .map(p -> new SimpleEntry<>((String) p.getValue(), (String) p.getKey()))
                // Transform into map of principal to permissions
                .collect(Collectors.toMap(SimpleEntry::getKey, p -> {
                    String roleString = p.getValue().substring(GLOBAL_PROP_PREFIX.length());
                    UserRole role = UserRole.valueOf(roleString);
                    // Role must be valid and not a patron
                    if (role == null || role.isPatronRole()) {
                        throw new IllegalArgumentException(
                                "Invalid global role " + p.getValue() +
                                " defined for " + p.getKey());
                    }
                    // Add principal to role mapping
                    globalPrincipalToRole.put(p.getKey(), role);

                    // Expand role assignment into permissions
                    return role.getPermissions().stream()
                            .map(q -> q.name()).collect(Collectors.toSet());
                }));
    }

    /**
     * Returns true if any of the principals given are assigned a global role
     * which grants the requested permission
     *
     * @param agentPrincipals
     * @param permission
     * @return
     */
    public boolean hasGlobalPermission(Set<String> agentPrincipals, Permission permission) {
        return agentPrincipals.stream().anyMatch(p -> {
            Set<String> permissions = globalPrincipalToPermissions.get(p);
            if (permissions == null) {
                return false;
            }
            return permissions.contains(permission.name());
        });
    }

    /**
     * Returns true if any of the provided principals is assigned a global role
     *
     * @param agentPrincipals
     * @return true if agentPrincipals contains a global principal
     */
    public boolean hasGlobalPrincipal(Set<String> agentPrincipals) {
        return globalPrincipals.stream().anyMatch(agentPrincipals::contains);
    }

    /**
     * Returns a set of UserRoles which are globally assigned to the provided
     * principals
     *
     * @param agentPrincipals
     * @return set of UserRoles globally assigned to the principals
     */
    public Set<UserRole> getGlobalUserRoles(Set<String> agentPrincipals) {
        return agentPrincipals.stream()
                .filter(p -> globalPrincipalToRole.containsKey(p))
                .map(p -> globalPrincipalToRole.get(p))
                .collect(Collectors.toSet());
    }
}
