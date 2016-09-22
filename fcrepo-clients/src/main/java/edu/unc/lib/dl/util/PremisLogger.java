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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.UUID;

import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.util.FileManager;

import edu.unc.lib.dl.fcrepo4.RepositoryPathConstants;
import edu.unc.lib.dl.fedora.PID;

/**
 * Logs PREMIS events for a repository object
 * 
 * @author lfarrell
 *
 */
public class PremisLogger {
	private static final Logger log = LoggerFactory
			.getLogger(PremisLogger.class);

	private File premisFile;
	private PID objectPid;
	private Model model;

	public PremisLogger(PID pid) {
		this.objectPid = pid;
	}

	public PremisLogger(PID pid, File file) {
		this.objectPid = pid;
		this.premisFile = file;
	}

	/**
	 * Allows for an arbitrary timestamp to be set for a premis event
	 * @param eventType
	 * @return PremisEventBuilder
	 */
	public PremisEventBuilder buildEvent(Resource eventType, Date date) {
		if (date == null) {
			date = new Date();
		}

		return new PremisEventBuilder(generateEventId(), eventType, date, this);
	}

	/**
	 * Returns an instance of buildEvent with the timestamp automatically set to the current time
	 * @param eventType
	 * @return PremisEventBuilder
	 */
	public PremisEventBuilder buildEvent(Resource eventType) {
		return new PremisEventBuilder(generateEventId(), eventType, new Date(), this);
	}

	/**
	 * Adds an event to the log file
	 * 
	 * @param eventResc
	 * @return
	 */
	public PremisLogger writeEvent(Resource eventResc) {
		// Add the event to the model for this event log
		Model model = getModel().add(eventResc.getModel());

		if (premisFile != null) {
			// Persist the log to file
			try (FileOutputStream rdfFile = new FileOutputStream(premisFile)) {
				RDFDataMgr.write(rdfFile, model, RDFFormat.TURTLE_PRETTY);
			} catch (IOException e) {
				log.debug("Failed to serialize properties for object {} for the following reason {}", 
					this.objectPid.getUUID(), e.getMessage());
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
			model.read(in, null, "TURTLE");
		}

		return model;
	}

	private String generateEventId() {
		String uuid = UUID.randomUUID().toString();
		String eventId = URIUtil.join(objectPid.getRepositoryPath(),
				RepositoryPathConstants.EVENTS_CONTAINER, uuid);
		return eventId;
	}
}
