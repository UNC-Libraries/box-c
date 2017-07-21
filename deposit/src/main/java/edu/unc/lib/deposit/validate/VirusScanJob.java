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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.philvarner.clamavj.ClamScan;
import com.philvarner.clamavj.ScanResult;

import edu.unc.lib.deposit.work.AbstractDepositJob;
import edu.unc.lib.dl.event.PremisEventBuilder;
import edu.unc.lib.dl.event.PremisLogger;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.CdrDeposit;
import edu.unc.lib.dl.rdf.Premis;
import edu.unc.lib.dl.util.SoftwareAgentConstants.SoftwareAgent;

/**
 * Scans all staged files registered in the deposit for viruses.
 * 
 * @author count0
 *
 */
public class VirusScanJob extends AbstractDepositJob {
    private static final Logger log = LoggerFactory
            .getLogger(VirusScanJob.class);

    private ClamScan clamScan;

    public VirusScanJob() {
        super();
    }

    public ClamScan getClamScan() {
        return clamScan;
    }

    public void setClamScan(ClamScan clamScan) {
        this.clamScan = clamScan;
    }

    public VirusScanJob(String uuid, String depositUUID) {
        super(uuid, depositUUID);
    }

    @Override
    public void runJob() {
        log.debug("Running virus checks on : {}", getDepositDirectory());

        Map<String, String> failures = new HashMap<>();

        Model model = getReadOnlyModel();
        List<Entry<PID, String>> hrefs = getPropertyPairList(model, CdrDeposit.stagingLocation);

        setTotalClicks(hrefs.size());
        int scannedObjects = 0;

        for (Entry<PID, String> href : hrefs) {
            verifyRunning();

            URI manifestURI = getStagedUri(href.getValue());

            File file = new File(manifestURI.getPath());

            ScanResult result = clamScan.scan(file);

            switch (result.getStatus()) {
            case FAILED:
                failures.put(manifestURI.toString(), result.getSignature());
                break;
            case ERROR:
                throw new Error(
                        "Virus checks are producing errors: "
                                + result.getException()
                                        .getLocalizedMessage());
            case PASSED:
                PremisLogger premisLogger = getPremisLogger(href.getKey());
                PremisEventBuilder premisEventBuilder = premisLogger.buildEvent(Premis.VirusCheck);

                premisEventBuilder.addSoftwareAgent(SoftwareAgent.clamav.getFullname())
                    .addEventDetail("File passed pre-ingest scan for viruses")
                    .write();

                scannedObjects++;
                break;
            }
            addClicks(1);
        }

        if (failures.size() > 0) {
            StringBuilder sb = new StringBuilder("Virus checks failed for some files:\n");
            for (String uri : failures.keySet()) {
                sb.append(uri).append(" - ").append(failures.get(uri)).append("\n");
            }
            failJob(failures.size() + " virus check(s) failed.", sb.toString());
        } else {
            if (scannedObjects != hrefs.size()) {
                failJob("Virus scan job did not attempt to scan all files.",
                        (hrefs.size() - scannedObjects) + " objects were not scanned.");
            }

            PID depositPID = getDepositPID();
            PremisLogger premisDepositLogger = getPremisLogger(depositPID);
            premisDepositLogger.buildEvent(Premis.VirusCheck)
                    .addSoftwareAgent(SoftwareAgent.clamav.getFullname())
                    .addEventDetail(scannedObjects + "files scanned for viruses.")
                    .write();
        }
    }
}