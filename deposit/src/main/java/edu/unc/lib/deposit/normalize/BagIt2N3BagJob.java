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

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;

import gov.loc.repository.bagit.hash.SupportedAlgorithm;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.rdf.CdrDeposit;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositField;
import gov.loc.repository.bagit.domain.Bag;
import gov.loc.repository.bagit.domain.FetchItem;
import gov.loc.repository.bagit.domain.Manifest;
import gov.loc.repository.bagit.exceptions.*;
import gov.loc.repository.bagit.hash.StandardBagitAlgorithmNameToSupportedAlgorithmMapping;
import gov.loc.repository.bagit.reader.BagReader;
import gov.loc.repository.bagit.reader.ManifestReader;
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
        String sourcePath = status.get(DepositField.sourcePath.name());
        Path sourceFile = Paths.get(sourcePath);

        // Verify that the bag has all the required parts
        try {
            Bag bagReader = reader.read(sourceFile);

            // Check that bag exists. Throws MissingBagitFileException
            MandatoryVerifier.checkBagitFileExists(bagReader.getRootDir(), bagReader.getVersion());

            BagVerifier verifier = new BagVerifier();
            verifier.isComplete(bagReader, false);
            verifier.isValid(bagReader, false);
        } catch (IOException | MaliciousPathException | InterruptedException | MissingBagitFileException e) {
            failJob("Can't find BagIt bag", "A BagIt bag could not be found at the source path.");
        } catch (UnsupportedAlgorithmException | InvalidBagitFileFormatException | MissingPayloadDirectoryException
                | MissingPayloadManifestException | FileNotInPayloadDirectoryException | UnparsableVersionException
                | VerificationException | CorruptChecksumException e) {
            String msg = e.getMessage();
            failJob("Unable to normalize bag " + sourcePath + ", it was not complete according to bagit specifications",
                    msg);
        }

        try {
            Bag bagReader = reader.read(sourceFile);

            Set<Manifest> payloadManifests = bagReader.getPayLoadManifests();
            StandardBagitAlgorithmNameToSupportedAlgorithmMapping algorithm =
                    new StandardBagitAlgorithmNameToSupportedAlgorithmMapping();

            Property md5sumProp = CdrDeposit.md5sum;
            Property locationProp = CdrDeposit.stagingLocation;
            Property cleanupLocProp = CdrDeposit.cleanupLocation;

            // Turn the bag itself into the top level folder for this deposit
            org.apache.jena.rdf.model.Bag sourceBag = getSourceBag(depositBag, new File(sourcePath));

            int i = 0;
            // Add all of the payload objects into the bag folder
            for (Manifest payLoadManifest : payloadManifests) {
                Map<Path, String> payLoadList = payLoadManifest.getFileToChecksumMap();
                SupportedAlgorithm checksumType = payLoadManifest.getAlgorithm();

                for (Map.Entry<Path, String> checksum : payLoadList.entrySet()) {
                    Path filePath = checksum.getKey();
                    String fullFilePath = filePath.toAbsolutePath().toString();
                    log.debug("Adding object {}: {}", i++, filePath.toString());

                    Resource fileResource = getFileResource(sourceBag, fullFilePath);

                    // add checksums
                    if (checksumType.equals(algorithm.getSupportedAlgorithm("MD5"))) {
                        model.add(fileResource, md5sumProp, checksum.getValue());
                    }
                    if (checksumType.equals(algorithm.getSupportedAlgorithm("SHA1"))) {
                        model.add(fileResource, md5sumProp, checksum.getValue());
                    }

                    // Find staged path for the file
                    model.add(fileResource, locationProp, sourceFile.toUri().toString());
                }
            }

            // Register tag file as deposit manifests, then register  them for cleanup later 
            Set<Manifest> tags = bagReader.getTagManifests();

            for (Manifest tag : tags) {
                Map<Path, String> tagList = tag.getFileToChecksumMap();

                for (Path path : tagList.keySet()) {
                    Path fullPath = path.toAbsolutePath();
                    String pathUri = fullPath.toUri().toString();
                    getDepositStatusFactory().addManifest(getDepositUUID(), pathUri);
                    model.add(depositBag, cleanupLocProp, pathUri);
                }
            }

            // Register the bag itself for cleanup
            model.add(depositBag, cleanupLocProp, sourceFile.toAbsolutePath().toUri().toString());
        } catch (IOException | UnparsableVersionException | MaliciousPathException |
                UnsupportedAlgorithmException | InvalidBagitFileFormatException e) {
            log.warn("Unable to process bag files. {}", e.getMessage());
        }
    }
}