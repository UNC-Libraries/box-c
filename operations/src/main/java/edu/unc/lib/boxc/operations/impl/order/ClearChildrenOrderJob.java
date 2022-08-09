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

import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.operations.api.order.OrderChildrenRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Job which clears the order of children in a container
 *
 * @author bbpennel
 */
public class ClearChildrenOrderJob implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(ClearChildrenOrderJob.class);

    private RepositoryObjectFactory repositoryObjectFactory;
    private OrderChildrenRequest request;

    @Override
    public void run() {
        log.debug("Deleting order property for {}", request.getParentObject().getPid());
        repositoryObjectFactory.deleteProperty(request.getParentObject(), Cdr.memberOrder);
    }

    public void setRepositoryObjectFactory(RepositoryObjectFactory repositoryObjectFactory) {
        this.repositoryObjectFactory = repositoryObjectFactory;
    }

    public void setRequest(OrderChildrenRequest request) {
        this.request = request;
    }
}
