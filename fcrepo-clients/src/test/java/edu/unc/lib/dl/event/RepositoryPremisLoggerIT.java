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

import java.time.Instant;
import java.util.Date;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.junit.Before;
import org.junit.Test;

import edu.unc.lib.dl.fcrepo4.AbstractFedoraIT;
import edu.unc.lib.dl.fcrepo4.RepositoryObject;
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

    private RepositoryObject parentObject;

    @Before
    public void init() throws Exception {
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

        // Retrieve all of the events
        Model logModel = logger.getEventsModel();
        Resource logEventResc = logModel.getResource(eventResc.getURI());

        assertTrue("Must contain premis:hasEvent references from obj to event",
                logModel.contains(parentObject.getResource(), Premis.hasEvent, logEventResc));
        assertTrue(logEventResc.hasProperty(Premis.hasEventType, Premis.VirusCheck));
    }

    @Test
    public void addEventsTest() throws Exception {
        Resource event1Resc = logger.buildEvent(Premis.VirusCheck)
                .addSoftwareAgent(SoftwareAgent.clamav.toString())
                .write();

        Date ingestDate = Date.from(Instant.parse("2010-01-02T12:00:00Z"));
        Resource event2Resc = logger.buildEvent(Premis.Ingestion, ingestDate)
                .addEventDetail("Ingested")
                .write();

        Resource event3Resc = logger.buildEvent(Premis.MessageDigestCalculation)
                .write();

        // Make a new logger to make sure everything is clean
        PremisLogger retrieveLogger = new RepositoryPremisLogger(parentObject, pidMinter,
                repoObjLoader, repoObjFactory);

        Model logModel = retrieveLogger.getEventsModel();
        Resource logEvent1Resc = logModel.getResource(event1Resc.getURI());
        Resource logEvent2Resc = logModel.getResource(event2Resc.getURI());
        Resource logEvent3Resc = logModel.getResource(event3Resc.getURI());

        assertTrue(logEvent1Resc.hasProperty(Premis.hasEventType, Premis.VirusCheck));
        assertTrue(logEvent2Resc.hasProperty(Premis.hasEventType, Premis.Ingestion));
        assertEquals("2010-01-02T12:00:00.000Z", logEvent2Resc.getProperty(Premis.hasEventDateTime).getString());
        assertTrue(logEvent3Resc.hasProperty(Premis.hasEventType, Premis.MessageDigestCalculation));

        // Verify that hasEvent relations are present
        assertTrue(logModel.contains(parentObject.getResource(), Premis.hasEvent, logEvent1Resc));
        assertTrue(logModel.contains(parentObject.getResource(), Premis.hasEvent, logEvent2Resc));
        assertTrue(logModel.contains(parentObject.getResource(), Premis.hasEvent, logEvent3Resc));
    }
}
