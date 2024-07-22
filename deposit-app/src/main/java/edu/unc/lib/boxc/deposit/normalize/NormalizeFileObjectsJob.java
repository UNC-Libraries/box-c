package edu.unc.lib.boxc.deposit.normalize;

import static edu.unc.lib.boxc.deposit.work.DepositGraphUtils.getChildIterator;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.jena.rdf.model.Bag;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;

import edu.unc.lib.boxc.deposit.impl.model.DepositModelHelpers;
import edu.unc.lib.boxc.deposit.work.AbstractDepositJob;
import edu.unc.lib.boxc.model.api.SoftwareAgentConstants.SoftwareAgent;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.rdf.CdrAcl;
import edu.unc.lib.boxc.model.api.rdf.CdrDeposit;
import edu.unc.lib.boxc.model.api.rdf.Premis;
import edu.unc.lib.boxc.model.fcrepo.ids.AgentPids;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.api.events.PremisLogger;

/**
 * Normalization job to ensure all FileObjects being ingested are wrapped in
 * Work objects, generating a work for the file object if none is present.
 *
 * @author bbpennel
 *
 */
public class NormalizeFileObjectsJob extends AbstractDepositJob {

    public NormalizeFileObjectsJob() {
        super();
    }

    public NormalizeFileObjectsJob(String uuid, String depositUUID) {
        super(uuid, depositUUID);
    }

    @Override
    public void runJob() {
        Model model = getReadOnlyModel();
        Bag depositBag = model.getBag(getDepositPID().getURI().toString());

        normalizeChildren(depositBag);
    }

    private void normalizeChildren(Resource parent) {
        NodeIterator childIt = getChildIterator(parent);
        if (childIt == null) {
            return;
        }

        while (childIt.hasNext()) {
            Resource childResc = childIt.next().asResource();
            // Found work, nothing needs to be added, skip
            if (childResc.hasProperty(RDF.type, Cdr.Work)) {
                continue;
            }
            if (childResc.hasProperty(RDF.type, Cdr.FileObject)) {
                // Found standalone fileObject, wrap with work
                commit(() -> wrapWithWork(parent, childResc));
            } else {
                // Found other type, traverse
                normalizeChildren(childResc);
            }
        }
    }

    private void wrapWithWork(Resource parent, Resource fileResc) {
        Model model = parent.getModel();

        URI stagingUri = URI.create(getStagingLocation(fileResc));
        String filename = new File(stagingUri).getName();

        PID filePid = PIDs.get(fileResc.getURI());

        // Construct the new Work object
        PID workPid = pidMinter.mintContentPid();

        Bag workBag = model.createBag(workPid.getURI());
        workBag.addProperty(RDF.type, Cdr.Work);
        workBag.addProperty(CdrDeposit.label, filename);

        // Add premis event for work creation
        addPremisEvent(workPid, filePid);

        // Switch parent from linking to fileObject to Work
        Bag parentBag = model.getBag(parent);
        unlinkChild(parentBag, fileResc);
        parentBag.add(workBag);
        workBag.add(fileResc);

        // Transfer MODS from file to work
        transferDescription(filePid, workPid);

        // Move ACLs and other properties from file to work
        transferProperties(fileResc, workBag.asResource());
    }

    private void unlinkChild(Bag parentBag, Resource fileResc) {
        StmtIterator stmtIt = parentBag.getModel().listStatements(parentBag, null, fileResc);
        Statement stmt = stmtIt.next();
        parentBag.remove(stmt);
        stmtIt.close();
    }

    private void transferDescription(PID filePid, PID workPid) {
        Path modsFile = getModsPath(filePid);
        if (Files.exists(modsFile)) {
            Path modsDest = getModsPath(workPid, true);
            try {
                Files.move(modsFile, modsDest);
            } catch (IOException e) {
                failJob(e, "Failed to transfer description file {0} to work at {1}",
                        modsFile, modsDest);
            }
        }
    }

    private void transferProperties(Resource fileResc, Resource workResc) {
        Model model = fileResc.getModel();

        // StmtIterator stmtIt = fileResc.getModel().listStatements(parentBag, null, fileResc);
        List<Statement> aclStmts = new ArrayList<>();

        StmtIterator stmtIt = fileResc.listProperties();

        while (stmtIt.hasNext()) {
            Statement stmt = stmtIt.nextStatement();
            Property pred = stmt.getPredicate();
            if (!CdrAcl.NS.equals(pred.getNameSpace())) {
                continue;
            }

            aclStmts.add(stmt);
        }
        stmtIt.close();

        model.remove(aclStmts);

        for (Statement stmt : aclStmts) {
            model.add(workResc, stmt.getPredicate(), stmt.getObject());
        }

    }

    private String getStagingLocation(Resource resc) {
        Resource origResc = DepositModelHelpers.getDatastream(resc);
        if (origResc == null) {
            return null;
        }
        return origResc.getProperty(CdrDeposit.stagingLocation).getString();
    }

    private void addPremisEvent(PID workPid, PID filePid) {
        PremisLogger premisDepositLogger = getPremisLogger(workPid);
        premisDepositLogger.buildEvent(Premis.Creation)
                .addEventDetail("Work created to contain FileObject {0}",
                        filePid.getRepositoryPath())
                .addSoftwareAgent(AgentPids.forSoftware(SoftwareAgent.depositService))
                .write();
    }
}
