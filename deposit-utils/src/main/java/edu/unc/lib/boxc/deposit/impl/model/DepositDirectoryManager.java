package edu.unc.lib.boxc.deposit.impl.model;

import static edu.unc.lib.boxc.deposit.api.DepositConstants.DESCRIPTION_DIR;
import static edu.unc.lib.boxc.deposit.api.DepositConstants.EVENTS_DIR;
import static edu.unc.lib.boxc.deposit.api.DepositConstants.HISTORY_DIR;
import static edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants.HASHED_PATH_DEPTH;
import static edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants.HASHED_PATH_SIZE;
import static edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPaths.idToPath;
import static java.nio.file.Files.newOutputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import edu.unc.lib.boxc.model.api.DatastreamType;
import edu.unc.lib.boxc.model.api.exceptions.RepositoryException;
import edu.unc.lib.boxc.model.api.ids.PID;

/**
 * Manages a deposit directory for a single deposit
 *
 * @author bbpennel
 */
public class DepositDirectoryManager {
    public static final String MODEL_FILENAME = "model.n3";

    private Path depositDir;
    private Path descriptionDir;
    private Path historyDir;
    private Path eventsDir;
    private PID depositPid;
    private boolean hashNesting;

    public DepositDirectoryManager(PID depositPid, Path depositBaseDir, boolean hashNesting) {
        this(depositPid, depositBaseDir, hashNesting, true);
    }

    public DepositDirectoryManager(PID depositPid, Path depositBaseDir, boolean hashNesting, boolean createDirs) {
        this.depositPid = depositPid;
        this.depositDir = depositBaseDir.resolve(this.depositPid.getId());
        this.descriptionDir = depositDir.resolve(DESCRIPTION_DIR);
        this.historyDir = depositDir.resolve(HISTORY_DIR);
        this.eventsDir = depositDir.resolve(EVENTS_DIR);
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
     * Write the provided MODS out to file in the deposit
     *
     * @param pid
     * @param modsEl
     */
    public void writeMods(PID pid, Element modsEl) {
        Path modsPath = getModsPath(pid);

        try (OutputStream fos = newOutputStream(modsPath)) {
            // Make a new document for just the MODS, which should add in the xml declaration
            Document modsDoc = new Document();
            modsDoc.addContent(modsEl.clone());
            new XMLOutputter(Format.getPrettyFormat()).output(modsDoc, fos);
        } catch (IOException e) {
            throw new RepositoryException("Unable to write MODS for " + pid.getId(), e);
        }
    }

    /**
     * @param pid
     * @return get the path to where the MODS file for the given PID should be located
     */
    public Path getModsPath(PID pid) {
        return makeMetadataFilePath(descriptionDir, pid, ".xml");
    }

    public Path writeHistoryFile(PID pid, DatastreamType type, InputStream historyStream) {
        Path historyPath = makeMetadataFilePath(historyDir, pid, type.getId() + ".xml");

        try {
            Files.copy(historyStream, historyPath);
        } catch (IOException e) {
            throw new RepositoryException("Unable to write history for " + pid.getId(), e);
        }

        return historyPath;
    }

    public Path getHistoryFile(PID pid, DatastreamType type) {
        return makeMetadataFilePath(historyDir, pid, type.getId() + ".xml");
    }

    /**
     * Get the path in which the PREMIS event log for the provided object should be written.
     *
     * @param pid
     * @return
     */
    public Path getPremisPath(PID pid) {
        return makeMetadataFilePath(eventsDir, pid, ".nt");
    }

    private Path makeMetadataFilePath(Path parentPath, PID pid, String extension) {
        Path mdPath = parentPath;
        if (hashNesting) {
            String hashing = idToPath(pid.getId(), HASHED_PATH_DEPTH, HASHED_PATH_SIZE);
            mdPath = mdPath.resolve(hashing);
            try {
                Files.createDirectories(mdPath);
            } catch (IOException e) {
                throw new RepositoryException("Failed to create hashed metadata directory: " + mdPath, e);
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

    public Path getDepositDir() {
        return depositDir;
    }

    public Path getDescriptionDir() {
        return descriptionDir;
    }

    public Path getHistoryDir() {
        return historyDir;
    }

    public Path getEventsDir() {
        return eventsDir;
    }
}
