package edu.unc.lib.boxc.deposit.normalize;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.jdom2.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositField;
import edu.unc.lib.boxc.deposit.impl.mets.METSProfile;
import edu.unc.lib.boxc.model.api.SoftwareAgentConstants.SoftwareAgent;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.rdf.Premis;
import edu.unc.lib.boxc.model.fcrepo.ids.AgentPids;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.api.events.PremisLogger;
import edu.unc.lib.boxc.persist.api.PackagingType;

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

        validateProfile(METSProfile.CDR_SIMPLE);

        interruptJobIfStopped();

        Model depModel = getReadOnlyModel();
        Model model = ModelFactory.createDefaultModel().add(depModel);

        // Store a reference to the manifest file
        addManifestURI(model);

        Document mets = loadMETS();
        // assign any missing PIDs
        assignPIDs(mets);
        // manifest updated to have record of all PIDs
        saveMETS(mets);

        Map<String, String> status = getDepositStatus();
        URI sourceUri = URI.create(status.get(DepositField.sourceUri.name()));
        Path sourceDir = Paths.get(sourceUri).getParent();

        CDRMETSGraphExtractor extractor = new CDRMETSGraphExtractor(mets, this.getDepositPID());
        LOG.info("Extractor initialized");
        extractor.addArrangement(model);
        LOG.info("Extractor arrangement added");
        extractor.helper.addFileAssociations(model, sourceDir);
        LOG.info("Extractor file associations added");
        extractor.addAccessControls(model);
        LOG.info("Extractor access controls added");

        extractor.saveDescriptions(new FilePathFunction() {
        @Override
            public String getPath(String piduri) {
                PID pid = PIDs.get(piduri);
                return getModsPath(pid, true).toAbsolutePath().toString();
            }
        });
        LOG.info("MODS descriptions saved");

        PID depositPID = getDepositPID();
        PremisLogger premisDepositLogger = getPremisLogger(depositPID);
        premisDepositLogger.buildEvent(Premis.Accession)
                .addEventDetail("Normalized deposit package from {0} to {1}", PackagingType.METS_CDR.getUri(),
                        PackagingType.BAG_WITH_N3.getUri())
                .addSoftwareAgent(AgentPids.forSoftware(SoftwareAgent.depositService))
                .write();

        commit(() -> depModel.add(model));
    }

}
