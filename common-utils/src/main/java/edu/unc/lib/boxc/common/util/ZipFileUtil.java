package edu.unc.lib.boxc.common.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Process zip files
 * @author count0
 *
 */
public class ZipFileUtil {
    private static final Logger log = LoggerFactory.getLogger(ZipFileUtil.class);

    private ZipFileUtil() {
    }

    /**
     * Create a temporary directory, unzip the contents of the given zip file to
     * it, and return the directory.
     *
     * If anything goes wrong during this process, clean up the temporary
     * directory and throw an exception.
     */
    public static File unzipToTemp(File zipFile) throws IOException {
        // get a temporary directory to work with
        File tempDir = File.createTempFile("ingest", null);
        tempDir.delete();
        tempDir.mkdir();
        tempDir.deleteOnExit();
        log.debug("Unzipping to temporary directory: {}", tempDir.getPath());
        try {
            unzip(new FileInputStream(zipFile), tempDir);
            return tempDir;
        } catch (IOException e) {
            // attempt cleanup, then re-throw
            FileUtils.deleteDirectory(tempDir);
            throw e;
        }
    }

    /**
     * Create a temporary directory, unzip the contents of the given zip file to
     * it, and return the directory.
     *
     * If anything goes wrong during this process, clean up the temporary
     * directory and throw an exception.
     */
    public static File unzipToTemp(InputStream zipStream) throws IOException {
        // get a temporary directory to work with
        File tempDir = File.createTempFile("ingest", null);
        tempDir.delete();
        tempDir.mkdir();
        tempDir.deleteOnExit();
        log.debug("Unzipping to temporary directory: {}", tempDir.getPath());
        try {
            unzip(zipStream, tempDir);
            return tempDir;
        } catch (IOException e) {
            // attempt cleanup, then re-throw
            FileUtils.deleteDirectory(tempDir);
            throw e;
        }
    }

    /**
     * Unzips the contents of the zip file to the directory.
     *
     * If anything goes wrong during this process, clean up the temporary
     * directory and throw an exception.
     */
    public static void unzipToDir(File zipFile, File destDir)
            throws IOException {
        log.debug("Unzipping to directory: {}", destDir.getPath());
        unzip(new FileInputStream(zipFile), destDir);
    }

    /**
     * Unzip to the given directory, creating subdirectories as needed, and
     * ignoring empty directories. Uses apache zip tools until java utilies are
     * updated to support utf-8.
     */
    public static void unzip(InputStream is, File destDir) throws IOException {
        BufferedInputStream bis = new BufferedInputStream(is);
        try (ZipArchiveInputStream zis = new ZipArchiveInputStream(bis)) {
            ArchiveEntry entry = null;
            while ((entry = zis.getNextZipEntry()) != null) {
                if (!entry.isDirectory()) {
                    File f = new File(destDir, entry.getName());

                    if (!isFileInsideDirectory(f, destDir)) {
                        throw new IOException(
                                "Attempt to write to path outside of destination directory: "
                                        + entry.getName());
                    }

                    f.getParentFile().mkdirs();
                    int count;
                    byte data[] = new byte[8192];
                    // write the files to the disk
                    try (FileOutputStream fos = new FileOutputStream(f);
                            BufferedOutputStream dest = new BufferedOutputStream(
                                    fos, 8192)) {
                        while ((count = zis.read(data, 0, 8192)) != -1) {
                            dest.write(data, 0, count);
                        }
                    }
                }
            }
        }
    }

    private static boolean isFileInsideDirectory(File child, File parent)
            throws IOException {

        child = child.getCanonicalFile();
        parent = parent.getCanonicalFile();

        while (child != null) {
            child = child.getParentFile();

            if (parent.equals(child)) {
                return true;
            }
        }

        return false;

    }

}
