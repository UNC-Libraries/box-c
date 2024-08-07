package edu.unc.lib.boxc.operations.impl.edit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.util.List;
import java.util.UUID;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.common.test.SelfReturningAnswer;
import edu.unc.lib.boxc.fcrepo.exceptions.TransactionCancelledException;
import edu.unc.lib.boxc.fcrepo.utils.FedoraTransaction;
import edu.unc.lib.boxc.fcrepo.utils.TransactionManager;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.BinaryObject;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.api.rdf.DcElements;
import edu.unc.lib.boxc.model.api.rdf.Ebucore;
import edu.unc.lib.boxc.model.api.rdf.Premis;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.model.fcrepo.services.RepositoryObjectFactoryImpl;
import edu.unc.lib.boxc.operations.api.events.PremisEventBuilder;
import edu.unc.lib.boxc.operations.api.events.PremisLogger;
import edu.unc.lib.boxc.operations.api.events.PremisLoggerFactory;
import edu.unc.lib.boxc.operations.jms.OperationsMessageSender;

/**
 *
 * @author harring
 *
 */
public class EditFilenameServiceTest {
    private AutoCloseable closeable;

    @Mock
    private AccessControlService aclService;
    @Mock
    private RepositoryObjectFactoryImpl repoObjFactory;
    @Mock
    private RepositoryObjectLoader repoObjLoader;
    @Mock
    private TransactionManager txManager;
    @Mock
    private OperationsMessageSender messageSender;
    @Mock
    private FedoraTransaction tx;
    @Mock
    private FileObject repoObj;
    @Mock
    private BinaryObject binaryObj;
    @Mock
    private WorkObject workObj;
    @Mock
    private Model model;
    @Mock
    private Resource resc;
    @Mock
    private AgentPrincipals agent;
    @Mock
    private AccessGroupSet groups;
    @Mock
    private PremisLogger premisLogger;
    @Mock
    private PremisLoggerFactory premisLoggerFactory;

    @Captor
    private ArgumentCaptor<String> labelCaptor;
    @Captor
    private ArgumentCaptor<List<PID>> pidCaptor;

    private PremisEventBuilder eventBuilder;

    private PID pid;

    private EditFilenameService service;

    @BeforeEach
    public void init() throws Exception {
        closeable = openMocks(this);

        pid = PIDs.get(UUID.randomUUID().toString());

        service = new EditFilenameService();

        service.setAclService(aclService);
        service.setRepositoryObjectFactory(repoObjFactory);
        service.setRepositoryObjectLoader(repoObjLoader);
        service.setTransactionManager(txManager);
        service.setOperationsMessageSender(messageSender);
        service.setPremisLoggerFactory(premisLoggerFactory);

        when(repoObjLoader.getRepositoryObject(any(PID.class))).thenReturn(repoObj);
        when(repoObj.getOriginalFile()).thenReturn(binaryObj);
        when(binaryObj.getModel()).thenReturn(model);
        when(model.getResource(anyString())).thenReturn(resc);
        when(agent.getPrincipals()).thenReturn(groups);
        when(agent.getUsername()).thenReturn("user");

        eventBuilder = mock(PremisEventBuilder.class, new SelfReturningAnswer());
        when(premisLoggerFactory.createPremisLogger(repoObj)).thenReturn(premisLogger);
        when(premisLogger.buildEvent(eq(Premis.FilenameChange))).thenReturn(eventBuilder);
        when(agent.getUsernameUri()).thenReturn("agentname");
        when(eventBuilder.write()).thenReturn(resc);

        when(txManager.startTransaction()).thenReturn(tx);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                throw new TransactionCancelledException("", invocation.getArgument(0));
            }

        }).when(tx).cancel(any(Throwable.class));
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void editFilenameTest() {
        when(binaryObj.getFilename()).thenReturn("Old file name");
        String label = "a brand-new title!";
        service.editLabel(agent, pid, label);

        verify(repoObjFactory).createExclusiveRelationship(eq(binaryObj), eq(Ebucore.filename), eq(label));
        verify(repoObjFactory).createExclusiveRelationship(eq(repoObj), eq(DcElements.title), eq(label));
        verify(premisLogger).buildEvent(eq(Premis.FilenameChange));
        verify(eventBuilder).addEventDetail(labelCaptor.capture());
        assertEquals(labelCaptor.getValue(), "Object renamed from Old file name to " + label);
        verify(eventBuilder).writeAndClose();

        verify(messageSender).sendUpdateDescriptionOperation(anyString(), pidCaptor.capture());
        assertEquals(pid, pidCaptor.getValue().get(0));
    }

    @Test
    public void editNoFilenameTest() {
        String label = "a brand-new title too!";
        service.editLabel(agent, pid, label);

        verify(repoObjFactory).createExclusiveRelationship(eq(binaryObj), eq(Ebucore.filename), eq(label));
        verify(repoObjFactory).createExclusiveRelationship(eq(repoObj), eq(DcElements.title), eq(label));
        verify(premisLogger).buildEvent(eq(Premis.FilenameChange));
        verify(eventBuilder).addEventDetail(labelCaptor.capture());
        assertEquals(labelCaptor.getValue(), "Object renamed from no ebucore:filename to " + label);
        verify(eventBuilder).writeAndClose();

        verify(messageSender).sendUpdateDescriptionOperation(anyString(), pidCaptor.capture());
        assertEquals(pid, pidCaptor.getValue().get(0));
    }

    @Test
    public void editFilenamelNonFileObjTest() {
        when(repoObjLoader.getRepositoryObject(any(PID.class))).thenReturn(workObj);

        try {
            service.editLabel(agent, pid, "label");
        } catch (Exception e) {
           assertEquals(e.getCause().getClass(), IllegalArgumentException.class);
        }
    }

    @Test
    public void insufficientAccessTest() {
        Assertions.assertThrows(TransactionCancelledException.class, () -> {
            doThrow(new AccessRestrictionException()).when(aclService)
                    .assertHasAccess(anyString(), eq(pid), any(), eq(Permission.editDescription));

            service.editLabel(agent, pid, "label");
        });
    }
}