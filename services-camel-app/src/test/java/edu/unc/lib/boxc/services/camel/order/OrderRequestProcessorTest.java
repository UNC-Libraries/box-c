package edu.unc.lib.boxc.services.camel.order;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.api.order.OrderValidator;
import edu.unc.lib.boxc.operations.impl.order.OrderJobFactory;
import edu.unc.lib.boxc.operations.impl.order.OrderValidatorFactory;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingActionType;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingMessageSender;
import edu.unc.lib.boxc.operations.jms.order.MultiParentOrderRequest;
import edu.unc.lib.boxc.operations.jms.order.OrderOperationType;
import edu.unc.lib.boxc.operations.jms.order.OrderRequestSerializationHelper;
import edu.unc.lib.boxc.services.camel.TestHelper;
import org.apache.camel.Exchange;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.AfterEach;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

/**
 * @author bbpennel
 */
public class OrderRequestProcessorTest {
    private static final String PARENT1_UUID = "f277bb38-272c-471c-a28a-9887a1328a1f";
    private static final String PARENT2_UUID = "75e29173-2e4d-418d-a9a6-caa68413edaf";
    private static final String PARENT3_UUID = "aa65d023-778b-42a8-8680-3a7fa6883c13";
    private static final String CHILD1_UUID = "83c2d7f8-2e6b-4f0b-ab7e-7397969c0682";
    private static final String CHILD2_UUID = "0e33ad0b-7a16-4bfa-b833-6126c262d889";
    private static final String CHILD3_UUID = "9cb6cc61-d88e-403e-b959-2396cd331a12";
    private static final String EMAIL = "user1@example.com";
    private PID parentPid1;
    private PID parentPid2;
    private AutoCloseable closeable;
    @Mock
    private AccessControlService accessControlService;
    @Mock
    private OrderValidatorFactory orderValidatorFactory;
    @Mock
    private OrderValidator orderValidator;
    @Mock
    private OrderJobFactory orderJobFactory;
    @Mock
    private Runnable orderJob;
    @Mock
    private IndexingMessageSender indexingMessageSender;
    @Mock
    private OrderNotificationService orderNotificationService;
    @Captor
    private ArgumentCaptor<List<String>> errorsCaptor;
    @Captor
    private ArgumentCaptor<List<PID>> successesCaptor;
    private static final String USERNAME = "user1";
    private AgentPrincipals agent = new AgentPrincipalsImpl(USERNAME, new AccessGroupSetImpl("agroup"));
    private OrderRequestProcessor processor;

    @Before
    public void setup() {
        closeable = openMocks(this);
        processor = new OrderRequestProcessor();
        processor.setAccessControlService(accessControlService);
        processor.setOrderJobFactory(orderJobFactory);
        processor.setIndexingMessageSender(indexingMessageSender);
        processor.setOrderValidatorFactory(orderValidatorFactory);
        processor.setOrderNotificationService(orderNotificationService);
        parentPid1 = PIDs.get(PARENT1_UUID);
        parentPid2 = PIDs.get(PARENT2_UUID);
        when(accessControlService.hasAccess(any(), any(), eq(Permission.orderMembers))).thenReturn(true);
        when(orderJobFactory.createJob(any())).thenReturn(orderJob);
        when(orderValidatorFactory.createValidator(any())).thenReturn(orderValidator);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void emptyParentToChildMapTest() throws Exception {
        mockRequestAsValid();

        var requestExchange = createRequestExchange(Collections.emptyMap());
        processor.process(requestExchange);

        assertJobNotRun();

        assertNotificationSent();
        assertNotificationWithoutErrors();
        assertNotificationWithoutSuccesses();
    }

    @Test
    public void emptySingleParentRequestTest() throws Exception {
        mockRequestAsValid();

        var requestExchange = createRequestExchange(Map.of(PARENT1_UUID, Collections.emptyList()));
        processor.process(requestExchange);

        assertNotificationSent();
        assertNotificationWithoutErrors();
        assertNotificationWithSuccesses(PARENT1_UUID);
    }

    @Test
    public void insufficientPermissionsTest() throws Exception {
        mockRequestAsValid();
        mockDoesNotHavePermission(PARENT1_UUID);

        var requestExchange = createRequestExchange(Map.of(PARENT1_UUID, Arrays.asList(CHILD1_UUID)));
        processor.process(requestExchange);

        assertJobNotRun();
        assertIndexingMessageNotSent(PARENT1_UUID);

        assertNotificationSent();
        assertNotificationWithErrors(
                "User user1 does not have permission to update member order for f277bb38-272c-471c-a28a-9887a1328a1f");
        assertNotificationWithoutSuccesses();
    }

    @Test
    public void invalidRequestBodyTest() throws Exception {
        mockRequestAsValid();

        var requestExchange = TestHelper.mockExchange("bad times");
        try {
            processor.process(requestExchange);
            fail();
        } catch (IllegalArgumentException e) {
            // expected
        }
        assertJobNotRun();
        assertNotificationNotSent();
    }

    @Test
    public void invalidRequestTest() throws Exception {
        var errors = new String[] { "This doesn't look good", "Whoa no thanks" };
        produceValidatorWithErrors(PARENT1_UUID, errors);

        var requestExchange = createRequestExchange(Map.of(PARENT1_UUID, Arrays.asList(CHILD1_UUID)));
        processor.process(requestExchange);

        assertJobNotRun();
        assertIndexingMessageNotSent(PARENT1_UUID);

        assertNotificationSent();
        assertNotificationWithErrors(errors);
        assertNotificationWithoutSuccesses();
    }

    @Test
    public void jobFailsTest() throws Exception {
        mockRequestAsValid();
        doThrow(new RuntimeException("Boom")).when(orderJob).run();

        var requestExchange = createRequestExchange(Map.of(PARENT1_UUID, Arrays.asList(CHILD1_UUID)));
        processor.process(requestExchange);

        assertJobRan();
        assertIndexingMessageNotSent(PARENT1_UUID);

        assertNotificationSent();
        assertNotificationWithErrors("Encountered an error while updating f277bb38-272c-471c-a28a-9887a1328a1f: Boom");
        assertNotificationWithoutSuccesses();
    }

    @Test
    public void validMultipleUpdatesTest() throws Exception {
        mockRequestAsValid();

        var requestExchange = createRequestExchange(Map.of(
                PARENT1_UUID, Arrays.asList(CHILD1_UUID),
                PARENT2_UUID, Arrays.asList(CHILD2_UUID, CHILD3_UUID)));
        processor.process(requestExchange);

        assertJobRanNTimes(2);
        assertIndexingMessageSent(PARENT1_UUID);
        assertIndexingMessageSent(PARENT2_UUID);

        assertNotificationSent();
        assertNotificationWithoutErrors();
        assertNotificationWithSuccesses(PARENT1_UUID, PARENT2_UUID);
    }

    @Test
    public void mixOfValidAndInvalidTest() throws Exception {
        var validationError = "Bad bad very not good";
        // All requests except for one for PARENT1 are valid
        mockRequestAsValid();
        produceValidatorWithErrors(PARENT1_UUID, validationError);
        // Second one does not have permission
        mockDoesNotHavePermission(PARENT2_UUID);

        var requestExchange = createRequestExchange(Map.of(
                PARENT1_UUID, Arrays.asList(CHILD1_UUID),
                PARENT2_UUID, Arrays.asList(CHILD2_UUID),
                PARENT3_UUID, Arrays.asList(CHILD3_UUID)));
        processor.process(requestExchange);

        assertJobRanNTimes(1);
        assertIndexingMessageSent(PARENT3_UUID);

        assertNotificationSent();
        assertNotificationWithErrors(validationError,
                "User user1 does not have permission to update member order for 75e29173-2e4d-418d-a9a6-caa68413edaf");
        assertNotificationWithSuccesses(PARENT3_UUID);
    }

    @Test
    public void invalidParentPidTest() throws Exception {
        mockRequestAsValid();

        var requestExchange = createRequestExchange(Map.of(
                "not_a_pid", Arrays.asList(CHILD1_UUID)));
        processor.process(requestExchange);

        assertJobNotRun();

        assertNotificationSent();
        assertNotificationWithErrors(
                "Invalid order request: Invalid qualified path not_a_pid, cannot construct PID");
        assertNotificationWithoutSuccesses();
    }

    @Test
    public void invalidChildPidTest() throws Exception {
        mockRequestAsValid();

        var requestExchange = createRequestExchange(Map.of(
                PARENT1_UUID, Arrays.asList("definitely_not_a_pid")));
        processor.process(requestExchange);

        assertJobNotRun();

        assertNotificationSent();
        assertNotificationWithErrors(
                "Invalid order request: Invalid qualified path definitely_not_a_pid, cannot construct PID");
        assertNotificationWithoutSuccesses();
    }

    private void mockDoesNotHavePermission(String parentId) {
        when(accessControlService.hasAccess(PIDs.get(parentId), agent.getPrincipals(), Permission.orderMembers))
                .thenReturn(false);
    }

    private void mockRequestAsValid() {
        when(orderValidator.isValid()).thenReturn(true);
    }

    // Setup production of a validator that will return errors for the given id
    private void produceValidatorWithErrors(String uuid, String... errors) {
        var validator = mock(OrderValidator.class);
        when(validator.isValid()).thenReturn(false);
        when(validator.getErrors()).thenReturn(Arrays.asList(errors));
        when(orderValidatorFactory.createValidator(argThat(
                argument -> argument.getParentPid().getId().equals(uuid)))).thenReturn(validator);
    }

    private void assertIndexingMessageSent(String parentId) {
        verify(indexingMessageSender).sendIndexingOperation(USERNAME, PIDs.get(parentId),
                IndexingActionType.UPDATE_MEMBER_ORDER);
    }

    private void assertIndexingMessageNotSent(String parentId) {
        verify(indexingMessageSender, never()).sendIndexingOperation(USERNAME, PIDs.get(parentId),
                IndexingActionType.UPDATE_MEMBER_ORDER);
    }

    private void assertJobRan() {
        verify(orderJob).run();
    }

    private void assertJobRanNTimes(int times) {
        verify(orderJob, Mockito.times(times)).run();
    }

    private void assertJobNotRun() {
        verify(orderJob, never()).run();
    }

    private void assertNotificationNotSent() {
        verify(orderNotificationService, never()).sendResults(any(MultiParentOrderRequest.class), any(), any());
    }

    private void assertNotificationSent() {
        verify(orderNotificationService).sendResults(any(MultiParentOrderRequest.class),
                successesCaptor.capture(), errorsCaptor.capture());
    }

    private void assertNotificationWithoutErrors() {
        assertNotificationWithErrors();
    }

    // Must call assertNotificationSent before using this
    private void assertNotificationWithErrors(String... expectedErrors) {
        var expected = Arrays.asList(expectedErrors);
        var errors = errorsCaptor.getValue();
        var message = "Expected errors:\n" + expected + "\nBut received:\n" + errors;
        assertTrue(message, errors.containsAll(expected));
        assertEquals(message, expectedErrors.length, errors.size());
    }

    private void assertNotificationWithoutSuccesses() {
        assertNotificationWithSuccesses();
    }

    // Must call assertNotificationSent before using this
    private void assertNotificationWithSuccesses(String... expectedSuccesses) {
        var expected = Arrays.stream(expectedSuccesses).map(PIDs::get).collect(Collectors.toList());
        var successes = successesCaptor.getValue();
        var message = "Expected successes:\n" + expected + "\nBut received:\n" + successes;
        assertTrue(message, successes.containsAll(expected));
        assertEquals(message, expectedSuccesses.length, successes.size());
    }

    private Exchange createRequestExchange(Map<String, List<String>> parentToOrder) throws IOException {
        var request = new MultiParentOrderRequest();
        request.setAgent(agent);
        request.setEmail(EMAIL);
        request.setOperation(OrderOperationType.SET);
        request.setParentToOrdered(parentToOrder);
        return TestHelper.mockExchange(OrderRequestSerializationHelper.toJson(request));
    }
}
