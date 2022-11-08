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
package edu.unc.lib.boxc.deposit.validate;

import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.services.MembershipService;
import edu.unc.lib.boxc.operations.api.order.OrderValidator;

import java.util.ArrayList;
import java.util.List;

/**
 * Validator that determines if the deposit object's member order
 * property is valid for setting member order
 * @author snluong
 */
public class DepositSetMemberOrderValidator implements OrderValidator {
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
        return true;
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
}
