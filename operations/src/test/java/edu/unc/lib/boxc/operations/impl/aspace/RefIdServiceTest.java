package edu.unc.lib.boxc.operations.impl.aspace;

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
import edu.unc.lib.boxc.model.api.rdf.CdrAspace;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.model.fcrepo.test.TestHelper;
import edu.unc.lib.boxc.operations.jms.aspace.RefIdRequest;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingActionType;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingMessageSender;
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

public class RefIdServiceTest {
    private static final String REF_ID = "2817ec3c77e5ea9846d5c070d58d402b";
    private RefIdService service;
    private PID pid;
    private String pidString;
    private AutoCloseable closeable;
    @Mock
    private AgentPrincipals agent;
    @Mock
    private AccessGroupSet accessGroupSet;
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
        service = new RefIdService();
        service.setRepoObjLoader(repositoryObjectLoader);
        service.setAclService(aclService);
        service.setRepositoryObjectFactory(repositoryObjectFactory);
        service.setIndexingMessageSender(indexingMessageSender);
        pid = TestHelper.makePid();
        pidString = pid.getId();
        when(agent.getPrincipals()).thenReturn(accessGroupSet);
    }

    @AfterEach
    public void after() throws Exception {
        closeable.close();
    }

    @Test
    public void testNoPermission() {
        var request = buildRequest(REF_ID);
        doThrow(new AccessRestrictionException("Access Denied")).when(aclService)
                .assertHasAccess(any(), eq(pid), any(), eq(Permission.editAspaceProperties));
        Assertions.assertThrows(AccessRestrictionException.class, () -> {
            service.updateRefId(request);
        });
    }

    @Test
    public void testNotAWork() {
        var fileObject = mock(FileObject.class);
        when(repositoryObjectLoader.getRepositoryObject(eq(pid))).thenReturn(fileObject);
        var request = buildRequest(REF_ID);
        Assertions.assertThrows(InvalidOperationForObjectType.class, () -> {
            service.updateRefId(request);
        });
    }

//    @Test
//    public void testUpdateCurrentRefIdWithIdenticalRefId() {
//        var workObject = mock(WorkObject.class);
//        var request = buildRequest();
//        var username = "number one user";
//        when(repositoryObjectLoader.getRepositoryObject(pid)).thenReturn(workObject);
//        when(agent.getUsername()).thenReturn(username);
//          service.updateRefId(request);
//        verify(repositoryObjectFactory, never()).createExclusiveRelationship(
//                eq(workObject), eq(CdrAspace.refId), eq(REF_ID));
//        verify(indexingMessageSender, never()).sendIndexingOperation(eq(username),
//                eq(pid), eq(IndexingActionType.ADD_ASPACE_REF_ID));
//    }
    @Test
    public void testUpdateCurrentRefIdWithBlankRefId() {
        var workObject = mock(WorkObject.class);
        var request = buildRequest("");
        var username = "number one user";
        when(repositoryObjectLoader.getRepositoryObject(pid)).thenReturn(workObject);
        when(agent.getUsername()).thenReturn(username);

        service.updateRefId(request);
        verify(repositoryObjectFactory).deleteProperty(eq(workObject), eq(CdrAspace.refId));
        // indexing action should be removing the aspace ref ID
//        verify(indexingMessageSender).sendIndexingOperation(eq(username),
//                eq(pid), eq(IndexingActionType.ADD_ASPACE_REF_ID));
    }
    @Test
    public void testUpdateNonExistentRefIdWithBlankRefId() {
        var workObject = mock(WorkObject.class);
        var request = buildRequest("");
        var username = "number one user";
        when(repositoryObjectLoader.getRepositoryObject(pid)).thenReturn(workObject);
        when(agent.getUsername()).thenReturn(username);

        service.updateRefId(request);
        verify(repositoryObjectFactory, never()).createExclusiveRelationship(
                eq(workObject), eq(CdrAspace.refId), eq(REF_ID));
        verify(indexingMessageSender, never()).sendIndexingOperation(eq(username),
                eq(pid), eq(IndexingActionType.UPDATE_ASPACE_REF_ID));
    }

    @Test
    public void testUpdateRefIdSuccess() {
        var workObject = mock(WorkObject.class);
        var request = buildRequest(REF_ID);
        var username = "number one user";
        when(repositoryObjectLoader.getRepositoryObject(pid)).thenReturn(workObject);
        when(agent.getUsername()).thenReturn(username);

        service.updateRefId(request);
        verify(repositoryObjectFactory).createExclusiveRelationship(
                eq(workObject), eq(CdrAspace.refId), eq(REF_ID));
        verify(indexingMessageSender).sendIndexingOperation(eq(username),
                eq(pid), eq(IndexingActionType.UPDATE_ASPACE_REF_ID));
    }

    private RefIdRequest buildRequest(String refId) {
        var request = new RefIdRequest();
        request.setPidString(pidString);
        request.setAgent(agent);
        request.setRefId(refId);
        return request;
    }
}
