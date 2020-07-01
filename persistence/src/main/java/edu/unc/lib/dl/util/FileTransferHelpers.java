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
package edu.unc.lib.dl.util;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import edu.unc.lib.dl.persist.api.transfer.BinaryTransferException;

/**
 * Helper methods to generate file paths and rollback file transfers
 *
 * @author lfarrell
 */
public class FileTransferHelpers {
    private FileTransferHelpers() {
    }

    public static Path createFilePath(URI destUri, String type, long currentTime) {
        URI fileUri = URI.create(destUri + "." + type + "-" + currentTime);
        return Paths.get(fileUri);
    }

    /**
     * Register cleanup of partially transferred files at shutdown time in case
     * the JVM exits before normal processes complete
     *
     * @param oldFilePath
     * @param newFilePath
     * @param destPath
     * @return
     */
    public static Thread registerCleanup(Path oldFilePath, Path newFilePath, Path destPath) {
        Thread cleanupThread = new Thread(() -> rollBackOldFile(oldFilePath, newFilePath, destPath));
        Runtime.getRuntime().addShutdownHook(cleanupThread);
        return cleanupThread;
    }

    /**
     * Clear a cleanup shutdown hook
     *
     * @param cleanupThread
     */
    public static void clearCleanupHook(Thread cleanupThread) {
        if (cleanupThread != null) {
            Runtime.getRuntime().removeShutdownHook(cleanupThread);
        }
    }

    /**
     * Roll back the file name if an error is thrown
     * @param oldFilePath
     * @param  newFilePath
     * @param destPath
     */
    public static void rollBackOldFile(Path oldFilePath, Path newFilePath, Path destPath) {
        try {
            if (Files.exists(oldFilePath)) {
                Files.move(oldFilePath, destPath);
            }

            Files.deleteIfExists(newFilePath);
        } catch (IOException e) {
            throw new BinaryTransferException("Failed to roll back " + oldFilePath.toString()
                    + "  in transfer to destination " + destPath, e);
        }
    }
}
