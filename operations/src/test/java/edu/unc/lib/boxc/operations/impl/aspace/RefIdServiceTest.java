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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
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

    @BeforeEach
    void setUp() {
        closeable = openMocks(this);
        service = new RefIdService();
        service.setRepoObjLoader(repositoryObjectLoader);
        service.setAclService(aclService);
        service.setRepositoryObjectFactory(repositoryObjectFactory);
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
        Assertions.assertThrows(AccessRestrictionException.class, () -> {
            var request = buildRequest();
            doThrow(new AccessRestrictionException("Access Denied")).when(aclService)
                    .assertHasAccess(any(), eq(pid), any(), eq(Permission.editAspaceProperties));
            service.updateRefId(request);
        });
    }

    @Test
    public void testNotAWork() {
        Assertions.assertThrows(InvalidOperationForObjectType.class, () -> {
            var fileObject = mock(FileObject.class);
            when(repositoryObjectLoader.getRepositoryObject(eq(pid))).thenReturn(fileObject);
            var request = buildRequest();
            service.updateRefId(request);
        });
    }

    @Test
    public void testUpdateRefId() {
        var workObject = mock(WorkObject.class);
        var request = buildRequest();
        when(repositoryObjectLoader.getRepositoryObject(pid)).thenReturn(workObject);

        service.updateRefId(request);
        verify(repositoryObjectFactory).createExclusiveRelationship(
                eq(workObject), eq(CdrAspace.refId), eq(REF_ID));
    }

    private RefIdRequest buildRequest() {
        var request = new RefIdRequest();
        request.setPidString(pidString);
        request.setAgent(agent);
        request.setRefId(REF_ID);
        return request;
    }
}
