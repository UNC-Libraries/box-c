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
package edu.unc.lib.boxc.operations.impl.events;

import static edu.unc.lib.boxc.model.api.DatastreamType.MD_EVENTS;
import static edu.unc.lib.boxc.model.fcrepo.ids.DatastreamPids.getMdEventsPid;
import static java.lang.System.lineSeparator;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.Date;
import java.util.concurrent.locks.Lock;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;

import edu.unc.lib.boxc.model.api.exceptions.ObjectPersistenceException;
import edu.unc.lib.boxc.model.api.exceptions.RepositoryException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.ids.PIDMinter;
import edu.unc.lib.boxc.model.api.objects.BinaryObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.rdf.Premis;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.model.fcrepo.event.RepositoryPremisLog;
import edu.unc.lib.boxc.operations.api.events.PremisLogger;
import edu.unc.lib.boxc.persist.api.transfer.BinaryTransferOutcome;
import edu.unc.lib.boxc.persist.api.transfer.BinaryTransferSession;
import edu.unc.lib.dl.util.RDFModelUtil;


/**
 * Logs PREMIS events for a repository object, which are persisted as PREMIS
 * event objects in the repository.
 *
 * @author bbpennel
 *
 */
public class RepositoryPremisLogger extends RepositoryPremisLog implements PremisLogger {

    private static final Logger log = getLogger(RepositoryPremisLogger.class);

    private PIDMinter pidMinter;
    private RepositoryObjectFactory repoObjFactory;
    private BinaryTransferSession transferSession;

    private boolean closed = false;

    public RepositoryPremisLogger(RepositoryObject repoObject, BinaryTransferSession transferSession,
            PIDMinter pidMinter, RepositoryObjectLoader repoObjLoader,
            RepositoryObjectFactory repoObjFactory) {
        super(repoObject, repoObjLoader);
        this.pidMinter = pidMinter;
        this.repoObjFactory = repoObjFactory;
        this.transferSession = transferSession;
    }

    @Override
    public PremisEventBuilderImpl buildEvent(PID eventPid, Resource eventType, Date date) {
        if (eventPid == null) {
            eventPid = pidMinter.mintPremisEventPid(repoObject.getPid());
        }
        if (date == null) {
            date = new Date();
        }

        return new PremisEventBuilderImpl(repoObject.getPid(), eventPid, eventType, date, this);
    }

    @Override
    public PremisEventBuilderImpl buildEvent(Resource eventType) {
        return buildEvent(null, eventType, null);
    }



    @Override
    public PremisLogger writeEvents(Resource... eventResources) {
        PID objPid = repoObject.getPid();
        PID logPid = getMdEventsPid(objPid);
        Lock logLock = lockManager.awaitWriteLock(logPid);
        try {
            Model logModel = ModelFactory.createDefaultModel();

            Statement s = repoObject.getResource(true).getProperty(Cdr.hasEvents);
            boolean isNewLog = s == null;

            // For new logs, add in representation statement
            if (isNewLog) {
                Resource repoObjResc = logModel.getResource(objPid.getRepositoryPath());
                repoObjResc.addProperty(RDF.type, Premis.Representation);
            }

            // Add new events to log
            for (Resource eventResc: eventResources) {
                logModel.add(eventResc.getModel());
            }

            // Stream the event RDF as NTriples
            InputStream modelStream;
            try {
                modelStream = RDFModelUtil.streamModel(logModel, RDFFormat.NTRIPLES);
            } catch (IOException e) {
                throw new ObjectPersistenceException("Failed to serialize event to RDF for " + objPid, e);
            }

            // Premis event log not created yet
            if (isNewLog) {
                createLog(modelStream);
            } else {
                log.debug("Adding events to PREMIS log for {}", objPid);
                // Event log exists, append new events to it
                BinaryObject logObj = repoObjLoader.getBinaryObject(logPid);

                InputStream newContentStream = new SequenceInputStream(
                        new ByteArrayInputStream(lineSeparator().getBytes(UTF_8)),
                        modelStream);

                try (InputStream existingLogStream = logObj.getBinaryStream()) {
                    InputStream mergedStream = new SequenceInputStream(
                            existingLogStream,
                            newContentStream);

                    updateOrCreateLog(mergedStream);
                } catch (IOException e) {
                    throw new RepositoryException("Failed to close log existing stream", e);
                }
            }
        } finally {
            logLock.unlock();
        }

        return this;
    }

    @Override
    public PremisLogger createLog(InputStream contentStream) {
        BinaryObject eventsObj = updateOrCreateLog(contentStream);

        // Link from the repository object to its event log
        repoObjFactory.createRelationship(repoObject, Cdr.hasEvents, eventsObj.getResource());

        return this;
    }

    private BinaryObject updateOrCreateLog(InputStream contentStream) {
        PID logPid = getMdEventsPid(repoObject.getPid());
        BinaryTransferOutcome outcome = transferSession.transferReplaceExisting(logPid, contentStream);

        return repoObjFactory.createOrUpdateBinary(logPid, outcome.getDestinationUri(),
                MD_EVENTS.getDefaultFilename(), MD_EVENTS.getMimetype(), outcome.getSha1(), null, null);
    }

    @Override
    public void close() {
        transferSession.close();
        closed = true;
    }

    @Override
    public boolean isClosed() {
        return closed;
    }
}
