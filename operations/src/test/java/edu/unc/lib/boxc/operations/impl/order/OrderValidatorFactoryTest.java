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
package edu.unc.lib.boxc.operations.impl.order;

import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.services.MembershipService;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.jms.order.OrderOperationType;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;

import static org.junit.Assert.assertTrue;

/**
 * @author bbpennel
 */
public class OrderValidatorFactoryTest {
    private static final String PARENT_UUID = "f277bb38-272c-471c-a28a-9887a1328a1f";
    private static final String CHILD1_UUID = "83c2d7f8-2e6b-4f0b-ab7e-7397969c0682";
    private PID parentPid;
    private PID child1Pid;
    @Mock
    private RepositoryObjectLoader repositoryObjectLoader;
    @Mock
    private MembershipService membershipService;
    private OrderValidatorFactory factory;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        factory = new OrderValidatorFactory();
        factory.setMembershipService(membershipService);
        factory.setRepositoryObjectLoader(repositoryObjectLoader);
        parentPid = PIDs.get(PARENT_UUID);
        child1Pid = PIDs.get(CHILD1_UUID);
    }

    @Test
    public void forSetOrderRequestTest() {
        var request = OrderRequestFactory.createRequest(OrderOperationType.SET, PARENT_UUID, Arrays.asList(CHILD1_UUID));
        assertTrue(factory.createValidator(request) instanceof SetOrderValidator);
    }

    @Test
    public void forClearOrderRequestTest() {
        var request = OrderRequestFactory.createRequest(OrderOperationType.CLEAR, PARENT_UUID, Arrays.asList(CHILD1_UUID));
        assertTrue(factory.createValidator(request) instanceof ClearOrderValidator);
    }
}
