package edu.unc.lib.boxc.deposit;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import edu.unc.lib.boxc.common.util.ZipFileUtil;

public class DepositTestUtils {

    public static String makeTestDir(File parent, String dirName, File zippedContent) {
        File workingDir = new File(parent, dirName);
        try {
            if (workingDir.exists()) {
                FileUtils.deleteDirectory(workingDir);
            }
            ZipFileUtil.unzipToDir(zippedContent, workingDir);
        } catch (IOException e) {
            throw new Error(
                    "Unable to unpack your deposit: " + zippedContent, e);
        }
        return workingDir.getAbsolutePath();
    }

}
