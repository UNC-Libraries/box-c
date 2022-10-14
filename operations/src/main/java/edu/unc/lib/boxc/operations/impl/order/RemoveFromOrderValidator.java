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

import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.operations.api.order.OrderValidator;
import edu.unc.lib.boxc.operations.jms.order.OrderOperationType;
import edu.unc.lib.boxc.operations.jms.order.OrderRequest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static edu.unc.lib.boxc.operations.api.order.MemberOrderHelper.formatUnsupportedMessage;
import static edu.unc.lib.boxc.operations.api.order.MemberOrderHelper.supportsMemberOrdering;
import static edu.unc.lib.boxc.operations.api.order.MemberOrderHelper.computeDuplicates;
import static edu.unc.lib.boxc.operations.api.order.MemberOrderHelper.formatErrorMessage;

/**
 * Validator for a request to remove member(s) from order of a work
 * @author snluong
 */
public class RemoveFromOrderValidator implements OrderValidator {
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
        var parentId = request.getParentPid().getId();
        var parentObj = repositoryObjectLoader.getRepositoryObject(request.getParentPid());
        if (!supportsMemberOrdering(parentObj.getResourceType())) {
            errors.add(formatUnsupportedMessage(request.getParentPid(), parentObj.getResourceType()));
            return false;
        }

        var requestPidSet = new HashSet<>(request.getOrderedChildren());
        if (requestPidSet.size() < request.getOrderedChildren().size()) {
            var duplicates = computeDuplicates(request.getOrderedChildren());
            errors.add(formatErrorMessage(OrderOperationType.REMOVE_FROM, parentId, "it contained duplicate member IDs", duplicates));
        }

        return errors.isEmpty();
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
