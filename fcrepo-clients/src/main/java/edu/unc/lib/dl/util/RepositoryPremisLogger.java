/**
 * Copyright 2016 The University of North Carolina at Chapel Hill
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
package edu.unc.lib.dl.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;

import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.PremisEventObject;
import edu.unc.lib.dl.fcrepo4.Repository;
import edu.unc.lib.dl.fcrepo4.RepositoryObject;
import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Premis;

/**
 * Logs PREMIS events for a repository object, which are persisted as PREMIS
 * event objects in the repository.
 * 
 * @author bbpennel
 *
 */
public class RepositoryPremisLogger implements PremisLogger {

	private Repository repository;
	private RepositoryObject repoObject;
	
	private List<PremisEventObject> events;
	
	public RepositoryPremisLogger(RepositoryObject repoObject, Repository repository) {
		this.repoObject = repoObject;
		this.repository = repository;
	}
	
	@Override
	public PremisEventBuilder buildEvent(Resource eventType, Date date) {
		if (date == null) {
			date = new Date();
		}

		return new PremisEventBuilder(repository.mintPremisEventUrl(repoObject.getPid()),
				eventType, date, this);
	}

	@Override
	public PremisEventBuilder buildEvent(Resource eventType) {
		return new PremisEventBuilder(repository.mintPremisEventUrl(repoObject.getPid()),
				eventType, new Date(), this);
	}

	@Override
	public PremisLogger writeEvent(Resource eventResc) throws IOException {
		Model eventModel = eventResc.getModel();
		PID eventPid = PIDs.get(eventResc.getURI());

		try {
			repository.createPremisEvent(eventPid, eventModel);
		} catch (FedoraException e) {
			throw new IOException("Failed to create event at " + eventPid, e);
		}

		return this;
	}

	@Override
	public List<PID> listEvents() throws IOException {
		Model model;
		try {
			model = repoObject.getModel();
		} catch (FedoraException e) {
			throw new IOException(e);
		}

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

	private void retrieveAllEvents() throws FedoraException, IOException {
		List<PID> eventPids = listEvents();

		for (PID pid : eventPids) {
			events.add(repository.getPremisEvent(pid));
		}
	}

	@Override
	public List<PremisEventObject> getEvents() throws IOException {
		if (events == null) {
			events = new ArrayList<>();
			try {
				retrieveAllEvents();
			} catch (FedoraException e) {
				throw new IOException(e);
			}
		}

		return events;
	}

}
