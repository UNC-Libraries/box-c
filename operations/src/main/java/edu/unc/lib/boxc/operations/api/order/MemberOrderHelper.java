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
package edu.unc.lib.boxc.operations.api.order;

import edu.unc.lib.boxc.model.api.ResourceType;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.operations.jms.order.OrderOperationType;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Helpers for member order operations
 *
 * @author bbpennel
 */
public class MemberOrderHelper {
    private MemberOrderHelper() {
    }

    /**
     * @param resourceType resource type to test
     * @return true if the supplied resourceType supports member ordering
     */
    public static boolean supportsMemberOrdering(ResourceType resourceType) {
        // Currently, only works support the operation, but that is expected to change in the future
        return ResourceType.Work.equals(resourceType);
    }

    /**
     * Format a standard message indicating that a resource does not support ordering
     * @param pid PID of the object that does not support operation
     * @param resourceType type of the object that does not support it
     * @return formatted message
     */
    public static String formatUnsupportedMessage(PID pid, ResourceType resourceType) {
        return "Object " + pid.getId() + " of type " + resourceType.name()
                + " does not support member ordering";
    }

    /**
     *
     * @param parentId UUID of parent object
     * @param reason reason child objects may not be ordered
     * @param problemPids PIDs of child objects that are not valid to be ordered
     * @return formatted error message
     */
    public static String formatErrorMessage(OrderOperationType type, String parentId, String reason, Collection<PID> problemPids) {
        return "Invalid request to " + type + " order for " + parentId
                + ", " + reason + ": "
                + problemPids.stream().map(PID::getId).collect(Collectors.joining(", "));
    }

    /**
     * Produces a map of pids to number of times the pid appears and collects duplicates
     * @param pids list of PIDs
     * @return list of duplicate PIDs
     */
    public static List<PID> computeDuplicates(List<PID> pids) {
        // Produces a map of pids to number of times the pid appears
        return pids.stream().collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet()
                .stream()
                // filter to all the pids that appear more than once
                .filter(e -> e.getValue() > 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
}
