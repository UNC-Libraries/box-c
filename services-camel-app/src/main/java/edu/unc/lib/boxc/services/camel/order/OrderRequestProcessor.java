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

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PidLockManager;
import edu.unc.lib.boxc.operations.impl.order.OrderJobFactory;
import edu.unc.lib.boxc.operations.impl.order.OrderRequestFactory;
import edu.unc.lib.boxc.operations.impl.order.OrderValidatorFactory;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingActionType;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingMessageSender;
import edu.unc.lib.boxc.operations.jms.order.MultiParentOrderRequest;
import edu.unc.lib.boxc.operations.jms.order.OrderRequest;
import edu.unc.lib.boxc.operations.jms.order.OrderRequestSerializationHelper;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;

/**
 * Processor for requests for updating the order of members
 *
 * @author bbpennel
 */
public class OrderRequestProcessor implements Processor {
    private static final Logger log = LoggerFactory.getLogger(OrderRequestProcessor.class);
    private static final PidLockManager lockManager = PidLockManager.getDefaultPidLockManager();
    private AccessControlService accessControlService;
    private OrderValidatorFactory orderValidatorFactory;
    private OrderJobFactory orderJobFactory;
    private IndexingMessageSender indexingMessageSender;
    private OrderNotificationService orderNotificationService;

    @Override
    public void process(Exchange exchange) throws Exception {
        var request = deserializeRequest(exchange);
        var successes = new ArrayList<PID>();
        var errors = new ArrayList<String>();
        for (var requestEntry: request.getParentToOrdered().entrySet()) {
            try {
                var singleRequest = OrderRequestFactory.createRequest(
                        request.getOperation(), requestEntry.getKey(), requestEntry.getValue());

                processSingleRequest(singleRequest, request.getAgent(), successes, errors);
            } catch (Exception e) {
                log.warn("Invalid order request", e);
                errors.add("Invalid order request: " + e.getMessage());
            }
        }
        orderNotificationService.sendResults(request, successes, errors);
    }

    private void processSingleRequest(OrderRequest singleRequest, AgentPrincipals agent,
                                      List<PID> successes, List<String> errors) {
        var parentPid = singleRequest.getParentPid();
        var principals = agent.getPrincipals();

        if (!accessControlService.hasAccess(parentPid, principals, Permission.orderMembers)) {
            errors.add("User " + agent.getUsername()
                    + " does not have permission to update member order for " + parentPid.getId());
            return;
        }
        // Lock the parent object to prevent simultaneous modifications
        Lock parentLock = lockManager.awaitWriteLock(parentPid);
        try {
            var validator = orderValidatorFactory.createValidator(singleRequest);
            if (validator.isValid()) {
                orderJobFactory.createJob(singleRequest).run();
                // Send message to trigger reindexing of members order
                indexingMessageSender.sendIndexingOperation(
                        agent.getUsername(), parentPid, IndexingActionType.UPDATE_MEMBER_ORDER);
                successes.add(parentPid);
            } else {
                errors.addAll(validator.getErrors());
            }
        } catch (Exception e) {
            log.error("Failed to updated order of {}", parentPid, e);
            errors.add("Encountered an error while updating " + parentPid.getId() + ": " + e.getMessage());
        } finally {
            parentLock.unlock();
        }
    }

    private MultiParentOrderRequest deserializeRequest(Exchange exchange) {
        Message in = exchange.getIn();
        try {
            return OrderRequestSerializationHelper.toRequest(in.getBody(String.class));
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to deserialize order request", e);
        }
    }

    public void setAccessControlService(AccessControlService accessControlService) {
        this.accessControlService = accessControlService;
    }

    public void setOrderValidatorFactory(OrderValidatorFactory orderValidatorFactory) {
        this.orderValidatorFactory = orderValidatorFactory;
    }

    public void setOrderJobFactory(OrderJobFactory orderJobFactory) {
        this.orderJobFactory = orderJobFactory;
    }

    public void setIndexingMessageSender(IndexingMessageSender indexingMessageSender) {
        this.indexingMessageSender = indexingMessageSender;
    }

    public void setOrderNotificationService(OrderNotificationService orderNotificationService) {
        this.orderNotificationService = orderNotificationService;
    }
}
