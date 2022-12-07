package edu.unc.lib.boxc.deposit;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.RDFNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.boxc.deposit.work.AbstractDepositJob;
import edu.unc.lib.boxc.model.api.rdf.CdrDeposit;
import edu.unc.lib.boxc.persist.api.sources.IngestSource;
import edu.unc.lib.boxc.persist.api.sources.IngestSourceManager;

/**
 * This job deletes the deposit's processing folder and sets all
 * Redis keys to expire after a configurable delay. It also may delete
 * staged files, surrounding folders, and/or high-level deposit staging folders
 * according to a policy specific to the staging area.
 *
 * @author count0
 *
 */
public class CleanupDepositJob extends AbstractDepositJob {
    private static final Logger LOG = LoggerFactory
            .getLogger(CleanupDepositJob.class);

    private IngestSourceManager sourceManager;

    // Redis expects to receive ints for its delays
    private int statusKeysExpireSeconds;

    public CleanupDepositJob() {
    }

    public CleanupDepositJob(String uuid, String depositUUID) {
        super(uuid, depositUUID);
    }

    /**
     * Delete a file at a URI and returns the parent File object.
     * @param uri the location to delete
     * @return parent File object
     */
    private File deleteFile(URI uri) {
        File parent = null;
        try {
            File cFile = new File(uri.getPath()).getCanonicalFile();
            parent = cFile.getParentFile();
            Files.delete(cFile.toPath());
            LOG.info("Deleted file: {}", cFile.getAbsoluteFile());
        } catch (NoSuchFileException e) {
            LOG.debug("Cannot cleanup file {}, it does not exist", uri);
        } catch (IOException e) {
            LOG.error("Cannot delete a staged file: " + uri.toString(), e);
        }
        return parent;
    }

    public int getStatusKeysExpireSeconds() {
        return this.statusKeysExpireSeconds;
    }

    @Override
    public void runJob() {
        Model m = getReadOnlyModel();

        // clean up staged files according to staging area policy
        deleteStagedFiles(m);

        // delete files identified for cleanup
        deleteCleanupFiles(m);

        // destroy the Jena model for this deposit
        destroyModel();

        // delete deposit folder
        try {
            FileUtils.deleteDirectory(getDepositDirectory());
            LOG.info("Deleted deposit directory: {}", getDepositDirectory());
        } catch (IOException e) {
            LOG.error("Cannot delete deposit directory: "
                    + getDepositDirectory().getAbsolutePath(), e);
        }

        // set this deposit's Redis keys to expire
        getDepositStatusFactory().expireKeys(getDepositUUID(),
                this.getStatusKeysExpireSeconds());
        getJobStatusFactory().expireKeys(getDepositUUID(),
                this.getStatusKeysExpireSeconds());
    }

    private void deleteStagedFiles(Model m) {
        NodeIterator ni = m.listObjectsOfProperty(CdrDeposit.stagingLocation);
        try {
            while (ni.hasNext()) {
                RDFNode n = ni.nextNode();
                URI stagingUri = URI.create(n.asLiteral().getString());
                IngestSource source = sourceManager.getIngestSourceForUri(stagingUri);

                if (!source.isReadOnly()) {
                    File parent = deleteFile(stagingUri);
                    try {
                        if (parent != null) {
                            Files.delete(parent.toPath());
                            LOG.info("Deleted parent folder: {}", parent.toPath());
                        }
                    } catch (DirectoryNotEmptyException e) {
                        LOG.debug("Parent directory {} not cleaned up because it is not empty",
                                parent.getAbsolutePath());
                    } catch (NoSuchFileException e) {
                        LOG.debug("Unable to cleanup parent directory {} because it does not exist",
                                parent.getAbsolutePath());
                    } catch (IOException e) {
                        failJob(e, "Failed to delete staging directory: {0}",
                                parent.getAbsolutePath());
                    }
                }
            }
        } finally {
            ni.close();
        }
    }

    // Cleanup files and directories specifically requested be cleaned up by an earlier job
    private void deleteCleanupFiles(Model m) {
        List<String> cleanupPaths = new ArrayList<>();

        // Create a list of files that need to be cleaned up
        NodeIterator it = m.listObjectsOfProperty(CdrDeposit.cleanupLocation);
        try {
            while (it.hasNext()) {
                RDFNode n = it.nextNode();
                URI cleanupUri = URI.create(n.asLiteral().getString());
                IngestSource source = sourceManager.getIngestSourceForUri(cleanupUri);

                if (!source.isReadOnly()) {
                    cleanupPaths.add(cleanupUri.getPath());
                }
            }
        } finally {
            it.close();
        }

        // Sort cleanup files so that deepest will be deleted first
        Collections.sort(cleanupPaths, Collections.reverseOrder());

        // Perform deletion of cleanup files in order
        for (String pathString : cleanupPaths) {
            File cleanupFile = new File(pathString);
            try {
                // non-recursive delete for files or folders
                Files.delete(cleanupFile.toPath());
                LOG.info("Deleted cleanup file: {}", cleanupFile.getAbsoluteFile());
            } catch (NoSuchFileException e) {
                LOG.info("Cleanup file {} does not exist, skipping",
                        pathString);
            } catch (DirectoryNotEmptyException e) {
                LOG.warn("Cleanup directory {} not removed because it was not empty", pathString);
            } catch (IOException e) {
                LOG.error("Failed to delete cleanup file {}", pathString, e);
            }
        }
    }

    public void setStatusKeysExpireSeconds(int seconds) {
        this.statusKeysExpireSeconds = seconds;
    }

    /**
     * @param sourceManager the sourceManager to set
     */
    public void setIngestSourceManager(IngestSourceManager sourceManager) {
        this.sourceManager = sourceManager;
    }
}
