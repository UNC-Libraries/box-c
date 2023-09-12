package edu.unc.lib.boxc.operations.impl.acl;

import static edu.unc.lib.boxc.auth.api.AccessPrincipalConstants.AUTHENTICATED_PRINC;
import static edu.unc.lib.boxc.auth.api.AccessPrincipalConstants.PUBLIC_PRINC;
import static edu.unc.lib.boxc.auth.api.UserRole.canManage;
import static edu.unc.lib.boxc.auth.api.UserRole.canViewMetadata;
import static edu.unc.lib.boxc.auth.api.UserRole.canViewOriginals;
import static edu.unc.lib.boxc.common.util.DateTimeUtil.formatDateToUTC;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyCollectionOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.openMocks;

import java.net.URI;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.jena.datatypes.xsd.XSDDateTime;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.UserRole;
import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.exceptions.InvalidAssignmentException;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.api.models.RoleAssignment;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.fcrepo.exceptions.ServiceException;
import edu.unc.lib.boxc.fcrepo.utils.TransactionManager;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.AdminUnit;
import edu.unc.lib.boxc.model.api.objects.BinaryObject;
import edu.unc.lib.boxc.model.api.objects.CollectionObject;
import edu.unc.lib.boxc.model.api.objects.ContentRootObject;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.api.rdf.CdrAcl;
import edu.unc.lib.boxc.model.api.rdf.Premis;
import edu.unc.lib.boxc.model.api.rdf.Prov;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.model.fcrepo.ids.AgentPids;
import edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPaths;
import edu.unc.lib.boxc.model.fcrepo.services.RepositoryInitializer;
import edu.unc.lib.boxc.model.fcrepo.test.AclModelBuilder;
import edu.unc.lib.boxc.model.fcrepo.test.RepositoryObjectTreeIndexer;
import edu.unc.lib.boxc.model.fcrepo.test.TestHelper;
import edu.unc.lib.boxc.operations.api.events.PremisLoggerFactory;
import edu.unc.lib.boxc.operations.impl.acl.PatronAccessAssignmentService.PatronAccessAssignmentRequest;
import edu.unc.lib.boxc.operations.jms.OperationsMessageSender;
import edu.unc.lib.boxc.operations.jms.JMSMessageUtil.CDRActions;

@ExtendWith(SpringExtension.class)
@ContextHierarchy({
    @ContextConfiguration("/spring-test/test-fedora-container.xml"),
    @ContextConfiguration("/spring-test/cdr-client-container.xml")
})
public class PatronAccessAssignmentServiceIT {

    private static final String USER_PRINC = "user";
    private static final String GRP_PRINC = "adminGroup";

    private static final String origFilename = "original.txt";
    private static final String origMimetype = "text/plain";

    private AutoCloseable closeable;

    @Autowired
    private String baseAddress;
    @Mock
    private AccessControlService aclService;
    @Autowired
    private RepositoryObjectFactory repoObjFactory;
    @Autowired
    private RepositoryObjectLoader repoObjLoader;
    @Mock
    private OperationsMessageSender operationsMessageSender;
    @Autowired
    private RepositoryObjectTreeIndexer treeIndexer;
    @Autowired
    private TransactionManager txManager;
    @Autowired
    private PremisLoggerFactory premisLoggerFactory;
    @Autowired
    private RepositoryInitializer repoInitializer;
    @Captor
    private ArgumentCaptor<List<PID>> pidListCaptor;

    private PatronAccessAssignmentService patronService;

    private AgentPrincipals agent;
    private AccessGroupSet groups;
    private ContentRootObject contentRoot;
    private AdminUnit adminUnit;
    private CollectionObject collObj;

    @BeforeEach
    public void init() throws Exception {
        closeable = openMocks(this);
        TestHelper.setContentBase(baseAddress);

        groups = new AccessGroupSetImpl(GRP_PRINC);
        agent = new AgentPrincipalsImpl(USER_PRINC, groups);

        patronService = new PatronAccessAssignmentService();
        patronService.setAclService(aclService);
        patronService.setOperationsMessageSender(operationsMessageSender);
        patronService.setRepositoryObjectLoader(repoObjLoader);
        patronService.setRepositoryObjectFactory(repoObjFactory);
        patronService.setTransactionManager(txManager);
        patronService.setPremisLoggerFactory(premisLoggerFactory);

        PID contentRootPid = RepositoryPaths.getContentRootPid();
        repoInitializer.initializeRepository();
        contentRoot = repoObjLoader.getContentRootObject(contentRootPid);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void insufficientPermissions() throws Exception {
        Assertions.assertThrows(AccessRestrictionException.class, () -> {
            createCollectionInUnit(null);
            PID pid = collObj.getPid();
            treeIndexer.indexAll(baseAddress);

            doThrow(new AccessRestrictionException()).when(aclService)
                    .assertHasAccess(anyString(), eq(pid), any(AccessGroupSetImpl.class), eq(Permission.changePatronAccess));

            PatronAccessDetails accessDetails = new PatronAccessDetails();
            accessDetails.setRoles(asList(
                    new RoleAssignment(PUBLIC_PRINC, canViewOriginals)));

            patronService.updatePatronAccess(new PatronAccessAssignmentRequest(agent, pid, accessDetails));
        });
    }

    @Test
    public void insufficientPermissionsNewFolder() throws Exception {
        Assertions.assertThrows(AccessRestrictionException.class, () -> {
            createCollectionInUnit(null);
            PID pid = collObj.getPid();
            treeIndexer.indexAll(baseAddress);

            doThrow(new AccessRestrictionException()).when(aclService)
                    .assertHasAccess(anyString(), eq(pid), any(AccessGroupSetImpl.class), eq(Permission.ingest));

            PatronAccessDetails accessDetails = new PatronAccessDetails();
            accessDetails.setRoles(asList(
                    new RoleAssignment(PUBLIC_PRINC, canViewOriginals)));

            patronService.updatePatronAccess(new PatronAccessAssignmentRequest(agent, pid, accessDetails)
                    .withFolderCreation(true));
        });
    }

    @Test
    public void setStaffRolesFailure() throws Exception {
        Assertions.assertThrows(ServiceException.class, () -> {
            createCollectionInUnit(null);
            PID pid = collObj.getPid();
            treeIndexer.indexAll(baseAddress);

            PatronAccessDetails accessDetails = new PatronAccessDetails();
            accessDetails.setRoles(asList(
                    new RoleAssignment(PUBLIC_PRINC, canManage)));

            patronService.updatePatronAccess(new PatronAccessAssignmentRequest(agent, pid, accessDetails));
        });
    }

    @Test
    public void assignToBinaryObject() throws Exception {
        Assertions.assertThrows(InvalidAssignmentException.class, () -> {
            createCollectionInUnit(null);
            WorkObject work = repoObjFactory.createWorkObject(null);
            collObj.addMember(work);
            URI contentUri = Files.createTempFile("test", ".txt").toUri();
            FileObject fileObj = work.addDataFile(contentUri, origFilename, origMimetype, null, null);
            BinaryObject binObj = fileObj.getOriginalFile();

            PID pid = binObj.getPid();
            treeIndexer.indexAll(baseAddress);

            PatronAccessDetails accessDetails = new PatronAccessDetails();
            accessDetails.setRoles(asList(
                    new RoleAssignment(PUBLIC_PRINC, canViewOriginals)));

            patronService.updatePatronAccess(new PatronAccessAssignmentRequest(agent, pid, accessDetails));
        });
    }

    @Test
    public void assignRolesToAdminUnit() throws Exception {
        Assertions.assertThrows(InvalidAssignmentException.class, () -> {
            createCollectionInUnit(null);
            PID pid = adminUnit.getPid();
            treeIndexer.indexAll(baseAddress);

            PatronAccessDetails accessDetails = new PatronAccessDetails();
            accessDetails.setRoles(asList(
                    new RoleAssignment(PUBLIC_PRINC, canViewMetadata)));

            patronService.updatePatronAccess(new PatronAccessAssignmentRequest(agent, pid, accessDetails));
        });
    }

    @Test
    public void assignNewRoles() throws Exception {
        createCollectionInUnit(null);
        PID pid = collObj.getPid();
        treeIndexer.indexAll(baseAddress);

        PatronAccessDetails accessDetails = new PatronAccessDetails();
        accessDetails.setRoles(asList(
                new RoleAssignment(PUBLIC_PRINC, canViewMetadata),
                new RoleAssignment(AUTHENTICATED_PRINC, canViewOriginals)));

        patronService.updatePatronAccess(new PatronAccessAssignmentRequest(agent, pid, accessDetails));

        RepositoryObject target = repoObjLoader.getRepositoryObject(pid);
        assertHasAssignment(PUBLIC_PRINC, canViewMetadata, target);
        assertHasAssignment(AUTHENTICATED_PRINC, canViewOriginals, target);

        assertNoEmbargo(target);

        List<String> eventDetails = getEventDetails(target);
        assertEquals(1, eventDetails.size());
        assertEventWithDetail(eventDetails, PUBLIC_PRINC + ": " + canViewMetadata.getPropertyString());
        assertEventWithDetail(eventDetails, AUTHENTICATED_PRINC + ": " + canViewOriginals.getPropertyString());

        assertMessageSent(pid);
    }

    @Test
    public void missingAgent() throws Exception {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            createCollectionInUnit(null);
            PID pid = collObj.getPid();
            treeIndexer.indexAll(baseAddress);

            PatronAccessDetails accessDetails = new PatronAccessDetails();
            accessDetails.setRoles(asList(
                    new RoleAssignment(PUBLIC_PRINC, canViewMetadata)));

            patronService.updatePatronAccess(new PatronAccessAssignmentRequest(null, pid, accessDetails));
        });
    }

    @Test
    public void missingTarget() throws Exception {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            createCollectionInUnit(null);
            treeIndexer.indexAll(baseAddress);

            PatronAccessDetails accessDetails = new PatronAccessDetails();
            accessDetails.setRoles(asList(
                    new RoleAssignment(PUBLIC_PRINC, canViewMetadata)));

            patronService.updatePatronAccess(new PatronAccessAssignmentRequest(agent, null, accessDetails));
        });
    }

    @Test
    public void missingAccessDetails() throws Exception {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            createCollectionInUnit(null);
            PID pid = collObj.getPid();
            treeIndexer.indexAll(baseAddress);

            patronService.updatePatronAccess(new PatronAccessAssignmentRequest(agent, pid, null));
        });
    }

    @Test
    public void missingRole() throws Exception {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            createCollectionInUnit(null);
            PID pid = collObj.getPid();
            treeIndexer.indexAll(baseAddress);

            PatronAccessDetails accessDetails = new PatronAccessDetails();
            accessDetails.setRoles(asList(
                    new RoleAssignment(PUBLIC_PRINC, null)));

            patronService.updatePatronAccess(new PatronAccessAssignmentRequest(agent, pid, accessDetails));
        });
    }

    @Test
    public void missingPrinicipal() throws Exception {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            createCollectionInUnit(null);
            PID pid = collObj.getPid();
            treeIndexer.indexAll(baseAddress);

            PatronAccessDetails accessDetails = new PatronAccessDetails();
            accessDetails.setRoles(asList(
                    new RoleAssignment(null, canViewMetadata)));

            patronService.updatePatronAccess(new PatronAccessAssignmentRequest(agent, pid, accessDetails));
        });
    }

    @Test
    public void blankPrinicipal() throws Exception {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            createCollectionInUnit(null);
            PID pid = collObj.getPid();
            treeIndexer.indexAll(baseAddress);

            PatronAccessDetails accessDetails = new PatronAccessDetails();
            accessDetails.setRoles(asList(
                    new RoleAssignment("  ", null)));

            patronService.updatePatronAccess(new PatronAccessAssignmentRequest(agent, pid, accessDetails));
        });
    }

    @Test
    public void revokeRole() throws Exception {
        createCollectionInUnit(new AclModelBuilder("Collection with patron roles")
                .addCanViewOriginals(PUBLIC_PRINC)
                .model);
        PID pid = collObj.getPid();
        treeIndexer.indexAll(baseAddress);

        PatronAccessDetails accessDetails = new PatronAccessDetails();
        accessDetails.setRoles(asList(
                new RoleAssignment(PUBLIC_PRINC, UserRole.none)));

        patronService.updatePatronAccess(new PatronAccessAssignmentRequest(agent, pid, accessDetails));

        RepositoryObject target = repoObjLoader.getRepositoryObject(pid);
        assertHasAssignment(PUBLIC_PRINC, UserRole.none, target);

        assertNoEmbargo(target);

        List<String> eventDetails = getEventDetails(target);
        assertEquals(1, eventDetails.size());
        assertEventWithDetail(eventDetails, PUBLIC_PRINC + ": " + UserRole.none.getPropertyString());

        assertMessageSent(pid);
    }

    @Test
    public void overwriteRoles() throws Exception {
        createCollectionInUnit(new AclModelBuilder("Collection with patron roles")
                .addCanViewOriginals(PUBLIC_PRINC)
                .addCanViewOriginals(AUTHENTICATED_PRINC)
                .model);
        PID pid = collObj.getPid();
        treeIndexer.indexAll(baseAddress);

        PatronAccessDetails accessDetails = new PatronAccessDetails();
        accessDetails.setRoles(asList(
                new RoleAssignment(AUTHENTICATED_PRINC, canViewMetadata)));

        patronService.updatePatronAccess(new PatronAccessAssignmentRequest(agent, pid, accessDetails));

        RepositoryObject target = repoObjLoader.getRepositoryObject(pid);
        assertNoAssignment(PUBLIC_PRINC, target);
        assertHasAssignment(AUTHENTICATED_PRINC, canViewMetadata, target);

        assertNoEmbargo(target);

        List<String> eventDetails = getEventDetails(target);
        assertEquals(1, eventDetails.size());
        assertEventWithDetail(eventDetails, AUTHENTICATED_PRINC + ": " + canViewMetadata.getPropertyString());
        assertFalse(eventDetails.get(0).contains(PUBLIC_PRINC));

        assertMessageSent(pid);
    }

    @Test
    public void clearExistingRoles() throws Exception {
        createCollectionInUnit(new AclModelBuilder("Collection with patron role")
                .addCanViewOriginals(PUBLIC_PRINC)
                .model);
        PID pid = collObj.getPid();
        treeIndexer.indexAll(baseAddress);

        PatronAccessDetails accessDetails = new PatronAccessDetails();

        patronService.updatePatronAccess(new PatronAccessAssignmentRequest(agent, pid, accessDetails));

        RepositoryObject target = repoObjLoader.getRepositoryObject(pid);
        assertNoAssignment(PUBLIC_PRINC, target);

        assertNoEmbargo(target);

        List<String> eventDetails = getEventDetails(target);
        assertEquals(1, eventDetails.size());
        assertEventWithDetail(eventDetails, "No roles assigned");

        assertMessageSent(pid);
    }

    @Test
    public void addNewEmbargo() throws Exception {
        createCollectionInUnit(null);
        PID pid = collObj.getPid();
        treeIndexer.indexAll(baseAddress);

        Date embargoUntil = getYearsInTheFuture(1).getTime();
        PatronAccessDetails accessDetails = new PatronAccessDetails();
        accessDetails.setEmbargo(embargoUntil);

        patronService.updatePatronAccess(new PatronAccessAssignmentRequest(agent, pid, accessDetails));

        RepositoryObject target = repoObjLoader.getRepositoryObject(pid);

        assertNoRoles(target);

        assertHasEmbargo(embargoUntil, target);

        List<String> eventDetails = getEventDetails(target);
        assertEquals(1, eventDetails.size());
        assertEventWithDetail(eventDetails, "Set an embargo that will expire " + formatDateToUTC(embargoUntil));

        assertMessageSent(pid);
    }

    @Test
    public void addExpiredEmbargo() throws Exception {
        Assertions.assertThrows(InvalidAssignmentException.class, () -> {
            createCollectionInUnit(null);
            PID pid = collObj.getPid();
            treeIndexer.indexAll(baseAddress);

            Date embargoUntil = new Date(0);
            PatronAccessDetails accessDetails = new PatronAccessDetails();
            accessDetails.setEmbargo(embargoUntil);

            patronService.updatePatronAccess(new PatronAccessAssignmentRequest(agent, pid, accessDetails));
        });
    }

    @Test
    public void replaceEmbargo() throws Exception {
        Calendar originalEmbargo = getYearsInTheFuture(1);
        createCollectionInUnit(new AclModelBuilder("Collection with embargo")
                .addEmbargoUntil(originalEmbargo)
                .model);
        PID pid = collObj.getPid();
        treeIndexer.indexAll(baseAddress);

        Date embargoUntil = getYearsInTheFuture(2).getTime();
        PatronAccessDetails accessDetails = new PatronAccessDetails();
        accessDetails.setEmbargo(embargoUntil);

        patronService.updatePatronAccess(new PatronAccessAssignmentRequest(agent, pid, accessDetails));

        RepositoryObject target = repoObjLoader.getRepositoryObject(pid);

        assertNoRoles(target);

        assertHasEmbargo(embargoUntil, target);

        List<String> eventDetails = getEventDetails(target);
        assertEquals(1, eventDetails.size());
        assertEventWithDetail(eventDetails, "Embargo expiration date changed from "
                + formatDateToUTC(originalEmbargo.getTime())
                + " to " + formatDateToUTC(embargoUntil));

        assertMessageSent(pid);
    }

    @Test
    public void removeEmbargo() throws Exception {
        Calendar originalEmbargo = getYearsInTheFuture(1);
        createCollectionInUnit(new AclModelBuilder("Collection with embargo")
                .addEmbargoUntil(originalEmbargo)
                .model);
        PID pid = collObj.getPid();
        treeIndexer.indexAll(baseAddress);

        PatronAccessDetails accessDetails = new PatronAccessDetails();

        patronService.updatePatronAccess(new PatronAccessAssignmentRequest(agent, pid, accessDetails));

        RepositoryObject target = repoObjLoader.getRepositoryObject(pid);

        assertNoRoles(target);

        assertNoEmbargo(target);

        List<String> eventDetails = getEventDetails(target);
        assertEquals(1, eventDetails.size());
        assertEventWithDetail(eventDetails, "Embargo expiration date changed from "
                + formatDateToUTC(originalEmbargo.getTime())
                + " to no embargo");

        assertMessageSent(pid);
    }

    @Test
    public void addNewEmbargoAndRole() throws Exception {
        createCollectionInUnit(null);
        PID pid = collObj.getPid();
        treeIndexer.indexAll(baseAddress);

        Date embargoUntil = getYearsInTheFuture(1).getTime();
        PatronAccessDetails accessDetails = new PatronAccessDetails();
        accessDetails.setEmbargo(embargoUntil);
        accessDetails.setRoles(asList(
                new RoleAssignment(AUTHENTICATED_PRINC, canViewMetadata)));

        patronService.updatePatronAccess(new PatronAccessAssignmentRequest(agent, pid, accessDetails));

        RepositoryObject target = repoObjLoader.getRepositoryObject(pid);

        assertHasAssignment(AUTHENTICATED_PRINC, canViewMetadata, target);

        assertHasEmbargo(embargoUntil, target);

        List<String> eventDetails = getEventDetails(target);
        assertEquals(2, eventDetails.size());
        assertEventWithDetail(eventDetails, AUTHENTICATED_PRINC + ": " + canViewMetadata.getPropertyString());
        assertEventWithDetail(eventDetails, "Set an embargo that will expire " + formatDateToUTC(embargoUntil));

        assertMessageSent(pid);
    }

    @Test
    public void skipAddEmbargo() throws Exception {
        createCollectionInUnit(null);
        PID pid = collObj.getPid();
        treeIndexer.indexAll(baseAddress);

        Date embargoUntil = getYearsInTheFuture(1).getTime();
        PatronAccessDetails accessDetails = new PatronAccessDetails();
        accessDetails.setEmbargo(embargoUntil);

        patronService.updatePatronAccess(new PatronAccessAssignmentRequest(agent, pid, accessDetails)
                .withSkipEmbargo(true));

        RepositoryObject target = repoObjLoader.getRepositoryObject(pid);

        assertNoRoles(target);

        assertNoEmbargo(target);

        List<String> eventDetails = getEventDetails(target);
        assertEquals(0, eventDetails.size());

        assertMessageNotSent(pid);
    }

    @Test
    public void skipRemoveEmbargo() throws Exception {
        Calendar originalEmbargo = getYearsInTheFuture(1);
        createCollectionInUnit(new AclModelBuilder("Collection with embargo")
                .addEmbargoUntil(originalEmbargo)
                .model);
        PID pid = collObj.getPid();
        treeIndexer.indexAll(baseAddress);

        PatronAccessDetails accessDetails = new PatronAccessDetails();

        patronService.updatePatronAccess(new PatronAccessAssignmentRequest(agent, pid, accessDetails)
                .withSkipEmbargo(true));

        RepositoryObject target = repoObjLoader.getRepositoryObject(pid);

        assertNoRoles(target);
        assertHasEmbargo(originalEmbargo.getTime(), target);

        List<String> eventDetails = getEventDetails(target);
        assertEquals(0, eventDetails.size());

        assertMessageNotSent(pid);
    }

    @Test
    public void rolesUpdatedWhenSkippingEmbargo() throws Exception {
        createCollectionInUnit(new AclModelBuilder("Collection")
                .model);
        PID pid = collObj.getPid();
        treeIndexer.indexAll(baseAddress);

        PatronAccessDetails accessDetails = new PatronAccessDetails();
        accessDetails.setRoles(asList(new RoleAssignment(AUTHENTICATED_PRINC, canViewOriginals)));

        patronService.updatePatronAccess(new PatronAccessAssignmentRequest(agent, pid, accessDetails)
                .withSkipEmbargo(true));

        RepositoryObject target = repoObjLoader.getRepositoryObject(pid);
        assertHasAssignment(AUTHENTICATED_PRINC, canViewOriginals, target);
        assertNoEmbargo(target);

        List<String> eventDetails = getEventDetails(target);
        assertEquals(1, eventDetails.size());
        assertEventWithDetail(eventDetails, AUTHENTICATED_PRINC + ": " + canViewOriginals.getPropertyString());

        assertMessageSent(pid);
    }

    @Test
    public void skipOverwritingRoles() throws Exception {
        createCollectionInUnit(new AclModelBuilder("Collection with patron roles")
                .addCanViewOriginals(PUBLIC_PRINC)
                .addCanViewOriginals(AUTHENTICATED_PRINC)
                .model);
        PID pid = collObj.getPid();
        treeIndexer.indexAll(baseAddress);

        PatronAccessDetails accessDetails = new PatronAccessDetails();
        patronService.updatePatronAccess(new PatronAccessAssignmentRequest(agent, pid, accessDetails)
                .withSkipRoles(true));

        RepositoryObject target = repoObjLoader.getRepositoryObject(pid);
        assertHasAssignment(PUBLIC_PRINC, canViewOriginals, target);
        assertHasAssignment(AUTHENTICATED_PRINC, canViewOriginals, target);

        assertNoEmbargo(target);

        List<String> eventDetails = getEventDetails(target);
        assertEquals(0, eventDetails.size());

        assertMessageNotSent(pid);
    }

    @Test
    public void embargoUpdatedWhenSkippingRoles() throws Exception {
        createCollectionInUnit(new AclModelBuilder("Collection with patron roles")
                .addCanViewOriginals(AUTHENTICATED_PRINC)
                .model);
        PID pid = collObj.getPid();
        treeIndexer.indexAll(baseAddress);

        Date embargoUntil = getYearsInTheFuture(1).getTime();
        PatronAccessDetails accessDetails = new PatronAccessDetails();
        accessDetails.setEmbargo(embargoUntil);
        patronService.updatePatronAccess(new PatronAccessAssignmentRequest(agent, pid, accessDetails)
                .withSkipRoles(true));

        RepositoryObject target = repoObjLoader.getRepositoryObject(pid);
        assertHasAssignment(AUTHENTICATED_PRINC, canViewOriginals, target);

        assertHasEmbargo(embargoUntil, target);

        List<String> eventDetails = getEventDetails(target);
        assertEquals(1, eventDetails.size());
        assertEventWithDetail(eventDetails, "Set an embargo that will expire " + formatDateToUTC(embargoUntil));

        assertMessageSent(pid);
    }

    @Test
    public void skipRolesAndEmbargoes() throws Exception {
        Calendar originalEmbargo = getYearsInTheFuture(1);
        createCollectionInUnit(new AclModelBuilder("Collection with patron and embargo")
                .addCanViewOriginals(AUTHENTICATED_PRINC)
                .addEmbargoUntil(originalEmbargo)
                .model);
        PID pid = collObj.getPid();
        treeIndexer.indexAll(baseAddress);

        PatronAccessDetails accessDetails = new PatronAccessDetails();
        patronService.updatePatronAccess(new PatronAccessAssignmentRequest(agent, pid, accessDetails)
                .withSkipRoles(true)
                .withSkipEmbargo(true));

        RepositoryObject target = repoObjLoader.getRepositoryObject(pid);
        assertHasAssignment(AUTHENTICATED_PRINC, canViewOriginals, target);

        assertHasEmbargo(originalEmbargo.getTime(), target);

        List<String> eventDetails = getEventDetails(target);
        assertEquals(0, eventDetails.size());

        assertMessageNotSent(pid);
    }

    @Test
    public void makeNoChangesToObjectWithNoPatronAccess() throws Exception {
        createCollectionInUnit(null);
        PID pid = collObj.getPid();
        treeIndexer.indexAll(baseAddress);

        PatronAccessDetails accessDetails = new PatronAccessDetails();

        String opId = patronService.updatePatronAccess(
                new PatronAccessAssignmentRequest(agent, pid, accessDetails));
        assertNull(opId);

        RepositoryObject target = repoObjLoader.getRepositoryObject(pid);

        assertNoRoles(target);
        assertNoEmbargo(target);

        List<String> eventDetails = getEventDetails(target);
        assertEquals(0, eventDetails.size());

        assertMessageNotSent(pid);
    }

    @Test
    public void makeNoChangesToObjectWithPatronAccess() throws Exception {
        Calendar originalEmbargo = getYearsInTheFuture(1);
        createCollectionInUnit(new AclModelBuilder("Collection with role and embargo")
                .addCanViewMetadata(AUTHENTICATED_PRINC)
                .addEmbargoUntil(originalEmbargo)
                .model);
        PID pid = collObj.getPid();
        treeIndexer.indexAll(baseAddress);

        PatronAccessDetails accessDetails = new PatronAccessDetails();
        accessDetails.setEmbargo(originalEmbargo.getTime());
        accessDetails.setRoles(asList(
                new RoleAssignment(AUTHENTICATED_PRINC, canViewMetadata)));

        String opId = patronService.updatePatronAccess(
                new PatronAccessAssignmentRequest(agent, pid, accessDetails));
        assertNull(opId);

        RepositoryObject target = repoObjLoader.getRepositoryObject(pid);

        assertHasAssignment(AUTHENTICATED_PRINC, canViewMetadata, target);
        assertHasEmbargo(originalEmbargo.getTime(), target);

        List<String> eventDetails = getEventDetails(target);
        assertEquals(0, eventDetails.size());

        assertMessageNotSent(pid);
    }

    @Test
    public void addPatronRolesToResourceWithStaffRoles() throws Exception {
        createCollectionInUnit(new AclModelBuilder("Collection with patron roles")
                .addCanManage(GRP_PRINC)
                .model);
        PID pid = collObj.getPid();
        treeIndexer.indexAll(baseAddress);

        PatronAccessDetails accessDetails = new PatronAccessDetails();
        accessDetails.setRoles(asList(
                new RoleAssignment(AUTHENTICATED_PRINC, canViewMetadata)));

        patronService.updatePatronAccess(new PatronAccessAssignmentRequest(agent, pid, accessDetails));

        RepositoryObject target = repoObjLoader.getRepositoryObject(pid);
        assertHasAssignment(AUTHENTICATED_PRINC, canViewMetadata, target);
        assertHasAssignment(GRP_PRINC, canManage, target);

        assertNoEmbargo(target);

        List<String> eventDetails = getEventDetails(target);
        assertEquals(1, eventDetails.size());
        assertEventWithDetail(eventDetails, AUTHENTICATED_PRINC + ": " + canViewMetadata.getPropertyString());

        assertMessageSent(pid);
    }

    @Test
    public void addPatronRoleToStaffPrincipal() throws Exception {
        Assertions.assertThrows(InvalidAssignmentException.class, () -> {
            createCollectionInUnit(null);
            PID pid = collObj.getPid();
            treeIndexer.indexAll(baseAddress);

            PatronAccessDetails accessDetails = new PatronAccessDetails();
            accessDetails.setRoles(asList(
                    new RoleAssignment(GRP_PRINC, canViewMetadata)));

            patronService.updatePatronAccess(new PatronAccessAssignmentRequest(agent, pid, accessDetails));
        });
    }

    private void createCollectionInUnit(Model collModel) {
        adminUnit = repoObjFactory.createAdminUnit(null);
        contentRoot.addMember(adminUnit);
        collObj = repoObjFactory.createCollectionObject(collModel);
        adminUnit.addMember(collObj);
    }

    private void assertHasAssignment(String princ, UserRole role, RepositoryObject obj) {
        Resource resc = obj.getResource();
        assertTrue(resc.hasProperty(role.getProperty(), princ),
                "Expected role " + role.name() + " was not assigned for " + princ);
    }

    private void assertNoAssignment(String princ, RepositoryObject obj) {
        Resource resc = obj.getResource();
        assertFalse(obj.getModel().contains(resc, null, princ), "No roles expected for " + princ);
    }

    private void assertNoEmbargo(RepositoryObject obj) {
        Resource resc = obj.getResource();
        assertFalse(resc.hasProperty(CdrAcl.embargoUntil),
                "Unexpect embargo assigned to " + obj.getPid().getId());
    }

    private void assertHasEmbargo(Date expectedEmbargo, RepositoryObject obj) {
        Resource resc = obj.getResource();
        Statement embargoStmt = resc.getProperty(CdrAcl.embargoUntil);
        assertNotNull(embargoStmt, "Embargo was expected by not found");
        Date assigned = ((XSDDateTime) embargoStmt.getLiteral().getValue()).asCalendar().getTime();
        assertEquals(expectedEmbargo, assigned, "Embargo did not match expected value");
    }

    private void assertMessageSent(PID pid) {
        verify(operationsMessageSender).sendOperationMessage(
                eq(USER_PRINC), eq(CDRActions.EDIT_ACCESS_CONTROL), pidListCaptor.capture());
        assertTrue(pidListCaptor.getValue().contains(pid));
    }

    private void assertMessageNotSent(PID pid) {
        verify(operationsMessageSender, never()).sendOperationMessage(
                anyString(), any(CDRActions.class), anyCollectionOf(PID.class));
    }

    private void assertEventWithDetail(List<String> eventDetails, String expected) {
        assertTrue(eventDetails.stream().anyMatch(d -> d.contains(expected)),
                "No event with expected detail '" + expected + "'");
    }

    private List<String> getEventDetails(RepositoryObject repoObj) {
        List<String> details = new ArrayList<>();

        Model eventsModel = repoObj.getPremisLog().getEventsModel();
        Resource objResc = eventsModel.getResource(repoObj.getPid().getRepositoryPath());
        StmtIterator it = eventsModel.listStatements(null, Prov.used, objResc);
        while (it.hasNext()) {
            Statement stmt = it.next();
            Resource eventResc = stmt.getSubject();

            assertTrue(eventResc.hasProperty(RDF.type, Premis.PolicyAssignment), "Event type was not set");
            Resource agentResc = eventResc.getPropertyResourceValue(Premis.hasEventRelatedAgentImplementor);
            assertEquals(AgentPids.forPerson(USER_PRINC).getRepositoryPath(), agentResc.getURI());
            details.add(eventResc.getProperty(Premis.note).getString());
        }

        return details;
    }

    private void assertNoRoles(RepositoryObject obj) {
        Resource resc = obj.getResource();
        StmtIterator it = resc.listProperties();
        while (it.hasNext()) {
            Statement stmt = it.next();
            UserRole role = UserRole.getRoleByProperty(stmt.getPredicate().getURI());
            if (role != null) {
                fail("No roles should be assigned, but " + role + " for " + stmt.getString() + " was present");
            }
        }
    }

    private Calendar getYearsInTheFuture(int numYears) {
        Date dt = new Date();
        Calendar c = Calendar.getInstance();
        c.setTime(dt);
        c.add(Calendar.DATE, 365 * numYears);
        return c;
    }
}
