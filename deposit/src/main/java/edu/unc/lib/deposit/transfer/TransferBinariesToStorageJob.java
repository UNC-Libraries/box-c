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

import static edu.unc.lib.deposit.work.DepositGraphUtils.getChildIterator;
import static edu.unc.lib.dl.fcrepo4.RepositoryPathConstants.DEPOSIT_RECORD_BASE;
import static edu.unc.lib.dl.model.DatastreamPids.getDepositManifestPid;
import static edu.unc.lib.dl.model.DatastreamPids.getOriginalFilePid;
import static edu.unc.lib.dl.model.DatastreamPids.getTechnicalMetadataPid;
import static edu.unc.lib.dl.util.DigestAlgorithm.DEFAULT_ALGORITHM;
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
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import edu.unc.lib.deposit.work.AbstractDepositJob;
import edu.unc.lib.dl.exceptions.InvalidChecksumException;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectFactory;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.model.DatastreamPids;
import edu.unc.lib.dl.persist.api.storage.BinaryDetails;
import edu.unc.lib.dl.persist.api.transfer.BinaryAlreadyExistsException;
import edu.unc.lib.dl.persist.api.transfer.BinaryTransferOutcome;
import edu.unc.lib.dl.persist.api.transfer.BinaryTransferSession;
import edu.unc.lib.dl.persist.services.deposit.DepositModelHelpers;
import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.rdf.CdrDeposit;

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

    @Autowired
    private RepositoryObjectFactory repoObjFactory;

    private Model model;

    /**
     *
     */
    public TransferBinariesToStorageJob() {
        this.rollbackDatasetOnFailure = false;
    }

    /**
     * @param uuid
     * @param depositUUID
     */
    public TransferBinariesToStorageJob(String uuid, String depositUUID) {
        super(uuid, depositUUID);
        this.rollbackDatasetOnFailure = false;
    }

    @Override
    public void runJob() {
        model = getReadOnlyModel();
        Bag depositBag = model.getBag(getDepositPID().getRepositoryPath());

        // Count how many objects are being deposited
        int i = 0;
        ResIterator subjectIterator = model.listSubjects();
        while (subjectIterator.hasNext()) {
            Resource resc = subjectIterator.next();
            // Only count subjects that have a type defined, which excludes binary resources
            if (resc.hasProperty(RDF.type)) {
                i++;
            }
        }

        resetClicks();
        setTotalClicks(i);

        // All objects in deposit should have the same destination, so pull storage loc from deposit record
        try (BinaryTransferSession transferSession = getTransferSession(model)) {
            transferBinaries(depositBag, transferSession);
        }
    }

    private void transferBinaries(Resource resc, BinaryTransferSession transferSession) {
        interruptJobIfStopped();

        PID objPid = PIDs.get(resc.toString());
        log.debug("Preparing to transfer binaries for {}", objPid);

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

        addClicks(1);

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
        // add storageUri if doesn't already exist. It will exist in a resume scenario.
        if (datastreamNotTransferred(resc, CdrDeposit.hasDatastreamOriginal)) {
            PID originalPid = getOriginalFilePid(objPid);
            Resource originalResc = DepositModelHelpers.getDatastream(resc);

            URI stagingUri = URI.create(originalResc.getProperty(CdrDeposit.stagingLocation).getString());
            transferFile(originalPid, stagingUri, transferSession, resc, CdrDeposit.hasDatastreamOriginal);
            log.debug("Finished transferring original file for {}", originalPid.getQualifiedId());
        }
    }

    private void transferModsHistoryFile(PID objPid, Resource resc, BinaryTransferSession transferSession) {
        if (datastreamNotTransferred(resc, CdrDeposit.hasDatastreamDescriptiveHistory)) {
            PID modsPid = DatastreamPids.getMdDescriptivePid(objPid);
            PID historyPid = DatastreamPids.getDatastreamHistoryPid(modsPid);

            Path stagingPath = getModsHistoryPath(objPid);
            if (Files.exists(stagingPath)) {
                transferFile(historyPid, stagingPath.toUri(), transferSession, resc,
                        CdrDeposit.hasDatastreamDescriptiveHistory);
                log.debug("Finished transferring MODS history file {}", modsPid.getQualifiedId());
            }
        }
    }

    private void transferFitsExtract(PID objPid, Resource resc, BinaryTransferSession transferSession) {
        if (datastreamNotTransferred(resc, CdrDeposit.hasDatastreamFits)) {
            PID fitsPid = getTechnicalMetadataPid(objPid);

            Path fitsPath = getTechMdPath(objPid, false);
            if (Files.notExists(fitsPath)) {
                failJob("Missing technical metadata datastream",
                        "Missing technical metadata datastream for FileObject " + objPid);
            }
            URI stagingUri = getTechMdPath(objPid, false).toUri();
            transferFile(fitsPid, stagingUri, transferSession, resc, CdrDeposit.hasDatastreamFits);
            log.debug("Finished transferring techmd file {}", fitsPid.getQualifiedId());
        }
    }

    private boolean datastreamNotTransferred(Resource resc, Property datastreamProp) {
        return !resc.hasProperty(datastreamProp) ||
               !resc.getPropertyResourceValue(datastreamProp)
                        .hasProperty(CdrDeposit.storageUri);
    }

    private void transferDepositManifests(PID objPid, Resource resc, BinaryTransferSession transferSession) {
        List<Statement> manifestStmts = resc.listProperties(CdrDeposit.hasDatastreamManifest).toList();
        for (Statement manifestStmt : manifestStmts) {
            Resource manifestResc = manifestStmt.getResource();

            String manifestPath = manifestResc.getProperty(CdrDeposit.stagingLocation).getString();
            URI manifestUri = URI.create(manifestPath);
            File manifestFile = new File(manifestUri);
            if (!manifestFile.exists()) {
                log.warn("Manifest {} does not exist, it may have already been transferred in deposit {}",
                        manifestPath, getDepositUUID());
                continue;
            }

            PID manifestPid = getDepositManifestPid(objPid, manifestFile.getName());
            transferFile(manifestPid, manifestUri, transferSession, resc, CdrDeposit.hasDatastreamManifest);
        }
    }

    private void transferFile(PID binPid, URI stagingUri, BinaryTransferSession transferSession,
            Resource parentResc, Property datastreamProperty) {
        URI storageUri = null;
        String digest = null;
        Resource binResc = model.getResource(binPid.getRepositoryPath());

        // Already has storageUri, skip transfer
        if (binResc.hasProperty(CdrDeposit.storageUri)) {
            return;
        }

        log.debug("Transferring file from {} for {}", stagingUri, binPid.getQualifiedId());

        Statement digestStmt = binResc.getProperty(DEFAULT_ALGORITHM.getDepositProperty());
        try {
            BinaryTransferOutcome outcome = transferSession.transfer(binPid, stagingUri);
            digest = outcome.getSha1();
            assertProvidedDigestMatches(digestStmt, digest, binPid, stagingUri);
            storageUri = outcome.getDestinationUri();
        } catch (BinaryAlreadyExistsException e) {
            // Make sure a PID collision with an existing repository object isn't happening
            if (repoObjFactory.objectExists(binPid.getRepositoryUri())) {
                failJob(e, "Cannot transfer binary {0}, an object with PID {1} already exists in the repository",
                        stagingUri, binPid.getQualifiedId());
            }
            // Check if the binary at the destination matches the staged copy
            if (transferSession.isTransferred(binPid, stagingUri)) {
                // binary was previously fully transferred so all we need to do is record the destination uri
                log.debug("Binary {} was already transferred, recording and moving on", binPid.getQualifiedId());
                BinaryDetails details = transferSession.getStoredBinaryDetails(binPid);
                storageUri = details.getDestinationUri();
                digest = details.getDigest();
            } else {
                // binary was not previously fully transferred, so retry with replacement enabled
                log.debug("Retransferring file from {} for {} with replacement enabled",
                        stagingUri, binPid.getQualifiedId());
                BinaryTransferOutcome outcome = transferSession.transferReplaceExisting(binPid, stagingUri);
                storageUri = outcome.getDestinationUri();
                digest = outcome.getSha1();
            }
        } finally {
            if (storageUri != null) {
                assertProvidedDigestMatches(digestStmt, digest, binPid, stagingUri);

                final URI finalStorageUri = storageUri;
                final String finalDigest = digest;
                commit(() -> {
                    binResc.addProperty(CdrDeposit.storageUri, finalStorageUri.toString());
                    binResc.addLiteral(DEFAULT_ALGORITHM.getDepositProperty(), finalDigest);
                    // Add linkage to the datastream if not already present
                    if (!parentResc.hasProperty(datastreamProperty, binResc)) {
                        parentResc.addProperty(datastreamProperty, binResc);
                    }
                });
            }
        }

        log.debug("Finished transferring file from {} to {}", stagingUri, storageUri);
    }

    private void assertProvidedDigestMatches(Statement providedStmt, String generatedDigest,
            PID binPid, URI stagingUri) {
        if (providedStmt != null) {
            String provided = providedStmt.getString();
            if (!provided.equals(generatedDigest)) {
                throw new InvalidChecksumException("Checksum of copied file for " + binPid
                        + " from " + stagingUri + " did not match expected SHA1: expected "
                        + provided + ", calculated " + generatedDigest);
            }
        }
    }
}
