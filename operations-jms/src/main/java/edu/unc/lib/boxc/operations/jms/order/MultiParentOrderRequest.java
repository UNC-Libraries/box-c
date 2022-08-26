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
package edu.unc.lib.boxc.operations.jms.order;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;

import java.util.List;
import java.util.Map;

/**
 * Request object for updating the order of children for a multiple containers
 *
 * @author bbpennel
 */
public class MultiParentOrderRequest {
    private OrderOperationType operation;
    private Map<String, List<String>> parentToOrdered;
    @JsonDeserialize(as = AgentPrincipalsImpl.class)
    private AgentPrincipals agent;
    private String email;

    /**
     * @return the operation type for this request
     */
    public OrderOperationType getOperation() {
        return operation;
    }

    public void setOperation(OrderOperationType operation) {
        this.operation = operation;
    }

    public void setParentToOrdered(Map<String, List<String>> parentToOrdered) {
        this.parentToOrdered = parentToOrdered;
    }

    /**
     * @return the mapping of parent ids to the ordered list of all of their children
     */
    public Map<String, List<String>> getParentToOrdered() {
        return parentToOrdered;
    }

    public AgentPrincipals getAgent() {
        return agent;
    }

    public void setAgent(AgentPrincipals agent) {
        this.agent = agent;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
