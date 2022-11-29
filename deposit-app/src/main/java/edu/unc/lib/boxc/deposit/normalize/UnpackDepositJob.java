package edu.unc.lib.boxc.deposit.normalize;

import java.io.File;
import java.io.IOException;

import edu.unc.lib.boxc.common.util.ZipFileUtil;
import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositField;
import edu.unc.lib.boxc.deposit.work.AbstractDepositJob;

/**
 * Unpacks the submission package into the deposit directory.
 * @author count0
 *
 */
public class UnpackDepositJob extends AbstractDepositJob {

    public UnpackDepositJob(String uuid, String depositUUID) {
        super(uuid, depositUUID);
    }

    public void runJob() {
        // unzip deposit file to directory
        String filename = getDepositStatus().get(DepositField.fileName.name());
        if (filename.toLowerCase().endsWith(".zip")) {
            File depositFile = new File(getDataDirectory(), filename);
            try {
                ZipFileUtil.unzipToDir(depositFile, getDataDirectory());
            } catch (IOException e) {
                throw new Error("Unable to unpack your deposit: " + getDepositPID().getUUID(), e);
            }
        }
    }

}
