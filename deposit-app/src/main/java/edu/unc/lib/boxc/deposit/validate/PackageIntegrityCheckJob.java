package edu.unc.lib.boxc.deposit.validate;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;

import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositField;
import edu.unc.lib.boxc.deposit.work.AbstractDepositJob;

/**
 *
 * @author count0
 *
 */
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
    }

    @Override
    public void runJob() {
        Map<String, String> status = getDepositStatus();
        String md5 = status.get(DepositField.depositMd5.name());
        String file = status.get(DepositField.fileName.name());
        if (md5 != null && file != null) {
            File payload = new File(getSubdir("data"), file);
            String computed = null;
            try (InputStream is = new FileInputStream(payload)) {
                computed = DigestUtils.md5Hex(is);
            } catch (IOException e) {
                failJob(e, "Unable to read deposit file while verifying package");
            }

            if (!md5.equals(computed)) {
                String msg = MessageFormat.format(
                        "The computed checksum ({1}) did not match the one provided ({0}) for \"{2}\".", md5, computed,
                        file);
                failJob("The deposit file \"" + file + "\" was corrupted during submission or upload.", msg);
            }
        }
    }

}
