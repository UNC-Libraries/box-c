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
package edu.unc.lib.dl.persist.services.deposit;

import static edu.unc.lib.dl.fcrepo4.RepositoryPathConstants.HASHED_PATH_DEPTH;
import static edu.unc.lib.dl.fcrepo4.RepositoryPathConstants.HASHED_PATH_SIZE;
import static edu.unc.lib.dl.fcrepo4.RepositoryPaths.idToPath;
import static edu.unc.lib.dl.util.DepositConstants.DESCRIPTION_DIR;
import static edu.unc.lib.dl.util.DepositConstants.EVENTS_DIR;
import static edu.unc.lib.dl.util.DepositConstants.HISTORY_DIR;
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

import edu.unc.lib.dl.exceptions.RepositoryException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.model.DatastreamType;

/**
 * Manages a deposit directory for a single deposit
 *
 * @author bbpennel
 */
public class DepositDirectoryManager {

    private Path depositDir;
    private Path descriptionDir;
    private Path historyDir;
    private Path eventsDir;
    private PID depositPid;
    private boolean hashNesting;

    public DepositDirectoryManager(PID depositPid, Path depositBaseDir, boolean hashNesting) {
        this.depositPid = depositPid;
        this.depositDir = depositBaseDir.resolve(this.depositPid.getId());
        this.descriptionDir = depositDir.resolve(DESCRIPTION_DIR);
        this.historyDir = depositDir.resolve(HISTORY_DIR);
        this.eventsDir = depositDir.resolve(EVENTS_DIR);
        this.hashNesting = hashNesting;

        createDepositDirectory();
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
        Path modsPath = makeMetadataFilePath(descriptionDir, pid, ".xml");

        try (OutputStream fos = newOutputStream(modsPath)) {
            // Make a new document for just the MODS, which should add in the xml declaration
            Document modsDoc = new Document();
            modsDoc.addContent(modsEl.clone());
            new XMLOutputter(Format.getPrettyFormat()).output(modsDoc, fos);
        } catch (IOException e) {
            throw new RepositoryException("Unable to write MODS for " + pid.getId(), e);
        }
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
