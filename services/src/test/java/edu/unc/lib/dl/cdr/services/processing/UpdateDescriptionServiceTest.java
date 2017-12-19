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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Collection;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import edu.unc.lib.dl.acl.exception.AccessRestrictionException;
import edu.unc.lib.dl.acl.service.AccessControlService;
import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.AgentPrincipals;
import edu.unc.lib.dl.acl.util.Permission;
import edu.unc.lib.dl.fcrepo4.ContentObject;
import edu.unc.lib.dl.fcrepo4.FileObject;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.services.OperationsMessageSender;
import edu.unc.lib.dl.validation.MODSValidator;
import edu.unc.lib.dl.validation.MetadataValidationException;

/**
 *
 * @author harring
 *
 */
public class UpdateDescriptionServiceTest {

    @Mock
    private AccessControlService aclService;
    @Mock
    private RepositoryObjectLoader repoObjLoader;
    @Mock
    private OperationsMessageSender messageSender;
    @Mock
    private MODSValidator modsValidator;

    @Mock
    private AgentPrincipals agent;
    @Mock
    private AccessGroupSet groups;
    @Mock
    private ContentObject obj;

    @Captor
    private ArgumentCaptor<Collection<PID>> pidsCaptor;

    private UpdateDescriptionService service;
    private PID objPid;
    private InputStream modsStream;

    @SuppressWarnings("unchecked")
    @Before
    public void init() throws Exception{
        initMocks(this);

        when(agent.getPrincipals()).thenReturn(groups);
        when(agent.getUsername()).thenReturn("username");
        when(repoObjLoader.getRepositoryObject(any(PID.class))).thenReturn(obj);
        when(obj.addDescription(eq(modsStream))).thenReturn(mock(FileObject.class));
        when(messageSender.sendUpdateDescriptionOperation(anyString(), any(Collection.class)))
                .thenReturn("message_id");

        objPid = PIDs.get(UUID.randomUUID().toString());
        modsStream = new FileInputStream(new File("src/test/resources/txt.txt"));

        service = new UpdateDescriptionService();
        service.setAclService(aclService);
        service.setRepositoryObjectLoader(repoObjLoader);
        service.setOperationsMessageSender(messageSender);
        service.setModsValidator(modsValidator);
    }

    @Test
    public void updateDescriptionTest() throws Exception {
        service.updateDescription(agent, objPid, modsStream);

        verify(messageSender).sendUpdateDescriptionOperation(anyString(), pidsCaptor.capture());
        Collection<PID> pids = pidsCaptor.getValue();
        assertEquals(pids.size(), 1);
        assertTrue(pids.contains(objPid));
    }

    @Test(expected = AccessRestrictionException.class)
    public void insufficientAccessTest() throws Exception {
        doThrow(new AccessRestrictionException()).when(aclService)
                .assertHasAccess(anyString(), eq(objPid), any(AccessGroupSet.class), any(Permission.class));

        service.updateDescription(agent, objPid, modsStream);
    }

    @Test(expected = MetadataValidationException.class)
    public void invalidModsTest() throws Exception {
        doThrow(new MetadataValidationException()).when(modsValidator).validate(any(InputStream.class));

        service.updateDescription(agent, objPid, modsStream);
    }

}
