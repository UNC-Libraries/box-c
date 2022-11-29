package edu.unc.lib.boxc.operations.impl.events;

import static edu.unc.lib.boxc.model.fcrepo.ids.DatastreamPids.getMdEventsPid;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.concurrent.locks.Lock;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.vocabulary.RDF;

import edu.unc.lib.boxc.model.api.exceptions.ObjectPersistenceException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.ids.PIDMinter;
import edu.unc.lib.boxc.model.api.rdf.Premis;
import edu.unc.lib.boxc.model.fcrepo.ids.PidLockManager;
import edu.unc.lib.boxc.operations.api.events.PremisLogger;

/**
 * Logs PREMIS events for a repository object to a backing file
 *
 * @author lfarrell
 *
 */
public class FilePremisLogger implements PremisLogger {
    private static final PidLockManager lockManager = PidLockManager.getDefaultPidLockManager();

    private File premisFile;
    private PID objectPid;
    private PID logPid;
    private Model model;

    private PIDMinter pidMinter;

    public FilePremisLogger(PID pid, File file, PIDMinter pidMinter) {
        this.objectPid = pid;
        this.premisFile = file;
        this.pidMinter = pidMinter;
        this.logPid = getMdEventsPid(objectPid);
    }

    @Override
    public PremisEventBuilderImpl buildEvent(PID eventPid, Resource eventType, Date date) {
        if (eventPid == null) {
            eventPid = pidMinter.mintPremisEventPid(objectPid);
        }
        if (date == null) {
            date = new Date();
        }

        return new PremisEventBuilderImpl(objectPid, eventPid, eventType, date, this);
    }

    /**
     * Returns an instance of buildEvent with the timestamp automatically set to the current time
     *
     * @param eventType
     * @return PremisEventBuilder
     */
    @Override
    public PremisEventBuilderImpl buildEvent(Resource eventType) {
        return buildEvent(null, eventType, null);
    }

    /**
     * Adds an event to the log file
     *
     * @param eventResources
     * @return
     */
    @Override
    public PremisLogger writeEvents(Resource... eventResources) {
        Lock logLock = lockManager.awaitWriteLock(logPid);
        try {
            Model logModel = getModel();
            // For new logs, add in representation statement
            if (!premisFile.exists()) {
                Resource repoObjResc = logModel.getResource(objectPid.getRepositoryPath());
                repoObjResc.addProperty(RDF.type, Premis.Representation);
            }

            // Add the events to the model for this event log
            for (Resource eventResc: eventResources) {
                logModel.add(eventResc.getModel());
            }

            // Persist the log to file
            try (FileOutputStream rdfFile = new FileOutputStream(premisFile)) {
                RDFDataMgr.write(rdfFile, logModel, RDFFormat.NTRIPLES);
            } catch (IOException e) {
                throw new ObjectPersistenceException("Failed to stream PREMIS log to file for " + objectPid, e);
            }

            return this;
        } finally {
            logLock.unlock();
        }
    }

    @Override
    public PremisLogger createLog(InputStream contentStream) {
        throw new NotImplementedException("Method is not implemented");
    }

    /**
     * Returns the Model containing events from this logger
     *
     * @return
     */
    public Model getModel() {
        if (model != null) {
            return model;
        }

        model = ModelFactory.createDefaultModel();

        if (premisFile != null && premisFile.exists()) {
            InputStream in = RDFDataMgr.open(premisFile.getAbsolutePath());
            model.read(in, null, "N-TRIPLES");
        }

        return model;
    }

    @Override
    public Model getEventsModel() {
        return getModel();
    }

    @Override
    public void close() {
        // Nothing to release currently
    }

    @Override
    public boolean isClosed() {
        return false;
    }
}
