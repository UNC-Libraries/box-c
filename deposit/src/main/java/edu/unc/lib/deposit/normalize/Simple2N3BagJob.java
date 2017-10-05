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
package edu.unc.lib.deposit.normalize;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.Map;

import org.apache.jena.rdf.model.Bag;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.UriUtils;

import edu.unc.lib.deposit.work.AbstractDepositJob;
import edu.unc.lib.dl.event.PremisLogger;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.CdrDeposit;
import edu.unc.lib.dl.rdf.Premis;
import edu.unc.lib.dl.util.DepositConstants;
import edu.unc.lib.dl.util.PackagingType;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositField;
import edu.unc.lib.dl.util.SoftwareAgentConstants.SoftwareAgent;

/**
 * Normalizes a simple deposit object into an N3 deposit structure.
 *
 * Expects to receive a single file in the data directory, as referenced in deposit status.
 *
 * @author count0
 * @date Jun 20, 2014
 */
public class Simple2N3BagJob extends AbstractDepositJob {

    private static final Logger log = LoggerFactory.getLogger(Simple2N3BagJob.class);

    public Simple2N3BagJob() {
        super();
    }

    public Simple2N3BagJob(String uuid, String depositUUID) {
        super(uuid, depositUUID);
    }

    @Override
    public void runJob() {

        // deposit RDF bag
        PID depositPID = getDepositPID();
        Model model = getWritableModel();
        Bag depositBag = model.createBag(depositPID.getURI().toString());

        // Generate a uuid for the main object
        PID mainPID = pidMinter.mintContentPid();

        // Identify the important file from the deposit
        Map<String, String> depositStatus = getDepositStatus();
        String filename = depositStatus.get(DepositField.fileName.name());
        String slug = depositStatus.get(DepositField.depositSlug.name());
        String mimetype = depositStatus.get(DepositField.fileMimetype.name());

        // Create the main resource as a simple resource
        Resource mainResource = model.createResource(mainPID.getURI());

        populateFileObject(model, mainResource, slug, filename, mimetype);

        // Store main resource as child of the deposit
        depositBag.add(mainResource);

        if (!this.getDepositDirectory().exists()) {
            log.info("Creating deposit dir {}", this.getDepositDirectory().getAbsolutePath());
            this.getDepositDirectory().mkdir();
        }

        // Add normalization event to deposit record
        PremisLogger premisDepositLogger = getPremisLogger(depositPID);
        Resource premisDepositEvent = premisDepositLogger.buildEvent(Premis.Normalization)
                .addEventDetail("Normalized deposit package from {0} to {1}",
                        PackagingType.SIMPLE_OBJECT.getUri(), PackagingType.BAG_WITH_N3.getUri())
                .addSoftwareAgent(SoftwareAgent.depositService.getFullname())
                .create();
        premisDepositLogger.writeEvent(premisDepositEvent);
    }

    private void populateFileObject(Model model, Resource mainResource, String alabel, String filename,
            String mimetype) {
        File contentFile = new File(this.getDataDirectory(), filename);
        if (!contentFile.exists()) {
            failJob("Failed to find upload file for simple deposit: " + filename,
                    contentFile.getAbsolutePath());
        }

        if (alabel == null) {
            alabel = contentFile.getName();
        }
        model.add(mainResource, CdrDeposit.label, alabel);
        model.add(mainResource, CdrDeposit.size, Long.toString(contentFile.length()));
        if (mimetype != null) {
            model.add(mainResource, CdrDeposit.mimetype, mimetype);
        }

        // Reference the content file as the data file
        try {
            model.add(mainResource, CdrDeposit.stagingLocation,
                    DepositConstants.DATA_DIR + "/" + UriUtils.encodePathSegment(contentFile.getName(), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            failJob(e, "Failed to add staging location for {0} due to encoding issues", contentFile.getName());
        }
    }

}
