package edu.unc.lib.boxc.services.camel.order;

import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.impl.utils.EmailHandler;
import edu.unc.lib.boxc.operations.jms.order.MultiParentOrderRequest;
import edu.unc.lib.boxc.operations.jms.order.OrderOperationType;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.AfterEach;
import org.mockito.Mock;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.io.File;
import java.util.Arrays;


/**
 * @author snluong
 */
public class OrderNotificationServiceTest {
    private static final String PARENT1_UUID = "f277bb38-272c-471c-a28a-9887a1328a1f";
    private static final String PARENT2_UUID = "75e29173-2e4d-418d-a9a6-caa68413edaf";
    private static final String EMAIL = "user1@example.com";
    private static final String USERNAME = "user1";
    private PID parentPid1;
    private PID parentPid2;
    private AgentPrincipals agent = new AgentPrincipalsImpl(USERNAME, new AccessGroupSetImpl("agroup"));
    private MultiParentOrderRequest request = new MultiParentOrderRequest();
    private OrderNotificationService orderNotificationService;
    private AutoCloseable closeable;

    @Mock
    private EmailHandler emailHandler;
    @Mock
    private OrderNotificationBuilder orderNotificationBuilder;

    @Before
    public void setup() {
        closeable = openMocks(this);
        request.setAgent(agent);
        request.setOperation(OrderOperationType.SET);
        parentPid1 = PIDs.get(PARENT1_UUID);
        parentPid2 = PIDs.get(PARENT2_UUID);
        orderNotificationService = new OrderNotificationService();
        orderNotificationService.setOrderNotificationBuilder(orderNotificationBuilder);
        orderNotificationService.setEmailHandler(emailHandler);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void sendResultsSendsEmail() {
        when(orderNotificationBuilder.construct(any(), any(), any())).thenReturn("Hi there");
        request.setEmail(EMAIL);
        var successes = Arrays.asList(parentPid1, parentPid2);
        var errors = Arrays.asList("First error", "Another error oh no");
        orderNotificationService.sendResults(request, successes, errors);
        assertEmailSent();
    }
    @Test
    public void doNotSendResultsEmailIfNoEmailAddress() {
        var successes = Arrays.asList(parentPid1, parentPid2);
        var errors = Arrays.asList("First error", "Another error oh no");
        orderNotificationService.sendResults(request, successes, errors);
        assertEmailNotSent();
    }

    private void assertEmailSent() {
        verify(emailHandler, times(1)).sendEmail(
                eq(EMAIL), any(), eq("Hi there"), isNull(String.class), isNull(File.class));
    }

    private void assertEmailNotSent() {
        verify(emailHandler, never()).sendEmail(any(), any(), any(), any(), any());
    }
}
