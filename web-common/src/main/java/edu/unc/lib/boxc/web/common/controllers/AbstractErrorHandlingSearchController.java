package edu.unc.lib.boxc.web.common.controllers;

import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.fcrepo.exceptions.AuthorizationException;
import edu.unc.lib.boxc.model.api.exceptions.InvalidPidException;
import edu.unc.lib.boxc.model.api.exceptions.NotFoundException;
import edu.unc.lib.boxc.model.api.exceptions.ObjectTypeMismatchException;
import edu.unc.lib.boxc.web.common.exceptions.InvalidRecordRequestException;
import edu.unc.lib.boxc.web.common.exceptions.ResourceNotFoundException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import javax.servlet.http.HttpServletRequest;

/**
 * Search controller with default error handling
 *
 * @author bbpennel
 */
public abstract class AbstractErrorHandlingSearchController extends AbstractSolrSearchController {
    private static final Logger log = LoggerFactory.getLogger(AbstractErrorHandlingSearchController.class);

    @ExceptionHandler({ NotFoundException.class, ResourceNotFoundException.class })
    public ResponseEntity<Object> handleInvalidRecordRequest() {
        return new ResponseEntity<>("Invalid record request", HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler({ AccessRestrictionException.class, AuthorizationException.class })
    public ResponseEntity<Object> handleForbiddenRecordRequest() {
        return new ResponseEntity<>("Access denied", HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler({ IllegalArgumentException.class, ObjectTypeMismatchException.class,
            InvalidRecordRequestException.class, InvalidPidException.class })
    public ResponseEntity<Object> handleObjectTypeMismatchException(RuntimeException ex, WebRequest request) {
        log.info("Bad request to {}: {}", getRequestUri(request), ex.getMessage());
        return new ResponseEntity<>("Invalid request", HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({ RuntimeException.class })
    protected ResponseEntity<Object> handleUncaught(RuntimeException ex, WebRequest request) {
        log.error("Uncaught exception from URL {}", getRequestUri(request), ex);
        return new ResponseEntity<>("Error", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private String getRequestUri(WebRequest request) {
        return StringUtils.truncate(((ServletWebRequest) request).getRequest().getRequestURI(), 512);
    }
}
