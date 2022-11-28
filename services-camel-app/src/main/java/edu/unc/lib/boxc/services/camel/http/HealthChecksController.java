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
package edu.unc.lib.boxc.services.camel.http;

import org.apache.camel.CamelContext;
import org.apache.camel.health.HealthCheck;
import org.apache.camel.health.HealthCheckHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Controller for performing health checks on the application and camel context
 *
 * @author bbpennel
 */
@Controller
public class HealthChecksController {
    @Autowired
    private CamelContext metaServicesRouter;

    /**
     * API endpoint which checks if the camel context is available.
     * @return response code of 503 if camel is not started, 200 if it is
     */
    @RequestMapping(value = "/health/camelUp", method = RequestMethod.GET)
    public @ResponseBody ResponseEntity<Object> isUpCheck() {
        var resp = HealthCheckHelper.invoke(metaServicesRouter);
        for (var result : resp) {
            if (!HealthCheck.State.UP.equals(result.getState())) {
                return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
            }
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
