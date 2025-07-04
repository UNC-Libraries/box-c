package edu.unc.lib.boxc.deposit.work;

import static edu.unc.lib.boxc.model.api.ids.PIDConstants.DEPOSITS_QUALIFIER;

import java.io.File;
import java.io.IOException;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ClosedChannelException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.PostConstruct;

import edu.unc.lib.boxc.deposit.impl.model.DepositDirectoryManager;
import edu.unc.lib.boxc.model.api.DatastreamType;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.jena.rdf.model.Bag;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Selector;
import org.apache.jena.rdf.model.SimpleSelector;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import edu.unc.lib.boxc.common.metrics.TimerFactory;
import edu.unc.lib.boxc.deposit.api.DepositConstants;
import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositField;
import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositState;
import edu.unc.lib.boxc.deposit.impl.model.DepositModelManager;
import edu.unc.lib.boxc.deposit.impl.model.DepositStatusFactory;
import edu.unc.lib.boxc.deposit.impl.model.JobStatusFactory;
import edu.unc.lib.boxc.model.api.exceptions.InterruptedLockException;
import edu.unc.lib.boxc.model.api.exceptions.RepositoryException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.ids.PIDMinter;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.rdf.CdrDeposit;
import edu.unc.lib.boxc.model.api.rdf.RDFModelUtil;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.api.events.PremisLogger;
import edu.unc.lib.boxc.operations.api.events.PremisLoggerFactory;
import edu.unc.lib.boxc.persist.api.storage.StorageLocation;
import edu.unc.lib.boxc.persist.api.storage.StorageLocationManager;
import edu.unc.lib.boxc.persist.api.transfer.BinaryTransferService;
import edu.unc.lib.boxc.persist.api.transfer.BinaryTransferSession;
import io.dropwizard.metrics5.Timer;

/**
 * Constructed with deposit directory and deposit ID. Facilitates event logging
 * with standard success/failure states.
 *
 * @author count0
 *
 */
public abstract class AbstractDepositJob implements Runnable {
    private static final Logger log = LoggerFactory
            .getLogger(AbstractDepositJob.class);
    public static final String DEPOSIT_QUEUE = "Deposit";

    protected final Timer timer = TimerFactory.createTimerForClass(getClass(), "job-duration");

    @Autowired
    private JobStatusFactory jobStatusFactory;

    @Autowired
    private DepositStatusFactory depositStatusFactory;

    @Autowired
    protected PIDMinter pidMinter;

    @Autowired
    protected PremisLoggerFactory premisLoggerFactory;

    @Autowired
    protected StorageLocationManager locationManager;
    @Autowired
    private BinaryTransferService transferService;

    // UUID for this deposit and its deposit record
    protected String depositUUID;

    protected PID depositPID;

    // UUID for this ingest job
    protected String jobUUID;

    // Root directory where all deposits are stored
    @Autowired
    private File depositsDirectory;

    private String depositJobId;

    protected boolean rollbackDatasetOnFailure = true;

    @Autowired
    protected DepositModelManager depositModelManager;
    protected DepositDirectoryManager depositDirectoryManager;

    public AbstractDepositJob() {
    }

    public AbstractDepositJob(String uuid, String depositUUID) {
        log.debug("Deposit job created: job:{} deposit:{}", uuid, depositUUID);
        this.jobUUID = uuid;
        this.setDepositUUID(depositUUID);
        this.depositJobId = depositUUID + ":" + this.getClass().getName();
    }

    @PostConstruct
    public void init() {
        this.depositDirectoryManager = new DepositDirectoryManager(depositPID, depositsDirectory.toPath(), true);
    }

    @Override
    public final void run() {
        try {
            try (Timer.Context context = timer.time()) {
                interruptJobIfStopped();

                runJob();
                depositModelManager.commit();
            } catch (JobPausedException e) {
                depositModelManager.commit();
                Thread.interrupted();
                throw e;
            } catch (Exception e) {
                // Clear the interrupted flag before attempting to interact with the dataset, or we may lose progress
                Thread.interrupted();
                depositModelManager.commitOrAbort(rollbackDatasetOnFailure);
                throw e;
            }
        } catch (Exception e) {
            // Recast nested exceptions to more informative types
            resolveNestedExceptions(e);
            throw e;
        }
    }

    private void resolveNestedExceptions(Exception e) {
        Throwable root = ExceptionUtils.getRootCause(e);
        if (root == null) {
            root = e;
        }
        if (root instanceof ClosedByInterruptException || root instanceof ClosedChannelException
                || root instanceof InterruptedException || root instanceof InterruptedLockException) {
            throw new JobInterruptedException("Job " + jobUUID
                    + " interrupted during TDB operation in deposit " + depositUUID, e);
        }
    }

    public abstract void runJob();

    public String getDepositUUID() {
        return depositUUID;
    }

    public void setDepositUUID(String depositUUID) {
        this.depositUUID = depositUUID;
        if (depositUUID != null) {
            this.depositPID = PIDs.get(DEPOSITS_QUALIFIER, depositUUID);
        }
    }

    public PID getDepositPID() {
        return depositPID;
    }

    public String getJobUUID() {
        return jobUUID;
    }

    public void setJobUUID(String uuid) {
        this.jobUUID = uuid;
    }

    protected JobStatusFactory getJobStatusFactory() {
        return jobStatusFactory;
    }

    public void setJobStatusFactory(JobStatusFactory jobStatusFactory) {
        this.jobStatusFactory = jobStatusFactory;
    }

    protected DepositStatusFactory getDepositStatusFactory() {
        return depositStatusFactory;
    }

    public void setDepositStatusFactory(
            DepositStatusFactory depositStatusFactory) {
        this.depositStatusFactory = depositStatusFactory;
    }

    private Map<String, String> depositStatus;
    public Map<String, String> getDepositStatus() {
        if (depositStatus == null) {
            depositStatus = Collections.unmodifiableMap(getDepositStatusFactory().get(depositUUID));
        }
        return depositStatus;
    }

    /**
     * @param field
     * @return get the value of a deposit status field
     */
    public String getDepositField(DepositField field) {
        return getDepositStatus().get(field.name());
    }

    public File getAltTextDir() {
        return depositDirectoryManager.getAltTextDir().toFile();
    }

    public File getDescriptionDir() {
        return depositDirectoryManager.getDescriptionDir().toFile();
    }

    public File getDescriptionHistoryDir() {
        return depositDirectoryManager.getHistoryDir().toFile();
    }

    /**
     * Get the path where MODS should be stored for the given pid
     *
     * @param pid pid of the object
     * @return path for mods
     */
    public Path getModsPath(PID pid) {
        return getModsPath(pid, false);
    }

    /**
     * Get the path where MODS should be stored for the given pid
     *
     * @param pid pid of the object
     * @param createDirs if true, then parent directories for path will be created
     * @return path for mods
     */
    public Path getModsPath(PID pid, boolean createDirs) {
        return depositDirectoryManager.getModsPath(pid, createDirs);
    }

    /**
     * Get the path where alt text should be stored for the given pid
     * @param pid pid of the object
     * @param createDirs if true, then parent directories for path will be created
     * @return Path for the alt text
     */
    public Path getAltTextPath(PID pid, boolean createDirs) {
        return depositDirectoryManager.getAltTextPath(pid, createDirs);
    }

    /**
     * Get path to where MODS history should be stored
     *
     * @param pid
     * @return
     */
    public Path getModsHistoryPath(PID pid) {
        return depositDirectoryManager.getHistoryFile(pid, DatastreamType.MD_DESCRIPTIVE, false);
    }

    public File getDepositsDirectory() {
        return depositsDirectory;
    }

    public File getDepositDirectory() {
        return depositDirectoryManager.getDepositDir().toFile();
    }

    public File getTechMdDirectory() {
        return depositDirectoryManager.getTechMdDir().toFile();
    }

    public File getDataDirectory() {
        return depositDirectoryManager.getDataDir().toFile();
    }

    public File getEventsDirectory() {
        return depositDirectoryManager.getEventsDir().toFile();
    }

    public PremisLoggerFactory getPremisLoggerFactory() {
        return premisLoggerFactory;
    }

    public void setPremisLoggerFactory(PremisLoggerFactory premisLoggerFactory) {
        this.premisLoggerFactory = premisLoggerFactory;
    }

    protected PID getDestinationPID() {
        Map<String, String> depositStatus = getDepositStatus();
        String destinationPath = depositStatus.get(DepositField.containerId.name());
        PID destPid = destinationPath != null ? PIDs.get(destinationPath) : null;
        if (destPid == null) {
            failJob("Invalid destination URI", "The provide destination uri " + destinationPath
                    + " was not a valid repository path");
        }
        return destPid;
    }

    public void failJob(String message, String details) {
        throw buildFailJob(message, details);
    }

    protected JobFailedException buildFailJob(String message, String details) {
        log.debug("failed deposit: {}", message);
        throw new JobFailedException(message, details);
    }

    public void failJob(Throwable throwable, String messageformat, Object... args) {
        throw buildFailJob(throwable, messageformat, args);
    }

    protected JobFailedException buildFailJob(Throwable throwable, String messageformat, Object... args) {
        String message = MessageFormat.format(messageformat, args);
        log.debug("failed deposit: {}", message, throwable);
        return new JobFailedException(message, throwable);
    }

    /**
     * Interrupts the current deposit job if needed, either due to
     * a thread interruption or by the state of the deposit no longer being
     * set to "running"
     *
     * @throws JobInterruptedException thrown if the job was interrupted
     */
    protected void interruptJobIfStopped() throws JobInterruptedException {
        String depositId = getDepositUUID();
        String jobName = getClass().getSimpleName() + ":" + jobUUID;
        if (Thread.currentThread().isInterrupted()) {
            throw new JobInterruptedException("Deposit job " + depositJobId
                    + " has been interrupted, interrupting deposit " + depositId);
        }

        DepositState state = getDepositStatusFactory().getState(depositId);

        // Only throw error if state is changed and it's not in a finished state
        if (!DepositState.running.equals(state) && !DepositState.finished.equals(state)) {
            if (DepositState.paused.equals(state)) {
                throw new JobPausedException("Deposit " + depositId + " was paused, interrupting job " + jobName);
            } else {
                throw new JobInterruptedException("State for deposit " + depositId + " changed from 'running' to '"
                        + state.name() + "', interrupting job " + jobName);
            }
        }
    }

    protected boolean isObjectCompleted(PID objectPid) {
        return jobStatusFactory.objectIsCompleted(depositJobId, objectPid.getQualifiedId());
    }

    protected void markObjectCompleted(PID objectPid) {
        jobStatusFactory.addObjectCompleted(depositJobId, objectPid.getQualifiedId());
    }

    public File getPremisFile(PID pid) {
        return depositDirectoryManager.getPremisPath(pid, true).toFile();
    }

    public Path getTechMdPath(PID pid, boolean createDirs) {
        return depositDirectoryManager.getTechMdPath(pid, createDirs);
    }

    /**
     * Creates new PremisLogger object from which instances can build and write Premis events to a file
     *
     * @param pid
     * @return PremisLogger object
     */
    public PremisLogger getPremisLogger(PID pid) {
        File file = getPremisFile(pid);

        try {
            if (!file.exists()) {
                Files.createDirectories(file.getParentFile().toPath());
            }
            return premisLoggerFactory.createPremisLogger(pid, file);
        } catch (Exception e) {
            failJob(e, "Unexpected problem with deposit events file {0}.", file.getAbsoluteFile().toString());
        }

        return null;
    }

    public Model getWritableModel() {
        return depositModelManager.getWriteModel(depositPID);
    }

    public Model getReadOnlyModel() {
        return depositModelManager.getReadModel(depositPID);
    }

    public void commit(Runnable runnable) {
        depositModelManager.commit(runnable, true);
    }

    public void closeModel() {
        depositModelManager.commit();
    }

    public void destroyModel() {
        depositModelManager.removeModel(depositPID);
    }

    protected void setTotalClicks(int totalClicks) {
        getJobStatusFactory().setTotalCompletion(getJobUUID(), totalClicks);
    }

    protected void addClicks(int clicks) {
        getJobStatusFactory().incrCompletion(getJobUUID(), clicks);
    }

    protected void resetClicks() {
        getJobStatusFactory().setCompletion(getJobUUID(), 0);
    }

    public File getSubdir(String subpath) {
        return new File(getDepositDirectory(), subpath);
    }

    protected void serializeObjectModel(PID pid, Model objModel) {
        File propertiesFile = new File(getSubdir(DepositConstants.AIPS_DIR), pid.getUUID() + ".ttl");

        try {
            RDFModelUtil.serializeModel(objModel, propertiesFile);
        } catch (IOException e) {
            failJob(e, "Failed to serialize properties for object {0} to {1}",
                    pid, propertiesFile.getAbsolutePath());
        }
    }

    /**
     * Retrieve a list of PID to value pairs for the given property
     *
     * @param model
     * @param property
     * @return
     */
    protected List<Entry<PID, String>> getPropertyPairList(Model model, Property property) {
        List<Entry<PID, String>> results = new ArrayList<>();

        Selector stageSelector = new SimpleSelector((Resource) null, property, (RDFNode) null);
        StmtIterator i = model.listStatements(stageSelector);
        while (i.hasNext()) {
            Statement s = i.nextStatement();
            PID p = PIDs.get(s.getSubject().getURI());
            String href = s.getObject().asLiteral().getString();
            Entry<PID, String> entry = new SimpleEntry<>(p, href);
            results.add(entry);
        }

        return results;
    }

    /**
     * Retrieve a list of FileObject pids with their associated original datastream staging location
     * @param model
     * @return
     */
    protected List<Entry<PID, String>> getOriginalStagingPairList(Model model) {
        List<Entry<PID, String>> results = new ArrayList<>();

        Selector stageSelector = new SimpleSelector((Resource) null, CdrDeposit.hasDatastreamOriginal, (RDFNode) null);
        StmtIterator i = model.listStatements(stageSelector);
        while (i.hasNext()) {
            Statement s = i.nextStatement();
            PID fileObjPid = PIDs.get(s.getSubject().getURI());
            try {
                Resource originalResc = s.getResource();

                String href = originalResc.getProperty(CdrDeposit.stagingLocation).getString();
                Entry<PID, String> entry = new SimpleEntry<>(fileObjPid, href);
                results.add(entry);
            } catch (Exception e) {
                throw new RepositoryException("Failed to get stagingLocation for " + fileObjPid, e);
            }
        }

        return results;
    }

    protected BinaryTransferSession getTransferSession(Model depositModel) {
        Bag depositBag = depositModel.getBag(getDepositPID().getRepositoryPath());
        String destLocationId = depositBag.getProperty(Cdr.storageLocation).getString();
        StorageLocation destLocation = locationManager.getStorageLocationById(destLocationId);
        return transferService.getSession(destLocation);
    }

    /**
     * Get the String value of the specified property if present, or return null.
     *
     * @param resc
     * @param property
     * @return
     */
    protected String getPropertyValue(Resource resc, Property property) {
        Statement stmt = resc.getProperty(property);
        if (stmt == null) {
            return null;
        }
        return stmt.getString();
    }
}
