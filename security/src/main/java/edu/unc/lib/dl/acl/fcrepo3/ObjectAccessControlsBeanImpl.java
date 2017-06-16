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
package edu.unc.lib.dl.acl.fcrepo3;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.ObjectAccessControlsBean;
import edu.unc.lib.dl.acl.util.Permission;
import edu.unc.lib.dl.acl.util.UserRole;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.util.DateTimeUtil;
import edu.unc.lib.dl.xml.JDOMNamespaceUtil;

/**
 * Encapsulates the complete set of access controls that apply to a particular object.
 *
 * @author count0
 *
 */
public class ObjectAccessControlsBeanImpl implements ObjectAccessControlsBean {
    private static final Logger LOG = LoggerFactory.getLogger(ObjectAccessControlsBeanImpl.class);

    private static final String ACTIVE_STATE = ContentModelHelper.FedoraProperty.Active.toString();

    PID object = null;
    // Inherited or directly applied groups
    Map<UserRole, Set<String>> baseRoleGroups = null;
    // Global roles
    Map<UserRole, Set<String>> globalRoleGroups = null;
    // Roles after merging base, global and embargoes
    Map<UserRole, Set<String>> activeRoleGroups = null;
    List<Date> activeEmbargoes = null;
    boolean ancestorsPublished = true;
    Boolean isPublished = true;
    Boolean isActive = true;
    boolean ancestorsActive = true;

    public ObjectAccessControlsBeanImpl(PID pid, Map<UserRole, Set<String>> baseRoleGroups) {
        this.object = pid;
        this.baseRoleGroups = baseRoleGroups;
        this.activeRoleGroups = this.getMergedRoleGroups();
    }

    /**
     * Constructs a new ObjectAccessControlsBean object from a collection of
     * pipe delimited role uri/group pairings, representing all role/group
     * relationships assigned to this object
     *
     * @param pid
     * @param roleGroups
     */
    public ObjectAccessControlsBeanImpl(PID pid, Collection<String> roleGroups) {
        this.object = pid;
        this.baseRoleGroups = new HashMap<UserRole, Set<String>>();
        for (String roleGroup : roleGroups) {
            LOG.debug("roleGroup: " + roleGroup);
            String[] roleGroupArray = roleGroup.split("\\|");
            if (roleGroupArray.length == 2) {
                String role = roleGroupArray[0];
                if (role.indexOf('#') == -1) {
                    role = JDOMNamespaceUtil.CDR_ROLE_NS.getURI() + role;
                }

                UserRole userRole = UserRole.getUserRole(role);
                if (userRole == null) {
                    continue;
                }
                Set<String> groupSet = baseRoleGroups.get(userRole);
                if (groupSet == null) {
                    groupSet = new HashSet<String>();
                    baseRoleGroups.put(userRole, groupSet);
                }
                groupSet.add(roleGroupArray[1]);
            }
        }

        this.activeRoleGroups = this.getMergedRoleGroups();
    }

    /**
     * Constructs a new ObjectAccessControlsBean object from a map of role/group relations and active embargoes
     *
     * @param pid
     * @param roles
     * @param embargoes
     */
    public ObjectAccessControlsBeanImpl(PID pid, Map<String, ? extends Collection<String>> roles,
            Map<String, ? extends Collection<String>> globalRoles, Collection<String> embargoes,
            Collection<String> publicationStatus, Collection<String> objectState) {
        this.object = pid;
        this.baseRoleGroups = new HashMap<UserRole, Set<String>>();
        this.globalRoleGroups = new HashMap<UserRole, Set<String>>();
        if (objectState != null) {
            this.isActive = !objectState.contains("Deleted");
            this.ancestorsActive = !objectState.contains("Deleted Ancestor");
        }
        if (publicationStatus != null) {
            this.isPublished = !publicationStatus.contains("Unpublished");
            this.ancestorsPublished = !publicationStatus.contains("Unpublished Ancestor");
        }
        copyRoles(roles, this.baseRoleGroups);
        copyRoles(globalRoles, this.globalRoleGroups);
        extractEmbargoes(embargoes);
        this.activeRoleGroups = this.getMergedRoleGroups();
    }

    /**
     * Construct a new access control bean by applying triples from an item on
     * top of an existing access control bean as if it were the parent for the
     * new object
     *
     * @param baseAcls
     *            parent objects access control information
     * @param pid
     *            pid of the new object
     * @param triples
     *            map of triples containing the access control of the new object
     */
    public ObjectAccessControlsBeanImpl(ObjectAccessControlsBean baseAclsG, PID pid,
            Map<String, List<String>> triples) {
        ObjectAccessControlsBeanImpl baseAcls = (ObjectAccessControlsBeanImpl) baseAclsG;

        this.object = pid;

        List<String> inherit = triples.get(ContentModelHelper.CDRProperty.inheritPermissions.toString());
        boolean inheritPermissions = inherit == null || inherit.size() == 0 || !"false".equals(inherit.get(0));

        if (inheritPermissions) {
            this.baseRoleGroups = new HashMap<UserRole, Set<String>>(baseAcls.baseRoleGroups);
            if (baseAcls.activeEmbargoes != null) {
                this.activeEmbargoes = new ArrayList<Date>(baseAcls.activeEmbargoes);
            }
            // Remove non-inheritable roles (list)
            if (this.baseRoleGroups.containsKey(UserRole.list)) {
                this.baseRoleGroups.remove(UserRole.list);
            }
        } else {
            this.baseRoleGroups = new HashMap<UserRole, Set<String>>();
        }
        if (baseAcls.globalRoleGroups != null) {
            this.globalRoleGroups = new HashMap<UserRole, Set<String>>(baseAcls.globalRoleGroups);
        }
        List<String> embargoes = triples.get(ContentModelHelper.CDRProperty.embargoUntil.toString());
        this.extractEmbargoes(embargoes);

        for (Entry<String, List<String>> tripleEntry : triples.entrySet()) {
            int index = tripleEntry.getKey().indexOf('#');
            if (index > 0) {
                String namespace = tripleEntry.getKey().substring(0, index + 1);
                if (JDOMNamespaceUtil.CDR_ROLE_NS.getURI().equals(namespace)) {
                    UserRole userRole = UserRole.getUserRole(tripleEntry.getKey());
                    if (inheritPermissions) {
                        Set<String> groups = this.baseRoleGroups.get(userRole);
                        if (groups == null) {
                            groups = new HashSet<String>(tripleEntry.getValue());
                            this.baseRoleGroups.put(userRole, groups);
                        } else {
                            groups.addAll(tripleEntry.getValue());
                        }
                    } else {
                        Set<String> groups = new HashSet<String>(tripleEntry.getValue());
                        this.baseRoleGroups.put(userRole, groups);
                    }
                }
            }
        }

        this.ancestorsPublished = baseAcls.isPublished();
        List<String> publicationTriples = triples.get(ContentModelHelper.CDRProperty.isPublished.toString());
        this.isPublished = publicationTriples == null || "yes".equals(publicationTriples.get(0));

        this.ancestorsActive = baseAcls.isActive();
        List<String> objectState = triples.get(ContentModelHelper.FedoraProperty.state.toString());
        this.isActive = objectState == null || ACTIVE_STATE.equals(objectState.get(0));

        this.activeRoleGroups = this.getMergedRoleGroups();
    }

    @SuppressWarnings("unchecked")
    private static void copyRoles(Map<String, ? extends Collection<String>> roles, Map<UserRole,
            Set<String>> destination) {
        if (roles == null) {
            return;
        }
        Iterator<?> roleIt = roles.entrySet().iterator();
        while (roleIt.hasNext()) {
            Map.Entry<String, Collection<String>> entry = (Map.Entry<String, Collection<String>>) roleIt.next();
            UserRole userRole = UserRole.getUserRole(entry.getKey());
            if (userRole != null) {
                Set<String> groups = new HashSet<String>(entry.getValue());
                destination.put(userRole, groups);
            }
        }
    }

    private void extractEmbargoes(Collection<String> embargoes) {
        if (embargoes != null) {
            this.activeEmbargoes = new ArrayList<Date>(embargoes.size());
            for (String embargo : embargoes) {
                try {
                    this.activeEmbargoes.add(DateTimeUtil.parseUTCToDate(embargo));
                } catch (ParseException e) {
                    LOG.warn("Failed to parse embargo " + embargo, e);
                } catch (IllegalArgumentException e) {
                    LOG.warn("Failed to parse embargo " + embargo, e);
                }
            }
        }
    }

    public Map<UserRole, Set<String>> getActiveRoleGroups() {
        return this.activeRoleGroups;
    }

    /**
     * Generates a new role/group mapping by filtering out role mappings that do
     * not have administrative viewing rights. This is based on if there are any
     * active embargoes, the object is unpublished or not active.
     *
     * @return
     */
    private Map<UserRole, Set<String>> getMergedRoleGroups() {
        boolean removePatrons = !this.isPublished() || !this.isActive();

        boolean metadataPatrons = false;
        if (!removePatrons) {
            // Check to see if there are active embargoes, and if there are that their window has not passed
            Date lastActiveEmbargo = getLastActiveEmbargoUntilDate();
            metadataPatrons = lastActiveEmbargo != null;
        }

        // Patrons were blocked, remove groups granted to non-admin user rules
        if (removePatrons) {
            activeRoleGroups = new HashMap<UserRole, Set<String>>();
            if (this.baseRoleGroups != null) {
                for (Map.Entry<UserRole, Set<String>> roleGroups : this.baseRoleGroups.entrySet()) {
                    if (roleGroups.getKey().getPermissions().contains(Permission.viewAdminUI)) {
                        activeRoleGroups.put(roleGroups.getKey(), roleGroups.getValue());
                    }
                }
            }
        } else if (metadataPatrons) {
            activeRoleGroups = new HashMap<UserRole, Set<String>>();

            if (this.baseRoleGroups != null) {
                Set<String> metadataGroups = baseRoleGroups.get(UserRole.metadataPatron);
                if (metadataGroups == null) {
                    metadataGroups = new HashSet<String>();
                }

                for (Map.Entry<UserRole, Set<String>> roleGroups : this.baseRoleGroups.entrySet()) {
                    if (roleGroups.getKey().getPermissions().contains(Permission.viewAdminUI)) {
                        activeRoleGroups.put(roleGroups.getKey(), roleGroups.getValue());
                    } else {
                        metadataGroups.addAll(roleGroups.getValue());
                    }
                }

                if (metadataGroups.size() > 0) {
                    activeRoleGroups.put(UserRole.metadataPatron, metadataGroups);
                }
            }
        } else {
            // Patrons not blocked, return all the grants plus the global roles.
            activeRoleGroups = new HashMap<UserRole, Set<String>>(this.baseRoleGroups);
        }
        if (this.globalRoleGroups != null) {
            for (Map.Entry<UserRole, Set<String>> roleGroups : this.globalRoleGroups.entrySet()) {
                if (roleGroups.getKey().getPermissions().contains(Permission.viewAdminUI)) {
                    Set<String> roleGroup = activeRoleGroups.get(roleGroups.getKey());
                    if (roleGroup == null) {
                        activeRoleGroups.put(roleGroups.getKey(), roleGroups.getValue());
                    } else {
                        roleGroup.addAll(roleGroups.getValue());
                    }
                }
            }
        }
        return activeRoleGroups;
    }

    /**
     * Find the last active embargo date, if applicable
     *
     * @return the embargo date or null
     */
    public Date getLastActiveEmbargoUntilDate() {
        Date result = null;
        if (this.activeEmbargoes != null) {
            Date dateNow = new Date();
            for (Date embargoDate : this.activeEmbargoes) {
                if (embargoDate.after(dateNow)) {
                    if (result == null || embargoDate.after(result)) {
                        result = embargoDate;
                    }
                }
            }
        }
        return result;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("Object Access Controls (").append(object.getPid()).append(")")
                .append("\nRoles granted to groups:\n");
        for (UserRole r : baseRoleGroups.keySet()) {
            result.append(r.getPredicate()).append("\n");
            for (String g : baseRoleGroups.get(r)) {
                result.append(g).append("\t");
            }
        }
        result.append("\nActive embargo dates:");
        if (activeEmbargoes != null) {
            for (Date d : activeEmbargoes) {
                try {
                    result.append(DateTimeUtil.formatDateToUTC(d));
                } catch (ParseException e) {
                    LOG.error("Failed to parse date " + d, e);
                }
            }
        }
        return result.toString();
    }

    public PID getObject() {
        return object;
    }

    /**
     * Builds a set of all the active user roles granted to the given groups.
     *
     * @param groups
     * @return
     */
    public Set<UserRole> getRoles(String[] groups) {
        return this.getRoles(groups, this.activeRoleGroups);
    }

    public Set<UserRole> getBaseRoles(String[] groups) {
        return this.getRoles(groups, this.baseRoleGroups);
    }

    /**
     * Builds a set of all the user roles granted to the given groups.
     *
     * @param groups
     * @param roleGroups
     * @return
     */
    private Set<UserRole> getRoles(String[] groups, Map<UserRole, Set<String>> roleGroups) {
        Set<UserRole> result = new HashSet<UserRole>();
        for (String group : groups) { // get all user roles
            for (UserRole r : roleGroups.keySet()) {
                if (roleGroups.get(r).contains(group)) {
                    result.add(r);
                }
            }
        }
        return result;
    }

    public Set<UserRole> getRoles(AccessGroupSet groups) {
        return this.getRoles(groups, this.activeRoleGroups);
    }

    public Set<UserRole> getBaseRoles(AccessGroupSet groups) {
        return this.getRoles(groups, this.baseRoleGroups);
    }

    private Set<UserRole> getRoles(AccessGroupSet groups, Map<UserRole, Set<String>> roleGroups) {
        Set<UserRole> result = new HashSet<UserRole>();
        for (String group : groups) { // get all user roles
            for (UserRole r : roleGroups.keySet()) {
                if (roleGroups.get(r).contains(group)) {
                    result.add(r);
                }
            }
        }
        return result;
    }

    /**
     * Determines if this access object contains roles matching any of the
     * groups in the supplied access group set
     *
     * @param groups
     *            group membershps
     * @return true if any of the groups are associated with a role for this
     *         object
     */
    public boolean containsAny(AccessGroupSet groups) {
        Map<UserRole, Set<String>> roleGroups = this.activeRoleGroups;
        for (String group : groups) {
            for (UserRole r : roleGroups.keySet()) {
                if (roleGroups.get(r).contains(group)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Determines if a user has a specific type of permission on this object, given a set of groups.
     *
     * @param groups
     *           user memberships
     * @param permission
     *           the permission requested
     * @return if permitted
     */
    public boolean hasPermission(AccessGroupSet groups, Permission permission) {
        Set<UserRole> roles = this.getRoles(groups);
        return hasPermission(groups, permission, roles);
    }

    public static boolean hasPermission(AccessGroupSet groups, Permission permission, Set<UserRole> roles) {
        for (UserRole r : roles) {
            if (r.getPermissions().contains(permission)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determines if a user has a specific type of permission on this object, given a set of groups.
     *
     * @param groups
     *           user memberships
     * @param permission
     *           the permission requested
     * @return if permitted
     */
    public boolean hasPermission(String[] groups, Permission permission) {
        Set<UserRole> roles = this.getRoles(groups);
        for (UserRole r : roles) {
            if (r.getPermissions().contains(permission)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Retrieves the set of all permissions granted to a set of access groups
     *
     * @param groups
     * @return
     */
    public Set<String> getPermissionsByGroups(AccessGroupSet groups) {
        Set<String> permissions = new HashSet<String>();
        Set<UserRole> roles = this.getRoles(groups);
        for (UserRole r : roles) {
            for (Permission permission : r.getPermissions()) {
                if (!permissions.contains(permission.name())) {
                    permissions.add(permission.name());
                }
            }
        }
        return permissions;
    }

    /**
     * Returns all groups assigned to this object that possess the given permission
     *
     * @param permission
     * @return
     */
    public Set<String> getGroupsByPermission(Permission permission) {
        Set<String> groups = new HashSet<String>();
        for (Map.Entry<UserRole, Set<String>> r2g : this.activeRoleGroups.entrySet()) {
            if (r2g.getKey().getPermissions().contains(permission)) {
                groups.addAll(r2g.getValue());
            }
        }
        return groups;
    }

    /**
     * Returns all groups assigned to the given role
     *
     * @param userRole
     * @return
     */
    public Set<String> getGroupsByUserRole(UserRole userRole) {
        return this.activeRoleGroups.get(userRole);
    }

    /**
     * Returns a list where each entry contains a single role uri + group
     * pairing assigned to this object. Values are pipe delimited
     *
     * @return
     */
    public List<String> roleGroupsToList() {
        List<String> result = new ArrayList<String>();
        for (Map.Entry<UserRole, Set<String>> r2g : this.activeRoleGroups.entrySet()) {
            String roleName = r2g.getKey().getURI().toString();
            for (String group : r2g.getValue()) {
                result.add(roleName + "|" + group);
            }
        }
        return result;
    }

    /**
     * Returns a list where each entry contains a single role name + group
     * pairing assigned to this object. Values are pipe delimited
     * 
     * @return
     */
    public List<String> roleGroupsToUnprefixedList() {
        List<String> result = new ArrayList<String>();
        for (Map.Entry<UserRole, Set<String>> r2g : this.activeRoleGroups.entrySet()) {
            String roleName = r2g.getKey().getPredicate().toString();
            for (String group : r2g.getValue()) {
                result.add(roleName + "|" + group);
            }
        }
        return result;
    }

    public boolean isPublished() {
        return this.ancestorsPublished && (isPublished == null || isPublished);
    }

    public Boolean getIsPublished() {
        return isPublished;
    }

    public void setIsPublished(Boolean isPublished) {
        this.isPublished = isPublished;
    }

    public boolean isAncestorsPublished() {
        return ancestorsPublished;
    }

    public void setAncestorsPublished(boolean ancestorsPublished) {
        this.ancestorsPublished = ancestorsPublished;
    }

    public boolean isActive() {
        return this.ancestorsActive && (isActive == null || isActive);
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public boolean isAncestorsActive() {
        return ancestorsActive;
    }

    public void setAncestorsActive(boolean ancestorsActive) {
        this.ancestorsActive = ancestorsActive;
    }
}
