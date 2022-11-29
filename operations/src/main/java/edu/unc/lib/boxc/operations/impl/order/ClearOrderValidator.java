package edu.unc.lib.boxc.operations.impl.order;

import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.operations.api.order.OrderValidator;
import edu.unc.lib.boxc.operations.jms.order.OrderRequest;

import java.util.ArrayList;
import java.util.List;

import static edu.unc.lib.boxc.operations.api.order.MemberOrderHelper.formatUnsupportedMessage;
import static edu.unc.lib.boxc.operations.api.order.MemberOrderHelper.supportsMemberOrdering;

/**
 * Validator for a request to clear member order of a work
 * @author snluong
 */
public class ClearOrderValidator implements OrderValidator {
    private RepositoryObjectLoader repositoryObjectLoader;
    private OrderRequest request;
    private Boolean result;
    private List<String> errors = new ArrayList<>();

    @Override
    public boolean isValid() {
        if (result != null) {
            return result;
        }
        result = validate();
        return result;
    }

    private boolean validate() {
        var parentPid = request.getParentPid();
        var parentObj = repositoryObjectLoader.getRepositoryObject(parentPid);
        if (!supportsMemberOrdering(parentObj.getResourceType())) {
            errors.add(formatUnsupportedMessage(parentPid, parentObj.getResourceType()));
            return false;
        }
        return true;
    }

    @Override
    public List<String> getErrors() {
        return errors;
    }

    public void setRepositoryObjectLoader(RepositoryObjectLoader repositoryObjectLoader) {
        this.repositoryObjectLoader = repositoryObjectLoader;
    }

    public void setRequest(OrderRequest request) {
        this.request = request;
    }
}
