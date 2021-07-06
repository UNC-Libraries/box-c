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

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.jena.rdf.model.Bag;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.rdf.CdrDeposit;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositField;

/**
 * Normalizes a simple directory submission into n3 for deposit
 *
 * @author lfarrell
 */
public class DirectoryToBagJob extends AbstractFileServerToBagJob {
    private static final Logger log = LoggerFactory.getLogger(DirectoryToBagJob.class);
    private static final int MAX_DEPTH = 128;

    public DirectoryToBagJob() {
        super();
    }

    public DirectoryToBagJob(String uuid, String depositUUID) {
        super(uuid, depositUUID);
    }

    @Override
    public void runJob() {
        Model depModel = getReadOnlyModel();
        // Cache all the changes for committing at the end
        Model model = ModelFactory.createDefaultModel().add(depModel);

        Bag depositBag = model.createBag(getDepositPID().getURI());

        Map<String, String> status = getDepositStatus();
        URI sourceUri = URI.create(status.get(DepositField.sourceUri.name()));
        Path sourcePath = Paths.get(sourceUri);

        interruptJobIfStopped();

        // Turn the base directory itself into the top level folder for this deposit
        Bag sourceBag = getSourceBag(depositBag, sourcePath);

        // Add all of the payload objects into the bag folder
        try (Stream<Path> stream = Files.walk(sourcePath, MAX_DEPTH)) {
            stream.forEach(file -> {
                // skip adding the base directory to the deposit
                if (file.equals(sourcePath)) {
                    return;
                }

                log.debug("Adding object: {}", file.getFileName());

                Path filePath = sourcePath.getParent().relativize(file);
                String filename = filePath.getFileName().toString();

                if (Files.isRegularFile(file)) {
                    Resource originalResource = getFileResource(sourceBag, filePath);

                    // Find staged path for the file
                    Path storedPath = file.toAbsolutePath();
                    model.add(originalResource, CdrDeposit.stagingLocation, storedPath.toUri().toString());
                } else {
                    Bag folderBag = getFolderBag(sourceBag, filePath);
                    model.add(folderBag, CdrDeposit.label, filename);
                    model.add(folderBag, RDF.type, Cdr.Folder);
                }
            });
        } catch (IOException e) {
            failJob(e, "Failed to read deposit directory {0}", sourcePath);
        }
        commit(() -> depModel.add(model));
    }
}