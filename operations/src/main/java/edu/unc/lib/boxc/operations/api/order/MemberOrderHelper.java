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
     * Format a standard message indicating the RepositoryObject could not be loaded
     * @param pid PID of the object that cannot be found
     * @return formatted message
     */
    public static String formatNotFoundMessage(OrderOperationType type, String parentId) {
        return "Invalid request to " + type + " order for " + parentId
                + ", RepositoryObjectLoader could not load object with that ID";
    }

    /**
     *
     * @param parentId UUID of parent object
     * @param reason reason child objects may not be ordered
     * @param problemPids PIDs of child objects that are not valid to be ordered
     * @return formatted error message
     */
    public static String formatErrorMessage(OrderOperationType type,
                                            String parentId,
                                            String reason,
                                            Collection<PID> problemPids) {
        return "Invalid request to " + type + " order for " + parentId
                + ", " + reason + ": "
                + problemPids.stream().map(PID::getId).sorted().collect(Collectors.joining(", "));
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

    /**
     * Serialize a list of PIDs for storage in the repository
     * @param pids list of PIDs
     * @return String containing pids as an order field value.
     */
    public static String serializeOrder(List<PID> pids) {
        return pids.stream().map(PID::getId).collect(Collectors.joining("|"));
    }
}
