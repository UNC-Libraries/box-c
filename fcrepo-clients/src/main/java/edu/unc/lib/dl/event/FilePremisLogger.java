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
package edu.unc.lib.dl.event;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.util.FileManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.PremisEventObject;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectDriver;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectFactory;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;
import edu.unc.lib.dl.fcrepo4.RepositoryPIDMinter;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Premis;
import edu.unc.lib.dl.util.ObjectPersistenceException;

/**
 * Logs PREMIS events for a repository object to a backing file
 *
 * @author lfarrell
 *
 */
public class FilePremisLogger implements PremisLogger {

    private static final Logger log = LoggerFactory.getLogger(FilePremisLogger.class);

    private File premisFile;
    private PID objectPid;
    private Model model;

    private RepositoryPIDMinter pidMinter;
    private RepositoryObjectLoader repoObjLoader;
    private RepositoryObjectDriver repoObjDriver;
    private RepositoryObjectFactory repoObjFactory;

    public FilePremisLogger(PID pid, File file, RepositoryPIDMinter pidMinter, RepositoryObjectLoader repoObjLoader,
            RepositoryObjectFactory repoObjFactory, RepositoryObjectDriver repoObjDriver) {
        this.objectPid = pid;
        this.premisFile = file;
        this.pidMinter = pidMinter;
        this.repoObjLoader = repoObjLoader;
        this.repoObjFactory = repoObjFactory;
        this.repoObjDriver = repoObjDriver;
    }

    /**
     * Allows for an arbitrary timestamp to be set for a premis event
     *
     * @param eventType
     * @return PremisEventBuilder
     */
    @Override
    public PremisEventBuilder buildEvent(Resource eventType, Date date) {
        if (date == null) {
            date = new Date();
        }

        return new PremisEventBuilder(pidMinter.mintPremisEventPid(objectPid),
                eventType, date, this);
    }

    /**
     * Returns an instance of buildEvent with the timestamp automatically set to the current time
     *
     * @param eventType
     * @return PremisEventBuilder
     */
    @Override
    public PremisEventBuilder buildEvent(Resource eventType) {
        return new PremisEventBuilder(pidMinter.mintPremisEventPid(objectPid),
                eventType, new Date(), this);
    }

    /**
     * Adds an event to the log file
     *
     * @param eventResc
     * @return
     */
    @Override
    public PremisLogger writeEvent(Resource eventResc) {
        // Add the event to the model for this event log
        Model model = getModel().add(eventResc.getModel());

        if (premisFile != null) {
            // Persist the log to file
            try (FileOutputStream rdfFile = new FileOutputStream(premisFile)) {
                RDFDataMgr.write(rdfFile, model, RDFFormat.TURTLE_PRETTY);
            } catch (IOException e) {
                throw new ObjectPersistenceException("Failed to stream PREMIS log to file for " + objectPid, e);
            }
        }

        return this;
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

        Model model = ModelFactory.createDefaultModel();

        if (premisFile != null && premisFile.exists()) {
            InputStream in = FileManager.get().open(premisFile.getAbsolutePath());
            model.read(in, null, Lang.TURTLE.getName());
        }

        return model;
    }

    @Override
    public List<PID> listEvents() {
        List<PID> eventPids = new ArrayList<>();

        // Find all of the individual events and turn their identifiers into pids
        for (ResIterator eventIt = model.listResourcesWithProperty(Premis.hasEventType); eventIt.hasNext(); ) {
            Resource eventResc = eventIt.nextResource();
            PID eventPid = PIDs.get(eventResc.getURI());

            eventPids.add(eventPid);
        }

        return eventPids;
    }

    @Override
    public List<PremisEventObject> getEvents() {
        List<PremisEventObject> events = new ArrayList<>();
        ResIterator eventIt = getModel().listResourcesWithProperty(Premis.hasEventType);
        // Find all of the events and construct a list of PremisEventObjects from them.
        try {
            gatherAllObjectsForEvents(eventIt, events);
        } finally {
            eventIt.close();
        }

        log.debug("Retrieved {} events from file log for object {}",
                events.size(), objectPid.getQualifiedId());
        return events;
    }

    private void gatherAllObjectsForEvents(ResIterator eventIt, List<PremisEventObject> events) {
            while (eventIt.hasNext()) {
                Resource eventResc = eventIt.nextResource();
                PID eventPid = PIDs.get(eventResc.getURI());
                // Construct a model for just this event
                Model eventModel = ModelFactory.createDefaultModel();
                StmtIterator stmtIt = eventResc.listProperties();
                // Add all statements with this resc as subject to the model
                eventModel.add(stmtIt);
                stmtIt.close();
                // Get a fresh iterator to check all objects of all the triples for properties
                stmtIt = eventResc.listProperties();
                while (stmtIt.hasNext()) {
                    RDFNode objNode = stmtIt.next().getObject();
                    if (objNode.isResource()) {
                        StmtIterator objIt = objNode.asResource().listProperties();
                        // Add statements to the event's model for any objects that have properties
                        if (objIt != null) {
                            eventModel.add(objIt);
                            objIt.close();
                        }
                    }
                }
                stmtIt.close();
                // Construct the event object with a presupplied model
                PremisEventObject event = new PremisEventObject(eventPid,
                        repoObjDriver, repoObjFactory);
                event.storeModel(eventModel);

                events.add(event);
        }
    }
}
