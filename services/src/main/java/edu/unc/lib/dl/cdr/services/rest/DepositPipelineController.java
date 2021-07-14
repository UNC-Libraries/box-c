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
package edu.unc.lib.dl.cdr.services.rest;

import static edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore.getAgentPrincipals;
import static edu.unc.lib.dl.util.RedisWorkerConstants.DepositPipelineState.quieted;
import static edu.unc.lib.dl.util.RedisWorkerConstants.DepositPipelineState.shutdown;
import static java.util.Collections.singletonMap;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.fcrepo.services.GlobalPermissionEvaluator;
import edu.unc.lib.dl.util.DepositPipelineStatusFactory;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositPipelineAction;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositPipelineState;

/**
 * Controller for API endpoints to interact with the deposit pipeline.
 *
 * @author bbpennel
 */
@Controller
@RequestMapping(value = { "/edit/depositPipeline" })
public class DepositPipelineController {
    private static final Logger log = LoggerFactory
            .getLogger(DepositPipelineController.class);

    public static final String ERROR_KEY = "error";
    public static final String STATE_KEY = "state";
    public static final String ACTION_KEY = "requestedAction";

    @Autowired
    private DepositPipelineStatusFactory pipelineStatusFactory;

    @Autowired
    private GlobalPermissionEvaluator globalPermissionEvaluator;

    /**
     * Retrieve the current state of the deposit pipeline
     *
     * @return json response containing the state, or an error if unauthorized
     */
    @GetMapping(produces = APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<Object> getState() {
        AccessGroupSet principals = getAgentPrincipals().getPrincipals();
        if (!globalPermissionEvaluator.hasGlobalPermission(principals, Permission.ingest)) {
            return new ResponseEntity<>(singletonMap(ERROR_KEY, "Unauthorized"), HttpStatus.UNAUTHORIZED);
        }

        DepositPipelineState pipelineState = pipelineStatusFactory.getPipelineState();
        String pipeline = pipelineState == null ? "unknown" : pipelineState.toString();

        log.debug("Retrieve deposit pipeline state {}", pipelineState);

        return new ResponseEntity<>(singletonMap(STATE_KEY, pipeline), HttpStatus.OK);
    }

    /**
     * Request an action be taken on the deposit pipeline
     *
     * @param actionName name of the action being requested
     * @return response indicating the requested action if successful, or an error if unauthorized
     *      or an invalid action was requested
     */
    @PostMapping( path = { "{actionName}", "/{actionName}" }, produces = APPLICATION_JSON_VALUE )
    public @ResponseBody ResponseEntity<Object> requestAction(@PathVariable String actionName) {
        AccessGroupSet principals = getAgentPrincipals().getPrincipals();
        if (!globalPermissionEvaluator.hasGlobalPermission(principals, Permission.createAdminUnit)) {
            return new ResponseEntity<>(singletonMap(ERROR_KEY, "Unauthorized"), HttpStatus.UNAUTHORIZED);
        }

        DepositPipelineAction action = DepositPipelineAction.fromName(actionName);
        if (action == null) {
            String allowed = Arrays.stream(DepositPipelineAction.values())
                    .map(DepositPipelineAction::name)
                    .collect(Collectors.joining(", "));
            return new ResponseEntity<>(
                    singletonMap(ERROR_KEY, "Invalid action specified, must specify one of the follow: " + allowed),
                    HttpStatus.BAD_REQUEST);
        }

        DepositPipelineState currentState = pipelineStatusFactory.getPipelineState();
        String errorMsg = null;
        if (DepositPipelineState.stopped.equals(currentState) || shutdown.equals(currentState)) {
            errorMsg = "Cannot perform actions while in the '" + currentState.name()
                    + "' state, the service must be restarted";
        } else if (DepositPipelineAction.quiet.equals(action) && !DepositPipelineState.active.equals(currentState)) {
            errorMsg = "Cannot perform quiet, the pipeline must be 'active' but is " +  currentState.name();
        } else if (DepositPipelineAction.unquiet.equals(action) && !quieted.equals(currentState)) {
            errorMsg = "Cannot perform unquiet, the pipeline must be 'quieted' but is " +  currentState.name();
        }

        if (errorMsg != null) {
            return new ResponseEntity<>(singletonMap(ERROR_KEY, errorMsg),
                    HttpStatus.CONFLICT);
        }

        log.info("Requesting deposit pipeline action {}", actionName);
        pipelineStatusFactory.requestPipelineAction(action);

        return new ResponseEntity<>(singletonMap(ACTION_KEY, actionName), HttpStatus.OK);
    }

}
