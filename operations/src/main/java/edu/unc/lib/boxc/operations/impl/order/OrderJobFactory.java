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
import edu.unc.lib.boxc.operations.api.order.OrderRequest;
import edu.unc.lib.boxc.operations.api.order.SingleParentOrderRequest;

/**
 * Factory which constructs jobs for updating ordering of objects
 *
 * @author bbpennel
 */
public class OrderJobFactory {
    private RepositoryObjectFactory repositoryObjectFactory;
    private RepositoryObjectLoader repositoryObjectLoader;

    /**
     * @param request
     * @return new ordering job runnable
     */
    public Runnable createJob(OrderRequest request) {
        switch(request.getOperation()) {
        case SET: {
            var job = new SetOrderJob();
            job.setRepositoryObjectFactory(repositoryObjectFactory);
            job.setRepositoryObjectLoader(repositoryObjectLoader);
            job.setRequest((SingleParentOrderRequest) request);
            return job;
        }
        case CLEAR: {
            var job = new ClearOrderJob();
            job.setRepositoryObjectFactory(repositoryObjectFactory);
            job.setRepositoryObjectLoader(repositoryObjectLoader);
            job.setRequest((SingleParentOrderRequest) request);
            return job;
        }
        default:
            throw new UnsupportedOperationException("Unable to determine job type for request " + request);
        }
    }

    public void setRepositoryObjectFactory(RepositoryObjectFactory repositoryObjectFactory) {
        this.repositoryObjectFactory = repositoryObjectFactory;
    }

    public void setRepositoryObjectLoader(RepositoryObjectLoader repositoryObjectLoader) {
        this.repositoryObjectLoader = repositoryObjectLoader;
    }
}
