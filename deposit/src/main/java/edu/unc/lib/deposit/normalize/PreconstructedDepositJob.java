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

import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.jena.rdf.model.Model;
import org.slf4j.Logger;

import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositField;
import edu.unc.lib.boxc.deposit.impl.model.DepositDirectoryManager;
import edu.unc.lib.boxc.model.api.rdf.RDFModelUtil;
import edu.unc.lib.deposit.work.AbstractDepositJob;

/**
 * Normalization job to import a preconstructed deposit directory
 *
 * @author bbpennel
 */
public class PreconstructedDepositJob extends AbstractDepositJob {
    private static final Logger log = getLogger(PreconstructedDepositJob.class);

    public PreconstructedDepositJob() {
        super();
    }

    public PreconstructedDepositJob(String uuid, String depositUUID) {
        super(uuid, depositUUID);
    }

    @Override
    public void runJob() {
        try {
            DepositDirectoryManager dirManager = new DepositDirectoryManager(
                    depositPID, getDepositsDirectory().toPath(), true);
            Map<String, String> depositStatus = getDepositStatus();
            // Determine if we are importing an external deposit dir, or starting from one already in place
            String sourceUriProp = depositStatus.get(DepositField.sourceUri.name());
            if (sourceUriProp != null) {
                URI sourceUri = URI.create(sourceUriProp);
                Path sourcePath = Paths.get(sourceUri);
                // Check to see if the path is within the deposits directory or external
                if (!sourcePath.startsWith(getDepositsDirectory().toPath())) {
                    // external path, so move into the deposits directory
                    log.info("Importing external deposit directory {}", sourcePath);
                    FileUtils.copyDirectory(sourcePath.toFile(), getDepositDirectory());
                }
            }
            // Check to see if there is a model file to import
            Path modelPath = dirManager.getModelPath();
            if (Files.exists(modelPath)) {
                log.info("Importing preconstructed model file included in deposit directory");
                Model importModel = RDFModelUtil.createModel(Files.newInputStream(modelPath), "N3");
                Model depositModel = getReadOnlyModel();
                commit(() -> {
                    depositModel.removeAll();
                    depositModel.add(importModel);
                });
            } else {
                log.info("No model file provided, skipping import of additional deposit model properties");
            }
        } catch (IOException e) {
            failJob(e, "Failed to import deposit directory");
        }
    }
}
