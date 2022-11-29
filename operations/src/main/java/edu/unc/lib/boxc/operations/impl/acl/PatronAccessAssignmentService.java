package edu.unc.lib.boxc.operations.impl.acl;

import static edu.unc.lib.boxc.auth.api.Permission.changePatronAccess;
import static edu.unc.lib.boxc.auth.api.Permission.ingest;
import static edu.unc.lib.boxc.auth.api.services.EmbargoUtil.isEmbargoActive;
import static edu.unc.lib.boxc.common.util.DateTimeUtil.formatDateToUTC;
import static edu.unc.lib.boxc.model.api.rdf.CdrAcl.embargoUntil;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.springframework.util.Assert.notNull;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.datatypes.xsd.XSDDateTime;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.UserRole;
import edu.unc.lib.boxc.auth.api.exceptions.InvalidAssignmentException;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.api.models.RoleAssignment;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.auth.fcrepo.services.ContentObjectAccessRestrictionValidator;
import edu.unc.lib.boxc.common.metrics.TimerFactory;
import edu.unc.lib.boxc.fcrepo.exceptions.ServiceException;
import edu.unc.lib.boxc.fcrepo.utils.FedoraTransaction;
import edu.unc.lib.boxc.fcrepo.utils.TransactionManager;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.AdminUnit;
import edu.unc.lib.boxc.model.api.objects.ContentObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.rdf.Premis;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.model.fcrepo.ids.AgentPids;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.api.events.PremisLogger;
import edu.unc.lib.boxc.operations.api.events.PremisLoggerFactory;
import edu.unc.lib.boxc.operations.jms.OperationsMessageSender;
import edu.unc.lib.boxc.operations.jms.JMSMessageUtil.CDRActions;
import io.dropwizard.metrics5.Timer;

/**
 * Service which replaces patron access assignments for content objects.
 *
 * @author bbpennel
 *
 */
public class PatronAccessAssignmentService {

    private static final Logger log = LoggerFactory.getLogger(PatronAccessAssignmentService.class);
    private static final String NEWLINE = System.getProperty("line.separator");

    private AccessControlService aclService;
    private RepositoryObjectLoader repositoryObjectLoader;
    private RepositoryObjectFactory repositoryObjectFactory;
    private OperationsMessageSender operationsMessageSender;
    private TransactionManager txManager;
    private PremisLoggerFactory premisLoggerFactory;

    private ContentObjectAccessRestrictionValidator accessValidator;

    private static final Timer timer = TimerFactory.createTimerForClass(PatronAccessAssignmentService.class);

    public PatronAccessAssignmentService() {
        accessValidator = new ContentObjectAccessRestrictionValidator();
    }

    public String updatePatronAccess(PatronAccessAssignmentRequest request) {
        notNull(request.getAgent(), "Must provide an agent for this operation");
        notNull(request.getTargetPid(), "Must provide the PID of the object to update");
        notNull(request.getAccessDetails(), "Must provide patron access details");

        PID target = request.getTargetPid();
        AgentPrincipals agent = request.getAgent();
        FedoraTransaction tx = txManager.startTransaction();
        log.info("Starting update of patron access on {}", target.getId());
        try (Timer.Context context = timer.time()) {
            Permission permissionToCheck = request.isFolderCreation() ? ingest : changePatronAccess;
            aclService.assertHasAccess("Insufficient privileges to assign patron roles for object " + target.getId(),
                    target, agent.getPrincipals(), permissionToCheck);

            assertAssignmentsComplete(request.getAccessDetails());

            assertOnlyPatronRoles(request.getAccessDetails());

            RepositoryObject repoObj = repositoryObjectLoader.getRepositoryObject(target);

            if (repoObj instanceof AdminUnit || !(repoObj instanceof ContentObject)) {
                throw new InvalidAssignmentException("Cannot assign patron access to object " + target.getId()
                    + ", objects of type " + repoObj.getClass().getName() + " are not eligible.");
            }

            Model updated = ModelFactory.createDefaultModel().add(repoObj.getModel(true));
            Resource rolesEventResc = replacePatronRoles(repoObj, updated, request);
            Resource embargoEventResc = updateEmbargo(repoObj, updated, request);

            // No changes occurred, perform no updates to the resource
            if (rolesEventResc == null && embargoEventResc == null) {
                return null;
            }

            // Validate that the access control assignments are allowed
            accessValidator.validate(updated.getResource(repoObj.getPid().getRepositoryPath()));

            repositoryObjectFactory.createOrTransformObject(repoObj.getUri(), updated);

            writePremisEvents(repoObj, rolesEventResc, embargoEventResc);

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

    /**
     * Replace all the patron roles for the provided object
     *
     * @param repoObj
     * @param model
     * @param request
     * @return Returns the premis event resource created to capture this event, or null if no patron roles changed.
     */
    private Resource replacePatronRoles(RepositoryObject repoObj, Model model, PatronAccessAssignmentRequest request) {
        if (request.isSkipRoles()) {
            return null;
        }

        Collection<RoleAssignment> assignments = request.getAccessDetails().getRoles();
        AgentPrincipals agent = request.getAgent();

        // Update a copy of the model for this object
        Resource resc = model.getResource(repoObj.getPid().getRepositoryPath());

        // So nor proceed if there are no changes to role assignments
        if (!hasRoleChanges(resc, assignments)) {
            return null;
        }

        // Clear out all the existing staff roles
        for (UserRole role: UserRole.getPatronRoles()) {
            resc.removeAll(role.getProperty());
        }

        // Add the new role assignments
        if (assignments != null) {
            for (RoleAssignment assignment: assignments) {
                resc.addProperty(assignment.getRole().getProperty(), assignment.getPrincipal());
            }
        }

        // Add PREMIS event indicating the role changes
        return premisLoggerFactory.createPremisLogger(repoObj).buildEvent(Premis.PolicyAssignment)
                .addImplementorAgent(AgentPids.forPerson(agent))
                .addEventDetail(createRoleEventDetails(assignments))
                .create();
    }

    /**
     * Returns true if there are any differences between the patron role
     * assignments on the existing resource versus the incoming role asisgnments
     *
     * @param resc
     * @param assignments
     * @return
     */
    private boolean hasRoleChanges(Resource resc, Collection<RoleAssignment> assignments) {
        Set<String> existingPatronRoles = new HashSet<>();
        StmtIterator it = resc.listProperties();
        while (it.hasNext()) {
            Statement stmt = it.next();
            UserRole role = UserRole.getRoleByProperty(stmt.getPredicate().toString());
            if (role != null && role.isPatronRole()) {
                existingPatronRoles.add(stmt.getString() + "|" + role.name());
            }
        }

        Set<String> incomingRoles;
        if (assignments == null) {
            incomingRoles = Collections.emptySet();
        } else {
            incomingRoles = assignments.stream()
                    .map(a -> a.getPrincipal() + "|" + a.getRole().name())
                    .collect(Collectors.toSet());
        }

        if (incomingRoles.size() != existingPatronRoles.size()) {
            return true;
        }

        return !incomingRoles.containsAll(existingPatronRoles);
    }

    private Resource updateEmbargo(RepositoryObject repoObj, Model model, PatronAccessAssignmentRequest request) {
        if (request.isSkipEmbargo()) {
            return null;
        }
        Date newEmbargo = request.getAccessDetails().getEmbargo();
        if (newEmbargo != null && !isEmbargoActive(newEmbargo)) {
            throw new InvalidAssignmentException("Cannot assign expired embargo to object "
                    + repoObj.getPid().getId());
        }

        Resource resc = model.getResource(repoObj.getPid().getRepositoryPath());

        String existingEmbargo = null;
        // Remove any existing embargoes
        if (resc.hasProperty(embargoUntil)) {
            existingEmbargo = formatDateToUTC(((XSDDateTime) resc.getProperty(embargoUntil).getLiteral()
                    .getValue()).asCalendar().getTime());
            resc.removeAll(embargoUntil);
        }

        String newEmbargoString = null;
        // Add the new embargo to the record, if supplied
        if (newEmbargo != null) {
            newEmbargoString = formatDateToUTC(newEmbargo);
            resc.addProperty(embargoUntil, newEmbargoString, XSDDatatype.XSDdateTime);
        }

        String eventText = createEmbargoEventDetails(existingEmbargo, newEmbargoString);
        if (eventText == null) {
            // No embargo change, return no event.
            return null;
        } else {
            // Produce the premis event for this embargo
            return premisLoggerFactory.createPremisLogger(repoObj).buildEvent(Premis.PolicyAssignment)
                    .addImplementorAgent(AgentPids.forPerson(request.getAgent()))
                    .addEventDetail(eventText)
                    .create();
        }
    }

    private String createRoleEventDetails(Collection<RoleAssignment> assignments) {
        StringBuilder details = new StringBuilder("Patron roles for item set to:");
        details.append(NEWLINE);
        if (assignments == null || assignments.isEmpty()) {
            details.append("No roles assigned");
        } else {
            String roleDetails = assignments.stream()
                    .map(a -> a.getPrincipal() + ": " + a.getRole().getPropertyString())
                    .collect(Collectors.joining(NEWLINE));
            details.append(roleDetails);
        }

        return details.toString();
    }

    public static void assertAssignmentsComplete(PatronAccessDetails details) {
        if (details.getRoles() == null) {
            return;
        }
        for (RoleAssignment assignment: details.getRoles()) {
            if (assignment.getRole() == null) {
                throw new IllegalArgumentException("Assignment must provide a role");
            }
            if (isBlank(assignment.getPrincipal())) {
                throw new IllegalArgumentException("Assignment must provide a principal");
            }
        }
    }

    public static void  assertOnlyPatronRoles(PatronAccessDetails details) {
        if (details.getRoles() != null && details.getRoles().stream().anyMatch(a -> a.getRole().isStaffRole())) {
            throw new ServiceException("Only patron roles are applicable for this service");
        }
    }

    private String createEmbargoEventDetails(String existingEmbargo, String newEmbargo) {
        if (existingEmbargo == null) {
            if (newEmbargo == null) {
                // No change
                return null;
            } else {
                // Assigned a new embargo
                return "Set an embargo that will expire " + newEmbargo;
            }
        } else {
            if (newEmbargo == null) {
                // Cleared an existing embargo
                return "Embargo expiration date changed from " + existingEmbargo + " to no embargo.";
            } else if (newEmbargo.equals(existingEmbargo)) {
                // No change
                return null;
            } else {
                // Change existing embargo
                return "Embargo expiration date changed from " + existingEmbargo + " to " + newEmbargo;
            }
        }
    }

    private void writePremisEvents(RepositoryObject repoObj, Resource rolesEvent, Resource embargoEvent) {
        try (PremisLogger logger = premisLoggerFactory.createPremisLogger(repoObj)) {
            if (rolesEvent != null) {
                if (embargoEvent != null) {
                    logger.writeEvents(rolesEvent, embargoEvent);
                } else {
                    logger.writeEvents(rolesEvent);
                }
            } else {
                // This method is only called if either roles or embargo event is non-null
                logger.writeEvents(embargoEvent);
            }
        }
    }

    public void setAclService(AccessControlService aclService) {
        this.aclService = aclService;
    }

    public void setRepositoryObjectLoader(RepositoryObjectLoader repositoryObjectLoader) {
        this.repositoryObjectLoader = repositoryObjectLoader;
    }

    public void setRepositoryObjectFactory(RepositoryObjectFactory repositoryObjectFactory) {
        this.repositoryObjectFactory = repositoryObjectFactory;
    }

    public void setOperationsMessageSender(OperationsMessageSender operationsMessageSender) {
        this.operationsMessageSender = operationsMessageSender;
    }

    public void setTransactionManager(TransactionManager txManager) {
        this.txManager = txManager;
    }

    public void setAccessValidator(ContentObjectAccessRestrictionValidator accessValidator) {
        this.accessValidator = accessValidator;
    }

    public void setPremisLoggerFactory(PremisLoggerFactory premisLoggerFactory) {
        this.premisLoggerFactory = premisLoggerFactory;
    }

    public static class PatronAccessAssignmentRequest {
        @JsonDeserialize(as = AgentPrincipalsImpl.class)
        private AgentPrincipals agent;
        private PID target;
        private PatronAccessDetails accessDetails;
        private boolean isFolderCreation;
        private boolean skipEmbargo;
        private boolean skipRoles;

        public PatronAccessAssignmentRequest() {
        }

        public PatronAccessAssignmentRequest(AgentPrincipals agent, PID target, PatronAccessDetails accessDetails) {
            this.agent = agent;
            this.target = target;
            this.accessDetails = accessDetails;
        }

        public AgentPrincipals getAgent() {
            return agent;
        }

        public void setAgent(AgentPrincipals agent) {
            this.agent = agent;
        }

        @JsonIgnore
        public PID getTargetPid() {
            return target;
        }

        public String getTarget() {
            return target.getRepositoryPath();
        }

        public void setTargetPid(PID target) {
            this.target = target;
        }

        public void setTarget(String target) {
            this.target = PIDs.get(target);
        }

        public PatronAccessDetails getAccessDetails() {
            return accessDetails;
        }

        public void setAccessDetails(PatronAccessDetails accessDetails) {
            this.accessDetails = accessDetails;
        }

        public boolean isFolderCreation() {
            return isFolderCreation;
        }

        public void setFolderCreation(boolean isFolderCreation) {
            this.isFolderCreation = isFolderCreation;
        }

        public PatronAccessAssignmentRequest withFolderCreation(boolean isFolderCreation) {
            this.isFolderCreation = isFolderCreation;
            return this;
        }

        public boolean isSkipEmbargo() {
            return skipEmbargo;
        }

        public void setSkipEmbargo(boolean skipEmbargo) {
            this.skipEmbargo = skipEmbargo;
        }

        public PatronAccessAssignmentRequest withSkipEmbargo(boolean skipEmbargo) {
            this.skipEmbargo = skipEmbargo;
            return this;
        }

        public boolean isSkipRoles() {
            return skipRoles;
        }

        public void setSkipRoles(boolean skipRoles) {
            this.skipRoles = skipRoles;
        }

        public PatronAccessAssignmentRequest withSkipRoles(boolean skipRoles) {
            this.skipRoles = skipRoles;
            return this;
        }
    }
}
