package edu.unc.lib.boxc.web.services.rest;

import static edu.unc.lib.boxc.auth.api.AccessPrincipalConstants.USER_NAMESPACE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import edu.unc.lib.boxc.auth.api.AccessPrincipalConstants;
import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.api.models.RoleAssignment;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.auth.fcrepo.services.InheritedAclFactory;
import edu.unc.lib.boxc.auth.fcrepo.services.ObjectAclFactory;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.AdminUnit;
import edu.unc.lib.boxc.model.api.objects.CollectionObject;
import edu.unc.lib.boxc.model.api.objects.ContentObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.impl.acl.PatronAccessDetails;
import edu.unc.lib.boxc.web.common.auth.PatronPrincipalProvider;

/**
 * API controller for retrieving access-control information about an object
 *
 * @author lfarrell
 * @author harring
 *
 */
@Controller
public class AccessControlRetrievalController {
    private static final Logger log = LoggerFactory.getLogger(AccessControlRetrievalController.class);
    public static final String INHERITED_ROLES = "inherited";
    public static final String ASSIGNED_ROLES = "assigned";
    public static final String ALLOWED_PATRON_PRINCIPALS = "allowedPrincipals";
    public static final String ROLES_KEY = "roles";
    public static final String EMBARGO_KEY = "embargo";
    public static final String DELETED_KEY = "deleted";

    @Autowired
    private AccessControlService aclService;
    @Autowired
    private ObjectAclFactory objectAclFactory;
    @Autowired
    private InheritedAclFactory inheritedAclFactory;
    @Autowired
    private RepositoryObjectLoader repositoryObjectLoader;
    @Autowired
    private PatronPrincipalProvider patronPrincipalProvider;

    @GetMapping(value = "/acl/staff/{id}", produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Object> getStaffRoles(@PathVariable("id") String id) {
        log.debug("Retrieving staff roles for {}", id);
        PID pid = PIDs.get(id);

        AgentPrincipals agent = AgentPrincipalsImpl.createFromThread();
        aclService.assertHasAccess("Insufficient permissions to retrieve staff roles for " + id,
                pid, agent.getPrincipals(), Permission.viewHidden);

        RepositoryObject repoObj = repositoryObjectLoader.getRepositoryObject(pid);

        Map<String, Object> result = new HashMap<>();
        List<RoleAssignment> inherited = null;
        List<RoleAssignment> assigned = null;

        if (repoObj instanceof AdminUnit) {
            assigned = objectAclFactory.getStaffRoleAssignments(pid);
            inherited = Collections.emptyList();
        } else if (repoObj instanceof CollectionObject) {
            assigned = objectAclFactory.getStaffRoleAssignments(pid);
            RepositoryObject parent = repoObj.getParent();
            inherited = inheritedAclFactory.getStaffRoleAssignments(parent.getPid());
        } else if (repoObj instanceof ContentObject) {
            assigned = Collections.emptyList();
            inherited = inheritedAclFactory.getStaffRoleAssignments(pid);
        } else {
            result.put("error", "Cannot retrieve staff roles for object " + id
                    + " of type " + repoObj.getClass().getName());
            return new ResponseEntity<>(result, HttpStatus.BAD_REQUEST);
        }

        stripUserPrefix(inherited);
        stripUserPrefix(assigned);

        Map<String, Object> inheritedInfo = new HashMap<>();
        inheritedInfo.put(ROLES_KEY, inherited);
        Map<String, Object> assignedInfo = new HashMap<>();
        assignedInfo.put(ROLES_KEY, assigned);

        result.put(INHERITED_ROLES, inheritedInfo);
        result.put(ASSIGNED_ROLES, assignedInfo);

        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    /**
     * @return Returns a json array of allowed patron principals
     */
    @GetMapping(value = "/acl/patron/allowedPrincipals", produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Object> getPatronAccess() {
        log.debug("Retrieving allowed patron principals");
        AgentPrincipals agent = AgentPrincipalsImpl.createFromThread();
        if (!agent.getPrincipals().contains(AccessPrincipalConstants.ADMIN_ACCESS_PRINC)) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
        return new ResponseEntity<>(patronPrincipalProvider.getConfiguredPatronPrincipals(), HttpStatus.OK);
    }

    /**
     * Returns all details that impact patron access, including inherited and
     * directly assigned patron roles, embargoes and deletion status.
     *
     * Inherited roles are computed to reflect inheritance decisions, embargoes and
     * deletions.
     *
     * @param id
     * @return
     */
    @GetMapping(value = "/acl/patron/{id}", produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Object> getPatronAccess(@PathVariable("id") String id) {
        log.debug("Retrieving patron access details for {}", id);
        PID pid = PIDs.get(id);

        AgentPrincipals agent = AgentPrincipalsImpl.createFromThread();
        aclService.assertHasAccess("Insufficient permissions to retrieve patron access for " + id,
                pid, agent.getPrincipals(), Permission.viewHidden);

        RepositoryObject repoObj = repositoryObjectLoader.getRepositoryObject(pid);

        Map<String, Object> result = new HashMap<>();

        if (repoObj instanceof AdminUnit) {
            result.put("error", "Cannot retrieve patron access for a unit");
            return new ResponseEntity<>(result, HttpStatus.BAD_REQUEST);
        } else if (repoObj instanceof ContentObject) {
            result.put(INHERITED_ROLES, addInheritedPatronInfo(repoObj));
            result.put(ASSIGNED_ROLES, addAssignedPatronInfo(pid));
            result.put(ALLOWED_PATRON_PRINCIPALS, patronPrincipalProvider.getConfiguredPatronPrincipals());

            return new ResponseEntity<>(result, HttpStatus.OK);
        } else {
            result.put("error", "Cannot retrieve staff roles for object " + id
                    + " of type " + repoObj.getClass().getName());
            return new ResponseEntity<>(result, HttpStatus.BAD_REQUEST);
        }
    }

    private PatronAccessDetails addAssignedPatronInfo(PID pid) {
        PatronAccessDetails assignedInfo = new PatronAccessDetails();
        assignedInfo.setRoles(objectAclFactory.getPatronRoleAssignments(pid));
        assignedInfo.setDeleted(objectAclFactory.isMarkedForDeletion(pid));
        assignedInfo.setEmbargo(objectAclFactory.getEmbargoUntil(pid));

        return assignedInfo;
    }

    private PatronAccessDetails addInheritedPatronInfo(RepositoryObject repoObj) {
        PatronAccessDetails inheritedInfo = new PatronAccessDetails();

        RepositoryObject parent = repoObj.getParent();
        PID pid = parent.getPid();
        if (!(parent instanceof AdminUnit)) {
            inheritedInfo.setRoles(inheritedAclFactory.getPatronAccess(pid));
            inheritedInfo.setEmbargo(inheritedAclFactory.getEmbargoUntil(pid));
        }
        inheritedInfo.setDeleted(inheritedAclFactory.isMarkedForDeletion(pid));

        return inheritedInfo;
    }

    // Trim user namespace off of user principals for the response
    private void stripUserPrefix(List<RoleAssignment> assignments) {
        for (RoleAssignment assignment: assignments) {
            String princ = assignment.getPrincipal();
            if (princ.startsWith(USER_NAMESPACE)) {
                princ = princ.substring(USER_NAMESPACE.length());
                assignment.setPrincipal(princ);
            }
        }
    }
}