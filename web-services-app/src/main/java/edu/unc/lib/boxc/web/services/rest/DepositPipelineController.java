package edu.unc.lib.boxc.web.services.rest;

import static edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore.getAgentPrincipals;
import static edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositPipelineState.quieted;
import static edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositPipelineState.shutdown;
import static java.util.Collections.singletonMap;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.Arrays;
import java.util.stream.Collectors;

import edu.unc.lib.boxc.deposit.api.PipelineAction;
import edu.unc.lib.boxc.deposit.impl.jms.DepositPipelineMessage;
import edu.unc.lib.boxc.deposit.impl.jms.DepositPipelineMessageService;
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
import edu.unc.lib.boxc.auth.api.services.GlobalPermissionEvaluator;
import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositPipelineAction;
import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositPipelineState;
import edu.unc.lib.boxc.deposit.impl.model.DepositPipelineStatusFactory;

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
    private DepositPipelineMessageService depositPipelineMessageService;

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
    public @ResponseBody ResponseEntity<Object> requestAction(@PathVariable("actionName") String actionName) {
        var agent = getAgentPrincipals();
        AccessGroupSet principals = agent.getPrincipals();
        if (!globalPermissionEvaluator.hasGlobalPermission(principals, Permission.createAdminUnit)) {
            return new ResponseEntity<>(singletonMap(ERROR_KEY, "Unauthorized"), HttpStatus.UNAUTHORIZED);
        }

        PipelineAction action;
        try {
            action = PipelineAction.valueOf(actionName.toUpperCase());
        } catch (IllegalArgumentException e) {
            String allowed = Arrays.stream(DepositPipelineAction.values())
                    .map(DepositPipelineAction::name)
                    .collect(Collectors.joining(", "));
            return new ResponseEntity<>(
                    singletonMap(ERROR_KEY, "Invalid action specified, must specify one of the follow: " + allowed),
                    HttpStatus.BAD_REQUEST);
        }

        String errorMsg = getInvalidStateMessage(action);
        if (errorMsg != null) {
            return new ResponseEntity<>(singletonMap(ERROR_KEY, errorMsg),
                    HttpStatus.CONFLICT);
        }

        log.info("Requesting deposit pipeline action {}", action);
        var message = new DepositPipelineMessage();
        message.setAction(action);
        message.setUsername(agent.getUsername());
        depositPipelineMessageService.sendDepositPipelineMessage(message);
        log.info("Sent deposit pipeline action {}", action);

        return new ResponseEntity<>(singletonMap(ACTION_KEY, actionName), HttpStatus.OK);
    }

    private String getInvalidStateMessage(PipelineAction action) {
        DepositPipelineState currentState = pipelineStatusFactory.getPipelineState();
        String errorMsg = null;
        if (DepositPipelineState.stopped.equals(currentState) || shutdown.equals(currentState)) {
            errorMsg = "Cannot perform actions while in the '" + currentState.name()
                    + "' state, the service must be restarted";
        } else if (PipelineAction.QUIET.equals(action) && !DepositPipelineState.active.equals(currentState)) {
            errorMsg = "Cannot perform quiet, the pipeline must be 'active' but is " +  currentState.name();
        } else if (PipelineAction.UNQUIET.equals(action) && !quieted.equals(currentState)) {
            errorMsg = "Cannot perform unquiet, the pipeline must be 'quieted' but is " +  currentState.name();
        }
        return errorMsg;
    }

}
