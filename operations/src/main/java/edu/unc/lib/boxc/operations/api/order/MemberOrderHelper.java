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
     * @return true if the supplied resourceType supposed member ordering
     */
    public static boolean supportsMemberOrdering(ResourceType resourceType) {
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
}
