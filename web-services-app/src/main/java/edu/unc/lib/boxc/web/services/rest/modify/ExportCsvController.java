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
package edu.unc.lib.boxc.web.services.rest.modify;

import static edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore.getAgentPrincipals;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import edu.unc.lib.boxc.model.api.exceptions.NotFoundException;
import edu.unc.lib.boxc.model.api.exceptions.RepositoryException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.web.common.controllers.AbstractSolrSearchController;
import edu.unc.lib.boxc.web.services.processing.ExportCsvService;

/**
 * Controller which generates a CSV listing of a repository object
 * and all of its children, recursively depth first.
 *
 * @author bbpennel
 */
@Controller
@RequestMapping("exportTree/csv")
public class ExportCsvController extends AbstractSolrSearchController {
    private static final Logger log = LoggerFactory.getLogger(ExportCsvController.class);

    @Autowired
    private ExportCsvService exportCsvService;

    @RequestMapping(value = "{pid}", method = RequestMethod.GET)
    public ResponseEntity<Object> export(@PathVariable("pid") String pidString, HttpServletRequest request,
                                                      HttpServletResponse response) {
        PID pid = PIDs.get(pidString);

        try {
            String filename = pid.getId().replace(":", "_") + ".csv";
            response.addHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
            response.addHeader("Content-Type", "text/csv");

            exportCsvService.streamCsv(pid, getAgentPrincipals(), response.getOutputStream());

            response.setStatus(HttpStatus.OK.value());
        } catch (NotFoundException e) {
            log.warn("Object {} not found: {}", pid, e.getMessage());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (RepositoryException | IOException e) {
            log.error("Error exporting CSV for {}, {}", pidString, e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return null;
    }


}