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
package edu.unc.lib.deposit.validate;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.deposit.staging.StagingException;
import edu.unc.lib.deposit.staging.StagingPolicyManager;
import edu.unc.lib.deposit.work.AbstractDepositJob;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.CdrDeposit;

/**
 * Verifies that files referenced by this deposit for ingest are present and
 * available
 *
 * @author count0
 *
 */
public class ValidateFileAvailabilityJob extends AbstractDepositJob {
    private static final Logger log = LoggerFactory
            .getLogger(ValidateFileAvailabilityJob.class);

    private StagingPolicyManager policyManager;

    public ValidateFileAvailabilityJob() {
    }

    public ValidateFileAvailabilityJob(String uuid, String depositUUID) {
        super(uuid, depositUUID);
    }

    @Override
    public void runJob() {

        Set<String> failures = new HashSet<>();
        Set<String> badlyStagedFiles = new HashSet<>();

        Model model = getReadOnlyModel();
        // Construct a map of objects to file paths to verify
        List<Entry<PID, String>> hrefs = new ArrayList<>();
        hrefs.addAll(getPropertyPairList(model, CdrDeposit.stagingLocation));

        setTotalClicks(hrefs.size());

        // Verify that the deposit is still running before proceeding with check
        verifyRunning();

        // Iterate through local file hrefs and verify that each one exists
        for (Entry<PID, String> entry : hrefs) {
            String href = entry.getValue();
            try {
                URI manifestURI = getStagedUri(href);

                File file = new File(manifestURI.getPath());
                if (!file.exists()) {
                    failures.add(manifestURI.toString());
                }

                if (!policyManager.isValidStagingLocation(manifestURI)) {
                    badlyStagedFiles.add(manifestURI.toString());
                }
            } catch (StagingException e) {
                log.debug("Failed to get staged file in deposit {}", getDepositUUID(), e);
                badlyStagedFiles.add(href);
            }

            addClicks(1);
        }

        // Generate failure message of all files from invalid staging locations
        StringBuilder sbInvalid = null;
        int invalidCount = badlyStagedFiles.size();
        if (invalidCount > 0) {
            sbInvalid = new StringBuilder(badlyStagedFiles.size() +
                    " files referenced by the deposit are located in invalid staging areas:\n");
            for (String file : badlyStagedFiles) {
                sbInvalid.append(" - ").append(file).append("\n");
            }
        }

        // Generate failure message of all missing files
        StringBuilder sbFailure = null;
        int failureCount = failures.size();
        if (failureCount > 0) {
            sbFailure = new StringBuilder(failureCount + "  files referenced by the deposit could not be found:\n");
            for (String uri : failures) {
                sbFailure.append(" - ").append(uri).append("\n");
            }
        }

        // fails job if any files were from invalid staging areas or could not be found
        if (invalidCount > 0 && failureCount > 0) {
            failJob("Deposit references invalid files", (sbInvalid.toString() + sbFailure.toString()));
        } else if (invalidCount > 0) {
            failJob("Deposit references invalid files", (sbInvalid.toString()));
        } else if (failureCount > 0) {
            failJob("Deposit references invalid files", (sbFailure.toString()));
        }
    }

    public void setStagingPolicyManager(StagingPolicyManager policyManager) {
        this.policyManager = policyManager;
    }

    private void addLocations(List<Entry<PID, String>> hrefs, Model model, Property property) {
        List<Entry<PID, String>> additions = getPropertyPairList(model, property);
        hrefs.addAll(additions);
    }
}
