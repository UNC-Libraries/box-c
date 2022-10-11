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
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.operations.jms.order.OrderRequest;
import org.apache.jena.atlas.lib.ListUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Collectors;

/**
 * Job which removes member(s) and resets order of children for a container object
 *
 * @author snluong
 */
public class RemoveFromOrderJob implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(RemoveFromOrderJob.class);

    private RepositoryObjectLoader repositoryObjectLoader;
    private RepositoryObjectFactory repositoryObjectFactory;
    private OrderRequest request;

    @Override
    public void run() {
        var workObject = repositoryObjectLoader.getWorkObject(request.getParentPid());
        var memberOrder = workObject.getMemberOrder();
        var childrenToRemove = request.getOrderedChildren();
        // subtract children from original order
        memberOrder.removeAll(childrenToRemove);
        // transform PIDs into UUIDs
        var updatedOrder = memberOrder.stream().map(PID::getId).collect(Collectors.joining("|"));
        log.debug("Updating order property for {} to {}", request.getParentPid(), updatedOrder);
        repositoryObjectFactory.createExclusiveRelationship(workObject, Cdr.memberOrder, updatedOrder);
    }

    public void setRepositoryObjectFactory(RepositoryObjectFactory repositoryObjectFactory) {
        this.repositoryObjectFactory = repositoryObjectFactory;
    }

    public void setRepositoryObjectLoader(RepositoryObjectLoader repositoryObjectLoader) {
        this.repositoryObjectLoader = repositoryObjectLoader;
    }

    public void setRequest(OrderRequest request) {
        this.request = request;
    }
}
