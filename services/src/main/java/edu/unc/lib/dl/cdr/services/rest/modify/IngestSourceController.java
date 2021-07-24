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

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.net.URI;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore;
import edu.unc.lib.boxc.deposit.api.DepositMethod;
import edu.unc.lib.boxc.deposit.api.exceptions.DepositException;
import edu.unc.lib.boxc.deposit.api.submit.DepositData;
import edu.unc.lib.boxc.deposit.impl.submit.DepositSubmissionService;
import edu.unc.lib.boxc.model.api.exceptions.RepositoryException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.persist.api.PackagingType;
import edu.unc.lib.boxc.persist.api.exceptions.InvalidIngestSourceCandidateException;
import edu.unc.lib.boxc.persist.api.sources.IngestSource;
import edu.unc.lib.boxc.persist.api.sources.IngestSourceManager;

/**
 * Controller for interacting with ingest sources, including finding packages
 * available for deposit, and submitting them.
 *
 * @author bbpennel
 */
@Controller
public class IngestSourceController {
    private static final Logger log = LoggerFactory.getLogger(IngestSourceController.class);
    public static final String SOURCES_KEY = "sources";
    public static final String CANDIDATES_KEY = "candidates";

    @Autowired
    private AccessControlService aclService;

    @Autowired
    private IngestSourceManager sourceManager;

    @Autowired
    private DepositSubmissionService depositService;

    @GetMapping(value = "/edit/ingestSources/list/{pid}", produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Object> listIngestSources(@PathVariable("pid") String pid) {
        PID destination = PIDs.get(pid);

        AgentPrincipals agent = AgentPrincipalsImpl.createFromThread();
        aclService.assertHasAccess("Insufficient permissions",
                destination, agent.getPrincipals(), Permission.ingest);

        try {
            Map<String, Object> results = new HashMap<>();
            results.put(SOURCES_KEY, sourceManager.listSources(destination));
            results.put(CANDIDATES_KEY, sourceManager.listCandidates(destination));

            return new ResponseEntity<>(results, HttpStatus.OK);
        } catch (RepositoryException e) {
            Map<String, Object> results = new HashMap<>();
            results.put("error", e.getMessage());
            log.warn("Failed to retrieve ingest sources due for {}", pid, e);
            return new ResponseEntity<>(results, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping(value = "/edit/ingestSources/ingest/{pid}", produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Object> ingestFromSource(@PathVariable("pid") String pid,
            @RequestBody List<IngestPackageDetails> packages) {

        log.info("Request to ingest from source to {}", pid);
        PID destination = PIDs.get(pid);

        AgentPrincipals agent = AgentPrincipalsImpl.createFromThread();
        aclService.assertHasAccess("Insufficient permissions to ingest to " + pid,
                destination, agent.getPrincipals(), Permission.ingest);

        Map<String, Object> results = new HashMap<>();
        results.put("action", "ingest");
        results.put("destination", pid);

        List<String> depositIds = new ArrayList<>();

        // Validate the packages requested for deposit
        for (IngestPackageDetails packageDetails : packages) {
            if (isBlank(packageDetails.getPackagePath())
                    || packageDetails.getPackagingType() == null) {
                results.put("error", "Package selected for deposit was missing either a path or packaging type");
                return new ResponseEntity<>(results, HttpStatus.BAD_REQUEST);
            }

            IngestSource source = sourceManager.getIngestSourceById(packageDetails.getSourceId());
            if (source == null) {
                results.put("error", "Invalid source specified: " + packageDetails.getSourceId());
                return new ResponseEntity<>(results, HttpStatus.BAD_REQUEST);
            }

            try {
                packageDetails.setPackageUri(source.resolveRelativePath(packageDetails.getPackagePath()));
            } catch (InvalidIngestSourceCandidateException e) {
                results.put("error", "Invalid ingest Source path: " + packageDetails.getPackagePath());
                return new ResponseEntity<>(results, HttpStatus.BAD_REQUEST);
            }
        }

        // Build deposit entries and add to queue
        for (IngestPackageDetails packageDetails : packages) {
            // Generate a filename if one was not provided
            String filename = packageDetails.getLabel();
            if (isBlank(filename)) {
                filename = Paths.get(packageDetails.getPackagePath()).getFileName().toString();
            }

            DepositData deposit = new DepositData(packageDetails.getPackageUri(),
                    null,
                    packageDetails.getPackagingType(),
                    DepositMethod.CDRAPI1.getLabel(),
                    agent);
            deposit.setSlug(filename);
            deposit.setDepositorEmail(GroupsThreadStore.getEmail());

            deposit.setStaffOnly(packageDetails.getStaffOnly());
            deposit.setCreateParentFolder(packageDetails.getCreateParentFolder());

            try {
                depositIds.add(depositService.submitDeposit(destination, deposit).getId());
            } catch (DepositException e) {
                log.error("Failed to submit ingest source deposit to {}", pid, e);
                results.put("error", e.getMessage());
                return new ResponseEntity<>(results, HttpStatus.BAD_REQUEST);
            }
        }

        results.put("depositIds", depositIds);

        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    public static class IngestPackageDetails {
        private String sourceId;
        // Path is relative to the base for the source
        private String packagePath;
        private URI packageUri;
        private PackagingType packagingType;
        private String label;
        private boolean staffOnly;
        private boolean createParentFolder;

        public IngestPackageDetails() {
        }

        public IngestPackageDetails(String sourceId, String packagePath, PackagingType packagingType, String label,
                                    boolean staffOnly) {
            this.sourceId = sourceId;
            this.packagePath = packagePath;
            this.packagingType = packagingType;
            this.label = label;
            this.staffOnly = staffOnly;
        }

        public String getSourceId() {
            return sourceId;
        }

        public void setSourceId(String sourceId) {
            this.sourceId = sourceId;
        }

        public String getPackagePath() {
            return packagePath;
        }

        public void setPackagePath(String packagePath) {
            this.packagePath = packagePath;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public PackagingType getPackagingType() {
            return packagingType;
        }

        public void setPackagingType(PackagingType packagingType) {
            this.packagingType = packagingType;
        }

        public boolean getStaffOnly() {
            return staffOnly;
        }

        public void setStaffOnly(boolean staffOnly) {
            this.staffOnly = staffOnly;
        }

        public URI getPackageUri() {
            return packageUri;
        }

        public void setPackageUri(URI packageUri) {
            this.packageUri = packageUri;
        }

        public boolean getCreateParentFolder() {
            return createParentFolder;
        }

        public void setCreateParentFolder(boolean createParentFolder) {
            this.createParentFolder = createParentFolder;
        }
    }
}
