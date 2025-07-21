package edu.unc.lib.boxc.deposit.normalize;

import static edu.unc.lib.boxc.model.api.DatastreamType.ORIGINAL_FILE;

import java.io.File;
import java.net.URI;
import java.util.Map;

import org.apache.jena.rdf.model.Bag;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositField;
import edu.unc.lib.boxc.deposit.impl.model.DepositModelHelpers;
import edu.unc.lib.boxc.deposit.work.AbstractDepositJob;
import edu.unc.lib.boxc.model.api.SoftwareAgentConstants.SoftwareAgent;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.rdf.CdrDeposit;
import edu.unc.lib.boxc.model.api.rdf.Premis;
import edu.unc.lib.boxc.model.fcrepo.ids.AgentPids;
import edu.unc.lib.boxc.operations.api.events.PremisLogger;
import edu.unc.lib.boxc.persist.api.PackagingType;

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
        Model depModel = getReadOnlyModel();
        Model model = ModelFactory.createDefaultModel().add(depModel);
        Bag depositBag = model.createBag(depositPID.getRepositoryPath());

        // Generate a uuid for the main object
        PID mainPID = pidMinter.mintContentPid();

        // Identify the important file from the deposit
        Map<String, String> depositStatus = getDepositStatus();
        URI sourceUri = URI.create(depositStatus.get(DepositField.sourceUri.name()));
        String slug = depositStatus.get(DepositField.depositSlug.name());
        String mimetype = depositStatus.get(DepositField.fileMimetype.name());

        // Create the main resource as a simple resource
        Resource mainResource = model.createResource(mainPID.getRepositoryPath());

        populateFileObject(mainResource, slug, sourceUri, mimetype);

        // Store main resource as child of the deposit
        depositBag.add(mainResource);

        commit(() -> depModel.add(model));

        if (!this.getDepositDirectory().exists()) {
            log.info("Creating deposit dir {}", this.getDepositDirectory().getAbsolutePath());
            this.getDepositDirectory().mkdir();
        }

        // Add normalization event to deposit record
        PremisLogger premisDepositLogger = getPremisLogger(depositPID);
        premisDepositLogger.buildEvent(Premis.Normalization)
                .addEventDetail("Normalized deposit package from {0} to {1}",
                        PackagingType.SIMPLE_OBJECT.getUri(), PackagingType.BAG_WITH_N3.getUri())
                .addSoftwareAgent(AgentPids.forSoftware(SoftwareAgent.depositService))
                .write();
    }

    private void populateFileObject(Resource mainResource, String alabel, URI sourceUri,
            String mimetype) {
        File contentFile = new File(sourceUri);
        if (!contentFile.exists()) {
            failJob("Failed to find upload file for simple deposit: " + sourceUri,
                    contentFile.getAbsolutePath());
        }

        if (alabel == null) {
            alabel = contentFile.getName();
        }
        mainResource.addLiteral(CdrDeposit.label, alabel);
        mainResource.addProperty(RDF.type, Cdr.FileObject);

        Resource originalResc = DepositModelHelpers.addDatastream(mainResource, ORIGINAL_FILE);
        originalResc.addLiteral(CdrDeposit.size, Long.toString(contentFile.length()));
        if (mimetype != null) {
            originalResc.addLiteral(CdrDeposit.mimetype, mimetype);
        }
        // Reference the content file as the data file
        originalResc.addLiteral(CdrDeposit.stagingLocation, sourceUri.toString());
    }

}
