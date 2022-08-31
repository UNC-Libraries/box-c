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
 * Service which sends user notifications about the outcome of order updating requests
 *
 * @author bbpennel
 */
public class OrderNotificationService {
    /**
     * Send a notification about the results of an order operation caused by a MultiParentOrderRequest
     * @param request
     * @param successes list of pids for objects that successfully updated
     * @param errors
     */
    public void sendResults(MultiParentOrderRequest request, List<PID> successes, List<String> errors) {
        // TODO implement this
    }
}
