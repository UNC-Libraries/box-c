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

import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.api.order.OrderValidator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static edu.unc.lib.boxc.deposit.work.DepositGraphUtils.getChildIterator;

/**
 * Validator that determines if the deposit object's member order
 * property is valid for setting member order
 * @author snluong
 */
public class DepositSetMemberOrderValidator implements OrderValidator {
    private Boolean result;
    private List<String> errors = new ArrayList<>();
    private Resource resource;

    @Override
    public boolean isValid() {
        if (result != null) {
            return result;
        }
        result = validate();
        return result;
    }

    private boolean validate() {
        var resourceId = PIDs.get(resource.getURI()).getId();
        var errorMsgStart = "Invalid member order for " + resourceId + ", ";
        // check if resource is valid for member ordering
        if (!resource.hasProperty(RDF.type, Cdr.Work)) {
            errors.add("Object " + resourceId + " does not support member ordering");
            return false;
        }

        var order = resource.getProperty(Cdr.memberOrder).getString();
        var memberOrderIds = Arrays.asList(order.split("\\|"));
        var distinctIds = new HashSet<>(memberOrderIds);
        var childIds = getChildIds(resource);

        // Make sure there are no duplicates in the member order list
        if (distinctIds.size() < memberOrderIds.size()) {
            var duplicates = findDuplicates(memberOrderIds);
            errors.add(errorMsgStart + "it contained duplicate member IDs: " + convertToString(duplicates) );
        }

        // compare list of children against the order ids to verify they are children
        var nonMemberErrorMsg = errorMsgStart + "the following IDs are not members: ";
        validateIds(distinctIds, childIds, nonMemberErrorMsg);

        // Make sure all the children are accounted for in the member order list
        var childrenNotAllPresentErrorMsg = errorMsgStart + "the following members were expected but not listed: ";
        validateIds(childIds, distinctIds, childrenNotAllPresentErrorMsg);

        return errors.isEmpty();
    }

    @Override
    public List<String> getErrors() {
        return errors;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    private String convertToString(Set<String> set) {
        return String.join(", ", set);
    }

    private HashSet<String> getChildIds(Resource resource) {
        var iterator = getChildIterator(resource);
        var childIds = new HashSet<String>();
        while (iterator.hasNext()) {
            // collect all the ids into a list, passing through PIDs.get() so we can just get the id
            Resource childResource = (Resource) iterator.next();
            var childId = PIDs.get(childResource.getURI()).getId();
            childIds.add(childId);
        }
        return childIds;
    }
    /*
        we will try to add the ID in memberOrderIds to the noDuplicates set
        if it returns false, that means there is already an identical ID in the set
        then we'll add it to the duplicates set instead
     */
    private Set<String> findDuplicates(List<String> memberOrderIds) {
        Set<String> duplicates = new HashSet<>();
        Set<String> noDuplicates = new HashSet<>();
        for (String id : memberOrderIds) {
            if (!noDuplicates.add(id)) {
                duplicates.add(id);
            }
        }
        return duplicates;
    }

    private void validateIds(Set<String> mainSet, Set<String> setToSubtract, String errorMsg) {
        var leftoverIds = getLeftoverIds(mainSet, setToSubtract);
        if (!leftoverIds.isEmpty()) {
            errors.add(errorMsg + convertToString(leftoverIds));
        }
    }

    private Set<String> getLeftoverIds(Set<String> mainSet, Set<String> setToSubtract) {
        var leftoverSet = new HashSet<>(mainSet);
        leftoverSet.removeAll(setToSubtract);
        return leftoverSet;
    }
}
