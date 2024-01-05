package edu.unc.lib.boxc.auth.api;

import static edu.unc.lib.boxc.auth.api.Permission.assignStaffRoles;
import static edu.unc.lib.boxc.auth.api.Permission.bulkUpdateDescription;
import static edu.unc.lib.boxc.auth.api.Permission.changePatronAccess;
import static edu.unc.lib.boxc.auth.api.Permission.createAdminUnit;
import static edu.unc.lib.boxc.auth.api.Permission.createCollection;
import static edu.unc.lib.boxc.auth.api.Permission.destroy;
import static edu.unc.lib.boxc.auth.api.Permission.destroyUnit;
import static edu.unc.lib.boxc.auth.api.Permission.editDescription;
import static edu.unc.lib.boxc.auth.api.Permission.editResourceType;
import static edu.unc.lib.boxc.auth.api.Permission.ingest;
import static edu.unc.lib.boxc.auth.api.Permission.markForDeletion;
import static edu.unc.lib.boxc.auth.api.Permission.markForDeletionUnit;
import static edu.unc.lib.boxc.auth.api.Permission.move;
import static edu.unc.lib.boxc.auth.api.Permission.orderMembers;
import static edu.unc.lib.boxc.auth.api.Permission.reindex;
import static edu.unc.lib.boxc.auth.api.Permission.runEnhancements;
import static edu.unc.lib.boxc.auth.api.Permission.viewAccessCopies;
import static edu.unc.lib.boxc.auth.api.Permission.viewHidden;
import static edu.unc.lib.boxc.auth.api.Permission.viewMetadata;
import static edu.unc.lib.boxc.auth.api.Permission.viewOriginal;
import static edu.unc.lib.boxc.auth.api.Permission.viewReducedResolutionImages;
import static java.util.Arrays.asList;
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

import edu.unc.lib.boxc.model.api.rdf.CdrAcl;

/**
 * Enumeration of the roles which can be assigned to user principals. Defines
 * the properties of those roles as well as the permissions granted.
 *
 * @author bbpennel
 *
 */
public enum UserRole {
    // Patron roles
    none("none", false, null),
    canDiscover("canDiscover", false, null),
    canViewMetadata("canViewMetadata", false, canDiscover, viewMetadata),
    canViewAccessCopies("canViewAccessCopies", false, canViewMetadata, viewAccessCopies),
    canViewReducedQuality("canViewReducedQuality", false, canViewAccessCopies,
            viewReducedResolutionImages),
    canViewOriginals("canViewOriginals", false, canViewReducedQuality, viewOriginal),
    // Staff roles
    canAccess("canAccess", true, canViewOriginals, viewHidden),
    canIngest("canIngest", true, canAccess, ingest),
    canDescribe("canDescribe", true, canAccess, editDescription, bulkUpdateDescription),
    canProcess("canProcess", true, canDescribe,
            move, orderMembers, markForDeletion, changePatronAccess),
    canManage("canManage", true, canProcess,
            ingest, editResourceType, createCollection),
    unitOwner("unitOwner", true, canManage,
            markForDeletionUnit, destroy, assignStaffRoles),
    // Admin role receives all permissions
    administrator("administrator", true, null, Permission.values());

    public static final List<String> PATRON_ROLE_PRECEDENCE = asList(
            UserRole.none.getPropertyString(),
            UserRole.canViewMetadata.getPropertyString(),
            UserRole.canViewAccessCopies.getPropertyString(),
            UserRole.canViewReducedQuality.getPropertyString(),
            UserRole.canViewOriginals.getPropertyString()
            );

    private String predicate;
    private String propertyString;
    private Property property;
    private Set<Permission> permissions;
    private Set<String> permissionNames;
    private Boolean isStaffRole;

    private static List<UserRole> staffRoles;
    private static List<UserRole> patronRoles;

    private static Map<Permission, Set<UserRole>> permissionToRoles;

    UserRole(String predicate, boolean isStaffRole, UserRole inheritPermsFrom, Permission... perms) {
        this.predicate = predicate;
        this.propertyString = CdrAcl.getURI() + predicate;
        this.property = createProperty(propertyString);
        this.isStaffRole = isStaffRole;
        this.permissions = new HashSet<>(Arrays.asList(perms));
        if (inheritPermsFrom != null) {
            this.permissions.addAll(inheritPermsFrom.getPermissions());
        }
        this.permissionNames = permissions.stream().map(p -> p.name()).collect(toSet());
    }

    /**
     * Return a list of all user roles which have the specified permission
     *
     * @param inPermissions
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