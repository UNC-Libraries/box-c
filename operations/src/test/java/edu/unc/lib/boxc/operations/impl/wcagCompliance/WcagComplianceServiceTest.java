package edu.unc.lib.boxc.operations.impl.wcagCompliance;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.model.api.exceptions.InvalidOperationForObjectType;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.rdf.CdrAspace;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.model.fcrepo.test.TestHelper;
import edu.unc.lib.boxc.operations.jms.aspace.RefIdRequest;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingActionType;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingMessageSender;
import edu.unc.lib.boxc.operations.jms.wcagCompliance.WcagComplianceRequest;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

public class WcagComplianceServiceTest {
    private static final String LEVEL = "WCAG 1.0 Level A";
    private WcagComplianceService service;
    private PID pid;
    private String pidString;
    private AutoCloseable closeable;
    private String username = "number one user";
    @Mock
    private AgentPrincipals agent;
    @Mock
    private AccessGroupSet accessGroupSet;
    @Mock
    private FileObject fileObject;
    @Mock
    private Resource resource;
    @Mock
    private Statement statement;
    @Mock
    private AccessControlService aclService;
    @Mock
    private RepositoryObjectLoader repositoryObjectLoader;
    @Mock
    private RepositoryObjectFactory repositoryObjectFactory;
    @Mock
    private IndexingMessageSender indexingMessageSender;

    @BeforeEach
    void setUp() {
        closeable = openMocks(this);
        service = new WcagComplianceService();
        service.setAclService(aclService);
        service.setRepoObjLoader(repositoryObjectLoader);
        service.setRepositoryObjectFactory(repositoryObjectFactory);
        service.setIndexingMessageSender(indexingMessageSender);
        pid = TestHelper.makePid();
        pidString = pid.getId();
        when(agent.getPrincipals()).thenReturn(accessGroupSet);
        when(repositoryObjectLoader.getRepositoryObject(pid)).thenReturn(fileObject);
        when(fileObject.getResource()).thenReturn(resource);
        when(resource.getProperty(any())).thenReturn(statement);
        when(agent.getUsername()).thenReturn(username);
    }

    @AfterEach
    public void after() throws Exception {
        closeable.close();
    }

    @Test
    public void testNoPermission() {
        var request = buildRequest(LEVEL);
        doThrow(new AccessRestrictionException("Access Denied")).when(aclService)
                .assertHasAccess(any(), eq(pid), any(), eq(Permission.editResourceType));
        Assertions.assertThrows(AccessRestrictionException.class, () -> {
            service.updateWcagCompliance(request);
        });
    }

    @Test
    public void testNotAWork() {
        var workObject = mock(WorkObject.class);
        when(repositoryObjectLoader.getRepositoryObject(eq(pid))).thenReturn(workObject);
        var request = buildRequest(LEVEL);
        Assertions.assertThrows(InvalidOperationForObjectType.class, () -> {
            service.updateWcagCompliance(request);
        });
    }

    @Test
    public void testInvalidLevelValueRequested() {
        var request = buildRequest("WCAG Best Level Ever");
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            service.updateWcagCompliance(request);
        });
    }

    @Test
    public void testUpdateCurrentLevelWithIdenticalLevel() {
        var request = buildRequest(LEVEL);
        when(statement.getString()).thenReturn(LEVEL);

        service.updateWcagCompliance(request);
        verify(repositoryObjectFactory, never()).createExclusiveRelationship(
                eq(fileObject), eq(Cdr.wcagCompliance), eq(LEVEL));
        verify(indexingMessageSender, never()).sendIndexingOperation(eq(username),
                eq(pid), eq(IndexingActionType.ADD));
    }

    @Test
    public void testUpdateCurrentLevelWithBlankLevel() {
        var request = buildRequest("");
        when(statement.getString()).thenReturn(LEVEL);

        service.updateWcagCompliance(request);
        verify(repositoryObjectFactory).deleteProperty(eq(fileObject), eq(Cdr.wcagCompliance));
        verify(repositoryObjectFactory, never()).createExclusiveRelationship(
                eq(fileObject), eq(Cdr.wcagCompliance), eq(LEVEL));
        verify(indexingMessageSender).sendIndexingOperation(eq(username),
                eq(pid), eq(IndexingActionType.ADD));
    }

    @Test
    public void testUpdateEmptyStringLevelWithBlankLevel() {
        var request = buildRequest("");
        when(statement.getString()).thenReturn("");

        service.updateWcagCompliance(request);
        verify(repositoryObjectFactory).deleteProperty(eq(fileObject), eq(Cdr.wcagCompliance));
        verify(repositoryObjectFactory, never()).createExclusiveRelationship(
                eq(fileObject), eq(Cdr.wcagCompliance), eq(LEVEL));
        verify(indexingMessageSender).sendIndexingOperation(eq(username),
                eq(pid), eq(IndexingActionType.ADD));
    }

    @Test
    public void testUpdateNonExistentLevelWithBlankLevel() {
        var request = buildRequest("");
        when(statement.getString()).thenReturn(null);

        service.updateWcagCompliance(request);
        verify(repositoryObjectFactory, never()).createExclusiveRelationship(
                eq(fileObject), eq(Cdr.wcagCompliance), eq(LEVEL));
        verify(indexingMessageSender, never()).sendIndexingOperation(eq(username),
                eq(pid), eq(IndexingActionType.ADD));
    }

    @Test
    public void testUpdateLevelSuccess() {
        when(resource.getProperty(any())).thenReturn(null);
        var request = buildRequest(LEVEL);

        service.updateWcagCompliance(request);
        verify(repositoryObjectFactory).createExclusiveRelationship(
                eq(fileObject), eq(Cdr.wcagCompliance), eq(LEVEL));
        verify(indexingMessageSender).sendIndexingOperation(eq(username),
                eq(pid), eq(IndexingActionType.ADD));

    }

    private WcagComplianceRequest buildRequest(String level) {
        var request = new WcagComplianceRequest();
        request.setPidString(pidString);
        request.setAgent(agent);
        request.setLevel(level);
        return request;
    }
}
