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

import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.operations.api.order.OrderOperationType;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;

import static org.junit.Assert.assertTrue;

/**
 * @author bbpennel
 */
public class OrderJobFactoryTest {
    private static final String CHILD1_UUID = "83c2d7f8-2e6b-4f0b-ab7e-7397969c0682";
    private static final String CHILD2_UUID = "0e33ad0b-7a16-4bfa-b833-6126c262d889";
    private static final String PARENT_UUID = "9cb6cc61-d88e-403e-b959-2396cd331a12";
    @Mock
    private RepositoryObjectFactory repositoryObjectFactory;
    @Mock
    private RepositoryObjectLoader repositoryObjectLoader;
    private OrderJobFactory factory;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        factory = new OrderJobFactory();
        factory.setRepositoryObjectFactory(repositoryObjectFactory);
        factory.setRepositoryObjectLoader(repositoryObjectLoader);
    }

    @Test
    public void createClearRequestTest() {
        var request = OrderRequestFactory.createRequest(OrderOperationType.CLEAR, PARENT_UUID);
        var job = factory.createJob(request);
        assertTrue(job instanceof ClearOrderJob);
    }

    @Test
    public void createSetRequestTest() {
        var request = OrderRequestFactory.createRequest(OrderOperationType.SET, PARENT_UUID,
                Arrays.asList(CHILD1_UUID, CHILD2_UUID));
        var job = factory.createJob(request);
        assertTrue(job instanceof SetOrderJob);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void createUnsupportedOperationequestTest() {
        var request = OrderRequestFactory.createRequest(OrderOperationType.ADD_TO, PARENT_UUID,
                Arrays.asList(CHILD1_UUID));
        factory.createJob(request);
    }
}
