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

import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.deposit.work.AbstractDepositJob;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.persist.api.ingest.IngestSource;
import edu.unc.lib.dl.persist.api.ingest.IngestSourceManager;
import edu.unc.lib.dl.persist.api.ingest.UnknownIngestSourceException;
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

    private IngestSourceManager sourceManager;

    public ValidateFileAvailabilityJob() {
    }

    public ValidateFileAvailabilityJob(String uuid, String depositUUID) {
        super(uuid, depositUUID);
    }

    @Override
    public void runJob() {

        Set<String> badlyStagedFiles = new HashSet<>();

        Model model = getReadOnlyModel();
        // Construct a map of objects to file paths to verify
        List<Entry<PID, String>> hrefs = getPropertyPairList(model, CdrDeposit.stagingLocation);

        setTotalClicks(hrefs.size());

        // Verify that the deposit is still running before proceeding with check
        interruptJobIfStopped();

        // Iterate through local file hrefs and verify that each one exists
        for (Entry<PID, String> entry : hrefs) {
            String href = entry.getValue();
            try {
                URI manifestURI = URI.create(entry.getValue());
                // If no ingest source can be found for the file, then file not available
                IngestSource source = sourceManager.getIngestSourceForUri(manifestURI);
                if (!source.exists(manifestURI)) {
                    log.debug("Failed find staged file {} in deposit {}", href, getDepositUUID());
                    badlyStagedFiles.add(href);
                }
            } catch (UnknownIngestSourceException e) {
                log.debug("Could not determine staging location for {} in deposit {}", href, getDepositUUID(), e);
                badlyStagedFiles.add(href);
            }

            addClicks(1);
        }

        // Generate failure message of all files from invalid staging locations
        int invalidCount = badlyStagedFiles.size();
        if (invalidCount > 0) {
            StringBuilder sbInvalid = new StringBuilder(badlyStagedFiles.size() +
                    " files referenced by the deposit are located in invalid staging areas:\n");
            for (String file : badlyStagedFiles) {
                sbInvalid.append(" - ").append(file).append("\n");
            }

            failJob("Deposit references invalid files", (sbInvalid.toString()));
        }
    }

    /**
     * @param sourceManager the sourceManager to set
     */
    public void setIngestSourceManager(IngestSourceManager sourceManager) {
        this.sourceManager = sourceManager;
    }
}
