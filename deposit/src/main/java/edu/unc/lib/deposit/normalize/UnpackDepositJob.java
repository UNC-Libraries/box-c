package edu.unc.lib.deposit.normalize;

import java.io.File;
import java.io.IOException;

import edu.unc.lib.deposit.work.AbstractDepositJob;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositField;
import edu.unc.lib.dl.util.ZipFileUtil;

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
