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

import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.api.order.OrderOperationType;
import edu.unc.lib.boxc.operations.api.order.OrderRequest;
import edu.unc.lib.boxc.operations.api.order.SingleParentOrderRequest;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Factory for creating OrderRequest objects
 *
 * @author bbpennel
 */
public class OrderRequestFactory {
    private OrderRequestFactory() {
    }

    /**
     *
     * @param operation
     * @param parentId
     * @return new OrderRequest with the provided parameters
     */
    public static OrderRequest createRequest(OrderOperationType operation,
                                             String parentId) {
        return createRequest(operation, parentId, Collections.emptyList());
    }

    /**
     * @param operation
     * @param parentId
     * @param children
     * @return new OrderRequest with the provided parameters
     */
    public static OrderRequest createRequest(OrderOperationType operation,
                                                         String parentId,
                                                         List<String> children) {
        var request = new SingleParentOrderRequest();
        request.setOperation(operation);
        request.setParentPid(PIDs.get(parentId));
        request.setOrderedChildren(children.stream().map(PIDs::get).collect(Collectors.toList()));
        return request;
    }
}
