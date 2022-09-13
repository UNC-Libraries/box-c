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
package edu.unc.lib.boxc.services.camel.order;

import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.jms.order.MultiParentOrderRequest;
import edu.unc.lib.boxc.operations.jms.order.OrderOperationType;
import org.junit.Before;
import org.junit.Test;

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

    @Before
    public void setup() {
        request.setAgent(agent);
        request.setOperation(OrderOperationType.SET);
        parentPid1 = PIDs.get(PARENT1_UUID);
        parentPid2 = PIDs.get(PARENT2_UUID);
    }
    @Test
    public void sendResultsSendsEmail() {
        request.setEmail(EMAIL);
        var successes = Arrays.asList(parentPid1, parentPid2);
        var errors = Arrays.asList("First error", "Another error oh no");
        orderNotificationService.sendResults(request, successes, errors);
    }
    @Test
    public void doNotSendResultsEmailIfNoEmailAddress() {
        var successes = Arrays.asList(parentPid1, parentPid2);
        var errors = Arrays.asList("First error", "Another error oh no");
        orderNotificationService.sendResults(request, successes, errors);

    }
}
