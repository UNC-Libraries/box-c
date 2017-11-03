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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;

import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.PremisEventObject;
import edu.unc.lib.dl.fcrepo4.RepositoryObject;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectFactory;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;
import edu.unc.lib.dl.fcrepo4.RepositoryPIDMinter;
import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Premis;
import edu.unc.lib.dl.util.ObjectPersistenceException;

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

    private List<PremisEventObject> events;

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
        return new PremisEventBuilder(pidMinter.mintPremisEventPid(repoObject.getPid()),
                eventType, new Date(), this);
    }

    @Override
    public PremisLogger writeEvent(Resource eventResc) {
        Model eventModel = eventResc.getModel();
        PID eventPid = PIDs.get(eventResc.getURI());

        try {
            repoObjFactory.createPremisEvent(eventPid, eventModel);
        } catch (FedoraException e) {
            throw new ObjectPersistenceException("Failed to create event at " + eventPid, e);
        }

        return this;
    }

    @Override
    public List<PID> listEvents() {
        Model model = repoObject.getModel();

        List<PID> pids = new ArrayList<>();
        NodeIterator nodeIt = model.listObjectsOfProperty(Premis.hasEvent);
        while (nodeIt.hasNext()) {
            RDFNode node = nodeIt.nextNode();
            if (node.isResource()) {
                pids.add(PIDs.get(node.asResource().getURI()));
            }
        }

        return pids;
    }

    private void retrieveAllEvents() {
        List<PID> eventPids = listEvents();

        for (PID pid : eventPids) {
            events.add(repoObjLoader.getPremisEventObject(pid));
        }
    }

    @Override
    public List<PremisEventObject> getEvents() {
        if (events == null) {
            events = new ArrayList<>();
            retrieveAllEvents();
        }

        return events;
    }

}
