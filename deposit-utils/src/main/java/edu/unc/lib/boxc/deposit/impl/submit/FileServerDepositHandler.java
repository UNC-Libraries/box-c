package edu.unc.lib.boxc.deposit.impl.submit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.boxc.deposit.api.exceptions.DepositException;
import edu.unc.lib.boxc.deposit.api.submit.DepositData;
import edu.unc.lib.boxc.model.api.ids.PID;

/**
 * Handler for deposit packages located in place on a file server.
 *
 * @author bbpennel
 *
 */
public class FileServerDepositHandler extends AbstractDepositHandler {
    private static Logger log = LoggerFactory.getLogger(FileServerDepositHandler.class);

    @Override
    public PID doDeposit(PID destination, DepositData deposit) throws DepositException {
        log.debug("Preparing to perform a deposit from file server of type {} to {}",
                deposit.getPackagingType(), destination.getQualifiedId());

        PID depositPID = pidMinter.mintDepositRecordPid();

        registerDeposit(depositPID, destination, deposit, null);
        return depositPID;
    }

}
