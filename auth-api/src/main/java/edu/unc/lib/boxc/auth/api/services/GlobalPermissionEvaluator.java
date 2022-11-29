package edu.unc.lib.boxc.auth.api.services;

import java.util.Set;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.UserRole;

/**
 * Utility which evaluates the permissions of agents against globally defined roles.
 * @author bbpennel
 */
public interface GlobalPermissionEvaluator {

    /**
     * Returns true if any of the principals given are assigned a global role
     * which grants the requested permission
     *
     * @param agentPrincipals
     * @param permission
     * @return
     */
    boolean hasGlobalPermission(Set<String> agentPrincipals, Permission permission);

    /**
     * Returns true if any of the provided principals is assigned a global role
     *
     * @param agentPrincipals
     * @return true if agentPrincipals contains a global principal
     */
    boolean hasGlobalPrincipal(Set<String> agentPrincipals);

    /**
     * Returns a set of UserRoles which are globally assigned to the provided
     * principals
     *
     * @param agentPrincipals
     * @return set of UserRoles globally assigned to the principals
     */
    Set<UserRole> getGlobalUserRoles(Set<String> agentPrincipals);

}