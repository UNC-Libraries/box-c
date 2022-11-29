package edu.unc.lib.boxc.deposit.impl.submit;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositField;
import edu.unc.lib.boxc.deposit.api.exceptions.DepositException;
import edu.unc.lib.boxc.deposit.api.submit.DepositData;
import edu.unc.lib.boxc.model.api.ids.PID;

/**
 * Deposit handler for simple deposit package types, allowing deposit of a single file.
 *
 * @author bbpennel
 *
 */
public class SimpleObjectDepositHandler extends AbstractDepositHandler {
    private static Logger log = LoggerFactory
            .getLogger(SimpleObjectDepositHandler.class);

    @Override
    public PID doDeposit(PID destination, DepositData deposit) throws DepositException {
        log.debug("Preparing to perform a Simple Object deposit to {}",
                destination.getQualifiedId());

        PID depositPID = pidMinter.mintDepositRecordPid();

        File created = writeStreamToDataDir(depositPID, deposit);
        deposit.setSourceUri(created.toPath().normalize().toUri());

        Map<String, String> options = new HashMap<>();
        options.put(DepositField.excludeDepositRecord.name(), "true");

        registerDeposit(depositPID, destination, deposit, options);

        return depositPID;
    }
}
