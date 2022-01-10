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
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import javax.servlet.http.HttpServletRequest;

/**
 * Abstract controller which adds general exception handling
 *
 * @author bbpennel
 */
public abstract class AbstractErrorHandlingController {
    private static final Logger log = LoggerFactory.getLogger(AbstractErrorHandlingController.class);

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler({ InvalidRecordRequestException.class, InvalidPidException.class,
            NotFoundException.class, ResourceNotFoundException.class })
    public String handleInvalidRecordRequest(RuntimeException ex, HttpServletRequest request) {
        request.setAttribute("pageSubtitle", "Invalid request");
        log.debug("Invalid record request", ex);
        return "error/invalidRecord";
    }

    // Separate from handleInvalidRecordRequest in case need to override the status code elsewhere
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler({ AccessRestrictionException.class, AuthorizationException.class })
    public String handleForbiddenRecordRequest(RuntimeException ex, HttpServletRequest request) {
        request.setAttribute("pageSubtitle", "Invalid request");
        log.debug("Access denied", ex);
        return "error/invalidRecord";
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler({ IllegalArgumentException.class, ObjectTypeMismatchException.class })
    public String handleObjectTypeMismatchException(RuntimeException ex, WebRequest request) {
        request.setAttribute("pageSubtitle", "Invalid request", WebRequest.SCOPE_REQUEST);
        log.info("Bad request to {}: {}", getRequestUri(request), ex.getMessage());
        return "error/invalidRecord";
    }

    @ExceptionHandler({ RuntimeException.class })
    protected String handleUncaught(RuntimeException ex, WebRequest request) {
        log.error("Uncaught exception from URL {}", getRequestUri(request), ex);
        return "error/exception";
    }

    private String getRequestUri(WebRequest request) {
        return StringUtils.truncate(((ServletWebRequest) request).getRequest().getRequestURI(), 512);
    }
}
