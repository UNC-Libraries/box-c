package edu.unc.lib.boxc.operations.jms.order;

import edu.unc.lib.boxc.model.api.ids.PID;

import java.util.List;

/**
 * Request object for updating the order of children for a single container
 *
 * @author bbpennel
 */
public class SingleParentOrderRequest implements OrderRequest {
    private PID parentPid;
    private List<PID> orderedChildren;
    private OrderOperationType operation;

    @Override
    public PID getParentPid() {
        return parentPid;
    }

    public void setParentPid(PID parentPid) {
        this.parentPid = parentPid;
    }

    @Override
    public List<PID> getOrderedChildren() {
        return orderedChildren;
    }

    public void setOrderedChildren(List<PID> orderedChildren) {
        this.orderedChildren = orderedChildren;
    }

    @Override
    public OrderOperationType getOperation() {
        return operation;
    }

    public void setOperation(OrderOperationType operation) {
        this.operation = operation;
    }

    @Override
    public String toString() {
        return "SingleParentOrderRequest{" +
                "parentPid=" + parentPid +
                ", orderedChildren=" + orderedChildren +
                ", operation=" + operation +
                '}';
    }
}
