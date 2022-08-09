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

import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.operations.api.order.OrderChildrenRequest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;

import static org.mockito.Mockito.verify;

/**
 * @author bbpennel
 */
public class ClearChildrenOrderJobTest {
    @Mock
    private RepositoryObjectFactory repositoryObjectFactory;
    @Mock
    private WorkObject parentWork;
    private ClearChildrenOrderJob job;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        job = new ClearChildrenOrderJob();
        job.setRepositoryObjectFactory(repositoryObjectFactory);
    }

    @Test
    public void clearChildrenTest() {
        var request = new OrderChildrenRequest();
        request.setParentObject(parentWork);
        request.setOrderedChildren(Collections.emptyList());
        job.setRequest(request);
        job.run();

        verify(repositoryObjectFactory).deleteProperty(parentWork, Cdr.memberOrder);
    }
}
