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
package edu.unc.lib.dl.services.camel.binaryCleanup;

import static edu.unc.lib.dl.fcrepo4.FcrepoJmsConstants.EVENT_TYPE;
import static edu.unc.lib.dl.fcrepo4.FcrepoJmsConstants.IDENTIFIER;
import static edu.unc.lib.dl.fcrepo4.FcrepoJmsConstants.RESOURCE_TYPE;
import static edu.unc.lib.dl.fcrepo4.RepositoryPaths.getContentRootPid;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.File;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.camel.CamelContext;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.NotifyBuilder;
import org.apache.jena.rdf.model.Resource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import edu.unc.lib.dl.event.PremisLogger;
import edu.unc.lib.dl.fcrepo4.AdminUnit;
import edu.unc.lib.dl.fcrepo4.BinaryObject;
import edu.unc.lib.dl.fcrepo4.CollectionObject;
import edu.unc.lib.dl.fcrepo4.ContentRootObject;
import edu.unc.lib.dl.fcrepo4.FcrepoJmsConstants;
import edu.unc.lib.dl.fcrepo4.FedoraTransaction;
import edu.unc.lib.dl.fcrepo4.FolderObject;
import edu.unc.lib.dl.fcrepo4.RepositoryInitializer;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectFactory;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;
import edu.unc.lib.dl.fcrepo4.TransactionManager;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.model.DatastreamPids;
import edu.unc.lib.dl.persist.api.storage.StorageLocation;
import edu.unc.lib.dl.persist.api.storage.StorageLocationManager;
import edu.unc.lib.dl.persist.services.storage.StorageLocationTestHelper;
import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.rdf.Fcrepo4Repository;
import edu.unc.lib.dl.rdf.Premis;
import edu.unc.lib.dl.test.TestHelper;

/**
 * @author bbpennel
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextHierarchy({
    @ContextConfiguration("/spring-test/test-fedora-container.xml"),
    @ContextConfiguration("/spring-test/cdr-client-container.xml"),
    @ContextConfiguration("/spring-test/jms-context.xml"),
    @ContextConfiguration("/binary-cleanup-it-context.xml")
})
public class BinaryCleanupRouterIT {

    @Autowired
    private String baseAddress;
    @Autowired
    private RepositoryInitializer repositoryInitializer;
    @Autowired
    private RepositoryObjectFactory repoObjectFactory;
    @javax.annotation.Resource(name = "repositoryObjectLoaderNoCache")
    private RepositoryObjectLoader repoObjectLoader;
    @Autowired
    private StorageLocationManager storageLocationManager;
    @Autowired
    private TransactionManager txManager;

    @Produce(uri = "{{cdr.binaryCleanup.camel}}")
    private ProducerTemplate template;

    @Autowired
    private CamelContext cdrBinaryCleanup;

    private AdminUnit adminUnit;

    private CollectionObject collection;

    @Before
    public void init() {
        initMocks(this);

        TestHelper.setContentBase(baseAddress);

        repositoryInitializer.initializeRepository();
        PID contentRootPid = getContentRootPid();

        ContentRootObject contentRoot = repoObjectLoader.getContentRootObject(contentRootPid);
        adminUnit = repoObjectFactory.createAdminUnit(null);
        collection = repoObjectFactory.createCollectionObject(null);
        contentRoot.addMember(adminUnit);
        adminUnit.addMember(collection);
    }

    @Test
    public void nonBinaryObjectTest() throws Exception {
        FolderObject folder = repoObjectFactory.createFolderObject(null);

        NotifyBuilder notify = new NotifyBuilder(cdrBinaryCleanup)
                .whenCompleted(1)
                .create();

        Map<String, Object> headers = createEvent(folder.getPid(), Cdr.Folder);
        template.sendBodyAndHeaders("", headers);

        assertTrue("Route not satisfied", notify.matches(5l, TimeUnit.SECONDS));
    }

    @Test
    public void binaryOnlyCurrentVersionTest() throws Exception {
        FolderObject folder = repoObjectFactory.createFolderObject(null);
        PremisLogger premisLogger = folder.getPremisLog();
        premisLogger.buildEvent(Premis.Ingestion)
                    .addEventDetail("Ingested this thing")
                    .writeAndClose();

        PID mdEventsPid = DatastreamPids.getMdEventsPid(folder.getPid());
        BinaryObject mdEventsObj = repoObjectLoader.getBinaryObject(mdEventsPid);
        File headContentFile = new File(mdEventsObj.getContentUri());
        assertTrue("Binary must exist prior to cleanup", headContentFile.exists());

        Map<String, Object> headers = createEvent(mdEventsPid, Fcrepo4Repository.Binary);
        template.sendBodyAndHeaders("", headers);

        NotifyBuilder notify = new NotifyBuilder(cdrBinaryCleanup)
                .whenCompleted(1)
                .create();
        assertTrue("Route not satisfied", notify.matches(5l, TimeUnit.SECONDS));

        assertTrue("Binary must exist after cleanup", headContentFile.exists());
    }

    @Test
    public void binaryMultipleOlderVersionsTest() throws Exception {
        FolderObject folder = repoObjectFactory.createFolderObject(null);
        PremisLogger premisLogger = folder.getPremisLog();
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

        Map<String, Object> headers = createEvent(mdEventsPid, Fcrepo4Repository.Binary);
        template.sendBodyAndHeaders("", headers);

        NotifyBuilder notify1 = new NotifyBuilder(cdrBinaryCleanup)
                .whenCompleted(1)
                .create();
        assertTrue("Route not satisfied", notify1.matches(5l, TimeUnit.SECONDS));

        assertTrue("Head binary must exist after cleanup", headContentFile.exists());

        List<URI> afterUris = storageLoc.getAllStorageUris(mdEventsPid);
        assertEquals(1, afterUris.size());
        assertTrue(afterUris.contains(headContentUri));
    }

    @Test
    public void binaryNewerVersionInTxTest() throws Exception {
        FolderObject folder = repoObjectFactory.createFolderObject(null);
        PremisLogger premisLogger = folder.getPremisLog();
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

            Map<String, Object> headers = createEvent(mdEventsPid, Fcrepo4Repository.Binary);
            template.sendBodyAndHeaders("", headers);

            NotifyBuilder notify1 = new NotifyBuilder(cdrBinaryCleanup)
                    .whenCompleted(1)
                    .create();
            assertTrue("Route not satisfied", notify1.matches(5l, TimeUnit.SECONDS));

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

        NotifyBuilder notify2 = new NotifyBuilder(cdrBinaryCleanup)
                .whenCompleted(1)
                .create();

        Map<String, Object> headers = createEvent(mdEventsPid, Fcrepo4Repository.Binary);
        template.sendBodyAndHeaders("", headers);

        assertTrue("Route not satisfied", notify2.matches(5l, TimeUnit.SECONDS));

        // Both the head version and the uncommitted tx version should exist
        List<URI> afterUris = storageLoc.getAllStorageUris(mdEventsPid);
        assertEquals(1, afterUris.size());
        assertTrue(afterUris.contains(afterContentUri));
        assertTrue(afterUris.stream().allMatch(uri -> new File(uri).exists()));
    }

    private Map<String, Object> createEvent(PID pid, Resource... types) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(IDENTIFIER, pid.getRepositoryPath());
        headers.put(EVENT_TYPE, FcrepoJmsConstants.EVENT_MODIFY);
        headers.put(FCREPO_URI, pid.getRepositoryPath());
        headers.put(RESOURCE_TYPE, Arrays.stream(types).map(Resource::getURI).collect(Collectors.joining(",")));

        return headers;
    }
}
