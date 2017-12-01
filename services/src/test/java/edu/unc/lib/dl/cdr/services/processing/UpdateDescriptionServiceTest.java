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

import static edu.unc.lib.dl.acl.util.Permission.viewMetadata;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
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
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.services.OperationsMessageSender;

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
    private AgentPrincipals agent;
    @Mock
    private AccessGroupSet groups;

    @Captor
    private ArgumentCaptor<Collection<PID>> pidsCaptor;

    private UpdateDescriptionService service;
    private PID objectPid;
    private InputStream modsStream;

    @SuppressWarnings("unchecked")
    @Before
    public void init() throws Exception{
        initMocks(this);

        when(agent.getPrincipals()).thenReturn(groups);
        when(messageSender.sendUpdateDescriptionOperation(anyString(), any(Collection.class)))
                .thenReturn("message_id");

        objectPid = PIDs.get(UUID.randomUUID().toString());
        modsStream = new FileInputStream(new File("txt.txt"));

        service = new UpdateDescriptionService();
        service.setAclService(aclService);
        service.setRepositoryObjectLoader(repoObjLoader);
        service.setOperationsMessageSender(messageSender);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void updateDescriptionTest() throws Exception {
        service.updateDescription(agent, objectPid, modsStream);

        verify(messageSender).sendUpdateDescriptionOperation(anyString(), pidsCaptor.capture());
        Collection<PID> pids = pidsCaptor.getValue();
        assertEquals(pids.size(), 1);
        assertTrue(pids.contains(objectPid));
    }

    @Test(expected = AccessRestrictionException.class)
    public void insufficientAccessTest() {
        doThrow(new AccessRestrictionException()).when(aclService)
        .assertHasAccess(anyString(), eq(objectPid), any(AccessGroupSet.class), eq(viewMetadata));
    }

}
