package edu.unc.lib.boxc.persist.impl.transfer;

import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.net.URI;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;

import edu.unc.lib.boxc.persist.api.transfer.BinaryTransferException;

/**
 * Helper methods to generate file paths and rollback file transfers
 *
 * @author lfarrell
 */
public class FileTransferHelpers {
    private static final Logger log = getLogger(FileTransferHelpers.class);

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
     * @param filePath
     * @param destPath
     * @return
     */
    public static Thread registerCleanup(Path filePath) {
        Thread cleanupThread = new Thread(() -> cleanupFile(filePath));
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
    public static void cleanupFile(Path filePath) {
        try {
            Files.deleteIfExists(filePath);
            // Cleanup the parent directory if all it contained was this file
            Files.deleteIfExists(filePath.getParent());
        } catch (DirectoryNotEmptyException e) {
            log.debug("Parent of {} is not empty, skipping cleanup");
        } catch (IOException e) {
            throw new BinaryTransferException("Failed to cleanup " + filePath, e);
        }
    }
}
