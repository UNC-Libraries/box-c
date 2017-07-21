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

import java.util.Date;
import java.util.List;

import org.apache.jena.rdf.model.Resource;

import edu.unc.lib.dl.fcrepo4.PremisEventObject;
import edu.unc.lib.dl.fedora.PID;

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
     * Adds an event to this log
     *
     * @param eventResc
     * @return
     */
    public PremisLogger writeEvent(Resource eventResc);

    /**
     * Return a list of PID objects for each event in this logger
     *
     * @return
     */
    public List<PID> listEvents();

    /**
     * Return a list of PremisEventObjects for each event in this logger
     *
     * @return
     */
    public List<PremisEventObject> getEvents();
}
