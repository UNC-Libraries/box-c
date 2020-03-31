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
package edu.unc.lib.dl.persist.services.ingest;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.acl.util.AgentPrincipals;
import edu.unc.lib.dl.fcrepo4.RepositoryPIDMinter;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.persist.api.ingest.DepositData;
import edu.unc.lib.dl.persist.api.ingest.DepositHandler;
import edu.unc.lib.dl.util.DepositConstants;
import edu.unc.lib.dl.util.DepositException;
import edu.unc.lib.dl.util.DepositStatusFactory;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositAction;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositField;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositState;

/**
 * Abstract handler for processing a deposit type and submitting it to the
 * deposit pipeline.
 *
 * @author bbpennel
 *
 */
public abstract class AbstractDepositHandler implements DepositHandler {
    private static final Logger log = LoggerFactory.getLogger(AbstractDepositHandler.class);

    protected RepositoryPIDMinter pidMinter;
    private DepositStatusFactory depositStatusFactory;
    private File depositsDirectory;

    public void setDepositStatusFactory(DepositStatusFactory depositStatusFactory) {
        this.depositStatusFactory = depositStatusFactory;
    }

    public File getDepositsDirectory() {
        return depositsDirectory;
    }

    public void setDepositsDirectory(File depositsDirectory) {
        this.depositsDirectory = depositsDirectory;
    }

    /**
     * @param pidMinter the pidMinter to set
     */
    public void setPidMinter(RepositoryPIDMinter pidMinter) {
        this.pidMinter = pidMinter;
    }

    protected File makeDepositDirectory(PID depositPID) {
        File depositDir = new File(getDepositsDirectory(), depositPID.getId());
        depositDir.mkdir();
        return depositDir;
    }

    protected File makeDataDir(File depositDir) {
        File dataDir = new File(depositDir, DepositConstants.DATA_DIR);
        dataDir.mkdir();
        return dataDir;
    }

    protected File writeStreamToDataDir(PID depositPID, DepositData deposit) throws DepositException {
        File depositDir = makeDepositDirectory(depositPID);
        // Write the file contents out to the deposit data directory
        if (deposit.getInputStream() != null) {
            File dataDir = makeDataDir(depositDir);
            try (InputStream fileStream = deposit.getInputStream()) {
                // ensure that filename is only the filename, without modifiers
                String filename = new File(deposit.getFilename()).getName();
                File depositFile = new File(dataDir, filename);

                Files.copy(fileStream,
                        depositFile.toPath(),
                        StandardCopyOption.REPLACE_EXISTING);

                return depositFile;
            } catch (IOException e) {
                throw new DepositException("Failed to write file to deposit directory "
                        + depositDir.toPath(), e);
            }
        } else {
            throw new DepositException("No file content provided with deposit");
        }
    }

    protected void registerDeposit(PID depositPid, PID destination, DepositData deposit,
            Map<String, String> extras) {
        Map<String, String> status = new HashMap<>();
        if (extras != null) {
            status.putAll(extras);
        }

        AgentPrincipals agent = deposit.getDepositingAgent();

        // generic deposit fields
        status.put(DepositField.uuid.name(), depositPid.getId());
        status.put(DepositField.submitTime.name(), String.valueOf(System.currentTimeMillis()));
        status.put(DepositField.fileMimetype.name(), deposit.getMimeType());
        status.put(DepositField.depositorName.name(), agent.getUsername());
        status.put(DepositField.depositorEmail.name(), deposit.getDepositorEmail());
        status.put(DepositField.containerId.name(), destination.getId());
        status.put(DepositField.depositMethod.name(), deposit.getDepositMethod());
        status.put(DepositField.packagingType.name(), deposit.getPackagingType().getUri());
        status.put(DepositField.depositMd5.name(), deposit.getMd5());
        status.put(DepositField.accessionNumber.name(), deposit.getAccessionNumber());
        status.put(DepositField.mediaId.name(), deposit.getMediaId());
        status.put(DepositField.staffOnly.name(), String.valueOf(deposit.getStaffOnly()));

        if (deposit.getFilename() != null) {
            // Resolve filename to just the name portion of the value, in case of modifiers
            String filename = Paths.get(deposit.getFilename()).getFileName().toString();
            status.put(DepositField.fileName.name(), filename);
        }
        if (deposit.getSourceUri() != null) {
            status.put(DepositField.sourceUri.name(), deposit.getSourceUri().toString());
        }
        status.put(DepositField.depositSlug.name(), deposit.getSlug());
        if (deposit.getPriority() != null) {
            status.put(DepositField.priority.name(), deposit.getPriority().name());
        }
        String permGroups = agent.getPrincipals().joinAccessGroups(";");
        status.put(DepositField.permissionGroups.name(), permGroups);

        status.put(DepositField.state.name(), DepositState.unregistered.name());
        status.put(DepositField.actionRequest.name(), DepositAction.register.name());

        // Clean out any null deposit details
        Iterator<Entry<String, String>> it = status.entrySet().iterator();
        while (it.hasNext()) {
            Entry<String, String> entry = it.next();
            if (entry.getValue() == null) {
                it.remove();
            }
        }

        depositStatusFactory.save(depositPid.getId(), status);

        if (log.isInfoEnabled()) {
            log.info("Registered deposit with details {}", status);
        }
    }
}
