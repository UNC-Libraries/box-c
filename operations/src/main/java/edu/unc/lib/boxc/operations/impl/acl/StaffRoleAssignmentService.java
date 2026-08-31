package edu.unc.lib.boxc.operations.impl.acl;

import static edu.unc.lib.boxc.auth.api.Permission.assignStaffRoles;
import static org.springframework.util.Assert.notNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.boxc.auth.api.UserRole;
import edu.unc.lib.boxc.auth.api.exceptions.InvalidAssignmentException;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.api.models.RoleAssignment;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.auth.fcrepo.services.ContentObjectAccessRestrictionValidator;
import edu.unc.lib.boxc.auth.fcrepo.services.InheritedAclFactory;
import edu.unc.lib.boxc.common.metrics.TimerFactory;
import edu.unc.lib.boxc.fcrepo.exceptions.ServiceException;
import edu.unc.lib.boxc.fcrepo.utils.FedoraTransaction;
import edu.unc.lib.boxc.fcrepo.utils.TransactionManager;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.AdminUnit;
import edu.unc.lib.boxc.model.api.objects.CollectionObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.rdf.Premis;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.model.fcrepo.ids.AgentPids;
import edu.unc.lib.boxc.operations.api.events.PremisLoggerFactory;
import edu.unc.lib.boxc.operations.jms.OperationsMessageSender;
import edu.unc.lib.boxc.operations.jms.JMSMessageUtil.CDRActions;
import io.dropwizard.metrics5.Timer;

/**
 * Service which replaces staff role assignments for content objects.
 *
 * @author bbpennel
 *
 */
public class StaffRoleAssignmentService {
    private static final Logger log = LoggerFactory.getLogger(StaffRoleAssignmentService.class);
    private static final String NEWLINE = System.getProperty("line.separator");

    private AccessControlService aclService;
    private RepositoryObjectLoader repositoryObjectLoader;
    private RepositoryObjectFactory repositoryObjectFactory;
    private InheritedAclFactory aclFactory;
    private OperationsMessageSender operationsMessageSender;
    private TransactionManager txManager;
    private PremisLoggerFactory premisLoggerFactory;

    private ContentObjectAccessRestrictionValidator accessValidator;

    private static final Timer timer = TimerFactory.createTimerForClass(StaffRoleAssignmentService.class);

    public StaffRoleAssignmentService() {
        accessValidator = new ContentObjectAccessRestrictionValidator();
    }

    /**
     * Replaces all staff role assignments for the target object with the provided list of assignments.
     *
     * @param agent Agent requesting the operation
     * @param target PID of the object whose roles will be updated.
     * @param assignments List of maps, where each map contains one staff principal to role assignment.
     *      Permissions are identified by name.
     * @return job id
     */
    public String updateRoles(AgentPrincipals agent, PID target, Collection<RoleAssignment> assignments) {
        notNull(agent, "Must provide an agent for this operation");

        FedoraTransaction tx = txManager.startTransaction();
        log.info("Starting update of roles on {} to {}", target.getId(), assignments);
        try (Timer.Context context = timer.time()) {
            aclService.assertHasAccess("Insufficient privileges to assign staff roles for object " + target.getId(),
                    target, agent.getPrincipals(), assignStaffRoles);

            RepositoryObject repoObj = repositoryObjectLoader.getRepositoryObject(target);

            // Verify that the target is a collection or admin unit
            if (!(repoObj instanceof CollectionObject || repoObj instanceof AdminUnit)) {
                throw new InvalidAssignmentException("Cannot assign staff roles to  object " + target.getId()
                        + ", objects of type " + repoObj.getClass().getName() + " are not eligible.");
            }

            assertOnlyStaffRoles(assignments);

            replaceStaffRoles(repoObj, assignments);

            premisLoggerFactory.createPremisLogger(repoObj)
                    .buildEvent(Premis.PolicyAssignment)
                    .addImplementorAgent(AgentPids.forPerson(agent))
                    .addEventDetail(createEventDetails(target))
                    .writeAndClose();

            return operationsMessageSender.sendOperationMessage(agent.getUsername(),
                    CDRActions.EDIT_ACCESS_CONTROL,
                    Collections.singletonList(target));
        } catch (RuntimeException e) {
            tx.cancelAndIgnore();
            throw e;
        } finally {
            tx.close();
        }
    }

    private void assertOnlyStaffRoles(Collection<RoleAssignment> assignments) {
        if (assignments.stream().anyMatch(a -> a.getRole().isPatronRole())) {
            throw new ServiceException("Only staff roles are applicable for this service");
        }
    }

    private void replaceStaffRoles(RepositoryObject repoObj, Collection<RoleAssignment> assignments) {
        // Update a copy of the model for this object
        Model model = ModelFactory.createDefaultModel().add(repoObj.getModel(true));
        Resource resc = model.getResource(repoObj.getPid().getRepositoryPath());

        // Clear out all the existing staff roles
        for (UserRole role: UserRole.getStaffRoles()) {
            resc.removeAll(role.getProperty());
        }

        // Add the new role assignments
        for (RoleAssignment assignment: assignments) {
            resc.addProperty(assignment.getRole().getProperty(), assignment.getPrincipal());
        }

        // Verify that ACL assignments are valid before updating
        accessValidator.validate(resc);

        // Push the updated model back to fedora
        repositoryObjectFactory.createOrTransformObject(repoObj.getUri(), model);
    }

    private String createEventDetails(PID targetPid) {
        Map<String, Set<String>> princRoles = aclFactory.getPrincipalRoles(targetPid);
        Map<UserRole, List<String>> rolePrincs = new EnumMap<>(UserRole.class);
        // Flip things around, create a map of roles to all the principals that belong to them
        princRoles.entrySet().forEach(princRolesEntry ->
            princRolesEntry.getValue().forEach(role -> {
                UserRole userRole = UserRole.getRoleByProperty(role);
                if (userRole != null && userRole.isStaffRole()) {
                    List<String> principals = rolePrincs.getOrDefault(userRole, new ArrayList<>());
                    principals.add(princRolesEntry.getKey());
                    rolePrincs.put(userRole, principals);
                }
            })
        );

        StringBuilder details = new StringBuilder("Staff roles for item set to:");
        details.append(NEWLINE);
        if (rolePrincs.isEmpty()) {
            details.append("No roles assigned");
        } else {
            // Iterating through staff roles to ensure ordering is consistent
            for (UserRole role: UserRole.getStaffRoles()) {
                if (rolePrincs.containsKey(role)) {
                    // Log principals in alphabetic order to avoid unnecessary variation
                    Collections.sort(rolePrincs.get(role));
                    details.append(role.name()).append(": ")
                        .append(String.join(", ", rolePrincs.get(role))).append(NEWLINE);
                }
            }
        }

        return details.toString();
    }

    /**
     * @param aclService the aclService to set
     */
    public void setAclService(AccessControlService aclService) {
        this.aclService = aclService;
    }

    /**
     * @param repositoryObjectLoader the repositoryObjectLoader to set
     */
    public void setRepositoryObjectLoader(RepositoryObjectLoader repositoryObjectLoader) {
        this.repositoryObjectLoader = repositoryObjectLoader;
    }

    /**
     * @param aclFactory the aclFactory to set
     */
    public void setAclFactory(InheritedAclFactory aclFactory) {
        this.aclFactory = aclFactory;
    }

    /**
     * @param operationsMessageSender the operationsMessageSender to set
     */
    public void setOperationsMessageSender(OperationsMessageSender operationsMessageSender) {
        this.operationsMessageSender = operationsMessageSender;
    }

    /**
     * @param repositoryObjectFactory the repositoryObjectFactory to set
     */
    public void setRepositoryObjectFactory(RepositoryObjectFactory repositoryObjectFactory) {
        this.repositoryObjectFactory = repositoryObjectFactory;
    }

    /**
     * @param txManager the txManager to set
     */
    public void setTransactionManager(TransactionManager txManager) {
        this.txManager = txManager;
    }

    public void setPremisLoggerFactory(PremisLoggerFactory premisLoggerFactory) {
        this.premisLoggerFactory = premisLoggerFactory;
    }
}
