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

import static edu.unc.lib.dl.acl.util.Permission.ingest;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import edu.unc.lib.dl.acl.exception.AccessRestrictionException;
import edu.unc.lib.dl.acl.service.AccessControlService;
import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.AgentPrincipals;
import edu.unc.lib.dl.event.PremisEventBuilder;
import edu.unc.lib.dl.event.PremisLogger;
import edu.unc.lib.dl.fcrepo4.CollectionObject;
import edu.unc.lib.dl.fcrepo4.ContentContainerObject;
import edu.unc.lib.dl.fcrepo4.FedoraTransaction;
import edu.unc.lib.dl.fcrepo4.FolderObject;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectFactory;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;
import edu.unc.lib.dl.fcrepo4.TransactionCancelledException;
import edu.unc.lib.dl.fcrepo4.TransactionManager;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.rdf.Premis;
import edu.unc.lib.dl.test.SelfReturningAnswer;

/**
 *
 * @author harring
 *
 */
public class AddContainerServiceTest {

    @Mock
    private AccessControlService aclService;
    @Mock
    private RepositoryObjectLoader repoObjLoader;
    @Mock
    private RepositoryObjectFactory repoObjFactory;
    @Mock
    private TransactionManager txManager;
    @Mock
    private FedoraTransaction tx;
    @Mock
    private ContentContainerObject childContainer;
    @Mock
    private AgentPrincipals agent;
    @Mock
    private AccessGroupSet groups;
    @Mock
    private PremisLogger premisLogger;

    private PremisEventBuilder eventBuilder;
    private PID parentPid;
    private AddContainerService service;

    @Before
    public void init() {
        initMocks(this);

        when(agent.getPrincipals()).thenReturn(groups);

        eventBuilder = mock(PremisEventBuilder.class, new SelfReturningAnswer());
        when(childContainer.getPremisLog()).thenReturn(premisLogger);
        when(premisLogger.buildEvent(eq(Premis.Creation))).thenReturn(eventBuilder);

        when(txManager.startTransaction()).thenReturn(tx);

        doThrow(new TransactionCancelledException()).when(tx).cancel();

        parentPid = PIDs.get(UUID.randomUUID().toString());

        service = new AddContainerService();
        service.setAclService(aclService);
        service.setRepositoryObjectFactory(repoObjFactory);
        service.setRepositoryObjectLoader(repoObjLoader);
        service.setTransactionManager(txManager);
    }

    @Test(expected = TransactionCancelledException.class)
    public void insufficientAccessTest() {
        FolderObject folder = mock(FolderObject.class);
        when(repoObjLoader.getRepositoryObject(eq(parentPid))).thenReturn(folder);
        doThrow(new AccessRestrictionException()).when(aclService)
                .assertHasAccess(anyString(), eq(parentPid), any(AccessGroupSet.class), eq(ingest));

        try {
            service.addContainer(agent, parentPid, Cdr.Folder);
        } catch (TransactionCancelledException e) {
            assertEquals(AccessRestrictionException.class, e.getCause().getClass());
        }
    }

    @Test(expected = TransactionCancelledException.class)
    public void invalidObjectTypeTest() {
        CollectionObject collection = mock(CollectionObject.class);
        when(repoObjLoader.getRepositoryObject(eq(parentPid))).thenReturn(collection);
        doThrow(new TransactionCancelledException()).when(tx).cancel();

        service.addContainer(agent, parentPid, Cdr.AdminUnit);
    }

    @Test
    public void addCollectionToAdminUnitTest() {

        verify(premisLogger).buildEvent(eq(Premis.Creation));
        verify(eventBuilder).write();
    }
}
