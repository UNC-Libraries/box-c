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
import java.util.Date;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.util.FileManager;

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
    private File premisFile;
    private PID objectPid;
    private Model model;

    private RepositoryPIDMinter pidMinter;

    public FilePremisLogger(PID pid, File file, RepositoryPIDMinter pidMinter) {
        this.objectPid = pid;
        this.premisFile = file;
        this.pidMinter = pidMinter;
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
        return buildEvent(eventType, null);
    }

    /**
     * Adds an event to the log file
     *
     * @param eventResc
     * @return
     */
    @Override
    public PremisLogger writeEvents(Resource... eventResources) {
        Model logModel = getModel();
        String pidString = objectPid.getRepositoryPath();
        Resource objResc = logModel.getResource(pidString);
        // Add the events to the model for this event log
        for (Resource eventResc: eventResources) {
            objResc.addProperty(Premis.hasEvent, eventResc);
            logModel.add(eventResc.getModel());
        }

        if (premisFile != null) {
            // Persist the log to file
            try (FileOutputStream rdfFile = new FileOutputStream(premisFile)) {
                RDFDataMgr.write(rdfFile, logModel, RDFFormat.NTRIPLES);
            } catch (IOException e) {
                throw new ObjectPersistenceException("Failed to stream PREMIS log to file for " + objectPid, e);
            }
        }

        return this;
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
            InputStream in = FileManager.get().open(premisFile.getAbsolutePath());
            model.read(in, null, Lang.NTRIPLES.getName());
        }

        return model;
    }

    @Override
    public Model getEventsModel() {
        return getModel();
    }
}
