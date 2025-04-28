package edu.unc.lib.boxc.deposit.impl.submit;

import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants;
import edu.unc.lib.boxc.deposit.api.exceptions.DepositException;
import edu.unc.lib.boxc.deposit.api.submit.DepositData;
import edu.unc.lib.boxc.model.api.ids.PID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * @author bbpennel
 */
public class WorkFormDepositorHandler extends AbstractDepositHandler {
    private static Logger log = LoggerFactory.getLogger(WorkFormDepositorHandler.class);

    @Override
    public PID doDeposit(PID destination, DepositData deposit) throws DepositException {
        log.debug("Preparing to perform a DCR Work Form deposit to {}", destination.getQualifiedId());

        PID depositPID = pidMinter.mintDepositRecordPid();

        File formJson = writeStreamToDataDir(depositPID, deposit);
        deposit.setSourceUri(formJson.toPath().normalize().toUri());

        registerDeposit(depositPID, destination, deposit, null);

        return depositPID;
    }
}
