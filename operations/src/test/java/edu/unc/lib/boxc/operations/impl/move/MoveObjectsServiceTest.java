package edu.unc.lib.boxc.operations.impl.move;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.isA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

    @BeforeEach
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

    @AfterEach
    public void after() {
        actionAppender.stop();
    }

    @Test
    public void testNoPermissionOnDestination() throws Exception {
        Assertions.assertThrows(AccessRestrictionException.class, () -> {
            doThrow(new AccessRestrictionException()).when(aclService)
                    .assertHasAccess(anyString(), eq(destPid), any(), eq(Permission.move));

            service.moveObjects(mockAgent, destPid, asList(makeMoveObject(mockParent)));
        });
    }

    @Test
    public void testDestinationInvalidType() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            FileObject destFile = mock(FileObject.class);
            when(repositoryObjectLoader.getRepositoryObject(destPid)).thenReturn(destFile);

            service.moveObjects(mockAgent, destPid, asList(makePid()));
        });
    }

    @Test
    public void testNoPermissionToMoveObject() throws Exception {
        Assertions.assertThrows(AccessRestrictionException.class, () -> {
            PID movePid = makeMoveObject(mockParent);

            doThrow(new AccessRestrictionException()).when(aclService)
                    .assertHasAccess(anyString(), eq(movePid), any(), eq(Permission.move));

            service.moveObjects(mockAgent, destPid, asList(movePid));
        });
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
        Exception expected = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            WorkObject mockWorkObj = mock(WorkObject.class);
            when(repositoryObjectLoader.getRepositoryObject(destPid)).thenReturn(mockWorkObj);

            PID movePid = makePid();
            CollectionObject moveObj = mock(CollectionObject.class);
            when(repositoryObjectLoader.getRepositoryObject(movePid)).thenReturn(moveObj);

            List<PID> movePids = asList(movePid);
            service.moveObjects(mockAgent, destPid, movePids);
        });

        assertTrue(expected.getMessage().contains("is not a file and cannot be added to a work"));
    }

    @Test
    public void testMoveAdminUnitIntoFolder() {
        Exception expected = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            FolderObject mockFolderObj = mock(FolderObject.class);
            when(repositoryObjectLoader.getRepositoryObject(destPid)).thenReturn(mockFolderObj);

            PID movePid = makePid();
            AdminUnit moveObj = mock(AdminUnit.class);
            when(repositoryObjectLoader.getRepositoryObject(movePid)).thenReturn(moveObj);

            List<PID> movePids = asList(movePid);
            service.moveObjects(mockAgent, destPid, movePids);
        });

        assertTrue(expected.getMessage().contains("is not a folder or a work and cannot be added to a folder"));
    }

    @Test
    public void testMoveFolderIntoWork() {
        Exception expected = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            WorkObject mockWorkObj = mock(WorkObject.class);
            when(repositoryObjectLoader.getRepositoryObject(destPid)).thenReturn(mockWorkObj);

            PID movePid = makePid();
            FolderObject moveObj = mock(FolderObject.class);
            when(repositoryObjectLoader.getRepositoryObject(movePid)).thenReturn(moveObj);

            List<PID> movePids = asList(movePid);
            service.moveObjects(mockAgent, destPid, movePids);
        });

        assertTrue(expected.getMessage().contains("is not a file and cannot be added to a work"));
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
        assertEquals(DEST_OBJ_PATH, logRoot.get("destination_path").asText(), "Incorrect path for destination");

        JsonNode sourceNode = logRoot.get("sources").get(sourcePid.getId());
        assertEquals(SOURCE_OBJ_PATH, sourceNode.get("path").asText(), "Incorrect path for source");
        JsonNode sourceObjsNode = sourceNode.get("objects");

        // Build a list of objects logged for the source
        List<PID> pidsFromSource = new ArrayList<>();
        Iterator<JsonNode> objIt = sourceObjsNode.elements();
        while (objIt.hasNext()) {
            JsonNode objNode = objIt.next();
            pidsFromSource.add(PIDs.get(objNode.asText()));
        }

        assertTrue(pidsFromSource.containsAll(pids), "Source did not contain all expected pids");
        assertEquals(pids.size(), pidsFromSource.size(), "Incorrect number of pids from source");
    }

    @Test
    public void testNoDestination() throws Exception {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            service.moveObjects(mockAgent, null, asList(makePid()));
        });
    }

    @Test
    public void testNoPids() throws Exception {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            service.moveObjects(mockAgent, destPid, asList());
        });
    }

    @Test
    public void testNoUsername() throws Exception {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            when(mockAgent.getUsername()).thenReturn(null);
            service.moveObjects(mockAgent, destPid, asList(makePid()));
        });
    }

    @Test
    public void testNoPrincipals() throws Exception {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            when(mockAgent.getPrincipals()).thenReturn(null);
            service.moveObjects(mockAgent, destPid, asList(makePid()));
        });
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
