package edu.unc.lib.boxc.operations.jms.order;

import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;

import java.util.Arrays;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

/**
 * @author bbpennel
 */
public class MemberOrderRequestSenderTest {
    private static final String PARENT_UUID = "f277bb38-272c-471c-a28a-9887a1328a1f";
    private static final String CHILD1_UUID = "83c2d7f8-2e6b-4f0b-ab7e-7397969c0682";
    private static final String CHILD2_UUID = "0e33ad0b-7a16-4bfa-b833-6126c262d889";
    private final static String USERNAME = "test_user";
    private final static String EMAIL = "test_user@example.com";
    private final static AccessGroupSet GROUPS = new AccessGroupSetImpl("adminGroup");

    @Mock
    private JmsTemplate jmsTemplate;
    private MemberOrderRequestSender requestSender;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        requestSender = new MemberOrderRequestSender();
        requestSender.setJmsTemplate(jmsTemplate);
    }

    @Test
    public void sendToQueueTest() throws Exception {
        var request = new MultiParentOrderRequest();
        var agent = new AgentPrincipalsImpl(USERNAME, GROUPS);
        request.setAgent(agent);
        request.setOperation(OrderOperationType.SET);
        request.setEmail(EMAIL);
        var parentToOrdered = Map.of(PARENT_UUID, Arrays.asList(CHILD1_UUID, CHILD2_UUID));
        request.setParentToOrdered(parentToOrdered);

        requestSender.sendToQueue(request);

        verify(jmsTemplate).send(any());
    }
}
