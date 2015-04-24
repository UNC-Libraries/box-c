package edu.unc.lib.deposit.validate;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;
import java.util.Map;

import org.apache.commons.codec.binary.Hex;

import edu.unc.lib.deposit.work.AbstractDepositJob;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositField;

public class PackageIntegrityCheckJob extends AbstractDepositJob {

	/**
	 * Verifies the integrity of the deposit file received from SWORD.
	 * @param uuid
	 * @param depositUUID
	 */
	public PackageIntegrityCheckJob(String uuid, String depositUUID) {
		super(uuid, depositUUID);
	}

	public PackageIntegrityCheckJob() {
		super();
	};

	@Override
	public void runJob() {
		Map<String, String> status = getDepositStatus();
		String md5 = status.get(DepositField.depositMd5.name());
		String file = status.get(DepositField.fileName.name());
		if(md5 != null && file != null) {
			File payload = new File(getSubdir("data"), file);
			MessageDigest md;
			try {
				md = MessageDigest.getInstance("MD5");
			} catch (NoSuchAlgorithmException e1) {
				throw new Error("Unexpected missing algorithm.", e1);
			}
			try (InputStream is = Files.newInputStream(Paths.get(payload.getAbsolutePath()))) {
			  DigestInputStream dis = new DigestInputStream(is, md);
			  while(dis.read() != -1);
			} catch (IOException e) {
				throw new Error("Cannot read payload file.", e);
			}
			byte[] digest = md.digest();
			String computed = String.valueOf(Hex.encodeHex(digest));
			if(!md5.equals(computed)) {
				String msg = MessageFormat.format("The computed checksum ({1}) did not match the one provided ({0}) for \"{2}\".", md5, computed, file);
				failJob("The deposit file \"" + file + "\" was corrupted during submission or upload.", msg);
			}
		}
	}

}
