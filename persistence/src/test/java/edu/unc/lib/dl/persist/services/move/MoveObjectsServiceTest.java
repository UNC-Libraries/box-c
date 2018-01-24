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
package edu.unc.lib.dl.persist.services.move;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.isA;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.net.URI;
import java.util.UUID;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Resource;
import org.fcrepo.client.DeleteBuilder;
import org.fcrepo.client.FcrepoClient;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import edu.unc.lib.dl.acl.exception.AccessRestrictionException;
import edu.unc.lib.dl.acl.service.AccessControlService;
import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.AgentPrincipals;
import edu.unc.lib.dl.acl.util.Permission;
import edu.unc.lib.dl.fcrepo4.ContentContainerObject;
import edu.unc.lib.dl.fcrepo4.ContentObject;
import edu.unc.lib.dl.fcrepo4.FedoraTransaction;
import edu.unc.lib.dl.fcrepo4.FileObject;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;
import edu.unc.lib.dl.fcrepo4.TransactionCancelledException;
import edu.unc.lib.dl.fcrepo4.TransactionManager;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.sparql.SparqlQueryService;

/**
 *
 * @author bbpennel
 *
 */
public class MoveObjectsServiceTest {

    @Mock
    private AccessControlService aclService;
    @Mock
    private RepositoryObjectLoader repositoryObjectLoader;
    @Mock
    private TransactionManager transactionManager;
    @Mock
    private SparqlQueryService sparqlQueryService;
    @Mock
    private FcrepoClient fcrepoClient;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private PID destPid;
    @Mock
    private ContentContainerObject mockDestObj;
    @Mock
    private AccessGroupSet mockAccessSet;
    @Mock
    private AgentPrincipals mockAgent;
    @Mock
    private FedoraTransaction mockTx;
    @Mock
    private QueryExecution mockQueryExec;
    @Mock
    private ResultSet mockResultSet;
    @Mock
    private QuerySolution mockQuerySolution;
    @Mock
    private Resource mockResource;
    @Mock
    private DeleteBuilder mockDeleteBuilder;

    private MoveObjectsService service;

    @Before
    public void init() throws Exception {
        initMocks(this);

        service = new MoveObjectsService();
        service.setAclService(aclService);
        service.setFcrepoClient(fcrepoClient);
        service.setRepositoryObjectLoader(repositoryObjectLoader);
        service.setSparqlQueryService(sparqlQueryService);
        service.setTransactionManager(transactionManager);

        destPid = makePid();
        when(repositoryObjectLoader.getRepositoryObject(destPid)).thenReturn(mockDestObj);

        when(transactionManager.startTransaction()).thenReturn(mockTx);
        doAnswer(new Answer<Exception>() {
            @Override
            public Exception answer(InvocationOnMock invocation) throws Throwable {
                throw new TransactionCancelledException("", invocation.getArgumentAt(0, Exception.class));
            }
        }).when(mockTx).cancel(any(Exception.class));

        when(sparqlQueryService.executeQuery(anyString())).thenReturn(mockQueryExec);
        when(mockQueryExec.execSelect()).thenReturn(mockResultSet);
        when(mockResultSet.nextSolution()).thenReturn(mockQuerySolution);
        when(mockQuerySolution.getResource(anyString())).thenReturn(mockResource);

        when(fcrepoClient.delete(any(URI.class))).thenReturn(mockDeleteBuilder);
    }

    @Test(expected = AccessRestrictionException.class)
    public void testNoPermissionOnDestination() throws Exception {
        doThrow(new AccessRestrictionException()).when(aclService)
                .assertHasAccess(anyString(), eq(destPid), any(AccessGroupSet.class), eq(Permission.move));

        service.moveObjects(mockAgent, destPid, asList(makePid()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDestinationInvalidType() {
        FileObject destFile = mock(FileObject.class);
        when(repositoryObjectLoader.getRepositoryObject(destPid)).thenReturn(destFile);

        service.moveObjects(mockAgent, destPid, asList(makePid()));
    }

    @Test
    public void testNoPermissionToMoveObject() throws Exception {
        expectedException.expectCause(isA(AccessRestrictionException.class));

        PID movePid = makePid();

        doThrow(new AccessRestrictionException()).when(aclService)
                .assertHasAccess(anyString(), eq(movePid), any(AccessGroupSet.class), eq(Permission.move));

        service.moveObjects(mockAgent, destPid, asList(movePid));
    }

    @Test
    public void testMoveObject() throws Exception {
        PID proxyPid = makePid();

        when(mockResultSet.hasNext()).thenReturn(true, false);
        when(mockResource.getURI()).thenReturn(proxyPid.getRepositoryPath());

        PID movePid = makePid();
        ContentObject moveObj = mock(ContentObject.class);
        when(repositoryObjectLoader.getRepositoryObject(movePid)).thenReturn(moveObj);

        service.moveObjects(mockAgent, destPid, asList(movePid));

        verify(fcrepoClient).delete(eq(proxyPid.getRepositoryUri()));
        verify(mockDestObj).addMember(moveObj);
    }

    private PID makePid() {
        return PIDs.get(UUID.randomUUID().toString());
    }
}
