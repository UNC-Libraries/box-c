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
package edu.unc.lib.deposit.transfer;

import static edu.unc.lib.dl.fcrepo4.RepositoryPathConstants.DEPOSIT_RECORD_BASE;
import static edu.unc.lib.dl.model.DatastreamPids.getDatastreamHistoryPid;
import static edu.unc.lib.dl.model.DatastreamPids.getDepositManifestPid;
import static edu.unc.lib.dl.model.DatastreamPids.getOriginalFilePid;
import static edu.unc.lib.dl.model.DatastreamPids.getTechnicalMetadataPid;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toSet;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.jena.rdf.model.Bag;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.deposit.work.AbstractDepositJob;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.model.DatastreamPids;
import edu.unc.lib.dl.persist.api.transfer.BinaryTransferSession;
import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.rdf.CdrDeposit;
import edu.unc.lib.dl.util.JobStatusFactory;

/**
 * Job which transfers binaries included in this deposit to the appropriate destination
 * storage location.
 *
 * @author bbpennel
 *
 */
public class TransferBinariesToStorageJob extends AbstractDepositJob {

    private static final Logger log = LoggerFactory.getLogger(TransferBinariesToStorageJob.class);

    private static final Set<Resource> TYPES_ALLOWING_DESC = new HashSet<>(asList(
            Cdr.Folder, Cdr.Work, Cdr.Collection, Cdr.AdminUnit, Cdr.FileObject));

    /**
     *
     */
    public TransferBinariesToStorageJob() {
    }

    /**
     * @param uuid
     * @param depositUUID
     */
    public TransferBinariesToStorageJob(String uuid, String depositUUID) {
        super(uuid, depositUUID);
    }

    @Override
    public void runJob() {
        Model model = getWritableModel();
        Bag depositBag = model.getBag(getDepositPID().getRepositoryPath());

        // All objects in deposit should have the same destination, so pull storage loc from deposit record
        try (BinaryTransferSession transferSession = getTransferSession(model)) {
            transferBinaries(depositBag, transferSession);
        }
    }

    private void transferBinaries(Resource resc, BinaryTransferSession transferSession) {
        PID objPid = PIDs.get(resc.toString());

        Set<Resource> rescTypes = resc.listProperties(RDF.type).toList().stream()
                .map(Statement::getResource).collect(toSet());

        if (TYPES_ALLOWING_DESC.stream().anyMatch(rescTypes::contains)) {
            transferModsHistoryFile(objPid, resc, transferSession);
        }

        if (rescTypes.contains(Cdr.FileObject)) {
            transferOriginalFile(objPid, resc, transferSession);
            transferFitsExtract(objPid, resc, transferSession);
        } else if (objPid.getQualifier().equals(DEPOSIT_RECORD_BASE)) {
            transferDepositManifests(objPid, resc, transferSession);
        }

        NodeIterator iterator = getChildIterator(resc);
        // No more children, nothing further to do in this tree
        if (iterator == null) {
            return;
        }

        try {
            while (iterator.hasNext()) {
                Resource childResc = (Resource) iterator.next();
                transferBinaries(childResc, transferSession);
            }
        } finally {
            iterator.close();
        }
    }

    private void transferOriginalFile(PID objPid, Resource resc, BinaryTransferSession transferSession) {
        JobStatusFactory jobStatusFactory = getJobStatusFactory();
        // add storageUri if doesn't already exist. It will exist in a resume scenario.
        if (resc.hasProperty(CdrDeposit.stagingLocation) && !resc.hasProperty(CdrDeposit.storageUri) &&
                !jobStatusFactory.objectIsIngested(jobUUID, objPid.getId())) {
            PID originalPid = getOriginalFilePid(objPid);
            URI stagingUri = URI.create(resc.getProperty(CdrDeposit.stagingLocation).getString());
            URI storageUri = transferSession.transfer(originalPid, stagingUri);
            resc.addLiteral(CdrDeposit.storageUri, storageUri.toString());
            jobStatusFactory.addObjectIngested(jobUUID, objPid.getId());
        }
    }

    private void transferModsHistoryFile(PID objPid, Resource resc, BinaryTransferSession transferSession) {
        JobStatusFactory jobStatusFactory = getJobStatusFactory();
        if (!resc.hasProperty(CdrDeposit.descriptiveHistoryStorageUri) &&
                !jobStatusFactory.objectIsIngested(jobUUID, objPid.getId())) {
            PID modsPid = DatastreamPids.getMdDescriptivePid(objPid);
            PID dsHistoryPid = getDatastreamHistoryPid(modsPid);

            Path stagingPath = getModsHistoryPath(objPid);

            if (Files.exists(stagingPath)) {
                URI stagingUri = stagingPath.toUri();
                URI storageUri = transferSession.transfer(dsHistoryPid, stagingUri);
                resc.addLiteral(CdrDeposit.descriptiveHistoryStorageUri, storageUri.toString());
                jobStatusFactory.addObjectIngested(jobUUID, objPid.getId());
            }
        }
    }

    private void transferFitsExtract(PID objPid, Resource resc, BinaryTransferSession transferSession) {
        JobStatusFactory jobStatusFactory = getJobStatusFactory();
        if (!resc.hasProperty(CdrDeposit.fitsStorageUri) &&
                !jobStatusFactory.objectIsIngested(jobUUID, objPid.getId())) {
            PID fitsPid = getTechnicalMetadataPid(objPid);
            URI stagingUri = getTechMdPath(objPid, false).toUri();
            URI storageUri = transferSession.transfer(fitsPid, stagingUri);
            resc.addLiteral(CdrDeposit.fitsStorageUri, storageUri.toString());
            jobStatusFactory.addObjectIngested(jobUUID, objPid.getId());
        }
    }

    private void transferDepositManifests(PID objPid, Resource resc, BinaryTransferSession transferSession) {
        List<String> manifestURIs = getDepositStatusFactory().getManifestURIs(getDepositUUID());
        for (String manifestPath : manifestURIs) {
            URI manifestUri = URI.create(manifestPath);
            File manifestFile = new File(manifestUri);
            if (!manifestFile.exists()) {
                log.warn("Manifest {} does not exist, it may have already been transferred in deposit {}",
                        manifestPath, getDepositUUID());
                continue;
            }

            JobStatusFactory jobStatusFactory = getJobStatusFactory();
            if (!jobStatusFactory.objectIsIngested(jobUUID, objPid.getId())) {
                PID manifestPid = getDepositManifestPid(objPid, manifestFile.getName());
                URI storageUri = transferSession.transfer(manifestPid, manifestUri);
                resc.addLiteral(CdrDeposit.storageUri, storageUri.toString());
                jobStatusFactory.addObjectIngested(jobUUID, objPid.getId());
            }
        }
    }
}
