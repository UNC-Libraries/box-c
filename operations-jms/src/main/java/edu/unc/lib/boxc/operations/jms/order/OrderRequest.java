package edu.unc.lib.boxc.operations.jms.order;

import edu.unc.lib.boxc.model.api.ids.PID;

import java.util.List;

/**
 * Request to perform an ordering operation
 *
 * @author bbpennel
 */
public interface OrderRequest {
    /**
     * @return the operation type for this request
     */
    public OrderOperationType getOperation();

    /**
     * @return parent of the objects being ordered
     */
    public PID getParentPid();

    /**
     * @return list of ordered children
     */
    public List<PID> getOrderedChildren();
}
