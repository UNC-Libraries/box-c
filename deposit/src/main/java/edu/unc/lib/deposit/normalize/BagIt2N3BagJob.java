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

import static edu.unc.lib.dl.rdf.CdrDeposit.cleanupLocation;
import static edu.unc.lib.dl.rdf.CdrDeposit.md5sum;
import static edu.unc.lib.dl.rdf.CdrDeposit.sha1sum;
import static edu.unc.lib.dl.rdf.CdrDeposit.stagingLocation;
import static gov.loc.repository.bagit.hash.StandardSupportedAlgorithms.MD5;
import static gov.loc.repository.bagit.hash.StandardSupportedAlgorithms.SHA1;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.util.RedisWorkerConstants.DepositField;
import gov.loc.repository.bagit.domain.Bag;
import gov.loc.repository.bagit.domain.Manifest;
import gov.loc.repository.bagit.exceptions.CorruptChecksumException;
import gov.loc.repository.bagit.exceptions.FileNotInPayloadDirectoryException;
import gov.loc.repository.bagit.exceptions.InvalidBagitFileFormatException;
import gov.loc.repository.bagit.exceptions.MaliciousPathException;
import gov.loc.repository.bagit.exceptions.MissingBagitFileException;
import gov.loc.repository.bagit.exceptions.MissingPayloadDirectoryException;
import gov.loc.repository.bagit.exceptions.MissingPayloadManifestException;
import gov.loc.repository.bagit.exceptions.UnparsableVersionException;
import gov.loc.repository.bagit.exceptions.UnsupportedAlgorithmException;
import gov.loc.repository.bagit.exceptions.VerificationException;
import gov.loc.repository.bagit.reader.BagReader;
import gov.loc.repository.bagit.verify.BagVerifier;
import gov.loc.repository.bagit.verify.MandatoryVerifier;

/**
 * Transforms bagit bags stored in a staging location into n3 for deposit
 *
 * @author bbpennel
 * @author daines
 * @author lfarrell
 * @date Jan 16, 2019
 */
public class BagIt2N3BagJob extends AbstractFileServerToBagJob {
    private static final Logger log = LoggerFactory.getLogger(BagIt2N3BagJob.class);
    private BagReader reader = new BagReader();

    public BagIt2N3BagJob() {
        super();
    }

    public BagIt2N3BagJob(String uuid, String depositUUID) {
        super(uuid, depositUUID);
    }

    @Override
    public void runJob() {

        Model model = getWritableModel();
        org.apache.jena.rdf.model.Bag depositBag = model.createBag(getDepositPID().getURI().toString());

        Map<String, String> status = getDepositStatus();
        URI sourceUri = URI.create(status.get(DepositField.sourceUri.name()));
        Path sourceFile = Paths.get(sourceUri);

        try {
            // Verify that the bag has all the required parts
            Bag bagReader = reader.read(sourceFile);

            // Check that bag exists. Throws MissingBagitFileException
            MandatoryVerifier.checkBagitFileExists(bagReader.getRootDir(), bagReader.getVersion());

            try (BagVerifier verifier = new BagVerifier()) {
                interruptJobIfStopped();
                verifier.isComplete(bagReader, false);
                interruptJobIfStopped();
                verifier.isValid(bagReader, false);
            }

            interruptJobIfStopped();

            Set<Manifest> payloadManifests = bagReader.getPayLoadManifests();

            // Turn the bag itself into the top level folder for this deposit
            org.apache.jena.rdf.model.Bag sourceBag = getSourceBag(depositBag, new File(sourceUri));

            int i = 0;
            // Add all of the payload objects into the bag folder
            for (Manifest payLoadManifest : payloadManifests) {
                Map<Path, String> payLoadList = payLoadManifest.getFileToChecksumMap();

                // Determine which checksum is being recorded
                String checksumType = payLoadManifest.getAlgorithm().getMessageDigestName();
                Property checksumProperty;
                if (MD5.getMessageDigestName().equals(checksumType)) {
                    checksumProperty = md5sum;
                } else if (SHA1.getMessageDigestName().equals(checksumType)) {
                    checksumProperty = sha1sum;
                } else {
                    checksumProperty = md5sum;
                }

                for (Map.Entry<Path, String> pathToChecksum : payLoadList.entrySet()) {
                    Path path = pathToChecksum.getKey();
                    // Make the file path relative to the base of the bag
                    Path relativePath = bagReader.getRootDir().relativize(path);
                    String filePath = relativePath.toString();
                    log.debug("Adding object {}: {}", i++, filePath);

                    Resource fileResource = getFileResource(sourceBag, filePath);

                    // add checksums
                    model.add(fileResource, checksumProperty, pathToChecksum.getValue());

                    // Find staged path for the file
                    Path storedPath = Paths.get(sourceFile.toAbsolutePath().toString(), filePath);
                    model.add(fileResource, stagingLocation, storedPath.toUri().toString());
                }
            }

            // Register tag file as deposit manifests, then register them for cleanup later 
            Files.list(bagReader.getRootDir())
                .filter(Files::isRegularFile)
                .forEach(path -> {
                    String pathString = path.toUri().toString();
                    getDepositStatusFactory().addManifest(getDepositUUID(), pathString);
                    model.add(depositBag, cleanupLocation, pathString);
                });

            // Register the bag itself for cleanup
            model.add(depositBag, cleanupLocation, sourceFile.toAbsolutePath().toUri().toString());
        } catch (IOException e) {
            failJob(e, "Unable to read bag file {0}", sourceUri);
        } catch (InterruptedException e) {
            failJob(e, "Interrupted while normalizing bag {0}", sourceUri);
        } catch (MissingBagitFileException | MissingPayloadDirectoryException | MissingPayloadManifestException
                | FileNotInPayloadDirectoryException | VerificationException | CorruptChecksumException
                | UnparsableVersionException | MaliciousPathException | UnsupportedAlgorithmException
                | InvalidBagitFileFormatException e) {
            failJob("Unable to normalize bag " + sourceUri + ", it was not complete according to bagit specifications",
                    e.getMessage());
        }
    }
}