package edu.unc.lib.boxc.deposit.impl.submit;

import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.deposit.api.DepositConstants;
import edu.unc.lib.boxc.deposit.api.DepositOperation;
import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositField;
import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositState;
import edu.unc.lib.boxc.deposit.api.exceptions.DepositException;
import edu.unc.lib.boxc.deposit.api.submit.DepositData;
import edu.unc.lib.boxc.deposit.api.submit.DepositHandler;
import edu.unc.lib.boxc.deposit.impl.jms.DepositOperationMessage;
import edu.unc.lib.boxc.deposit.impl.jms.DepositOperationMessageService;
import edu.unc.lib.boxc.deposit.impl.model.DepositStatusFactory;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.ids.PIDMinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

/**
 * Abstract handler for processing a deposit type and submitting it to the
 * deposit pipeline.
 *
 * @author bbpennel
 *
 */
public abstract class AbstractDepositHandler implements DepositHandler {
    private static final Logger log = LoggerFactory.getLogger(AbstractDepositHandler.class);

    protected PIDMinter pidMinter;
    private DepositStatusFactory depositStatusFactory;
    private DepositOperationMessageService depositOperationMessageService;
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

    public void setDepositOperationMessageService(DepositOperationMessageService depositOperationMessageService) {
        this.depositOperationMessageService = depositOperationMessageService;
    }

    /**
     * @param pidMinter the pidMinter to set
     */
    public void setPidMinter(PIDMinter pidMinter) {
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
        status.put(DepositField.staffOnly.name(), String.valueOf(deposit.getStaffOnly()));
        status.put(DepositField.overrideTimestamps.name(), String.valueOf(deposit.getOverrideTimestamps()));
        status.put(DepositField.createParentFolder.name(), String.valueOf(deposit.getCreateParentFolder()));
        status.put(DepositField.filesOnlyMode.name(), String.valueOf(deposit.getFilesOnlyMode()));

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

        // Clean out any null deposit details
        Iterator<Entry<String, String>> it = status.entrySet().iterator();
        while (it.hasNext()) {
            Entry<String, String> entry = it.next();
            if (entry.getValue() == null) {
                it.remove();
            }
        }

        depositStatusFactory.save(depositPid.getId(), status);
        // Send a message to the JMS queue to register the deposit
        var registerMessage = new DepositOperationMessage();
        registerMessage.setDepositId(depositPid.getId());
        registerMessage.setAction(DepositOperation.REGISTER);
        registerMessage.setUsername(agent.getUsername());
        depositOperationMessageService.sendDepositOperationMessage(registerMessage);

        if (log.isInfoEnabled()) {
            log.info("Registered deposit with details {}", status);
        }
    }
}
