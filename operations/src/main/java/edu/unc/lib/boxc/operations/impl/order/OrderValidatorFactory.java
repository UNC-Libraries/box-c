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
import edu.unc.lib.boxc.model.api.services.MembershipService;
import edu.unc.lib.boxc.operations.api.order.OrderRequest;
import edu.unc.lib.boxc.operations.api.order.OrderValidator;

/**
 * Factory to construct validators for order request
 *
 * @author bbpennel
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
        var validator = new SetOrderValidator();
        validator.setMembershipService(membershipService);
        validator.setRepositoryObjectLoader(repositoryObjectLoader);
        validator.setRequest(request);
        return validator;
    }

    public void setRepositoryObjectLoader(RepositoryObjectLoader repositoryObjectLoader) {
        this.repositoryObjectLoader = repositoryObjectLoader;
    }

    public void setMembershipService(MembershipService membershipService) {
        this.membershipService = membershipService;
    }
}
