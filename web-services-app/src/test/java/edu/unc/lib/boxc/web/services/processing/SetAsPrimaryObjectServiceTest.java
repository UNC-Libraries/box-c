package edu.unc.lib.boxc.web.services.processing;

import static edu.unc.lib.boxc.auth.api.Permission.editResourceType;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Collection;
import java.util.UUID;

import org.apache.jena.rdf.model.Resource;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.model.api.exceptions.InvalidOperationForObjectType;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.FolderObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.jms.OperationsMessageSender;

/**
 *
 * @author harring
 *
 */
public class SetAsPrimaryObjectServiceTest {

    @Mock
    private AccessControlService aclService;
    @Mock
    private RepositoryObjectLoader repoObjLoader;
    @Mock
    private OperationsMessageSender messageSender;
    @Mock
    private AgentPrincipals agent;
    @Mock
    private AccessGroupSet groups;
    @Mock
    private WorkObject workObj;
    @Mock
    private FileObject fileObj;
    @Mock
    private FolderObject folderObj;
    @Mock
    private RepositoryObjectFactory factory;
    @Mock
    private Resource primaryResc;
    @Captor
    private ArgumentCaptor<Collection<PID>> pidsCaptor;

    private PID fileObjPid;
    private PID folderObjPid;
    private PID workObjPid;
    private SetAsPrimaryObjectService service;

    @Before
    public void init() {
        initMocks(this);

        fileObjPid = makePid();
        folderObjPid = makePid();
        workObjPid = makePid();

        when(workObj.getPid()).thenReturn(workObjPid);
        when(fileObj.getPid()).thenReturn(fileObjPid);
        when(folderObj.getPid()).thenReturn(folderObjPid);

        when(agent.getPrincipals()).thenReturn(groups);
        when(repoObjLoader.getRepositoryObject(eq(fileObjPid))).thenReturn(fileObj);
        when(repoObjLoader.getRepositoryObject(eq(workObjPid))).thenReturn(workObj);

        service = new SetAsPrimaryObjectService();
        service.setAclService(aclService);
        service.setRepositoryObjectLoader(repoObjLoader);
        service.setOperationsMessageSender(messageSender);
    }

    @Test
    public void setFileObjectAsPrimaryTest() {
        when(fileObj.getParent()).thenReturn(workObj);

        service.setAsPrimaryObject(agent, fileObjPid);

        verify(workObj).setPrimaryObject(fileObjPid);
        verify(messageSender).sendSetAsPrimaryObjectOperation(isNull(), pidsCaptor.capture());

        Collection<PID> collections = pidsCaptor.getValue();
        assertEquals(collections.size(), 2);
        assertTrue(collections.contains(fileObjPid));
        assertTrue(collections.contains(fileObj.getParent().getPid()));
    }

    @Test(expected = AccessRestrictionException.class)
    public void insufficientAccessTest() {
        doThrow(new AccessRestrictionException()).when(aclService)
        .assertHasAccess(anyString(), eq(fileObjPid), any(), eq(editResourceType));

        service.setAsPrimaryObject(agent, fileObjPid);
    }

    @Test(expected = InvalidOperationForObjectType.class)
    public void setNonFileAsPrimaryTest() {
        when(repoObjLoader.getRepositoryObject(eq(folderObjPid))).thenReturn(folderObj);

        service.setAsPrimaryObject(agent, folderObjPid);
    }

    @Test(expected = InvalidOperationForObjectType.class)
    public void setPrimaryObjectOnNonWork() {
        when(fileObj.getParent()).thenReturn(folderObj);

        service.setAsPrimaryObject(agent, fileObjPid);
    }

    @Test
    public void testClearPrimaryObject() {
        when(fileObj.getParent()).thenReturn(workObj);
        when(workObj.getPrimaryObject()).thenReturn(fileObj);

        service.clearPrimaryObject(agent, fileObjPid);

        verify(workObj).clearPrimaryObject();
        verify(messageSender).sendSetAsPrimaryObjectOperation(isNull(), pidsCaptor.capture());

        Collection<PID> collections = pidsCaptor.getValue();
        assertEquals(collections.size(), 2);
        assertTrue(collections.contains(fileObjPid));
        assertTrue(collections.contains(fileObj.getParent().getPid()));
    }

    @Test
    public void testClearPrimaryObjectViaWork() {
        when(fileObj.getParent()).thenReturn(workObj);
        when(workObj.getPrimaryObject()).thenReturn(fileObj);

        service.clearPrimaryObject(agent, workObjPid);

        verify(workObj).clearPrimaryObject();
        verify(messageSender).sendSetAsPrimaryObjectOperation(isNull(), pidsCaptor.capture());

        Collection<PID> collections = pidsCaptor.getValue();
        assertEquals(collections.size(), 2);
        assertTrue(collections.contains(fileObjPid));
        assertTrue(collections.contains(workObjPid));
    }

    @Test
    public void testClearPrimaryObjectNoPrimarySet() {
        when(fileObj.getParent()).thenReturn(workObj);

        service.clearPrimaryObject(agent, workObjPid);

        verify(messageSender).sendSetAsPrimaryObjectOperation(isNull(), pidsCaptor.capture());

        Collection<PID> collections = pidsCaptor.getValue();
        assertEquals(collections.size(), 1);
        assertTrue(collections.contains(workObjPid));
    }

    @Test(expected = AccessRestrictionException.class)
    public void testClearPrimaryObjectAccessRestriction() {
        doThrow(new AccessRestrictionException()).when(aclService)
            .assertHasAccess(anyString(), eq(workObjPid), any(), eq(editResourceType));

        when(fileObj.getParent()).thenReturn(workObj);

        service.clearPrimaryObject(agent, fileObjPid);
    }

    private PID makePid() {
        return PIDs.get(UUID.randomUUID().toString());
    }

}
