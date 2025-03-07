package edu.unc.lib.boxc.operations.impl.delete;

import static edu.unc.lib.boxc.auth.api.Permission.markForDeletion;
import static edu.unc.lib.boxc.auth.api.Permission.markForDeletionUnit;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.common.test.SelfReturningAnswer;
import edu.unc.lib.boxc.model.api.exceptions.InvalidOperationForObjectType;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.AdminUnit;
import edu.unc.lib.boxc.model.api.objects.ContentObject;
import edu.unc.lib.boxc.model.api.objects.DepositRecord;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.rdf.Premis;
import edu.unc.lib.boxc.model.api.sparql.SparqlUpdateService;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.api.events.PremisEventBuilder;
import edu.unc.lib.boxc.operations.api.events.PremisLogger;
import edu.unc.lib.boxc.operations.api.events.PremisLoggerFactory;
import edu.unc.lib.boxc.operations.jms.OperationsMessageSender;

/**
 *
 * @author bbpennel
 *
 */
public class MarkForDeletionJobTest {

    private static final String MESSAGE = "Reason for the deletion";
    private AutoCloseable closeable;

    @Mock
    private AccessControlService aclService;
    @Mock
    private RepositoryObjectLoader repositoryObjectLoader;
    @Mock
    private SparqlUpdateService sparqlUpdateService;
    @Mock
    private OperationsMessageSender operationsMessageSender;
    @Mock
    private ContentObject contentObj;
    @Mock
    private AgentPrincipals agent;
    @Mock
    private AccessGroupSet groups;
    @Mock
    private PremisLogger premisLogger;
    @Mock
    private AdminUnit repoObj;
    @Mock
    private PremisLoggerFactory premisLoggerFactory;

    @Captor
    private ArgumentCaptor<String> messageCaptor;

    private PremisEventBuilder eventBuilder;

    private PID pid;

    private MarkForDeletionJob job;

    @BeforeEach
    public void init() {
        closeable = openMocks(this);

        when(repositoryObjectLoader.getRepositoryObject(any(PID.class))).thenReturn(contentObj);

        when(agent.getPrincipals()).thenReturn(groups);
        when(agent.getUsername()).thenReturn("user");

        when(contentObj.getMetadataUri()).thenReturn(URI.create(""));

        eventBuilder = mock(PremisEventBuilder.class, new SelfReturningAnswer());
        when(premisLoggerFactory.createPremisLogger(contentObj)).thenReturn(premisLogger);
        when(premisLogger.buildEvent(eq(Premis.Deaccession))).thenReturn(eventBuilder);

        pid = PIDs.get(UUID.randomUUID().toString());

        job = new MarkForDeletionJob(pid, MESSAGE, agent, repositoryObjectLoader, sparqlUpdateService, aclService,
                premisLoggerFactory);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void insufficientAccessTest() {
        Assertions.assertThrows(AccessRestrictionException.class, () -> {
            doThrow(new AccessRestrictionException()).when(aclService)
                    .assertHasAccess(anyString(), eq(pid), any(), eq(markForDeletion));

            job.run();
        });
    }

    @Test
    public void insufficientAccessAdminUnitTest() {
        Assertions.assertThrows(AccessRestrictionException.class, () -> {
            when(repositoryObjectLoader.getRepositoryObject(pid)).thenReturn(repoObj);
            doThrow(new AccessRestrictionException()).when(aclService)
                    .assertHasAccess(anyString(), eq(pid), any(), eq(markForDeletionUnit));

            job.run();
        });
    }

    @Test
    public void invalidObjectTypeTest() {
        Assertions.assertThrows(InvalidOperationForObjectType.class, () -> {
            DepositRecord depObj = mock(DepositRecord.class);
            when(repositoryObjectLoader.getRepositoryObject(any(PID.class))).thenReturn(depObj);

            job.run();
        });
    }

    @Test
    public void markForDeletionTest() {
        job.run();

        verify(sparqlUpdateService).executeUpdate(anyString(), anyString());

        verify(premisLogger).buildEvent(eq(Premis.Deaccession));

        verify(eventBuilder, times(2)).addEventDetail(messageCaptor.capture());
        List<String> details = messageCaptor.getAllValues();
        assertTrue(details.contains(MESSAGE));

        verify(eventBuilder).writeAndClose();
    }
}
