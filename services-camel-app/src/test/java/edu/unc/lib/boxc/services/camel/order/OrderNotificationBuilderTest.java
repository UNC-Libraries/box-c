package edu.unc.lib.boxc.services.camel.order;

import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.jms.order.MultiParentOrderRequest;
import edu.unc.lib.boxc.operations.jms.order.OrderOperationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * @author snluong
 */
public class OrderNotificationBuilderTest {
    private static final String PARENT1_UUID = "f277bb38-272c-471c-a28a-9887a1328a1f";
    private static final String PARENT2_UUID = "75e29173-2e4d-418d-a9a6-caa68413edaf";
    private static final String CHILD1_UUID = "83c2d7f8-2e6b-4f0b-ab7e-7397969c0682";
    private static final String CHILD2_UUID = "0e33ad0b-7a16-4bfa-b833-6126c262d889";
    private static final String CHILD3_UUID = "9cb6cc61-d88e-403e-b959-2396cd331a12";
    private static final String EMAIL = "user1@example.com";
    private static final String USERNAME = "user1";
    private PID parentPid1;
    private PID parentPid2;
    private AgentPrincipals agent = new AgentPrincipalsImpl(USERNAME, new AccessGroupSetImpl("agroup"));
    private MultiParentOrderRequest request = new MultiParentOrderRequest();
    private OrderNotificationBuilder orderNotificationBuilder;

    @BeforeEach
    public void setup(){
        var parentToOrder = Map.of(
                PARENT1_UUID, Arrays.asList(CHILD1_UUID, CHILD2_UUID), PARENT2_UUID, List.of(CHILD3_UUID));
        request.setEmail(EMAIL);
        request.setAgent(agent);
        request.setOperation(OrderOperationType.SET);
        request.setParentToOrdered(parentToOrder);
        parentPid1 = PIDs.get(PARENT1_UUID);
        parentPid2 = PIDs.get(PARENT2_UUID);
        orderNotificationBuilder = new OrderNotificationBuilder();
    }

    @Test
    public void constructEmailWithNoErrors() {
        var successes = Arrays.asList(parentPid1, parentPid2);
        var errors = new ArrayList<String>();
        var expected = "Here are the results of your bulk SetOrderUpdate request.\n" +
                "Number of parent objects requested: 2\n" +
                "Number successfully updated: 2\n" +
                "There were no errors.";

        assertEquals(expected, orderNotificationBuilder.construct(request, successes, errors));
    }

    @Test
    public void constructEmailWithErrorsAndNoSuccesses() {
        var successes = new ArrayList<PID>();
        var errors = Arrays.asList("First error", "Another error oh no");
        var expected = "Here are the results of your bulk SetOrderUpdate request.\n" +
                "Number of parent objects requested: 2\n" +
                "Number successfully updated: 0\n" +
                "There were the following errors:\n" +
                "-- First error\n" +
                "-- Another error oh no\n";

        assertEquals(expected, orderNotificationBuilder.construct(request, successes, errors));
    }

    @Test
    public void constructEmailWithOneParentOrderFailure() {
        var successes = List.of(parentPid1);
        var errors = List.of("Parent2 failed");
        var expected = "Here are the results of your bulk SetOrderUpdate request.\n" +
                "Number of parent objects requested: 2\n" +
                "Number successfully updated: 1\n" +
                "There were the following errors:\n" +
                "-- Parent2 failed\n";

        assertEquals(expected, orderNotificationBuilder.construct(request, successes, errors));
    }
}
