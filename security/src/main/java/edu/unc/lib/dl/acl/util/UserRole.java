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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import edu.unc.lib.dl.xml.JDOMNamespaceUtil;

/**
 * These are the properties that the repository manages in the rels-ext datastream.
 *
 * @author count0
 *
 */
public enum UserRole {
	list("list", new Permission[] {}),
	accessCopiesPatron("access-copies-patron", new Permission[] {Permission.viewDescription,
			Permission.viewDerivative}),
	metadataPatron("metadata-patron", new Permission[] {Permission.viewDescription}),
	patron("patron", new Permission[] {Permission.viewDescription, Permission.viewDerivative, Permission.viewOriginal}),
	observer("observer", new Permission[] {Permission.viewAdminUI, Permission.viewEmbargoed, Permission.viewDescription,
			Permission.viewDerivative, Permission.viewOriginal}),
	ingester("ingester", new Permission[] {Permission.viewAdminUI, Permission.viewEmbargoed,
			Permission.addRemoveContents, Permission.editDescription, Permission.viewDescription,
			Permission.viewDerivative, Permission.viewOriginal}),
	processor("processor", new Permission[] {Permission.viewAdminUI, Permission.viewEmbargoed,
			Permission.addRemoveContents, Permission.publish, Permission.editDescription, Permission.moveToTrash,
			Permission.viewDescription, Permission.viewDerivative, Permission.viewOriginal}),
	curator("curator", new Permission[] {Permission.viewAdminUI, Permission.viewEmbargoed, Permission.addRemoveContents,
			Permission.publish, Permission.editDescription, Permission.moveToTrash, Permission.editAccessControl,
			Permission.viewDescription, Permission.viewDerivative, Permission.viewOriginal,
			Permission.editResourceType}),
	administrator("administrator", new Permission[] {Permission.viewAdminUI, Permission.viewEmbargoed,
			Permission.addRemoveContents, Permission.publish, Permission.editDescription, Permission.moveToTrash,
			Permission.editAccessControl, Permission.purgeForever, Permission.viewDescription, Permission.viewDerivative,
			Permission.viewOriginal, Permission.editResourceType});
	private URI uri;
	private String predicate;
	private Set<Permission> permissions;

	UserRole(String predicate, Permission[] perms) {
		try {
			this.predicate = predicate;
			this.uri = new URI(JDOMNamespaceUtil.CDR_ROLE_NS.getURI() + predicate);
			HashSet<Permission> mypermissions = new HashSet<Permission>(perms.length);
			Collections.addAll(mypermissions, perms);
			this.permissions = Collections.unmodifiableSet(mypermissions);
		} catch (URISyntaxException e) {
			Error x = new ExceptionInInitializerError("Cannot initialize ContentModelHelper");
			x.initCause(e);
			throw x;
		}
	}

	public static boolean matchesMemberURI(String test) {
		for(UserRole r : UserRole.values()) {
			if(r.getURI().toString().equals(test)) {
				return true;
			}
		}
		return false;
	}

	public static UserRole getUserRole(String roleUri) {
		for(UserRole r : UserRole.values()) {
			if(r.getURI().toString().equals(roleUri)) {
				return r;
			}
		}
		return null;
	}

	/**
	 * Return a list of all user roles which have the specified permission
	 *
	 * @param permission
	 * @return
	 */
	public static Set<UserRole> getUserRoles(Collection<Permission> inPermissions) {

		Set<UserRole> roles = new HashSet<UserRole>();
		for (UserRole r : UserRole.values()) {
			if (r.permissions.containsAll(inPermissions)) {
				roles.add(r);
			}
		}
		return roles;
	}

	public URI getURI() {
		return this.uri;
	}

	public Set<Permission> getPermissions() {
		return permissions;
	}

	public String getPredicate() {
		return predicate;
	}

	public boolean equals(String value){
		return this.uri.toString().equals(value);
	}

	@Override
	public String toString() {
		return this.uri.toString();
	}
}