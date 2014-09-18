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

	public UnpackDepositJob(String uuid, String depositUUID) {
		super(uuid, depositUUID);
	}

	public void runJob() {
		// unzip deposit file to directory
		String filename = getDepositStatus().get(DepositField.fileName.name());
		if (filename.toLowerCase().endsWith(".zip")) {
			File depositFile = new File(getDepositDirectory(), "data/"
					+ filename);
			try {
				ZipFileUtil.unzipToDir(depositFile, getDepositDirectory());
			} catch (IOException e) {
				throw new Error("Unable to unpack your deposit: " + getDepositPID().getUUID(), e);
			}
		}
		
		// TODO move this block to a job
		// create or unzip bag directory
//		String filename = deposit.getFilename();
//		File metsFile = new File(dir, "mets.xml");
//		if(filename.endsWith(".zip")) {
//			try {
//				ZipFileUtil.unzipToDir(deposit.getFile(), dir);
//			} catch (IOException e) {
//				throw new SwordError(ErrorURIRegistry.INGEST_EXCEPTION, 400, "Unable to unpack your deposit: "+depositPID.getPid());
//			}
//		} else {
//			try {
//				FileUtils.renameOrMoveTo(deposit.getFile(), metsFile);
//			} catch (IOException e) {
//				throw new SwordError(ErrorURIRegistry.INGEST_EXCEPTION, 500, "Unable to create your deposit bag: "+depositPID.getPid(), e);
//			}
//		}
//		// normalize METS.xml to mets.xml
//		File legacyFile = new File(dir, "METS.xml");
//		if(legacyFile.exists() && !metsFile.exists()) {
//			try {
//				FileUtils.renameOrMoveTo(legacyFile, metsFile);
//			} catch(IOException e) {
//				throw new SwordError(ErrorURIRegistry.INGEST_EXCEPTION, 500, "Unable to create your deposit bag: "+depositPID.getPid(), e);
//			}
//		}
	}

}
