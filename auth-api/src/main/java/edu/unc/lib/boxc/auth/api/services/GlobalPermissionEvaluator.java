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