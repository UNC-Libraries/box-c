/**
 * Copyright 2008 The University of North Carolina at Chapel Hill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.unc.lib.deposit;

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
