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

import edu.unc.lib.dl.acl.exception.AccessRestrictionException;
import edu.unc.lib.dl.acl.service.AccessControlService;
import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.AgentPrincipals;
import edu.unc.lib.dl.fcrepo4.FileObject;
import edu.unc.lib.dl.fcrepo4.FolderObject;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectFactory;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;
import edu.unc.lib.dl.fcrepo4.WorkObject;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.model.InvalidOperationForObjectType;
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
    private SetAsPrimaryObjectService service;

    @Before
    public void init() {
        initMocks(this);

        fileObjPid = makePid();
        folderObjPid = makePid();

        when(agent.getPrincipals()).thenReturn(groups);
        when(repoObjLoader.getRepositoryObject(eq(fileObjPid))).thenReturn(fileObj);

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

    private PID makePid() {
        return PIDs.get(UUID.randomUUID().toString());
    }

}
