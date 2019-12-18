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
package edu.unc.lib.dl.persist.services.edit;

import static edu.unc.lib.dl.model.DatastreamPids.getMdDescriptivePid;
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

import java.io.InputStream;
import java.net.URI;
import java.util.Collection;
import java.util.UUID;

import org.apache.activemq.util.ByteArrayInputStream;
import org.apache.tika.io.IOUtils;
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
import edu.unc.lib.dl.fcrepo4.BinaryObject;
import edu.unc.lib.dl.fcrepo4.ContentObject;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.persist.services.storage.StorageLocation;
import edu.unc.lib.dl.persist.services.storage.StorageLocationManager;
import edu.unc.lib.dl.persist.services.transfer.BinaryTransferService;
import edu.unc.lib.dl.persist.services.transfer.BinaryTransferSession;
import edu.unc.lib.dl.services.OperationsMessageSender;
import edu.unc.lib.dl.validation.MODSValidator;
import edu.unc.lib.dl.validation.MetadataValidationException;

/**
 *
 * @author harring
 *
 */
public class UpdateDescriptionServiceTest {

    private static final String FILE_CONTENT = "Some content";

    @Mock
    private AccessControlService aclService;
    @Mock
    private RepositoryObjectLoader repoObjLoader;
    @Mock
    private OperationsMessageSender messageSender;
    @Mock
    private MODSValidator modsValidator;
    @Mock
    private StorageLocationManager locationManager;
    @Mock
    private StorageLocation destination;
    @Mock
    private BinaryTransferService transferService;
    @Mock
    private BinaryTransferSession transferSession;

    @Mock
    private AgentPrincipals agent;
    @Mock
    private AccessGroupSet groups;
    @Mock
    private ContentObject obj;

    private URI transferredUri;

    @Captor
    private ArgumentCaptor<InputStream> inputStreamCaptor;
    @Captor
    private ArgumentCaptor<Collection<PID>> pidsCaptor;

    private UpdateDescriptionService service;
    private PID objPid;
    private PID modsPid;
    private InputStream modsStream;

    @SuppressWarnings("unchecked")
    @Before
    public void init() throws Exception{
        initMocks(this);

        when(agent.getPrincipals()).thenReturn(groups);
        when(agent.getUsername()).thenReturn("username");
        when(repoObjLoader.getRepositoryObject(any(PID.class))).thenReturn(obj);
        when(messageSender.sendUpdateDescriptionOperation(anyString(), any(Collection.class)))
                .thenReturn("message_id");

        objPid = PIDs.get(UUID.randomUUID().toString());
        modsPid = getMdDescriptivePid(objPid);
        modsStream = new ByteArrayInputStream(FILE_CONTENT.getBytes());
        transferredUri = URI.create("file:///path/to/transferred/file.txt");

        when(obj.setDescription(any(URI.class))).thenReturn(mock(BinaryObject.class));
        when(obj.getPid()).thenReturn(objPid);

        when(transferSession.transferReplaceExisting(modsPid, modsStream)).thenReturn(transferredUri);

        service = new UpdateDescriptionService();
        service.setAclService(aclService);
        service.setRepositoryObjectLoader(repoObjLoader);
        service.setOperationsMessageSender(messageSender);
        service.setModsValidator(modsValidator);
        service.setLocationManager(locationManager);
        service.setTransferService(transferService);
    }

    @Test
    public void updateDescriptionTest() throws Exception {
        when(locationManager.getStorageLocation(obj)).thenReturn(destination);
        when(transferService.getSession(destination)).thenReturn(transferSession);

        service.updateDescription(agent, objPid, modsStream);

        verify(transferSession).transferReplaceExisting(eq(modsPid), inputStreamCaptor.capture());
        assertEquals(FILE_CONTENT, IOUtils.toString(inputStreamCaptor.getValue()));

        verify(obj).setDescription(transferredUri);

        assertMessageSent();

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

    @Test
    public void invalidModsValidationOff() throws Exception {
        service.setValidate(false);
        doThrow(new MetadataValidationException()).when(modsValidator).validate(any(InputStream.class));

        service.updateDescription(transferSession, agent, objPid, modsStream);
    }

    @Test
    public void updateDescriptionProvidedSession() throws Exception {
        service.updateDescription(transferSession, agent, objPid, modsStream);

        verify(transferSession).transferReplaceExisting(eq(modsPid), inputStreamCaptor.capture());
        assertEquals(FILE_CONTENT, IOUtils.toString(inputStreamCaptor.getValue()));

        verify(obj).setDescription(transferredUri);

        assertMessageSent();
    }

    private void assertMessageSent() {
        verify(messageSender).sendUpdateDescriptionOperation(anyString(), pidsCaptor.capture());
        Collection<PID> pids = pidsCaptor.getValue();
        assertEquals(1, pids.size());
        assertTrue(pids.contains(objPid));
    }
}
