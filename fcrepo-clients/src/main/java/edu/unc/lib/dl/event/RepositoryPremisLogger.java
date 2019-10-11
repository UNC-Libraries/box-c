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

import static edu.unc.lib.dl.fcrepo4.RepositoryPathConstants.MD_EVENTS;
import static edu.unc.lib.dl.fcrepo4.RepositoryPaths.getMetadataContainerUri;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.net.URI;
import java.util.Date;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.riot.RDFFormat;

import edu.unc.lib.dl.fcrepo4.BinaryObject;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.RepositoryObject;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectFactory;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;
import edu.unc.lib.dl.fcrepo4.RepositoryPIDMinter;
import edu.unc.lib.dl.fedora.NotFoundException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.rdf.Premis;
import edu.unc.lib.dl.util.ObjectPersistenceException;
import edu.unc.lib.dl.util.RDFModelUtil;
import edu.unc.lib.dl.util.URIUtil;

/**
 * Logs PREMIS events for a repository object, which are persisted as PREMIS
 * event objects in the repository.
 *
 * @author bbpennel
 *
 */
public class RepositoryPremisLogger implements PremisLogger {

    private RepositoryPIDMinter pidMinter;
    private RepositoryObjectLoader repoObjLoader;
    private RepositoryObjectFactory repoObjFactory;

    private RepositoryObject repoObject;

    public RepositoryPremisLogger(RepositoryObject repoObject, RepositoryPIDMinter pidMinter,
            RepositoryObjectLoader repoObjLoader, RepositoryObjectFactory repoObjFactory) {
        this.repoObject = repoObject;
        this.pidMinter = pidMinter;
        this.repoObjLoader = repoObjLoader;
        this.repoObjFactory = repoObjFactory;
    }

    @Override
    public PremisEventBuilder buildEvent(Resource eventType, Date date) {
        if (date == null) {
            date = new Date();
        }

        return new PremisEventBuilder(pidMinter.mintPremisEventPid(repoObject.getPid()),
                eventType, date, this);
    }

    @Override
    public PremisEventBuilder buildEvent(Resource eventType) {
        return buildEvent(eventType, null);
    }

    @Override
    public PremisLogger writeEvents(Resource... eventResources) {
        Model logModel = ModelFactory.createDefaultModel();
        for (Resource eventResc: eventResources) {
            // Add link from the object to this event
            logModel.add(repoObject.getResource(), Premis.hasEvent, eventResc);
            logModel.add(eventResc.getModel());
        }

        // Stream the event RDF as NTriples
        InputStream modelStream;
        try {
            modelStream = RDFModelUtil.streamModel(logModel, RDFFormat.NTRIPLES);
        } catch (IOException e) {
            throw new ObjectPersistenceException("Failed to serialize event to RDF for " + repoObject.getPid(), e);
        }

        Statement s = repoObject.getResource().getProperty(Cdr.hasEvents);
        // Premis event log not created yet
        if (s == null) {
            createLog(modelStream);
        } else {
            URI mdURI = getMetadataContainerUri(repoObject.getPid());
            // Event log exists, append new events to it
            PID logPid = PIDs.get(URIUtil.join(mdURI, MD_EVENTS));
            BinaryObject logObj = repoObjLoader.getBinaryObject(logPid);

            InputStream newContentStream = new SequenceInputStream(
                    new ByteArrayInputStream("\n".getBytes()),
                    modelStream);

            InputStream mergedStream = new SequenceInputStream(
                    logObj.getBinaryStream(),
                    newContentStream);

            repoObjFactory.updateBinary(mdURI, MD_EVENTS, mergedStream, "events.nt",
                    "application/n-triples", null, null, null);
        }

        return this;
    }

    @Override
    public PremisLogger createLog(InputStream contentStream) {
        URI mdURI = getMetadataContainerUri(repoObject.getPid());

        BinaryObject eventsObj = repoObjFactory.createBinary(mdURI, MD_EVENTS, contentStream,
                "events.nt", "application/n-triples", null, null, null);

        // Link from the repository object to its event log
        repoObjFactory.createRelationship(repoObject, Cdr.hasEvents, eventsObj.getResource());

        return this;
    }

    @Override
    public Model getEventsModel() {
        PID eventsPid = PIDs.get(URIUtil.join(getMetadataContainerUri(repoObject.getPid()), MD_EVENTS));
        try {
            BinaryObject eventsObj = repoObjLoader.getBinaryObject(eventsPid);
            return RDFModelUtil.createModel(eventsObj.getBinaryStream(), "N-TRIPLE");
        } catch (NotFoundException e) {
            return ModelFactory.createDefaultModel();
        }
    }
}
