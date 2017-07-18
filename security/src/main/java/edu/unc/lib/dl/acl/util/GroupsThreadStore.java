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
        }
    }

    public static AccessGroupSet getGroups() {
        return GroupsThreadStore.groups.get();
    }

    public static String getGroupString() {
        return GroupsThreadStore.groupString.get();
    }

    /**
     * Clears the CDR groups associated with the current thread.
     */
    public static void clearGroups() {
        GroupsThreadStore.groups.remove();
        GroupsThreadStore.groupString.remove();
    }

    public static void storeUsername(String username) {
        GroupsThreadStore.username.set(username);
    }

    public static String getUsername() {
        return GroupsThreadStore.username.get();
    }

    public static void storeEmail(String email) {
        GroupsThreadStore.email.set(email);
    }

    public static String getEmail() {
        return GroupsThreadStore.email.get();
    }

    /**
     * Clears the CDR groups associated with the current thread.
     */
    public static void clearUsername() {
        GroupsThreadStore.username.remove();
    }

    public static void clearStore() {
        GroupsThreadStore.groups.remove();
        GroupsThreadStore.username.remove();
        GroupsThreadStore.groupString.remove();
        GroupsThreadStore.email.remove();
    }
}
