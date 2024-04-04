package edu.unc.lib.boxc.web.services.rest.exceptions;

import edu.unc.lib.boxc.model.api.exceptions.InvalidOperationForObjectType;
import java.io.EOFException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.fcrepo.exceptions.AuthorizationException;
import edu.unc.lib.boxc.model.api.exceptions.InvalidPidException;
import edu.unc.lib.boxc.model.api.exceptions.NotFoundException;

/**
 *
 * @author bbpennel
 *
 */
@ControllerAdvice
public class RestResponseEntityExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(RestResponseEntityExceptionHandler.class);

    @ExceptionHandler(value = { AccessRestrictionException.class })
    protected ResponseEntity<Object> handleAccessRestriction(RuntimeException ex, WebRequest request) {
        AgentPrincipals agent = AgentPrincipalsImpl.createFromThread();
        log.warn("User {} has insufficient permissions to perform requested action {}",
                agent.getUsername(), getRequestUri(request));
        String bodyOfResponse = "Insufficient permissions";
        return handleExceptionInternal(ex, bodyOfResponse, new HttpHeaders(), HttpStatus.FORBIDDEN, request);
    }

    @ExceptionHandler(value = { AuthorizationException.class })
    protected ResponseEntity<Object> handleAuthorizationException(RuntimeException ex, WebRequest request) {
        log.error("Failed to authenticate with fedora", ex);
        String bodyOfResponse = "Authentication failure";
        return handleExceptionInternal(ex, bodyOfResponse, new HttpHeaders(), HttpStatus.FORBIDDEN, request);
    }

    @ExceptionHandler(value = { InvalidPidException.class })
    protected ResponseEntity<Object> handleInvalidPidException(RuntimeException ex, WebRequest request) {
        return handleExceptionInternal(ex, ex.getMessage(), new HttpHeaders(), HttpStatus.FORBIDDEN, request);
    }

    @ExceptionHandler(value = { NotFoundException.class })
    protected ResponseEntity<Object> handleObjectNotFound(RuntimeException ex, WebRequest request) {
        String bodyOfResponse = "Object not found";
        return handleExceptionInternal(ex, bodyOfResponse, new HttpHeaders(), HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler(value = { InvalidOperationForObjectType.class })
    protected ResponseEntity<Object> handleInvalidOperationForObjectType(RuntimeException ex, WebRequest request) {
        String bodyOfResponse = "Unsupported operation for object type";
        return handleExceptionInternal(ex, bodyOfResponse, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(value = { IllegalArgumentException.class })
    protected ResponseEntity<Object> handleIllegalArgumentException(RuntimeException ex, WebRequest request) {
        log.warn("Illegal argument in request to {}: {}", getRequestUri(request), ex.getMessage());
        return handleExceptionInternal(ex, ex.getMessage(), new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(value = { EOFException.class })
    public ResponseEntity<Object> handleEofException(EOFException ex, WebRequest request) {
        log.debug("Client closed connection to {}", getRequestUri(request), ex);
        return null;
    }

    @ExceptionHandler(value = { Exception.class })
    protected ResponseEntity<Object> handleUncaught(Exception ex, WebRequest request) {
        try {
            String bodyOfResponse = "Uncaught error";
            log.error("Uncaught exception from {}", getRequestUri(request), ex);
            return handleExceptionInternal(ex, bodyOfResponse, new HttpHeaders(),
                    HttpStatus.INTERNAL_SERVER_ERROR, request);
        } catch (Exception e) {
            log.error("Error occurred while handling uncaught exception {}", getRequestUri(request), e);
            return null;
        }
    }

    // Extract the query path of the current request
    private String getRequestUri(WebRequest request) {
        return ((ServletWebRequest) request).getRequest().getRequestURI();
    }
}
