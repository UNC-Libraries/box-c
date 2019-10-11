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

import java.io.InputStream;
import java.util.Date;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

/**
 * Logs and provides access to PREMIS events for a repository object
 *
 * @author bbpennel
 *
 */
public interface PremisLogger {

    /**
     * Allows for an arbitrary timestamp to be set for a premis event
     * @param eventType
     * @return PremisEventBuilder
     */
    public PremisEventBuilder buildEvent(Resource eventType, Date date);

    /**
     * Returns an instance of buildEvent with the timestamp automatically set to the current time
     * @param eventType
     * @return PremisEventBuilder
     */
    public PremisEventBuilder buildEvent(Resource eventType);

    /**
     * Adds events to this log
     *
     * @param eventResc
     * @return
     */
    public PremisLogger writeEvents(Resource... eventResc);

    /**
     * Creates the log for this logger from the given inputstream if
     * it does not exist.
     *
     * @param contentStream contents of the log
     * @return this logger
     */
    public PremisLogger createLog(InputStream contentStream);

    /**
     * @return a model containing the events in this logger.
     */
    public Model getEventsModel();
}
