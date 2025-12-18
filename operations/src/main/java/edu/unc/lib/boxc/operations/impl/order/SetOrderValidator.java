package edu.unc.lib.boxc.operations.impl.order;

import edu.unc.lib.boxc.model.api.exceptions.NotFoundException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.services.MembershipService;
import edu.unc.lib.boxc.operations.api.order.OrderValidator;
import edu.unc.lib.boxc.operations.jms.order.OrderOperationType;
import edu.unc.lib.boxc.operations.jms.order.OrderRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static edu.unc.lib.boxc.operations.api.order.MemberOrderHelper.formatNotFoundMessage;
import static edu.unc.lib.boxc.operations.api.order.MemberOrderHelper.formatUnsupportedMessage;
import static edu.unc.lib.boxc.operations.api.order.MemberOrderHelper.supportsMemberOrdering;
import static edu.unc.lib.boxc.operations.api.order.MemberOrderHelper.formatErrorMessage;
import static edu.unc.lib.boxc.operations.api.order.MemberOrderHelper.computeDuplicates;

/**
 * Validator for a request to set order. This is a stateful class, and will only validate once per instance.
 * @author bbpennel
 */
public class SetOrderValidator implements OrderValidator {
    private static final Logger LOG = LoggerFactory.getLogger(SetOrderValidator.class);
    private RepositoryObjectLoader repositoryObjectLoader;
    private MembershipService membershipService;
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
        var parentId = request.getParentPid().getId();

        try {
            var parentObj = repositoryObjectLoader.getRepositoryObject(request.getParentPid());
            if (!supportsMemberOrdering(parentObj.getResourceType())) {
                errors.add(formatUnsupportedMessage(request.getParentPid(), parentObj.getResourceType()));
                return false;
            }

            var requestPidSet = new HashSet<>(request.getOrderedChildren());
            if (requestPidSet.size() < request.getOrderedChildren().size()) {
                var duplicates = computeDuplicates(request.getOrderedChildren());
                errors.add(formatErrorMessage(OrderOperationType.SET,
                        parentId, "it contained duplicate member IDs", duplicates));
            }

            var members = membershipService.listMembers(request.getParentPid());
            var formattedMembers = members.stream().map(PID::getId).sorted().collect(Collectors.joining(", "));
            LOG.error("Parent pid is {}", parentId);
            LOG.error("Members are {}", formattedMembers);
            LOG.error("Requested Pids are {}", requestPidSet);
            var membersNotInRequest = difference(members, requestPidSet);
            if (!membersNotInRequest.isEmpty()) {
                errors.add(formatErrorMessage(OrderOperationType.SET, parentId,
                        "the following members were expected but not listed", membersNotInRequest));
            }

            var requestedNotInMembers = difference(requestPidSet, members);
            if (!requestedNotInMembers.isEmpty()) {
                errors.add(formatErrorMessage(OrderOperationType.SET,
                        parentId, "the following IDs are not members", requestedNotInMembers));
            }

            return errors.isEmpty();
        } catch (NotFoundException e) {
            errors.add(formatNotFoundMessage(OrderOperationType.SET, parentId));
            return false;
        }
    }

    /**
     * @param setOne
     * @param setTwo
     * @return the difference from (setOne - setTwo)
     */
    private static Set<PID> difference(Collection<PID> setOne, Collection<PID> setTwo) {
        Set<PID> result = new HashSet<>(setOne);
        result.removeAll(setTwo);
        return result;
    }

    @Override
    public List<String> getErrors() {
        return errors;
    }

    public void setRepositoryObjectLoader(RepositoryObjectLoader repositoryObjectLoader) {
        this.repositoryObjectLoader = repositoryObjectLoader;
    }

    public void setMembershipService(MembershipService membershipService) {
        this.membershipService = membershipService;
    }

    public void setRequest(OrderRequest request) {
        this.request = request;
    }
}
