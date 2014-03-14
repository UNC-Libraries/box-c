package edu.unc.lib.deposit.normalize;

import java.io.File;
import java.io.IOException;

import edu.unc.lib.deposit.work.AbstractDepositJob;
import edu.unc.lib.dl.util.ZipFileUtil;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositField;

/**
 * Unpacks the submission package into the deposit directory.
 * @author count0
 *
 */
public class UnpackDepositJob extends AbstractDepositJob {

	public UnpackDepositJob(String uuid, String depositDirectory,
			String depositId) {
		super(uuid, depositDirectory, depositId);
	}

	public UnpackDepositJob() {
		// unzip deposit file to directory
		String filename = getDepositStatus().get(DepositField.fileName.name());
		if (filename.endsWith(".zip") || filename.endsWith(".ZIP")) {
			File depositFile = new File(getDepositDirectory(), "data/"
					+ filename);
			try {
				ZipFileUtil.unzipToDir(depositFile, getDepositDirectory());
			} catch (IOException e) {
				throw new Error("Unable to unpack your deposit: " + getDepositPID().getPid());
			}
		}
	}

}
