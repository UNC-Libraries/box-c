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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Map;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.rdf.CdrDeposit;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositField;
import gov.loc.repository.bagit.Bag;
import gov.loc.repository.bagit.Bag.Format;
import gov.loc.repository.bagit.BagFactory;
import gov.loc.repository.bagit.BagFile;
import gov.loc.repository.bagit.BagHelper;
import gov.loc.repository.bagit.Manifest;
import gov.loc.repository.bagit.utilities.SimpleResult;

/**
 * Transforms bagit bags stored in a staging location into n3 for deposit
 *
 * @author bbpennel
 * @author daines
 * @date Nov 9, 2015
 */
public class BagIt2N3BagJob extends AbstractFileServerToBagJob {
    private static final Logger log = LoggerFactory.getLogger(BagIt2N3BagJob.class);

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

        File sourceFile = new File(sourcePath);

        if (BagHelper.getVersion(sourceFile) == null) {
            failJob("Can't find BagIt bag", "A BagIt bag could not be found at the source path.");
        }

        BagFactory bagFactory = new BagFactory();
        Bag bag = bagFactory.createBag(sourceFile);

        if (bag.getFormat() != Format.FILESYSTEM) {
            failJob("Unsupported BagIt bag format", "Only filesystem bags are supported.");
        }

        // Verify that the bag has all the required parts
        SimpleResult completeResult = bag.verifyComplete();
        if (!bag.verifyComplete().isSuccess()) {
            // Bag did not validate, generate error report and throw exception
            StringBuilder msg = new StringBuilder();
            for (String error : completeResult.getErrorMessages()) {
                msg.append(error).append("\n");
            }

            failJob("Unable to normalize bag " + sourcePath + ", it was not complete according to bagit specifications",
                    msg.toString());
        }

        Collection<BagFile> payload = bag.getPayload();

        Property md5sumProp = CdrDeposit.md5sum;
        Property locationProp = CdrDeposit.stagingLocation;
        Property cleanupLocProp = CdrDeposit.cleanupLocation;

        // Turn the bag itself into the top level folder for this deposit
        org.apache.jena.rdf.model.Bag sourceBag = getSourceBag(depositBag, sourceFile);

        int i = 0;
        // Add all of the payload objects into the bag folder
        for (BagFile file : payload) {
            log.debug("Adding object {}: {}", i++, file.getFilepath());

            String filePath = file.getFilepath();

            Map<Manifest.Algorithm, String> checksums = bag.getChecksums(filePath);

            Resource fileResource = getFileResource(sourceBag, filePath);

            // add checksums
            if (checksums.containsKey(Manifest.Algorithm.MD5)) {
                model.add(fileResource, md5sumProp, checksums.get(Manifest.Algorithm.MD5));
            }
            if (checksums.containsKey(Manifest.Algorithm.SHA1)) {
                model.add(fileResource, md5sumProp, checksums.get(Manifest.Algorithm.SHA1));
            }

            // Find staged path for the file
            Path storedPath = Paths.get(sourceFile.getAbsolutePath(), filePath);
            model.add(fileResource, locationProp, storedPath.toUri().toString());
        }

        String sourceAbsPath = sourceFile.getAbsolutePath();
        // Register tag file as deposit manifests, then register  them for cleanup later 
        for (BagFile tag : bag.getTags()) {
            Path path = Paths.get(sourceAbsPath, tag.getFilepath());
            String pathUri = path.toUri().toString();
            getDepositStatusFactory().addManifest(getDepositUUID(), pathUri);
            model.add(depositBag, cleanupLocProp, pathUri);
        }

        // Register the bag itself for cleanup
        model.add(depositBag, cleanupLocProp, sourceFile.toPath().toAbsolutePath().toUri().toString());
    }

}