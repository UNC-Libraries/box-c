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
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.jms.order.OrderOperationType;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author bbpennel
 */
public class ClearOrderJobTest {
    private static final String PARENT_UUID = "f277bb38-272c-471c-a28a-9887a1328a1f";
    @Mock
    private RepositoryObjectFactory repositoryObjectFactory;
    @Mock
    private RepositoryObjectLoader repositoryObjectLoader;
    @Mock
    private WorkObject parentWork;
    private PID parentPid;
    private OrderJobFactory orderJobFactory;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        parentPid = PIDs.get(PARENT_UUID);
        orderJobFactory = new OrderJobFactory();
        orderJobFactory.setRepositoryObjectFactory(repositoryObjectFactory);
        orderJobFactory.setRepositoryObjectLoader(repositoryObjectLoader);
        when(repositoryObjectLoader.getRepositoryObject(parentPid)).thenReturn(parentWork);
    }

    @Test
    public void clearChildrenTest() {
        var request = OrderRequestFactory.createRequest(OrderOperationType.CLEAR, PARENT_UUID);
        var job = orderJobFactory.createJob(request);
        job.run();

        verify(repositoryObjectFactory).deleteProperty(parentWork, Cdr.memberOrder);
    }
}
