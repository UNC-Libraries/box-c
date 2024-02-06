package edu.unc.lib.boxc.operations.impl.versioning;

import static edu.unc.lib.boxc.model.fcrepo.ids.DatastreamPids.getDatastreamHistoryPid;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.locks.Lock;

import edu.unc.lib.boxc.fcrepo.exceptions.OptimisticLockException;
import edu.unc.lib.boxc.persist.impl.InputStreamDigestUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.rdf.model.Model;
import org.slf4j.Logger;

import edu.unc.lib.boxc.fcrepo.exceptions.ServiceException;
import edu.unc.lib.boxc.fcrepo.utils.FedoraTransaction;
import edu.unc.lib.boxc.fcrepo.utils.TransactionManager;
import edu.unc.lib.boxc.model.api.exceptions.NotFoundException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.BinaryObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.model.fcrepo.ids.PidLockManager;
import edu.unc.lib.boxc.persist.api.exceptions.InvalidChecksumException;
import edu.unc.lib.boxc.persist.api.transfer.BinaryTransferOutcome;
import edu.unc.lib.boxc.persist.api.transfer.BinaryTransferService;
import edu.unc.lib.boxc.persist.api.transfer.BinaryTransferSession;

/**
 * Service for managing versions of a datastream using an XML based history log.
 *
 * This form of versioning should only be used for small or text based datastreams.
 *
 * @author bbpennel
 */
public class VersionedDatastreamService {
    private static final Logger log = getLogger(VersionedDatastreamService.class);

    private RepositoryObjectLoader repoObjLoader;
    private RepositoryObjectFactory repoObjFactory;
    private BinaryTransferService transferService;
    private TransactionManager transactionManager;
    private static final PidLockManager lockManager = PidLockManager.getDefaultPidLockManager();

    /**
     * Update a versioned datastream. If the datastream already exists, its previous
     * value will be moved into a history log object.
     *
     * @param newVersion details of the new datastream version
     * @return the BinaryObject representation of the datastream updated
     */
    public BinaryObject addVersion(DatastreamVersion newVersion) {
        PID dsPid = newVersion.getDsPid();

        Lock dsLock = lockManager.awaitWriteLock(dsPid);
        BinaryObject dsObj = getBinaryObject(dsPid);
        checkOptimisticLock(dsObj, newVersion);

        // Get a session for transferring the binary and its history
        BinaryTransferSession session = null;
        FedoraTransaction tx = null;
        try {
            session = getTransferSession(newVersion, dsObj);
            tx = transactionManager.startTransaction();
            // if datastream is new, go ahead and create it
            if (dsObj == null) {
                log.debug("Adding head version for datastream {}", dsPid);
                return updateHeadVersion(newVersion, session);
            } else {
                if (skipUpdateDueToUnmodifiedContent(dsObj, newVersion)) {
                    return dsObj;
                }
                log.debug("Adding history and head version for datastream {}", dsPid);
                // Datastream already exists
                // Add the current head version to the history log
                updateDatastreamHistory(session, dsObj);

                // Replace the head version with the new content
                return updateHeadVersion(newVersion, session);
            }
        } catch (Exception e) {
            if (tx != null) {
                tx.cancelAndIgnore();
            }
            throw e;
        } finally {
            if (tx != null) {
                tx.close();
            }
            dsLock.unlock();
            // Only close the transfer session if it was created within this method call
            if (session != null && newVersion.getTransferSession() == null) {
                session.close();
            }
        }
    }

    // Throws an OptimisticLockException if the binary has been modified after the locking timestamp in the newVersion
    private void checkOptimisticLock(BinaryObject dsObj, DatastreamVersion newVersion) {
        if (dsObj != null && newVersion.getUnmodifiedSince() != null) {
            Instant fcrepoModified = dsObj.getLastModified().toInstant();
            if (newVersion.getUnmodifiedSince().isBefore(fcrepoModified)) {
                throw new OptimisticLockException("Rejecting update to datastream " + dsObj.getPid().getQualifiedId()
                        + ", update specifies datastream must not have been modified since "
                        + newVersion.getUnmodifiedSince()
                        + " but version in the repository was last updated " + fcrepoModified);
            }
        }
    }

    private boolean skipUpdateDueToUnmodifiedContent(BinaryObject dsObj, DatastreamVersion newVersion) {
        if (newVersion.isSkipUnmodified()) {
            var oldSha1 = StringUtils.substringAfterLast(dsObj.getSha1Checksum(), ":");
            var newSha1 = InputStreamDigestUtil.computeDigest(newVersion.getContentStream());
            if (newSha1.equals(oldSha1)) {
                log.debug("Skipping update of {}, old version and new version have the same digest", dsObj.getPid());
                return true;
            } else {
                log.debug("Continuing with update of {}, content has changed", dsObj.getPid());
                try {
                    // Reset inputstream to beginning so we can write it to file
                    newVersion.getContentStream().reset();
                } catch (IOException e) {
                    throw new ServiceException("Invalid content stream, must support reset", e);
                }
            }
        }
        return false;
    }

    /**
     * Get the binary object represented by the given pid, or null if it does not exist.
     * @param dsPid
     * @return
     */
    private BinaryObject getBinaryObject(PID dsPid) {
        try {
            return repoObjLoader.getBinaryObject(dsPid);
        } catch (NotFoundException e) {
            return null;
        }
    }

    /**
     * Add the current state of the datastream to its history log. If no history log exists,
     * one will be created.
     *
     * @param session
     * @param currentDsObj
     */
    private void updateDatastreamHistory(BinaryTransferSession session, BinaryObject currentDsObj) {
        PID currentDsPid = currentDsObj.getPid();

        // Load existing datastream history if present
        PID dsHistoryPid = getDatastreamHistoryPid(currentDsPid);
        BinaryObject dsHistory = getBinaryObject(dsHistoryPid);
        // No history, start new one
        DatastreamHistoryLog historyLog;
        // For the first entry in the log, use when the datastream was created. After use modified date
        Date versionDate;
        if (dsHistory == null) {
            historyLog = new DatastreamHistoryLog(currentDsPid);
            versionDate = currentDsObj.getCreatedDate();
        } else {
            historyLog = new DatastreamHistoryLog(currentDsPid, dsHistory.getBinaryStream());
            versionDate = currentDsObj.getLastModified();
        }

        historyLog.addVersion(currentDsObj.getBinaryStream(),
                currentDsObj.getMimetype(),
                versionDate);

        BinaryTransferOutcome historyOutcome;
        try {
            historyOutcome = session.transferReplaceExisting(dsHistoryPid, historyLog.toInputStream());
        } catch (IOException e) {
            throw new ServiceException("Failed to serialize history for " + currentDsPid, e);
        }

        // Update the history object in fedora
        repoObjFactory.createOrUpdateBinary(dsHistoryPid,
                historyOutcome.getDestinationUri(),
                null,
                "text/xml",
                historyOutcome.getSha1(),
                null,
                null);
    }

    private BinaryObject updateHeadVersion(DatastreamVersion newVersion, BinaryTransferSession session) {
        PID dsPid = newVersion.getDsPid();
        // Transfer the incoming content to its storage location
        BinaryTransferOutcome dsOutcome;
        if (newVersion.getStagedContentUri() == null) {
            dsOutcome = session.transferReplaceExisting(dsPid, newVersion.getContentStream());
        } else {
            dsOutcome = session.transferReplaceExisting(dsPid, newVersion.getStagedContentUri());
        }

        if (newVersion.getSha1() != null && !newVersion.getSha1().equals(dsOutcome.getSha1()) ) {
            throw new InvalidChecksumException("Checksum mismatch when updating head version of datastream "
                    + newVersion.getDsPid().getQualifiedId() + ": expected " + newVersion.getSha1()
                    + ", calculated " + dsOutcome.getSha1());
        }

        return repoObjFactory.createOrUpdateBinary(newVersion.getDsPid(),
                dsOutcome.getDestinationUri(),
                newVersion.getFilename(),
                newVersion.getContentType(),
                dsOutcome.getSha1(),
                newVersion.getMd5(),
                newVersion.getProperties());
    }

    /**
     * Get a transfer session for copying the new content to its destination location.
     * This may either be a new session, or an existing session if one was provided.
     *
     * @param newVersion
     * @param dsObj
     * @return
     */
    private BinaryTransferSession getTransferSession(DatastreamVersion newVersion, RepositoryObject dsObj) {
        if (newVersion.getTransferSession() != null) {
            return newVersion.getTransferSession();
        }

        if (dsObj != null) {
            return transferService.getSession(dsObj);
        }

        PID parentPid = PIDs.get(newVersion.getDsPid().getId());
        if (parentPid.equals(newVersion.getDsPid())) {
            throw new IllegalArgumentException("Unable to determine parent of datastream, the provided PID does not "
                    + "confirm to the expected datastream PID format: " + newVersion.getDsPid().getQualifiedId());
        }
        RepositoryObject parentObj = repoObjLoader.getRepositoryObject(parentPid);
        return transferService.getSession(parentObj);
    }

    public void setRepositoryObjectLoader(RepositoryObjectLoader repoObjLoader) {
        this.repoObjLoader = repoObjLoader;
    }

    public void setRepositoryObjectFactory(RepositoryObjectFactory repoObjFactory) {
        this.repoObjFactory = repoObjFactory;
    }

    public void setBinaryTransferService(BinaryTransferService transferService) {
        this.transferService = transferService;
    }

    public void setTransactionManager(TransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    public static enum DatastreamVersioningOption {
        SKIP_UNMODIFIED
    }

    /**
     * Details of a datastream version
     *
     * @author bbpennel
     */
    public static class DatastreamVersion {
        private PID dsPid;
        private InputStream contentStream;
        private URI stagedContentUri;
        private String contentType;
        private BinaryTransferSession transferSession;
        private String md5;
        private String sha1;
        private String filename;
        private Model properties;
        // Date after which the datastream must not have been modified, for optimistic locking
        private Instant unmodifiedSince;
        // If true, then no new version should be created if the checksum of the new content is the same as the old
        private boolean skipUnmodified;

        public DatastreamVersion(PID dsPid) {
            this.dsPid = dsPid;
        }

        public PID getDsPid() {
            return dsPid;
        }

        public InputStream getContentStream() {
            return contentStream;
        }

        public void setContentStream(InputStream contentStream) {
            this.contentStream = contentStream;
        }

        public URI getStagedContentUri() {
            return stagedContentUri;
        }

        public void setStagedContentUri(URI contentUri) {
            this.stagedContentUri = contentUri;
        }

        public String getContentType() {
            return contentType;
        }

        public void setContentType(String contentType) {
            this.contentType = contentType;
        }

        public BinaryTransferSession getTransferSession() {
            return transferSession;
        }

        public void setTransferSession(BinaryTransferSession transferSession) {
            this.transferSession = transferSession;
        }

        public String getMd5() {
            return md5;
        }

        public void setMd5(String md5) {
            this.md5 = md5;
        }

        public String getSha1() {
            return sha1;
        }

        public void setSha1(String sha1) {
            this.sha1 = sha1;
        }

        public String getFilename() {
            return filename;
        }

        public void setFilename(String filename) {
            this.filename = filename;
        }

        public Model getProperties() {
            return properties;
        }

        public void setProperties(Model properties) {
            this.properties = properties;
        }

        public Instant getUnmodifiedSince() {
            return unmodifiedSince;
        }

        public void setUnmodifiedSince(Instant unmodifiedSince) {
            this.unmodifiedSince = unmodifiedSince;
        }

        public boolean isSkipUnmodified() {
            return skipUnmodified;
        }

        public void setSkipUnmodified(boolean skipUnmodified) {
            this.skipUnmodified = skipUnmodified;
        }
    }
}
