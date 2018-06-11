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
package edu.unc.lib.deposit.work;

import static edu.unc.lib.dl.util.DepositConstants.DESCRIPTION_DIR;
import static edu.unc.lib.dl.util.DepositConstants.TECHMD_DIR;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.PostConstruct;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Selector;
import org.apache.jena.rdf.model.SimpleSelector;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import edu.unc.lib.deposit.staging.StagingException;
import edu.unc.lib.dl.event.PremisLogger;
import edu.unc.lib.dl.event.PremisLoggerFactory;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.RepositoryPIDMinter;
import edu.unc.lib.dl.fcrepo4.RepositoryPathConstants;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.metrics.TimerFactory;
import edu.unc.lib.dl.util.DepositConstants;
import edu.unc.lib.dl.util.DepositStatusFactory;
import edu.unc.lib.dl.util.JobStatusFactory;
import edu.unc.lib.dl.util.RDFModelUtil;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositState;
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
    protected RepositoryPIDMinter pidMinter;

    @Autowired
    protected PremisLoggerFactory premisLoggerFactory;

    // UUID for this deposit and its deposit record
    private String depositUUID;

    private PID depositPID;

    // UUID for this ingest job
    private String jobUUID;

    // Root directory where all deposits are stored
    @Autowired
    private File depositsDirectory;

    // Directory for this deposit
    private File depositDirectory;

    // Directory for local data files
    private File dataDirectory;

    // Directory containing PREMIS event files for individual objects in this
    // deposit
    private File eventsDirectory;

    private File techmdDir;

    @Autowired
    private Dataset dataset;

    public AbstractDepositJob() {
    }

    public AbstractDepositJob(String uuid, String depositUUID) {
        log.debug("Deposit job created: job:{} deposit:{}", uuid, depositUUID);
        this.jobUUID = uuid;
        this.setDepositUUID(depositUUID);
    }

    @PostConstruct
    public void init() {
        this.depositDirectory = new File(depositsDirectory, depositUUID);
        this.dataDirectory = new File(depositDirectory,
                DepositConstants.DATA_DIR);
        this.eventsDirectory = new File(depositDirectory,
                DepositConstants.EVENTS_DIR);

        this.techmdDir = new File(depositDirectory, TECHMD_DIR);
    }

    @Override
    public final void run() {
        try (Timer.Context context = timer.time()) {
            runJob();
            if (dataset.isInTransaction()) {
                dataset.commit();
            }
        } catch (Throwable e) {
            if (dataset.isInTransaction()) {
                dataset.abort();
            }
            throw e;
        } finally {
            dataset.end();
        }
    }

    public abstract void runJob();

    public String getDepositUUID() {
        return depositUUID;
    }

    public void setDepositUUID(String depositUUID) {
        this.depositUUID = depositUUID;
        this.depositPID = PIDs.get(RepositoryPathConstants.DEPOSIT_RECORD_BASE, depositUUID);
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

    public Map<String, String> getDepositStatus() {
        Map<String, String> result = this.getDepositStatusFactory().get(
                depositUUID);
        return Collections.unmodifiableMap(result);
    }

    public File getDescriptionDir() {
        return new File(getDepositDirectory(), DESCRIPTION_DIR);
    }

    public File getDepositsDirectory() {
        return depositsDirectory;
    }

    public File getDepositDirectory() {
        return depositDirectory;
    }

    public File getTechMdDirectory() {
        return techmdDir;
    }

    public void setDepositDirectory(File depositDirectory) {
        this.depositDirectory = depositDirectory;
    }

    public File getDataDirectory() {
        return dataDirectory;
    }

    public File getEventsDirectory() {
        return eventsDirectory;
    }

    public PremisLoggerFactory getPremisLoggerFactory() {
        return premisLoggerFactory;
    }

    public void setPremisLoggerFactory(PremisLoggerFactory premisLoggerFactory) {
        this.premisLoggerFactory = premisLoggerFactory;
    }

    /**
     * Returns the manifest URIs for this deposit, or an empty list in case there are no manifests.
     *
     * @return
     */
    public List<String> getManifestFileURIs() {
        List<String> filePaths = depositStatusFactory.getManifestURIs(getDepositUUID());
        return filePaths;
    }

    /**
     * Resolves a staged file href and returns a URI for that href.
     *
     * @param href
     * @return
     * @throws StagingException
     *             thrown if the href is not a valid staging location uri
     */
    protected URI getStagedUri(String href) throws StagingException {
        URI manifestUri = null;
        try {
            manifestUri = new URI(href);
        } catch (URISyntaxException e) {
            throw new StagingException("Unable to parse manifest URI: " + href);
        }

        // Only supporting local file uris at this time
        if (manifestUri.getScheme() == null || manifestUri.getScheme().contains("file")) {
            // If the uri is relative, then resolve it against the deposit directory
            if (!manifestUri.isAbsolute()) {
                manifestUri = getDepositDirectory().toURI().resolve(manifestUri);
            }

            return manifestUri;
        } else {
            throw new StagingException(
                    "Unsupported uri scheme encountered while attempting to resolve staged file: " + href);
        }
    }

    public void failJob(String message, String details) {
        log.debug("failed deposit: {}", message);
        throw new JobFailedException(message, details);
    }

    public void failJob(Throwable throwable, String messageformat, Object... args) {
        String message = MessageFormat.format(messageformat, args);
        log.debug("failed deposit: {}", message, throwable);
        throw new JobFailedException(message, throwable);
    }

    protected void verifyRunning() {
        DepositState state = getDepositStatusFactory().getState(getDepositUUID());

        if (!DepositState.running.equals(state)) {
            throw new JobInterruptedException("State for job " + getDepositUUID()
                    + " is no longer running, interrupting");
        }
    }

    /**
     * Creates new PremisLogger object from which instances can build and write Premis events to a file
     *
     * @param pid
     * @return PremisLogger object
     */
    public PremisLogger getPremisLogger(PID pid) {
        File file = new File(depositDirectory, DepositConstants.EVENTS_DIR + "/" + pid.getUUID() + ".ttl");

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
        String uri = getDepositPID().getURI();
        this.dataset.begin(ReadWrite.WRITE);
        if (!this.dataset.containsNamedModel(uri)) {
            this.dataset.addNamedModel(uri, ModelFactory.createDefaultModel());
        }
        return this.dataset.getNamedModel(uri).begin();
    }

    public Model getReadOnlyModel() {
        String uri = getDepositPID().getURI();
        this.dataset.begin(ReadWrite.READ);
        return this.dataset.getNamedModel(uri).begin();
    }

    public void closeModel() {
        if (dataset.isInTransaction()) {
            dataset.commit();
            dataset.end();
        }
    }

    public void destroyModel() {
        String uri = getDepositPID().getURI();
        if (!dataset.isInTransaction()) {
            getWritableModel();
        }
        if (this.dataset.containsNamedModel(uri)) {
            this.dataset.removeNamedModel(uri);
        }
    }

    protected void setTotalClicks(int totalClicks) {
        getJobStatusFactory().setTotalCompletion(getJobUUID(), totalClicks);
    }

    protected void addClicks(int clicks) {
        getJobStatusFactory().incrCompletion(getJobUUID(), clicks);
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
     * Return an iterator for the children of the given resource, based on what
     * type of container it is.
     *
     * @param resc
     * @return
     */
    protected NodeIterator getChildIterator(Resource resc) {
        if (resc.hasProperty(RDF.type, RDF.Bag)) {
            return resc.getModel().getBag(resc).iterator();
        } else if (resc.hasProperty(RDF.type, RDF.Seq)) {
            return resc.getModel().getSeq(resc).iterator();
        } else {
            return null;
        }
    }
}