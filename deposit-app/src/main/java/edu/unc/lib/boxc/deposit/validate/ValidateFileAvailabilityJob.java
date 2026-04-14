package edu.unc.lib.boxc.deposit.validate;

import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.boxc.deposit.work.AbstractDepositJob;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.rdf.CdrDeposit;
import edu.unc.lib.boxc.persist.api.exceptions.UnknownIngestSourceException;
import edu.unc.lib.boxc.persist.api.sources.IngestSource;
import edu.unc.lib.boxc.persist.api.sources.IngestSourceManager;

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
