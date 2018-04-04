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

/**
 * This class works in conjunction with <code>GroupsToThreadStoreInterceptor</code> and
 * Fedora SOAP clients to forward the current user's group memberships to Fedora. This class
 * is responsible for storing and retrieving group information on a per thread basis.
 * @author count0
 *
 */
public abstract class GroupsThreadStore {
    private static ThreadLocal<AccessGroupSet> groups = new ThreadLocal<>(); // initial value is null
    private static ThreadLocal<String> username = new ThreadLocal<>();
    private static ThreadLocal<String> groupString = new ThreadLocal<>();
    private static ThreadLocal<String> email = new ThreadLocal<>();
    private static ThreadLocal<AgentPrincipals> agentPrincipals = new ThreadLocal<>();

    /**
     * Adds groups for forwarding with subsequent invocation of fedora clients
     * by the current thread. These groups will remain associated with the
     * thread until <code>clearGroups</code> is called by the same thread.
     * Please use set/clear within a try/finally or take similar measures to
     * make sure that groups are cleared.
     *
     * @param groups
     */
    public static void storeGroups(AccessGroupSet groups) {
        GroupsThreadStore.groups.set(groups);
        if (groups != null) {
            GroupsThreadStore.groupString.set(groups.joinAccessGroups(";"));
            GroupsThreadStore.agentPrincipals.remove();
        }
    }

    /**
     * Get group principals for the agent on the current thread.
     *
     * @return groups
     */
    public static AccessGroupSet getGroups() {
        return GroupsThreadStore.groups.get();
    }

    /**
     * Get a string representation of the group principals for the agent on the current thread
     *
     * @return string representation of groups
     */
    public static String getGroupString() {
        return GroupsThreadStore.groupString.get();
    }

    /**
     * Clears the CDR groups associated with the current thread.
     */
    public static void clearGroups() {
        GroupsThreadStore.groups.remove();
        GroupsThreadStore.groupString.remove();
        GroupsThreadStore.agentPrincipals.remove();
    }

    /**
     * Store a username for the agent on the current thread.
     *
     * @param username
     */
    public static void storeUsername(String username) {
        GroupsThreadStore.username.set(username);
        GroupsThreadStore.agentPrincipals.remove();
    }

    /**
     * Get the username of the agent on the current thread.
     *
     * @return
     */
    public static String getUsername() {
        return GroupsThreadStore.username.get();
    }

    /**
     * Store the email address of the agent on the current thread.
     *
     * @param email
     */
    public static void storeEmail(String email) {
        GroupsThreadStore.email.set(email);
    }

    /**
     * Get the email address of the agent on the current thread.
     *
     * @return
     */
    public static String getEmail() {
        return GroupsThreadStore.email.get();
    }

    /**
     * Get all principals for the agent on the current thread.
     *
     * @return
     */
    public static AgentPrincipals getAgentPrincipals() {
        AgentPrincipals principals = GroupsThreadStore.agentPrincipals.get();
        if (principals == null) {
            principals = new AgentPrincipals(username.get(), groups.get());
        }
        return principals;
    }

    /**
     * Clears the CDR groups associated with the current thread.
     */
    public static void clearUsername() {
        GroupsThreadStore.username.remove();
    }

    /**
     * Clear all fields stored for the agent on the current thread.
     */
    public static void clearStore() {
        GroupsThreadStore.groups.remove();
        GroupsThreadStore.username.remove();
        GroupsThreadStore.groupString.remove();
        GroupsThreadStore.email.remove();
        GroupsThreadStore.agentPrincipals.remove();
    }
}
