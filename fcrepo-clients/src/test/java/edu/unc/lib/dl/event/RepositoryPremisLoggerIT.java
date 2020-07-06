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

import static java.nio.file.Files.createTempFile;
import static org.apache.commons.io.FileUtils.copyInputStreamToFile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;

import edu.unc.lib.dl.fcrepo4.AbstractFedoraIT;
import edu.unc.lib.dl.fcrepo4.RepositoryObject;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.persist.api.transfer.BinaryTransferService;
import edu.unc.lib.dl.persist.api.transfer.BinaryTransferSession;
import edu.unc.lib.dl.rdf.Premis;
import edu.unc.lib.dl.rdf.Prov;
import edu.unc.lib.dl.util.SoftwareAgentConstants.SoftwareAgent;

/**
 *
 * @author bbpennel
 *
 */
public class RepositoryPremisLoggerIT extends AbstractFedoraIT {

    private RepositoryPremisLogger logger;

    private RepositoryObject parentObject;

    @Mock
    private BinaryTransferService transferService;
    @Mock
    private BinaryTransferSession mockSession;
    @Autowired
    private PremisLoggerFactory premisLoggerFactory;

    @Before
    public void init() throws Exception {
        initMocks(this);

        when(transferService.getSession(any(RepositoryObject.class))).thenReturn(mockSession);
        premisLoggerFactory.setBinaryTransferService(transferService);

        // No implementations of session available here, so mock from interface
        final Path path = createTempFile("content", null);
        when(mockSession.transferReplaceExisting(any(PID.class), any(InputStream.class)))
                .thenAnswer(new Answer<URI>()  {
                    @Override
                    public URI answer(InvocationOnMock invocation) throws Throwable {
                        InputStream contentStream = invocation.getArgumentAt(1, InputStream.class);
                        copyInputStreamToFile(contentStream, path.toFile());
                        return path.toUri();
                    }
                });
    }

    private void initPremisLogger(RepositoryObject repoObj) {
        logger = new RepositoryPremisLogger(parentObject, mockSession, pidMinter,
                repoObjLoader, repoObjFactory);
    }

    @Test
    public void addEventTest() throws Exception {
        parentObject = repoObjFactory.createDepositRecord(null);
        initPremisLogger(parentObject);

        Resource eventResc = logger.buildEvent(Premis.VirusCheck)
                .addSoftwareAgent(SoftwareAgent.clamav.toString())
                .write();

        // Retrieve all of the events
        Model logModel = logger.getEventsModel();
        Resource logEventResc = logModel.getResource(eventResc.getURI());

        assertTrue("Must contain prov:used references from obj to event",
                logModel.contains(logEventResc, Prov.used, parentObject.getResource()));
        assertTrue(logEventResc.hasProperty(RDF.type, Premis.VirusCheck));

        Resource objResc = logModel.getResource(parentObject.getPid().getRepositoryPath());
        assertTrue(objResc.hasProperty(RDF.type, Premis.Representation));
    }

    @Test
    public void addEventsTest() throws Exception {
        parentObject = repoObjFactory.createDepositRecord(null);
        initPremisLogger(parentObject);

        Resource event1Resc = logger.buildEvent(Premis.VirusCheck)
                .addSoftwareAgent(SoftwareAgent.clamav.toString())
                .write();

        // Add two of the events together
        Date ingestDate = Date.from(Instant.parse("2010-01-02T12:00:00Z"));
        Resource event2Resc = logger.buildEvent(null, Premis.Ingestion, ingestDate)
                .addEventDetail("Ingested")
                .create();

        Resource event3Resc = logger.buildEvent(Premis.MessageDigestCalculation)
                .create();

        logger.writeEvents(event2Resc, event3Resc);

        // Make a new logger to make sure everything is clean
        PremisLogger retrieveLogger = new RepositoryPremisLogger(parentObject, mockSession,
                pidMinter, repoObjLoader, repoObjFactory);

        Model logModel = retrieveLogger.getEventsModel();
        Resource logEvent1Resc = logModel.getResource(event1Resc.getURI());
        Resource logEvent2Resc = logModel.getResource(event2Resc.getURI());
        Resource logEvent3Resc = logModel.getResource(event3Resc.getURI());

        assertTrue(logEvent1Resc.hasProperty(RDF.type, Premis.VirusCheck));
        assertTrue(logEvent2Resc.hasProperty(RDF.type, Premis.Ingestion));
        assertEquals("2010-01-02T12:00:00.000Z", logEvent2Resc.getProperty(DCTerms.date).getString());
        assertTrue(logEvent3Resc.hasProperty(RDF.type, Premis.MessageDigestCalculation));

        // Verify that hasEvent relations are present
        Resource objResc = logModel.getResource(parentObject.getPid().getRepositoryPath());
        assertTrue(objResc.hasProperty(RDF.type, Premis.Representation));
        assertTrue(logModel.contains(logEvent1Resc, Prov.used, objResc));
        assertTrue(logModel.contains(logEvent2Resc, Prov.generated, objResc));
        assertTrue(logModel.contains(logEvent3Resc, Prov.used, objResc));

        retrieveLogger.close();
    }

    @Test
    public void getEventsModelForObjectWithoutLog() throws Exception {
        parentObject = repoObjFactory.createCollectionObject(null);
        initPremisLogger(parentObject);

        Model eventsModel = parentObject.getPremisLog().getEventsModel();

        assertTrue(eventsModel.isEmpty());
    }

    @Test
    public void makeMultipleChangesSimultaneously() throws InterruptedException {
        parentObject = repoObjFactory.createDepositRecord(null);
        initPremisLogger(parentObject);

        // make sure that there are no events in premis log before the writes
        PremisLogger retrieveInitialLogger = new RepositoryPremisLogger(parentObject, mockSession,
                pidMinter, repoObjLoader, repoObjFactory);
        Model logModel = retrieveInitialLogger.getEventsModel();
        assertFalse("New premis already contains events", logModel.listObjects().hasNext());

        // add new events
        Resource event1Resc = logger.buildEvent(Premis.VirusCheck)
                .addSoftwareAgent(SoftwareAgent.clamav.toString())
                .create();

        Date ingestDate = Date.from(Instant.parse("2010-01-02T12:00:00Z"));
        Resource event2Resc = logger.buildEvent(null, Premis.Ingestion, ingestDate)
                .addEventDetail("Ingested")
                .create();

        List<Thread> threads = new ArrayList<>();

        Runnable commitThread1 = new Runnable() {
            @Override
            public void run() {
                try (PremisLogger retrieveLogger = logger.writeEvents(event1Resc)) {
                    Model logModel = retrieveLogger.getEventsModel();
                    assertTrue(logModel.listObjects().hasNext());
                    Resource logEvent1Resc = logModel.getResource(event1Resc.getURI());
                    assertTrue(logEvent1Resc.hasProperty(RDF.type, Premis.VirusCheck));
                }
            }
        };
        Thread thread1 = new Thread(commitThread1);
        threads.add(thread1);

        Runnable commitThread2 = new Runnable() {
            @Override
            public void run() {
                try (PremisLogger retrieveLogger = logger.writeEvents(event2Resc)) {
                    Model logModel = retrieveLogger.getEventsModel();
                    assertTrue(logModel.listObjects().hasNext());
                    Resource logEvent2Resc = logModel.getResource(event2Resc.getURI());
                    assertTrue(logEvent2Resc.hasProperty(RDF.type, Premis.Ingestion));
                    assertEquals("2010-01-02T12:00:00.000Z", logEvent2Resc.getProperty(DCTerms.date).getString());
                }
            }
        };
        Thread thread2 = new Thread(commitThread2);
        threads.add(thread2);

        for (Thread thread : threads) {
            thread.start();
        }

        for (Thread thread : threads) {
            thread.join();
        }
    }
}
