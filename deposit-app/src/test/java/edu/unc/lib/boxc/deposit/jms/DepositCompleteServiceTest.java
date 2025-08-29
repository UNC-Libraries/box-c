package edu.unc.lib.boxc.deposit.jms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositField;
import edu.unc.lib.boxc.deposit.impl.model.DepositModelManager;
import edu.unc.lib.boxc.deposit.impl.model.DepositStatusFactory;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.ids.PIDConstants;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.jms.OperationsMessageSender;
import org.apache.jena.rdf.model.Bag;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
public class DepositCompleteServiceTest {

    private DepositCompleteService service;

    @Mock
    private DepositModelManager depositModelManager;
    @Mock
    private OperationsMessageSender opsMessageSender;
    @Mock
    private DepositStatusFactory depositStatusFactory;
    private Model model;
    private Bag depositBag;
    private Map<String, String> depositStatus;

    @Captor
    private ArgumentCaptor<List<PID>> destinationPidsCaptor;
    @Captor
    private ArgumentCaptor<List<PID>> addedPidsCaptor;

    private final static String DEPOSIT_ID = "d5dad136-7f67-444e-9f5f-1fc4cd9b0f78";
    private final static String CONTAINER_ID = "edad4d2e-9815-4b9e-b483-71c819994407";
    private final static String DEPOSITOR_NAME = "testuser";
    private final static String CHILD_ID_1 = "b7fd3051-f374-4bff-8f04-c4ea0c5ca667";
    private final static String CHILD_ID_2 = "ad7c1f6c-8652-4ea8-bd69-e9392103160b";

    @BeforeEach
    public void setup() {
        service = new DepositCompleteService();
        service.setDepositModelManager(depositModelManager);
        service.setOpsMessageSender(opsMessageSender);
        service.setDepositStatusFactory(depositStatusFactory);
        model = ModelFactory.createDefaultModel();
        PID depositPid = PIDs.get(PIDConstants.DEPOSITS_QUALIFIER, DEPOSIT_ID);
        depositBag = model.createBag(depositPid.getRepositoryPath());
        when(depositModelManager.getReadModel(any(PID.class))).thenReturn(model);
        depositStatus = new HashMap<>();
        when(depositStatusFactory.get(DEPOSIT_ID)).thenReturn(depositStatus);
    }

    @Test
    public void testSendDepositCompleteEventSuccess() {
        var childResc1 = model.getResource(PIDs.get(CHILD_ID_1).getRepositoryPath());
        var childResc2 = model.getResource(PIDs.get(CHILD_ID_2).getRepositoryPath());
        depositBag.add(childResc1);
        depositBag.add(childResc2);

        depositStatus.put(DepositField.containerId.name(), CONTAINER_ID);
        depositStatus.put(DepositField.depositorName.name(), DEPOSITOR_NAME);

        service.sendDepositCompleteEvent(DEPOSIT_ID);

        verify(opsMessageSender).sendAddOperation(
                eq(DEPOSITOR_NAME),
                destinationPidsCaptor.capture(),
                addedPidsCaptor.capture(),
                eq(null),
                eq(DEPOSIT_ID)
        );
        verify(depositModelManager).end();

        List<PID> destinationPids = destinationPidsCaptor.getValue();
        List<PID> addedPids = addedPidsCaptor.getValue();

        assertEquals(1, destinationPids.size());
        assertEquals(CONTAINER_ID, destinationPids.getFirst().getId());
        assertEquals(2, addedPids.size());
        assertEquals(CHILD_ID_1, addedPids.get(0).getId());
        assertEquals(CHILD_ID_2, addedPids.get(1).getId());
    }

    @Test
    public void testSendDepositCompleteEventWithNoChildren() {
        depositStatus.put(DepositField.containerId.name(), CONTAINER_ID);
        depositStatus.put(DepositField.depositorName.name(), DEPOSITOR_NAME);

        // Providing a resource that is not in the model to simulate no children
        service.sendDepositCompleteEvent(CHILD_ID_1);

        verify(depositModelManager).getReadModel(any(PID.class));
        verify(opsMessageSender, never()).sendAddOperation(any(), any(), any(), any(), any());
        verify(depositModelManager).end();
    }

    @Test
    public void testSendDepositCompleteEventEnsuresModelManagerEnd() {
        depositStatus.put(DepositField.containerId.name(), CONTAINER_ID);
        depositStatus.put(DepositField.depositorName.name(), DEPOSITOR_NAME);

        when(depositModelManager.getReadModel(any(PID.class))).thenThrow(new RuntimeException("Model error"));

        assertThrows(RuntimeException.class, () -> service.sendDepositCompleteEvent(DEPOSIT_ID));

        verify(depositModelManager).end();
    }
}