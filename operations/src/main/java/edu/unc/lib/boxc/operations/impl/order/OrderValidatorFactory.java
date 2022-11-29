package edu.unc.lib.boxc.operations.impl.order;

import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.services.MembershipService;
import edu.unc.lib.boxc.operations.jms.order.OrderRequest;
import edu.unc.lib.boxc.operations.api.order.OrderValidator;

/**
 * Factory to construct validators for order request
 *
 * @author bbpennel, snluong
 */
public class OrderValidatorFactory {
    private RepositoryObjectLoader repositoryObjectLoader;
    private MembershipService membershipService;

    /**
     * Constructs a new validator for the given request
     * @param request
     * @return new validator
     */
    public OrderValidator createValidator(OrderRequest request) {
        switch(request.getOperation()) {
        case SET: {
            var validator = new SetOrderValidator();
            validator.setMembershipService(membershipService);
            validator.setRepositoryObjectLoader(repositoryObjectLoader);
            validator.setRequest(request);
            return validator;
        }
        case CLEAR: {
            var validator = new ClearOrderValidator();
            validator.setRepositoryObjectLoader(repositoryObjectLoader);
            validator.setRequest(request);
            return validator;
        }
        case REMOVE_FROM: {
            var validator = new RemoveFromOrderValidator();
            validator.setRepositoryObjectLoader(repositoryObjectLoader);
            validator.setRequest(request);
            return validator;
        }
        default:
            throw new UnsupportedOperationException("Unable to determine validator for request " + request);
        }
    }

    public void setRepositoryObjectLoader(RepositoryObjectLoader repositoryObjectLoader) {
        this.repositoryObjectLoader = repositoryObjectLoader;
    }

    public void setMembershipService(MembershipService membershipService) {
        this.membershipService = membershipService;
    }
}
