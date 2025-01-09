package edu.unc.lib.boxc.deposit.impl.model;

import edu.unc.lib.boxc.deposit.api.DepositConstants;
import edu.unc.lib.boxc.model.api.DatastreamType;
import edu.unc.lib.boxc.model.api.exceptions.RepositoryException;
import edu.unc.lib.boxc.model.api.ids.PID;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static edu.unc.lib.boxc.deposit.api.DepositConstants.DESCRIPTION_DIR;
import static edu.unc.lib.boxc.deposit.api.DepositConstants.EVENTS_DIR;
import static edu.unc.lib.boxc.deposit.api.DepositConstants.HISTORY_DIR;
import static edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants.HASHED_PATH_DEPTH;
import static edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants.HASHED_PATH_SIZE;
import static edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPaths.idToPath;

/**
 * Manages a deposit directory for a single deposit
 *
 * @author bbpennel
 */
public class DepositDirectoryManager {
    public static final String MODEL_FILENAME = "model.n3";

    private final Path depositDir;
    private final Path descriptionDir;
    private final Path historyDir;
    private final Path eventsDir;
    private final Path altTextDir;
    private final Path techMdDir;
    private final Path dataDir;
    private final PID depositPid;
    private final boolean hashNesting;

    public DepositDirectoryManager(PID depositPid, Path depositBaseDir, boolean hashNesting) {
        this(depositPid, depositBaseDir, hashNesting, true);
    }

    public DepositDirectoryManager(PID depositPid, Path depositBaseDir, boolean hashNesting, boolean createDirs) {
        this.depositPid = depositPid;
        this.depositDir = depositBaseDir.resolve(this.depositPid.getId());
        this.descriptionDir = depositDir.resolve(DESCRIPTION_DIR);
        this.historyDir = depositDir.resolve(HISTORY_DIR);
        this.eventsDir = depositDir.resolve(EVENTS_DIR);
        this.altTextDir = depositDir.resolve(DepositConstants.ALT_TEXT_DIR);
        this.techMdDir = depositDir.resolve(DepositConstants.TECHMD_DIR);
        this.dataDir = depositDir.resolve(DepositConstants.DATA_DIR);
        this.hashNesting = hashNesting;

        if (createDirs) {
            createDepositDirectory();
        }
    }

    public void createDepositDirectory() {
        try {
            Files.createDirectories(depositDir);
            Files.createDirectories(descriptionDir);
            Files.createDirectories(historyDir);
            Files.createDirectories(eventsDir);
            Files.createDirectories(altTextDir);
            Files.createDirectories(techMdDir);
            Files.createDirectories(dataDir);
        } catch (IOException e) {
            throw new RepositoryException("Failed to create deposit directory: " + depositDir, e);
        }
    }

    public void cleanupDepositDirectory() {
        try {
            FileUtils.deleteDirectory(depositDir.toFile());
        } catch (IOException e) {
            throw new RepositoryException("Failed to cleanup deposit directory: " + depositDir, e);
        }
    }

    /**
     * @param pid
     * @return get the path to where the MODS file for the given PID should be located
     */
    public Path getModsPath(PID pid) {
        return getModsPath(pid, false);
    }

    public Path getModsPath(PID pid, boolean createDirs) {
        return makeMetadataFilePath(descriptionDir, pid, ".xml", createDirs);
    }

    public Path getAltTextPath(PID pid) {
        return getAltTextPath(pid, false);
    }

    /**
     * Get the path to the alt text file for the provided object
     * @param pid
     * @param createDirs
     * @return
     */
    public Path getAltTextPath(PID pid, boolean createDirs) {
        return makeMetadataFilePath(altTextDir, pid, ".txt", createDirs);
    }

    public Path writeHistoryFile(PID pid, DatastreamType type, InputStream historyStream) {
        Path historyPath = makeMetadataFilePath(historyDir, pid, type.getId() + ".xml", true);

        try {
            Files.copy(historyStream, historyPath);
        } catch (IOException e) {
            throw new RepositoryException("Unable to write history for " + pid.getId(), e);
        }

        return historyPath;
    }

    public Path getHistoryFile(PID pid, DatastreamType type) {
        return getHistoryFile(pid, type, false);
    }

    /**
     * Get the path to the history file for the provided object for the provided datastream type
     * @param pid
     * @param type
     * @param createDirs
     * @return
     */
    public Path getHistoryFile(PID pid, DatastreamType type, boolean createDirs) {
        return makeMetadataFilePath(historyDir, pid, type.getId() + ".xml", createDirs);
    }

    /**
     * Get the path in which the PREMIS event log for the provided object should be written.
     *
     * @param pid
     * @return
     */
    public Path getPremisPath(PID pid) {
        return getPremisPath(pid, false);
    }

    public Path getPremisPath(PID pid, boolean createDirs) {
        return makeMetadataFilePath(eventsDir, pid, ".nt", createDirs);
    }

    private Path makeMetadataFilePath(Path parentPath, PID pid, String extension, boolean createDirs) {
        Path mdPath = parentPath;
        if (hashNesting) {
            String hashing = idToPath(pid.getId(), HASHED_PATH_DEPTH, HASHED_PATH_SIZE);
            mdPath = mdPath.resolve(hashing);
            if (createDirs) {
                try {
                    Files.createDirectories(mdPath);
                } catch (IOException e) {
                    throw new RepositoryException("Failed to create hashed metadata directory: " + mdPath, e);
                }
            }
        }
        return mdPath.resolve(pid.getId() + extension);
    }

    /**
     * @return Path of triples file containing the deposit model
     */
    public Path getModelPath() {
        return depositDir.resolve(MODEL_FILENAME);
    }

    /**
     * @return the directory containing all files related to the deposit
     */
    public Path getDepositDir() {
        return depositDir;
    }

    /**
     * @return the directory where description files are stored
     */
    public Path getDescriptionDir() {
        return descriptionDir;
    }

    /**
     * @return the directory where history files are stored
     */
    public Path getHistoryDir() {
        return historyDir;
    }

    /**
     * @return the directory where PREMIS event log files are stored
     */
    public Path getEventsDir() {
        return eventsDir;
    }

    /**
     * @return the directory where alt text files are stored
     */
    public Path getAltTextDir() {
        return altTextDir;
    }

    /**
     * @return the directory where technical metadata files are stored
     */
    public Path getTechMdDir() {
        return techMdDir;
    }

    public Path getTechMdPath(PID pid) {
        return getTechMdPath(pid, false);
    }

    /**
     * Get the path to the technical metadata file for the provided object
     * @param pid
     * @param createDirs
     * @return
     */
    public Path getTechMdPath(PID pid, boolean createDirs) {
        return makeMetadataFilePath(techMdDir, pid, ".xml", createDirs);
    }

    /**
     * @return Directory where local data files are stored
     */
    public Path getDataDir() {
        return dataDir;
    }
}
