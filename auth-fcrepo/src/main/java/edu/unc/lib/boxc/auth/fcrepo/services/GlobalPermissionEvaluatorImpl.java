package edu.unc.lib.boxc.auth.fcrepo.services;

import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.UserRole;
import edu.unc.lib.boxc.auth.api.services.GlobalPermissionEvaluator;
import edu.unc.lib.boxc.auth.api.services.PrincipalClassifier;

/**
 * Utility which evaluates the permissions of agents against globally defined
 * roles.
 *
 * @author bbpennel
 *
 */
public class GlobalPermissionEvaluatorImpl implements GlobalPermissionEvaluator {

    protected static final String GLOBAL_PROP_PREFIX = "cdr.acl.globalRoles.";
    private Map<String, Set<String>> globalPrincipalToPermissions;
    private Map<String, UserRole> globalPrincipalToRole;
    private Set<String> globalPrincipals;

    public GlobalPermissionEvaluatorImpl(Properties properties) {
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
                // Split comma separated principals and add entry to stream for each
                .flatMap(e -> Arrays.stream(((String) e.getValue()).split(","))
                        .map(p -> new SimpleEntry<>(p, (String) e.getKey())))
                // Transform into map of principal to permissions
                .collect(Collectors.toMap(SimpleEntry::getKey, p -> {
                    if (PrincipalClassifier.isPatronPrincipal(p.getKey())) {
                        throw new IllegalArgumentException(
                                "Cannot grant global role to patron principal " + p.getKey());
                    }
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
    @Override
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
    @Override
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
    @Override
    public Set<UserRole> getGlobalUserRoles(Set<String> agentPrincipals) {
        return agentPrincipals.stream()
                .filter(p -> globalPrincipalToRole.containsKey(p))
                .map(p -> globalPrincipalToRole.get(p))
                .collect(Collectors.toSet());
    }
}
