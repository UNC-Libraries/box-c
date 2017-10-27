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
package edu.unc.lib.dl.acl.util;

import static edu.unc.lib.dl.acl.util.AccessPrincipalConstants.USER_NAMESPACE;

/**
 * Stores the authentication principals for an agent.
 *
 * @author bbpennel
 *
 */
public class AgentPrincipals {

    private String username;
    private AccessGroupSet groups;
    private AccessGroupSet principals;

    /**
     * Constructs an AgentPrincipals object
     *
     * @param username
     * @param groups
     */
    public AgentPrincipals(String username, AccessGroupSet groups) {
        this.username = username;
        this.groups = groups;
        this.principals = new AccessGroupSet(groups);
        if (username != null) {
            this.principals.add(getUsernameUri());
        }
    }

    /**
     * Construct an AgentPrincipals object from credentials stored in the
     * current thread.
     *
     * @return new AgentPrincipals object
     */
    public static AgentPrincipals createFromThread() {
        return new AgentPrincipals(GroupsThreadStore.getUsername(),
                GroupsThreadStore.getGroups());
    }

    /**
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * @return the namespaced username
     */
    public String getUsernameUri() {
        return USER_NAMESPACE + username;
    }

    /**
     * @return the groups
     */
    public AccessGroupSet getGroups() {
        return groups;
    }

    /**
     * @return set of all principals for this agent
     */
    public AccessGroupSet getPrincipals() {
        return principals;
    }
}
