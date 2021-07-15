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
package edu.unc.lib.dl.cdr.services.rest.modify;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.dl.persist.api.ingest.DepositData;
import edu.unc.lib.dl.persist.services.ingest.DepositSubmissionService;
import edu.unc.lib.dl.util.DepositMethod;
import edu.unc.lib.dl.util.PackagingType;
import edu.unc.lib.dl.util.UnsupportedPackagingTypeException;

/**
 * Controller for handling ingest submission requests
 *
 * @author bbpennel
 *
 */
@Controller
public class IngestController {
    private static final Logger log = LoggerFactory.getLogger(IngestController.class);

    @Autowired
    private DepositSubmissionService depositService;

    @PostMapping(value = "edit/ingest/{pid}")
    public @ResponseBody
    ResponseEntity<Object> ingestPackageController(@PathVariable("pid") String pid,
            @RequestParam("type") String type, @RequestParam(value = "name", required = false) String name,
            @RequestParam("file") MultipartFile ingestFile, HttpServletRequest request, HttpServletResponse response) {

        PID destPid = PIDs.get(pid);

        AgentPrincipals agent = AgentPrincipalsImpl.createFromThread();
        PackagingType packaging = PackagingType.getPackagingType(type);

        DepositData deposit = null;
        try {
            deposit = new DepositData(ingestFile.getInputStream(),
                    ingestFile.getOriginalFilename(),
                    ingestFile.getContentType(),
                    packaging,
                    DepositMethod.CDRAPI1.getLabel(),
                    agent);
            deposit.setDepositorEmail(GroupsThreadStore.getEmail());
            deposit.setSlug(name);
        } catch (IOException e) {
            log.error("Failed to get submitted file", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        PID depositPid;
        try {
            depositPid = depositService.submitDeposit(destPid, deposit);
        } catch (AccessRestrictionException e) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        } catch (UnsupportedPackagingTypeException e) {
            log.debug("Cannot handle deposit with packaging {}", type);
            return new ResponseEntity<>("Unsupported deposit package type " + type, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("Failed to submit deposit", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("action", "ingest");
        result.put("destination", pid);
        result.put("depositId", depositPid.getId());

        return new ResponseEntity<>(result, HttpStatus.OK);
    }
}
