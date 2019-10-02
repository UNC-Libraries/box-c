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

import static edu.unc.lib.dl.acl.util.Permission.assignStaffRoles;
import static edu.unc.lib.dl.acl.util.Permission.bulkUpdateDescription;
import static edu.unc.lib.dl.acl.util.Permission.changePatronAccess;
import static edu.unc.lib.dl.acl.util.Permission.createAdminUnit;
import static edu.unc.lib.dl.acl.util.Permission.createCollection;
import static edu.unc.lib.dl.acl.util.Permission.destroy;
import static edu.unc.lib.dl.acl.util.Permission.destroyUnit;
import static edu.unc.lib.dl.acl.util.Permission.editDescription;
import static edu.unc.lib.dl.acl.util.Permission.editResourceType;
import static edu.unc.lib.dl.acl.util.Permission.ingest;
import static edu.unc.lib.dl.acl.util.Permission.markForDeletion;
import static edu.unc.lib.dl.acl.util.Permission.markForDeletionUnit;
import static edu.unc.lib.dl.acl.util.Permission.move;
import static edu.unc.lib.dl.acl.util.Permission.reindex;
import static edu.unc.lib.dl.acl.util.Permission.viewAccessCopies;
import static edu.unc.lib.dl.acl.util.Permission.viewHidden;
import static edu.unc.lib.dl.acl.util.Permission.viewMetadata;
import static edu.unc.lib.dl.acl.util.Permission.viewOriginal;
import static java.util.stream.Collectors.toSet;
import static org.apache.jena.rdf.model.ResourceFactory.createProperty;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.jena.rdf.model.Property;

import edu.unc.lib.dl.rdf.CdrAcl;

/**
 * Enumeration of the roles which can be assigned to user principals. Defines
 * the properties of those roles as well as the permissions granted.
 *
 * @author bbpennel
 *
 */
public enum UserRole {
    list("list", new Permission[] {}),
    metadataPatron("metadata-patron", new Permission[] {
            Permission.viewMetadata}),
    accessCopiesPatron("access-copies-patron", new Permission[] {
            Permission.viewMetadata, Permission.viewAccessCopies}),
    patron("patron", new Permission[] {
            Permission.viewMetadata, Permission.viewAccessCopies, Permission.viewOriginal}),
    canView("canView", new Permission[] {
            Permission.viewHidden, Permission.viewMetadata, Permission.viewAccessCopies, Permission.viewOriginal}),
    // Patron roles
    none("none", false),
    canDiscover("canDiscover", false),
    canViewMetadata("canViewMetadata", false, viewMetadata),
    canViewAccessCopies("canViewAccessCopies", false, viewMetadata, viewAccessCopies),
    canViewOriginals("canViewOriginals", false, viewMetadata, viewAccessCopies, viewOriginal),
    // Staff roles
    canAccess("canAccess", true, viewHidden, viewMetadata, viewAccessCopies, viewOriginal),
    canIngest("canIngest", true, viewHidden, viewMetadata, viewAccessCopies, viewOriginal,
            ingest),
    canDescribe("canDescribe", true, viewHidden, viewMetadata, viewAccessCopies, viewOriginal,
            editDescription, bulkUpdateDescription),
    canManage("canManage", true, viewHidden, viewMetadata, viewAccessCopies, viewOriginal,
            ingest, editDescription, bulkUpdateDescription, move, markForDeletion,
            changePatronAccess, editResourceType),
    unitOwner("unitOwner", true, viewHidden, viewMetadata, viewAccessCopies, viewOriginal,
            ingest, editDescription, bulkUpdateDescription, move, markForDeletion, markForDeletionUnit,
            changePatronAccess, editResourceType, destroy, createCollection, assignStaffRoles),
    administrator("administrator", true, viewHidden, viewMetadata, viewAccessCopies, viewOriginal,
            ingest, editDescription, bulkUpdateDescription, move, markForDeletion, markForDeletionUnit,
            changePatronAccess, editResourceType, destroy, destroyUnit, createCollection,
            createAdminUnit, assignStaffRoles, reindex);

    private URI uri;
    private String predicate;
    private String propertyString;
    private Property property;
    private Set<Permission> permissions;
    private Set<String> permissionNames;
    private Boolean isStaffRole;

    private static List<UserRole> staffRoles;
    private static List<UserRole> patronRoles;

    private static Map<Permission, Set<UserRole>> permissionToRoles;

    UserRole(String predicate, boolean isStaffRole, Permission... perms) {
        this.predicate = predicate;
        this.propertyString = CdrAcl.getURI() + predicate;
        this.property = createProperty(propertyString);
        this.uri = URI.create(propertyString);
        this.isStaffRole = isStaffRole;
        this.permissions = new HashSet<>(Arrays.asList(perms));
        this.permissionNames = permissions.stream().map(p -> p.name()).collect(toSet());
    }

    @Deprecated
    UserRole(String predicate, Permission[] perms) {
        try {
            this.predicate = predicate;
            this.uri = new URI(CdrAcl.getURI() + predicate);
            this.propertyString = "";
            HashSet<Permission> mypermissions = new HashSet<>(perms.length);
            Collections.addAll(mypermissions, perms);
            this.permissions = Collections.unmodifiableSet(mypermissions);
        } catch (URISyntaxException e) {
            Error x = new ExceptionInInitializerError("Cannot initialize ContentModelHelper");
            x.initCause(e);
            throw x;
        }
    }

    @Deprecated
    public static boolean matchesMemberURI(String test) {
        for (UserRole r : UserRole.values()) {
            if (r.getURI().toString().equals(test)) {
                return true;
            }
        }
        return false;
    }

    @Deprecated
    public static UserRole getUserRole(String roleUri) {
        for (UserRole r : UserRole.values()) {
            if (r.propertyString.equals(roleUri)) {
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

        Set<UserRole> roles = new HashSet<>();
        for (UserRole r : UserRole.values()) {
            if (r.permissions.containsAll(inPermissions)) {
                roles.add(r);
            }
        }
        return roles;
    }

    public static Set<UserRole> getUserRolesWithPermission(Permission permission) {
        if (permissionToRoles == null) {
            permissionToRoles = new EnumMap<>(Permission.class);
            for (Permission perm: Permission.values()) {
                Set<UserRole> roles = Arrays.stream(UserRole.values())
                        .filter(u -> u.getPermissions().contains(perm))
                        .collect(Collectors.toSet());
                permissionToRoles.put(perm, roles);
            }
        }

        return permissionToRoles.get(permission);
    }

    /**
     * Return a set of staff UserRoles
     *
     * @return
     */
    public static List<UserRole> getStaffRoles() {
        if (staffRoles == null) {
            staffRoles = Arrays.stream(UserRole.values())
                    .filter(p -> p.isStaffRole != null && p.isStaffRole)
                    .sorted()
                    .collect(Collectors.toList());
        }
        return staffRoles;
    }

    /**
     * Return a set of patron UserRoles
     *
     * @return
     */
    public static List<UserRole> getPatronRoles() {
        if (patronRoles == null) {
            patronRoles = Arrays.stream(UserRole.values())
                    .filter(p -> p.isStaffRole != null && !p.isStaffRole)
                    .sorted()
                    .collect(Collectors.toList());
        }
        return patronRoles;
    }

    /**
     * Returns the UserRole which has a property string matching the provided value.
     *
     * @param property
     * @return
     */
    public static UserRole getRoleByProperty(String property) {
        for (UserRole role : values()) {
            if (role.getPropertyString().equals(property)) {
                return role;
            }
        }

        return null;
    }

    /**
     * Return the URI of the property for this role as a string.
     *
     * @return
     */
    public String getPropertyString() {
        return this.propertyString;
    }

    /**
     * @return the property
     */
    public Property getProperty() {
        return property;
    }

    public URI getURI() {
        return this.uri;
    }

    public Set<Permission> getPermissions() {
        return permissions;
    }

    public Set<String> getPermissionNames() {
        return permissionNames;
    }

    public String getPredicate() {
        return predicate;
    }

    public boolean isStaffRole() {
        return this.isStaffRole;
    }

    public boolean isPatronRole() {
        return !this.isStaffRole;
    }

    public boolean equals(String value) {
        return propertyString.equals(value);
    }

    @Override
    public String toString() {
        return this.propertyString;
    }
}