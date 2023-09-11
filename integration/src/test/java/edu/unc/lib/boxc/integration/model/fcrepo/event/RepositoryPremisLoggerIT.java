package edu.unc.lib.boxc.integration.model.fcrepo.event;

import static java.nio.file.Files.createTempFile;
import static org.apache.commons.codec.binary.Hex.encodeHexString;
import static org.apache.commons.io.FileUtils.copyInputStreamToFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;

import edu.unc.lib.boxc.integration.fcrepo.AbstractFedoraIT;
import edu.unc.lib.boxc.model.api.SoftwareAgentConstants.SoftwareAgent;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.BinaryObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObject;
import edu.unc.lib.boxc.model.api.rdf.Premis;
import edu.unc.lib.boxc.model.api.rdf.Prov;
import edu.unc.lib.boxc.model.fcrepo.ids.AgentPids;
import edu.unc.lib.boxc.model.fcrepo.ids.DatastreamPids;
import edu.unc.lib.boxc.model.fcrepo.ids.PidLockManager;
import edu.unc.lib.boxc.operations.api.events.PremisLogger;
import edu.unc.lib.boxc.operations.impl.events.PremisLoggerFactoryImpl;
import edu.unc.lib.boxc.operations.impl.events.RepositoryPremisLogger;
import edu.unc.lib.boxc.persist.api.DigestAlgorithm;
import edu.unc.lib.boxc.persist.api.transfer.BinaryTransferOutcome;
import edu.unc.lib.boxc.persist.api.transfer.BinaryTransferService;
import edu.unc.lib.boxc.persist.api.transfer.BinaryTransferSession;

/**
 *
 * @author bbpennel
 *
 */
public class RepositoryPremisLoggerIT extends AbstractFedoraIT {

    private RepositoryPremisLogger logger;

    private RepositoryObject parentObject;

    private PidLockManager lockManager;

    private Map<PID, String> previousDigestMap;

    private AutoCloseable closeable;

    @Mock
    private BinaryTransferService transferService;
    @Mock
    private BinaryTransferSession mockSession;
    @Autowired
    private PremisLoggerFactoryImpl premisLoggerFactory;

    @BeforeEach
    public void init() throws Exception {
        closeable = openMocks(this);

        lockManager = PidLockManager.getDefaultPidLockManager();

        when(transferService.getSession(any(RepositoryObject.class))).thenReturn(mockSession);
        premisLoggerFactory.setBinaryTransferService(transferService);

        // No implementations of session available here, so mock from interface
        final Path path = createTempFile("content", null);
        when(mockSession.transferReplaceExisting(any(PID.class), any(InputStream.class)))
                .thenAnswer(new Answer<BinaryTransferOutcome>()  {
                    @Override
                    public BinaryTransferOutcome answer(InvocationOnMock invocation) throws Throwable {
                        InputStream contentStream = invocation.getArgument(1);
                        DigestInputStream digestStream = new DigestInputStream(
                                contentStream, MessageDigest.getInstance(DigestAlgorithm.DEFAULT_ALGORITHM.getName()));
                        Path tempFilePath = createTempFile("temp_content", null);
                        copyInputStreamToFile(digestStream, tempFilePath.toFile());
                        Files.move(tempFilePath, path, StandardCopyOption.REPLACE_EXISTING);
                        BinaryTransferOutcome outcome = mock(BinaryTransferOutcome.class);
                        when(outcome.getDestinationUri()).thenReturn(path.toUri());
                        when(outcome.getSha1()).thenReturn(encodeHexString(digestStream.getMessageDigest().digest()));
                        return outcome;
                    }
                });

        previousDigestMap = new HashMap<>();
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
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
                .addSoftwareAgent(AgentPids.forSoftware(SoftwareAgent.clamav))
                .write();

        // Retrieve all of the events
        Model logModel = logger.getEventsModel();
        Resource logEventResc = logModel.getResource(eventResc.getURI());

        assertTrue(logModel.contains(logEventResc, Prov.used, parentObject.getResource()),
                "Must contain prov:used references from obj to event");
        assertTrue(logEventResc.hasProperty(RDF.type, Premis.VirusCheck));

        Resource objResc = logModel.getResource(parentObject.getPid().getRepositoryPath());
        assertTrue(objResc.hasProperty(RDF.type, Premis.Representation));

        assertEventLogDigestChanged(parentObject);
    }

    @Test
    public void addEventsTest() throws Exception {
        parentObject = repoObjFactory.createDepositRecord(null);
        initPremisLogger(parentObject);

        Resource event1Resc = logger.buildEvent(Premis.VirusCheck)
                .addSoftwareAgent(AgentPids.forSoftware(SoftwareAgent.clamav))
                .write();

        assertEventLogDigestChanged(parentObject);

        // Add two of the events together
        Date ingestDate = Date.from(Instant.parse("2010-01-02T12:00:00Z"));
        Resource event2Resc = logger.buildEvent(null, Premis.Ingestion, ingestDate)
                .addEventDetail("Ingested")
                .create();

        Resource event3Resc = logger.buildEvent(Premis.MessageDigestCalculation)
                .create();

        logger.writeEvents(event2Resc, event3Resc);

        assertEventLogDigestChanged(parentObject);

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
        PremisLogger retrieveLogger = new RepositoryPremisLogger(parentObject, mockSession,
                pidMinter, repoObjLoader, repoObjFactory);
        Model initialLogModel = retrieveLogger.getEventsModel();
        assertFalse(initialLogModel.listObjects().hasNext(), "New premis already contains events");

        // add original event
        Resource originalEventResc = logger.buildEvent(Premis.note)
                .addEventDetail("first premis event")
                .write();

        // add new events
        List<Thread> threads = new ArrayList<>();
        List<Resource> events = new ArrayList<>();
        List<String> eventUris = new ArrayList<>();

        Date ingestDate = Date.from(Instant.parse("2010-01-02T12:00:00Z"));
        for (int i = 1; i <= 200; i++) {
            Resource anotherEvent = logger.buildEvent(pidMinter.mintPremisEventPid(parentObject.getPid()), Premis.note,
                    ingestDate)
                    .addEventDetail("another premis event " + i)
                    .create();
            eventUris.add(anotherEvent.getURI());
            events.add(anotherEvent);
        }

        for (Resource event : events) {
            Runnable commitThread = new Runnable() {
                @Override
                public void run() {
                    logger.writeEvents(event);
                }
            };
            Thread thread = new Thread(commitThread);
            threads.add(thread);
        }

        for (Thread thread : threads) {
            thread.start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        // check that the first event was added correctly
        Model logModel = retrieveLogger.getEventsModel();
        Resource logOriginalEventResc = logModel.getResource(originalEventResc.getURI());
        assertEquals("first premis event", logOriginalEventResc.getProperty(Premis.note).getString());

        // check rest of events
        int i = 1;
        for (String uri : eventUris) {
            Resource logEventResc = logModel.getResource(uri);
            assertEquals("another premis event " + i, logEventResc.getProperty(Premis.note).getString());
            i++;
        }

        assertEventLogDigestChanged(parentObject);

        retrieveLogger.close();
    }

    @Test
    public void allowSimultaneousReadLocks() throws InterruptedException {
        parentObject = repoObjFactory.createDepositRecord(null);
        initPremisLogger(parentObject);

        // make sure that there are no events in premis log before the writes
        PremisLogger retrieveLogger = new RepositoryPremisLogger(parentObject, mockSession,
                pidMinter, repoObjLoader, repoObjFactory);
        Model initialLogModel = retrieveLogger.getEventsModel();
        assertFalse(initialLogModel.listObjects().hasNext(), "New premis already contains events");

        // add an event
        Resource event = logger.buildEvent(Premis.note)
                .addEventDetail("first premis event")
                .write();

        // create a read lock
        PID logPid = DatastreamPids.getMdEventsPid(parentObject.getPid());
        Lock logLock = lockManager.awaitReadLock(logPid);

        // start a thread to read the log
        List<String> premisNotes = new ArrayList<>();
        Runnable readThreadRunnable = new Runnable() {
            @Override
            public void run() {
                Model logModel = retrieveLogger.getEventsModel();
                Resource logEventResc = logModel.getResource(event.getURI());
                premisNotes.add(logEventResc.getProperty(Premis.note).getString());
            }
        };
        Thread readThread = new Thread(readThreadRunnable);
        readThread.start();
        readThread.join();
        assertEquals("first premis event", premisNotes.get(0));

        // release read lock
        logLock.unlock();

        assertEventLogDigestChanged(parentObject);

        retrieveLogger.close();
    }

    @Test
    public void readLockWaitsForWriteLock() throws InterruptedException {
        parentObject = repoObjFactory.createDepositRecord(null);
        initPremisLogger(parentObject);

        // make sure that there are no events in premis log before the writes
        PremisLogger retrieveLogger = new RepositoryPremisLogger(parentObject, mockSession,
                pidMinter, repoObjLoader, repoObjFactory);
        Model initialLogModel = retrieveLogger.getEventsModel();
        assertFalse(initialLogModel.listObjects().hasNext(), "New premis already contains events");

        // start write thread
        Resource anotherEvent = logger.buildEvent(Premis.note)
                .addEventDetail("first premis event")
                .create();
        PID logPid = DatastreamPids.getMdEventsPid(parentObject.getPid());

        AtomicBoolean startedWrite = new AtomicBoolean();
        Object writeMutex = new Object();
        Runnable writeThreadRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    synchronized(writeMutex) {
                        Lock writeLock = lockManager.awaitWriteLock(logPid);

                        startedWrite.set(true);
                        writeMutex.notify();

                        logger.writeEvents(anotherEvent);

                        Thread.sleep(50);

                        writeLock.unlock();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        Thread writeThread = new Thread(writeThreadRunnable);

        // start read thread
        List<String> premisNotes = new ArrayList<>();
        Runnable readThreadRunnable = new Runnable() {
            @Override
            public void run() {
                Model logModel = retrieveLogger.getEventsModel();
                Resource logEventResc = logModel.getResource(anotherEvent.getURI());
                premisNotes.add(logEventResc.getProperty(Premis.note).getString());
            }
        };
        Thread readThread = new Thread(readThreadRunnable);
        writeThread.start();

        // Block so that the read thread doesn't start before the write starts
        synchronized(writeMutex) {
            while (!startedWrite.get()) {
                writeMutex.wait(1000);
            }
        }

        readThread.start();

        assertEquals(0, premisNotes.size());

        // release write lock
        writeThread.join();

        // attempt to read log and succeed
        readThread.join();
        assertEquals("first premis event", premisNotes.get(0));

        assertEventLogDigestChanged(parentObject);

        retrieveLogger.close();
    }

    @Test
    public void writeLockWaitsForReadLock() throws InterruptedException {
        parentObject = repoObjFactory.createDepositRecord(null);
        initPremisLogger(parentObject);

        // make sure that there are no events in premis log before the writes
        PremisLogger retrieveLogger = new RepositoryPremisLogger(parentObject, mockSession,
                pidMinter, repoObjLoader, repoObjFactory);
        Model initialLogModel = retrieveLogger.getEventsModel();
        assertFalse(initialLogModel.listObjects().hasNext(), "New premis already contains events");

        // create a read lock
        PID logPid = DatastreamPids.getMdEventsPid(parentObject.getPid());
        Lock logLock = lockManager.awaitReadLock(logPid);

        // attempt to write log and fail
        Resource event = logger.buildEvent(Premis.VirusCheck)
                .addSoftwareAgent(AgentPids.forSoftware(SoftwareAgent.clamav))
                .create();
        Runnable commitThread = new Runnable() {
            @Override
            public void run() {
                logger.writeEvents(event);
            }
        };
        Thread thread = new Thread(commitThread);
        thread.start();

        Thread.sleep(25);

        assertTrue(thread.isAlive());

        // release read lock
        logLock.unlock();

        // attempt to write log and succeed
        thread.join();
        Model logModel = retrieveLogger.getEventsModel();
        Resource logEvent1Resc = logModel.getResource(event.getURI());
        assertTrue(logEvent1Resc.hasProperty(RDF.type, Premis.VirusCheck));

        assertEventLogDigestChanged(parentObject);

        retrieveLogger.close();
    }

    private void assertEventLogDigestChanged(RepositoryObject contentObj) {
        PID eventsPid = DatastreamPids.getMdEventsPid(contentObj.getPid());
        BinaryObject eventsBin = repoObjLoader.getBinaryObject(eventsPid);
        String newDigest = eventsBin.getSha1Checksum();
        assertNotNull("No sha1 set for events log", newDigest);
        String previousDigest = previousDigestMap.get(eventsPid);
        if (previousDigest != null) {
            assertNotEquals(previousDigest, newDigest, "Digest did not change from previous version");
        }
        previousDigestMap.put(eventsPid, newDigest);
    }
}
