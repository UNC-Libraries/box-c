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
package edu.unc.lib.dl.fedora;

/**
 * This class works in conjunction with <code>GroupsToThreadStoreInterceptor</code> and
 * Fedora SOAP clients to forward the current user's group memberships to Fedora. This class
 * is responsible for storing and retrieving group information on a per thread basis.
 * @author count0
 *
 */
public class GroupsThreadStore {
	private static ThreadLocal<String> groups = new ThreadLocal<String>(); // initial value is null
	
	/**
	 * Adds groups for forwarding with subsequent invocation of fedora clients by the current thread.
	 * These groups will remain associated with the thread until <code>clearGroups</code> is called by the same thread. Please
	 * use set/clear within a try/finally or take similar measures to make sure that groups are cleared.
	 * 
	 * @param groups
	 */
	public static void storeGroups(String groups) {
		GroupsThreadStore.groups.set(groups);
	}
	
	public static String getGroups() {
		return GroupsThreadStore.groups.get();
	}
	
	/**
	 * Clears the CDR groups associated with the current thread.
	 */
	public static void clearGroups() {
		GroupsThreadStore.groups.remove();
	}
}
