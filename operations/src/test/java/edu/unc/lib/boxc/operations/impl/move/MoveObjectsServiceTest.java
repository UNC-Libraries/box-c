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
package edu.unc.lib.boxc.operations.impl.move;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.isA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Resource;
import org.fcrepo.client.FcrepoClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.fcrepo.exceptions.TransactionCancelledException;
import edu.unc.lib.boxc.fcrepo.utils.FedoraTransaction;
import edu.unc.lib.boxc.fcrepo.utils.TransactionManager;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.AdminUnit;
import edu.unc.lib.boxc.model.api.objects.CollectionObject;
import edu.unc.lib.boxc.model.api.objects.ContentContainerObject;
import edu.unc.lib.boxc.model.api.objects.ContentObject;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.FolderObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.jms.OperationsMessageSender;
import edu.unc.lib.boxc.search.api.models.ObjectPath;
import edu.unc.lib.boxc.search.solr.services.ObjectPathFactory;

/**
 *
 * @author bbpennel
 *
 */
public class MoveObjectsServiceTest {
    private final static String DEST_OBJ_PATH = "/path/to/destination";
    private final static String SOURCE_OBJ_PATH = "/path/to/source";

    @Mock
    private AccessControlService aclService;
    @Mock
    private RepositoryObjectLoader repositoryObjectLoader;
    @Mock
    private TransactionManager transactionManager;
    @Mock
    private FcrepoClient fcrepoClient;
    @Mock
    private OperationsMessageSender operationsMessageSender;
    @Mock
    private ObjectPathFactory objectPathFactory;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private PID destPid;
    private PID sourcePid;
    @Mock
    private FolderObject mockDestObj;
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
    private Resource mockParentResource;
    @Mock
    private FolderObject mockParent;
    @Mock
    private ObjectPath destObjPath;
    @Mock
    private ObjectPath sourceObjPath;

    private MoveObjectsService service;

    private ListAppender<ILoggingEvent> actionAppender;

    @Before
    public void init() throws Exception {
        initMocks(this);

        service = new MoveObjectsService();
        service.setAclService(aclService);

        service.setRepositoryObjectLoader(repositoryObjectLoader);
        service.setTransactionManager(transactionManager);
        service.setOperationsMessageSender(operationsMessageSender);
        service.setObjectPathFactory(objectPathFactory);

        sourcePid = makePid();
        when(mockParent.getPid()).thenReturn(sourcePid);

        destPid = makePid();
        when(repositoryObjectLoader.getRepositoryObject(destPid)).thenReturn(mockDestObj);

        when(transactionManager.startTransaction()).thenReturn(mockTx);
        doAnswer(new Answer<Exception>() {
            @Override
            public Exception answer(InvocationOnMock invocation) throws Throwable {
                throw new TransactionCancelledException("", invocation.getArgument(0));
            }
        }).when(mockTx).cancel(any(Exception.class));

        when(mockQueryExec.execSelect()).thenReturn(mockResultSet);
        when(mockResultSet.nextSolution()).thenReturn(mockQuerySolution);
        when(mockQuerySolution.getResource("parent")).thenReturn(mockParentResource);

        when(mockAgent.getUsername()).thenReturn("user");
        when(mockAgent.getPrincipals()).thenReturn(mockAccessSet);

        when(destObjPath.toNamePath()).thenReturn(DEST_OBJ_PATH);
        when(objectPathFactory.getPath(eq(destPid))).thenReturn(destObjPath);

        when(sourceObjPath.toNamePath()).thenReturn(SOURCE_OBJ_PATH);
        when(objectPathFactory.getPath(eq(sourcePid))).thenReturn(sourceObjPath);

        // Adding retrievable logger for move log
        Logger actionLogger = (Logger) LoggerFactory.getLogger("moves");
        actionAppender = new ListAppender<>();
        actionLogger.setLevel(Level.INFO);
        actionLogger.addAppender(actionAppender);
        actionAppender.start();
    }

    @After
    public void after() {
        actionAppender.stop();
    }

    @Test(expected = AccessRestrictionException.class)
    public void testNoPermissionOnDestination() throws Exception {
        doThrow(new AccessRestrictionException()).when(aclService)
                .assertHasAccess(anyString(), eq(destPid), any(AccessGroupSetImpl.class), eq(Permission.move));

        service.moveObjects(mockAgent, destPid, asList(makeMoveObject(mockParent)));
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

        PID movePid = makeMoveObject(mockParent);

        doThrow(new AccessRestrictionException()).when(aclService)
                .assertHasAccess(anyString(), eq(movePid), any(AccessGroupSetImpl.class), eq(Permission.move));

        service.moveObjects(mockAgent, destPid, asList(movePid));
    }

    @Test
    public void testMoveObject() throws Exception {
        List<PID> movePids = asList(makeMoveObject(mockParent));
        service.moveObjects(mockAgent, destPid, movePids);

        verify(mockDestObj).addMember(any(ContentObject.class));
        verify(operationsMessageSender).sendMoveOperation(anyString(), anyListOf(PID.class),
                eq(destPid), anyListOf(PID.class), eq(null));

        verifyLogMessage(sourcePid, movePids);
    }

    @Test
    public void testMoveCollectionIntoWork() {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("is not a file and cannot be added to a work");

        WorkObject mockWorkObj = mock(WorkObject.class);
        when(repositoryObjectLoader.getRepositoryObject(destPid)).thenReturn(mockWorkObj);

        PID movePid = makePid();
        CollectionObject moveObj = mock(CollectionObject.class);
        when(repositoryObjectLoader.getRepositoryObject(movePid)).thenReturn(moveObj);

        List<PID> movePids = asList(movePid);
        service.moveObjects(mockAgent, destPid, movePids);
    }

    @Test
    public void testMoveAdminUnitIntoFolder() {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("is not a folder or a work and cannot be added to a folder");

        FolderObject mockFolderObj = mock(FolderObject.class);
        when(repositoryObjectLoader.getRepositoryObject(destPid)).thenReturn(mockFolderObj);

        PID movePid = makePid();
        AdminUnit moveObj = mock(AdminUnit.class);
        when(repositoryObjectLoader.getRepositoryObject(movePid)).thenReturn(moveObj);

        List<PID> movePids = asList(movePid);
        service.moveObjects(mockAgent, destPid, movePids);
    }

    @Test
    public void testMoveFolderIntoWork() {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("is not a file and cannot be added to a work");

        WorkObject mockWorkObj = mock(WorkObject.class);
        when(repositoryObjectLoader.getRepositoryObject(destPid)).thenReturn(mockWorkObj);

        PID movePid = makePid();
        FolderObject moveObj = mock(FolderObject.class);
        when(repositoryObjectLoader.getRepositoryObject(movePid)).thenReturn(moveObj);

        List<PID> movePids = asList(movePid);
        service.moveObjects(mockAgent, destPid, movePids);
    }

    @Test
    public void testMoveMultipleObjects() throws Exception {
        List<PID> movePids = asList(makeMoveObject(mockParent), makeMoveObject(mockParent));
        service.moveObjects(mockAgent, destPid, movePids);

        verify(mockDestObj, times(2)).addMember(any(ContentObject.class));
        verify(operationsMessageSender).sendMoveOperation(anyString(), anyListOf(PID.class),
                eq(destPid), anyListOf(PID.class), eq(null));

        verifyLogMessage(sourcePid, movePids);
    }

    private void verifyLogMessage(PID sourcePid, List<PID> pids) throws Exception {
        List<ILoggingEvent> logEntries = actionAppender.list;
        ObjectMapper mapper = new ObjectMapper();
        String logEntry = logEntries.get(0).getFormattedMessage();
        JsonNode logRoot = mapper.readTree(logEntry.getBytes());
        assertEquals("moved", logRoot.get("event").asText());
        assertNotNull(logRoot.get("move_id"));
        assertEquals("user", logRoot.get("user").asText());
        assertEquals(destPid.getId(), logRoot.get("destination_id").asText());
        assertEquals("Incorrect path for destination",
                DEST_OBJ_PATH, logRoot.get("destination_path").asText());

        JsonNode sourceNode = logRoot.get("sources").get(sourcePid.getId());
        assertEquals("Incorrect path for source",
                SOURCE_OBJ_PATH, sourceNode.get("path").asText());
        JsonNode sourceObjsNode = sourceNode.get("objects");

        // Build a list of objects logged for the source
        List<PID> pidsFromSource = new ArrayList<>();
        Iterator<JsonNode> objIt = sourceObjsNode.elements();
        while (objIt.hasNext()) {
            JsonNode objNode = objIt.next();
            pidsFromSource.add(PIDs.get(objNode.asText()));
        }

        assertTrue("Source did not contain all expected pids", pidsFromSource.containsAll(pids));
        assertEquals("Incorrect number of pids from source", pids.size(), pidsFromSource.size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNoDestination() throws Exception {
        service.moveObjects(mockAgent, null, asList(makePid()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNoPids() throws Exception {
        service.moveObjects(mockAgent, destPid, asList());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNoUsername() throws Exception {
        when(mockAgent.getUsername()).thenReturn(null);
        service.moveObjects(mockAgent, destPid, asList(makePid()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNoPrincipals() throws Exception {
        when(mockAgent.getPrincipals()).thenReturn(null);
        service.moveObjects(mockAgent, destPid, asList(makePid()));
    }

    private PID makeMoveObject(ContentContainerObject parent) {
        PID movePid = makePid();
        WorkObject moveObj = mock(WorkObject.class);
        when(repositoryObjectLoader.getRepositoryObject(movePid)).thenReturn(moveObj);
        when(moveObj.getParent()).thenReturn(parent);
        return movePid;
    }

    private PID makePid() {
        return PIDs.get(UUID.randomUUID().toString());
    }
}
