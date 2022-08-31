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

import edu.unc.lib.boxc.model.api.ResourceType;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.services.MembershipService;
import edu.unc.lib.boxc.operations.jms.order.OrderRequest;
import edu.unc.lib.boxc.operations.api.order.OrderValidator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Validator for a request to set order. This is a stateful class, and will only validate once per instance.
 * @author bbpennel
 */
public class SetOrderValidator implements OrderValidator {
    private RepositoryObjectLoader repositoryObjectLoader;
    private MembershipService membershipService;
    private OrderRequest request;
    private Boolean result;
    private List<String> errors = new ArrayList<>();

    @Override
    public boolean isValid() {
        if (result != null) {
            return result.booleanValue();
        }
        result = validate();
        return result;
    }

    private boolean validate() {
        var parentId = request.getParentPid().getId();
        var parentObj = repositoryObjectLoader.getRepositoryObject(request.getParentPid());
        if (!ResourceType.Work.equals(parentObj.getResourceType())) {
            errors.add("Object " + parentId + " of type " + parentObj.getResourceType().name()
                    + " does not support setting member order");
            return false;
        }

        var requestPidSet = new HashSet<>(request.getOrderedChildren());
        if (requestPidSet.size() < request.getOrderedChildren().size()) {
            var duplicates = computeDuplicates(request.getOrderedChildren());
            errors.add(formatErrorMessage(parentId, "it contained duplicate member IDs", duplicates));
        }

        var members = membershipService.listMembers(request.getParentPid());
        var membersNotInRequest = difference(members, requestPidSet);
        if (!membersNotInRequest.isEmpty()) {
            errors.add(formatErrorMessage(parentId,
                    "the following members were expected but not listed", membersNotInRequest));
        }

        var requestedNotInMembers = difference(requestPidSet, members);
        if (!requestedNotInMembers.isEmpty()) {
            errors.add(formatErrorMessage(parentId, "the following IDs are not members", requestedNotInMembers));
        }

        return errors.isEmpty();
    }

    private String formatErrorMessage(String parentId, String reason, Collection<PID> problemPids) {
        return "Invalid request to set order for " + parentId
                + ", " + reason + ": "
                + problemPids.stream().map(PID::getId).collect(Collectors.joining(", "));
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

    private List<PID> computeDuplicates(List<PID> pids) {
        // Produces a map of pids to number of times the pid appears
        return pids.stream().collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet()
                .stream()
                // filter to all the pids that appear more than once
                .filter(e -> e.getValue() > 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
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
