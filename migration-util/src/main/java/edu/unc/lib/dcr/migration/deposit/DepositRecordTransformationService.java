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
package edu.unc.lib.dcr.migration.deposit;

import static edu.unc.lib.dl.fcrepo4.RepositoryPathConstants.DEPOSIT_RECORD_BASE;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import edu.unc.lib.dl.exceptions.RepositoryException;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.RepositoryPIDMinter;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.persist.api.storage.StorageLocation;
import edu.unc.lib.dl.persist.api.storage.StorageLocationManager;
import edu.unc.lib.dl.persist.api.transfer.BinaryTransferService;
import edu.unc.lib.dl.persist.api.transfer.BinaryTransferSession;

/**
 * Service which transforms a set of deposit records from a list of pids.
 *
 * @author bbpennel
 */
public class DepositRecordTransformationService {

    private DepositRecordTransformerManager transformerManager;

    private BinaryTransferService transferService;

    private StorageLocationManager locationManager;

    private RepositoryPIDMinter pidMinter;

    private boolean generateIds;

    /**
     * Perform the transformation of the tree of content objects
     *
     * @return result code
     */
    public int perform(Path pidListPath, String destLocId) {
        StorageLocation destLoc = locationManager.getStorageLocationById(destLocId);
        try (
                Stream<String> pidStringStream = Files.lines(pidListPath);
                BinaryTransferSession transferSession = transferService.getSession(destLoc);
            ) {

            pidStringStream.forEach(originalString -> {
                PID originalPid = PIDs.get(DEPOSIT_RECORD_BASE, originalString);
                PID newPid = getTransformedPid(originalPid);
                transformerManager.createTransformer(originalPid, newPid, transferSession)
                    .fork();
            });
        } catch (IOException e) {
            throw new RepositoryException("Failed to load " + pidListPath, e);
        }

        // Wait for all transformers to finish
        return transformerManager.awaitTransformers();
    }

    public PID getTransformedPid(PID originalPid) {
        return generateIds ? pidMinter.mintDepositRecordPid() : PIDs.get(DEPOSIT_RECORD_BASE,
                originalPid.getId().toLowerCase());
    }

    public void setGenerateIds(boolean generateIds) {
        this.generateIds = generateIds;
    }

    public void setPidMinter(RepositoryPIDMinter pidMinter) {
        this.pidMinter = pidMinter;
    }

    public void setTransformerManager(DepositRecordTransformerManager transformerManager) {
        this.transformerManager = transformerManager;
    }

    public void setTransferService(BinaryTransferService transferService) {
        this.transferService = transferService;
    }

    public void setLocationManager(StorageLocationManager locationManager) {
        this.locationManager = locationManager;
    }
}
