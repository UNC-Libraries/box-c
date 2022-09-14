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
package edu.unc.lib.boxc.services.camel.order;

import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.operations.jms.order.MultiParentOrderRequest;

import java.util.List;

/**
 * Builds the email body for OrderNotificationService
 * @author snluong
 */
public class OrderNotificationBuilder {
    /**
     * Build a String containing info about the results of a MultiParentOrderRequest
     * @param request
     * @param successes list of pids for objects that successfully updated
     * @param errors list of errors for unsuccessful order operations
     */
    public String construct(MultiParentOrderRequest request, List<PID> successes, List<String> errors) {
        var parentCount = request.getParentToOrdered().keySet().size();
        var successCount = successes.size();
        var emailBody = "Here are the results of your bulk SetOrderUpdate request.\n";

        emailBody += "Number of parent objects requested: " + parentCount + "\n";
        emailBody += "Number successfully updated: " + successCount + "\n";

        StringBuilder emailErrors;
        if (errors.isEmpty()) {
            emailErrors = new StringBuilder("There were no errors.");
        } else {
            emailErrors = new StringBuilder("There were the following errors:\n");
            for (String error : errors) {
                emailErrors.append("-- ").append(error).append("\n");
            }
        }
        emailBody += emailErrors.toString();
        return emailBody;
    }
}
