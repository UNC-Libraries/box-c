package edu.unc.lib.boxc.deposit.impl.submit;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositField;
import edu.unc.lib.boxc.deposit.api.exceptions.DepositException;
import edu.unc.lib.boxc.deposit.api.submit.DepositData;
import edu.unc.lib.boxc.deposit.impl.mets.MetsHeaderScanner;
import edu.unc.lib.boxc.model.api.ids.PID;

/**
 *  Deposit handler for METS submissions which follow the CDR profile
 *
 * @author bbpennel
 *
 */
public class CDRMETSDepositHandler extends AbstractDepositHandler {
    private static Logger log = LoggerFactory.getLogger(CDRMETSDepositHandler.class);

    @Override
    public PID doDeposit(PID destination, DepositData deposit) throws DepositException {
        log.debug("Preparing to perform a DCR METS deposit to {}", destination.getQualifiedId());

        PID depositPID = pidMinter.mintDepositRecordPid();

        File metsFile = writeStreamToDataDir(depositPID, deposit);
        deposit.setSourceUri(metsFile.toPath().normalize().toUri());

        // extract info from METS header
        MetsHeaderScanner scanner = new MetsHeaderScanner();
        try {
            scanner.scan(metsFile, deposit.getFilename());
        } catch (Exception e1) {
            throw new DepositException(
                    "Unable to parse your METS file: " + deposit.getFilename(), e1);
        }

        // METS specific fields
        Map<String, String> status = new HashMap<>();
        status.put(DepositField.packageProfile.name(), scanner.getProfile());
        status.put(DepositField.metsType.name(), scanner.getType());
        status.put(DepositField.createTime.name(), scanner.getCreateDate());
        status.put(DepositField.intSenderDescription.name(), StringUtils.join(scanner.getNames(), ','));

        registerDeposit(depositPID, destination, deposit, status);
        return depositPID;
    }
}