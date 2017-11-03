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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.List;

import org.apache.http.HttpStatus;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.fcrepo.client.FcrepoResponse;
import org.junit.Before;
import org.junit.Test;

import edu.unc.lib.dl.fcrepo4.AbstractFedoraIT;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.PremisEventObject;
import edu.unc.lib.dl.fcrepo4.RepositoryObject;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.rdf.Premis;
import edu.unc.lib.dl.util.SoftwareAgentConstants.SoftwareAgent;

/**
 *
 * @author bbpennel
 *
 */
public class RepositoryPremisLoggerIT extends AbstractFedoraIT {

    private RepositoryPremisLogger logger;

    private PID parentPid;
    private RepositoryObject parentObject;

    @Before
    public void init() throws Exception {
        parentPid = pidMinter.mintDepositRecordPid();

        Model model = ModelFactory.createDefaultModel();
        Resource resc = model.createResource("");
        resc.addProperty(RDF.type, Cdr.DepositRecord);

        parentObject = repoObjFactory.createDepositRecord(model);

        logger = new RepositoryPremisLogger(parentObject, pidMinter,
                repoObjLoader, repoObjFactory);
    }

    @Test
    public void addEventTest() throws Exception {
        Resource eventResc = logger.buildEvent(Premis.VirusCheck)
                .addSoftwareAgent(SoftwareAgent.clamav.toString())
                .write();
        PID eventPid = PIDs.get(eventResc.getURI());

        // Retrieve all of the events
        List<PID> eventPids = logger.listEvents();

        assertTrue(eventPids.contains(eventPid));

        List<PremisEventObject> events = logger.getEvents();
        assertEquals(1, events.size());

        assertTrue(events.get(0).getResource().hasProperty(Premis.hasEventType, Premis.VirusCheck));

        try (FcrepoResponse resp = client.head(eventPid.getRepositoryUri()).perform()) {
            assertEquals("Event object not found", HttpStatus.SC_OK, resp.getStatusCode());
        }
    }

    @Test
    public void addEventsTest() throws Exception {
        Resource event1Resc = logger.buildEvent(Premis.VirusCheck)
                .addSoftwareAgent(SoftwareAgent.clamav.toString())
                .write();
        PID event1Pid = PIDs.get(event1Resc.getURI());

        Resource event2Resc = logger.buildEvent(Premis.Ingestion)
                .addEventDetail("Ingested")
                .write();
        PID event2Pid = PIDs.get(event2Resc.getURI());

        Resource event3Resc = logger.buildEvent(Premis.MessageDigestCalculation)
                .write();
        PID event3Pid = PIDs.get(event3Resc.getURI());

        // Make a new logger to make sure everything is clean
        PremisLogger retrieveLogger = new RepositoryPremisLogger(parentObject, pidMinter,
                repoObjLoader, repoObjFactory);

        // Retrieve all of the events
        List<PID> eventPids = retrieveLogger.listEvents();

        assertTrue(eventPids.contains(event1Pid));
        assertTrue(eventPids.contains(event2Pid));
        assertTrue(eventPids.contains(event3Pid));

        // Get all the events from fedora in chronological order
        List<PremisEventObject> events = retrieveLogger.getEvents();
        Collections.sort(events);

        assertEquals(3, events.size());

        // Verify that they all have the correct types set
        assertTrue(events.get(0).getResource().hasProperty(Premis.hasEventType, Premis.VirusCheck));
        assertTrue(events.get(1).getResource().hasProperty(Premis.hasEventType, Premis.Ingestion));
        assertTrue(events.get(2).getResource().hasProperty(Premis.hasEventType, Premis.MessageDigestCalculation));
    }
}
