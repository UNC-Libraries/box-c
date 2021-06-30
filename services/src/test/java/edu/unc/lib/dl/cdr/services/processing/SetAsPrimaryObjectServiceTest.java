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
package edu.unc.lib.dl.cdr.services.processing;

import static edu.unc.lib.dl.acl.util.Permission.editResourceType;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
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

import edu.unc.lib.boxc.model.api.exceptions.InvalidOperationForObjectType;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.model.fcrepo.objects.FileObjectImpl;
import edu.unc.lib.boxc.model.fcrepo.objects.FolderObjectImpl;
import edu.unc.lib.boxc.model.fcrepo.objects.WorkObjectImpl;
import edu.unc.lib.dl.acl.exception.AccessRestrictionException;
import edu.unc.lib.dl.acl.service.AccessControlService;
import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.AgentPrincipals;
import edu.unc.lib.dl.services.OperationsMessageSender;

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
    private WorkObjectImpl workObj;
    @Mock
    private FileObjectImpl fileObj;
    @Mock
    private FolderObjectImpl folderObj;
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
        verify(messageSender).sendSetAsPrimaryObjectOperation(anyString(), pidsCaptor.capture());

        Collection<PID> collections = pidsCaptor.getValue();
        assertEquals(collections.size(), 2);
        assertTrue(collections.contains(fileObjPid));
        assertTrue(collections.contains(fileObj.getParent().getPid()));
    }

    @Test(expected = AccessRestrictionException.class)
    public void insufficientAccessTest() {
        doThrow(new AccessRestrictionException()).when(aclService)
        .assertHasAccess(anyString(), eq(fileObjPid), any(AccessGroupSet.class), eq(editResourceType));

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
        verify(messageSender).sendSetAsPrimaryObjectOperation(anyString(), pidsCaptor.capture());

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
        verify(messageSender).sendSetAsPrimaryObjectOperation(anyString(), pidsCaptor.capture());

        Collection<PID> collections = pidsCaptor.getValue();
        assertEquals(collections.size(), 2);
        assertTrue(collections.contains(fileObjPid));
        assertTrue(collections.contains(workObjPid));
    }

    @Test
    public void testClearPrimaryObjectNoPrimarySet() {
        when(fileObj.getParent()).thenReturn(workObj);

        service.clearPrimaryObject(agent, workObjPid);

        verify(messageSender).sendSetAsPrimaryObjectOperation(anyString(), pidsCaptor.capture());

        Collection<PID> collections = pidsCaptor.getValue();
        assertEquals(collections.size(), 1);
        assertTrue(collections.contains(workObjPid));
    }

    @Test(expected = AccessRestrictionException.class)
    public void testClearPrimaryObjectAccessRestriction() {
        doThrow(new AccessRestrictionException()).when(aclService)
            .assertHasAccess(anyString(), eq(workObjPid), any(AccessGroupSet.class), eq(editResourceType));

        when(fileObj.getParent()).thenReturn(workObj);

        service.clearPrimaryObject(agent, fileObjPid);
    }

    private PID makePid() {
        return PIDs.get(UUID.randomUUID().toString());
    }

}
