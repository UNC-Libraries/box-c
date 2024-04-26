package edu.unc.lib.boxc.services.camel.binaryCleanup;

import com.google.common.collect.ImmutableMap;
import edu.unc.lib.boxc.fcrepo.utils.FedoraTransaction;
import edu.unc.lib.boxc.fcrepo.utils.TransactionManager;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.ids.PIDMinter;
import edu.unc.lib.boxc.model.api.objects.AdminUnit;
import edu.unc.lib.boxc.model.api.objects.BinaryObject;
import edu.unc.lib.boxc.model.api.objects.CollectionObject;
import edu.unc.lib.boxc.model.api.objects.ContentRootObject;
import edu.unc.lib.boxc.model.api.objects.FolderObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.rdf.Premis;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.model.fcrepo.ids.DatastreamPids;
import edu.unc.lib.boxc.model.fcrepo.services.RepositoryInitializer;
import edu.unc.lib.boxc.model.fcrepo.test.TestHelper;
import edu.unc.lib.boxc.operations.api.events.PremisLogger;
import edu.unc.lib.boxc.operations.api.events.PremisLoggerFactory;
import edu.unc.lib.boxc.persist.api.storage.StorageLocation;
import edu.unc.lib.boxc.persist.api.storage.StorageLocationManager;
import edu.unc.lib.boxc.persist.api.transfer.BinaryTransferService;
import edu.unc.lib.boxc.persist.impl.storage.StorageLocationTestHelper;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.File;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPaths.getContentRootPid;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

/**
 * @author bbpennel
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextHierarchy({
        @ContextConfiguration("/spring-test/test-fedora-container.xml"),
        @ContextConfiguration("/spring-test/cdr-client-container.xml")
})
public class BinaryCleanupProcessorIT {

    @Autowired
    private String baseAddress;
    @Autowired
    private RepositoryInitializer repositoryInitializer;
    @Autowired
    private PIDMinter pidMinter;
    @Autowired
    private RepositoryObjectFactory repoObjectFactory;
    @javax.annotation.Resource(name = "repositoryObjectLoaderNoCache")
    private RepositoryObjectLoader repoObjectLoader;
    @Autowired
    private StorageLocationManager storageLocationManager;
    @Autowired
    private TransactionManager txManager;
    @Autowired
    private BinaryTransferService binaryTransferService;
    @Autowired
    private PremisLoggerFactory premisLoggerFactory;
    private BinaryCleanupProcessor processor;
    @Mock
    private Exchange exchange;
    @Mock
    private Message message;

    private AdminUnit adminUnit;

    private CollectionObject collection;

    private AutoCloseable closeable;

    @Before
    public void init() {
        closeable = openMocks(this);

        TestHelper.setContentBase(baseAddress);

        repositoryInitializer.initializeRepository();
        PID contentRootPid = getContentRootPid();

        ContentRootObject contentRoot = repoObjectLoader.getContentRootObject(contentRootPid);
        adminUnit = repoObjectFactory.createAdminUnit(null);
        collection = repoObjectFactory.createCollectionObject(null);
        contentRoot.addMember(adminUnit);
        adminUnit.addMember(collection);

        processor = new BinaryCleanupProcessor();
        processor.setBinaryTransferService(binaryTransferService);
        processor.setStorageLocationManager(storageLocationManager);
        when(exchange.getIn()).thenReturn(message);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void noBinariesTest() throws Exception {
        PID pid = pidMinter.mintContentPid();
        PID dsPid = DatastreamPids.getMdDescriptivePid(pid);

        when(message.getBody(Map.class)).thenReturn(Collections.emptyMap());
        processor.process(exchange);
        // Does nothing, but should not fail
    }

    @Test
    public void binaryOnlyCurrentVersionTest() throws Exception {
        FolderObject folder = repoObjectFactory.createFolderObject(null);
        PremisLogger premisLogger = premisLoggerFactory.createPremisLogger(folder);
        premisLogger.buildEvent(Premis.Ingestion)
                .addEventDetail("Ingested this thing")
                .writeAndClose();

        PID mdEventsPid = DatastreamPids.getMdEventsPid(folder.getPid());
        BinaryObject mdEventsObj = repoObjectLoader.getBinaryObject(mdEventsPid);
        URI mdEventsUri = mdEventsObj.getContentUri();
        File headContentFile = new File(mdEventsUri);
        assertTrue("Binary must exist prior to cleanup", headContentFile.exists());

        when(message.getBody(Map.class)).thenReturn(ImmutableMap.of(mdEventsPid.getRepositoryPath(), mdEventsUri.toString()));
        processor.process(exchange);

        assertTrue("Binary must exist after cleanup", headContentFile.exists());
    }

    @Test
    public void binaryMultipleOlderVersionsTest() throws Exception {
        FolderObject folder = repoObjectFactory.createFolderObject(null);
        PremisLogger premisLogger = premisLoggerFactory.createPremisLogger(folder);
        // Add events one by one, to produce multiple versions of log datastream
        premisLogger.buildEvent(Premis.Ingestion)
                .addEventDetail("Ingested this thing")
                .write();

        premisLogger.buildEvent(Premis.VirusCheck)
                .addEventDetail("Scanning it")
                .write();

        premisLogger.buildEvent(Premis.FixityCheck)
                .addEventDetail("Checking it")
                .write();

        PID mdEventsPid = DatastreamPids.getMdEventsPid(folder.getPid());
        BinaryObject mdEventsObj = repoObjectLoader.getBinaryObject(mdEventsPid);
        URI headContentUri = mdEventsObj.getContentUri();
        File headContentFile = new File(headContentUri);
        assertTrue("Binary must exist prior to cleanup", headContentFile.exists());

        StorageLocation storageLoc = storageLocationManager.getStorageLocationById(StorageLocationTestHelper.LOC1_ID);
        List<URI> startingUris = storageLoc.getAllStorageUris(mdEventsPid);
        assertEquals(3, startingUris.size());

        when(message.getBody(Map.class)).thenReturn(ImmutableMap.of(mdEventsPid.getRepositoryPath(), headContentUri.toString()));
        processor.process(exchange);

        assertTrue("Head binary must exist after cleanup", headContentFile.exists());

        List<URI> afterUris = storageLoc.getAllStorageUris(mdEventsPid);
        assertEquals(1, afterUris.size());
        assertTrue(afterUris.contains(headContentUri));
    }

    @Test
    public void binaryNewerVersionInTxTest() throws Exception {
        FolderObject folder = repoObjectFactory.createFolderObject(null);
        PremisLogger premisLogger = premisLoggerFactory.createPremisLogger(folder);
        // Add events one by one, to produce multiple versions of log datastream
        premisLogger.buildEvent(Premis.Ingestion)
                .addEventDetail("Ingested this thing")
                .write();

        PID mdEventsPid = DatastreamPids.getMdEventsPid(folder.getPid());
        BinaryObject mdEventsObj = repoObjectLoader.getBinaryObject(mdEventsPid);
        URI headContentUri = mdEventsObj.getContentUri();
        File headContentFile = new File(headContentUri);
        assertTrue("Binary must exist prior to cleanup", headContentFile.exists());

        StorageLocation storageLoc = storageLocationManager.getStorageLocationById(StorageLocationTestHelper.LOC1_ID);

        FedoraTransaction tx = txManager.startTransaction();
        try {
            premisLogger.buildEvent(Premis.VirusCheck)
                    .addEventDetail("Scanning it")
                    .write();

            List<URI> startingUris = storageLoc.getAllStorageUris(mdEventsPid);
            assertEquals(2, startingUris.size());

            when(message.getBody(Map.class)).thenReturn(ImmutableMap.of(mdEventsPid.getRepositoryPath(), headContentUri.toString()));
            processor.process(exchange);

            // Both the head version and the uncommitted tx version should exist
            List<URI> afterUris = storageLoc.getAllStorageUris(mdEventsPid);
            assertEquals(2, afterUris.size());
            assertTrue(afterUris.containsAll(startingUris));
            assertTrue(afterUris.stream().allMatch(uri -> new File(uri).exists()));
        } finally {
            tx.close();
        }

        // Check that correct files are in place after ending the tx
        BinaryObject afterMdEventsObj = repoObjectLoader.getBinaryObject(mdEventsPid);
        URI afterContentUri = afterMdEventsObj.getContentUri();
        assertNotEquals("Content URI of Event Log must have updated", afterContentUri, headContentUri);

        when(message.getBody(Map.class)).thenReturn(ImmutableMap.of(mdEventsPid.getRepositoryPath(), afterContentUri.toString()));
        processor.process(exchange);

        // Both the head version and the uncommitted tx version should exist
        List<URI> afterUris = storageLoc.getAllStorageUris(mdEventsPid);
        assertEquals(1, afterUris.size());
        assertTrue(afterUris.contains(afterContentUri));
        assertTrue(afterUris.stream().allMatch(uri -> new File(uri).exists()));
    }
}
