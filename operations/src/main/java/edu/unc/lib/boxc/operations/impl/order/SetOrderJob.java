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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Collectors;

/**
 * Job which sets/overwrites the order of children for a container object
 *
 * @author bbpennel
 */
public class SetOrderJob implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(SetOrderJob.class);

    private RepositoryObjectLoader repositoryObjectLoader;
    private RepositoryObjectFactory repositoryObjectFactory;
    private OrderRequest request;

    @Override
    public void run() {
        var parentObject = repositoryObjectLoader.getRepositoryObject(request.getParentPid());
        var order = request.getOrderedChildren().stream().map(PID::getId).collect(Collectors.joining("|"));
        log.debug("Updating order property for {} to {}", request.getParentPid(), order);
        repositoryObjectFactory.createExclusiveRelationship(parentObject, Cdr.memberOrder, order);
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
