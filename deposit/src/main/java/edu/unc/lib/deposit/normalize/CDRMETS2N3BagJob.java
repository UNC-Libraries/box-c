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

import org.jdom2.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

import edu.unc.lib.dl.event.PremisLogger;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Premis;
import edu.unc.lib.dl.util.PackagingType;
import edu.unc.lib.dl.util.SoftwareAgentConstants.SoftwareAgent;
import edu.unc.lib.dl.xml.METSProfile;

/**
 * 
 * @author bbpennel
 *
 */
public class CDRMETS2N3BagJob extends AbstractMETS2N3BagJob {
    private static final Logger LOG = LoggerFactory.getLogger(CDRMETS2N3BagJob.class);
    public CDRMETS2N3BagJob() {
        super();
    }

    public CDRMETS2N3BagJob(String uuid, String depositUUID) {
        super(uuid, depositUUID);
    }

    @Override
    public void runJob() {
        validateMETS();
        // Store a reference to the manifest file
        addManifestURI();
        validateProfile(METSProfile.CDR_SIMPLE);
        Document mets = loadMETS();
        // assign any missing PIDs
        assignPIDs(mets);
        // manifest updated to have record of all PIDs
        saveMETS(mets);

        Model model = getWritableModel();
        CDRMETSGraphExtractor extractor = new CDRMETSGraphExtractor(mets, this.getDepositPID());
        LOG.info("Extractor initialized");
        extractor.addArrangement(model);
        LOG.info("Extractor arrangement added");
        extractor.helper.addFileAssociations(model, true);
        LOG.info("Extractor file associations added");
        extractor.addAccessControls(model);
        LOG.info("Extractor access controls added");

        final File modsFolder = getDescriptionDir();
        modsFolder.mkdir();
        extractor.saveDescriptions(new FilePathFunction() {
        @Override
            public String getPath(String piduri) {
                String uuid = PIDs.get(piduri).getUUID();
                return new File(modsFolder, uuid + ".xml").getAbsolutePath();
            }
        });
        LOG.info("MODS descriptions saved");

        PID depositPID = getDepositPID();
        PremisLogger premisDepositLogger = getPremisLogger(depositPID);
        Resource premisDepositEvent = premisDepositLogger.buildEvent(Premis.Normalization)
                .addEventDetail("Normalized deposit package from {0} to {1}", PackagingType.METS_CDR.getUri(),
                        PackagingType.BAG_WITH_N3.getUri())
                .addSoftwareAgent(SoftwareAgent.depositService.getFullname())
                .create();
        premisDepositLogger.writeEvent(premisDepositEvent);
    }

}
