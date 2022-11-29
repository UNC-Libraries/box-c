package edu.unc.lib.boxc.operations.impl.order;

import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.jms.order.OrderOperationType;
import edu.unc.lib.boxc.operations.jms.order.OrderRequest;
import edu.unc.lib.boxc.operations.jms.order.SingleParentOrderRequest;

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
