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
package edu.unc.lib.dcr.migration;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.Model;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import edu.unc.lib.dl.fcrepo4.AdminUnit;
import edu.unc.lib.dl.fcrepo4.BinaryObject;
import edu.unc.lib.dl.fcrepo4.CollectionObject;
import edu.unc.lib.dl.fcrepo4.ContentRootObject;
import edu.unc.lib.dl.fcrepo4.DepositRecord;
import edu.unc.lib.dl.fcrepo4.FileObject;
import edu.unc.lib.dl.fcrepo4.RepositoryInitializer;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectFactory;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;
import edu.unc.lib.dl.fcrepo4.RepositoryPaths;
import edu.unc.lib.dl.fcrepo4.WorkObject;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.model.DatastreamPids;
import edu.unc.lib.dl.persist.api.storage.StorageLocation;
import edu.unc.lib.dl.persist.api.storage.StorageLocationManager;
import edu.unc.lib.dl.persist.api.transfer.BinaryTransferOutcome;
import edu.unc.lib.dl.persist.api.transfer.BinaryTransferService;
import edu.unc.lib.dl.persist.api.transfer.BinaryTransferSession;
import edu.unc.lib.dl.test.RepositoryObjectTreeIndexer;
import edu.unc.lib.dl.test.TestHelper;
import edu.unc.lib.dl.util.RDFModelUtil;
import picocli.CommandLine;

/**
 * @author bbpennel
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextHierarchy({
    @ContextConfiguration("/spring-test/test-fedora-container.xml"),
    @ContextConfiguration("/spring-test/cdr-client-container.xml")
})
public class RecalculateDigestsCommandIT {
    private static final Logger log = getLogger(RecalculateDigestsCommandIT.class);

    @Rule
    public final TemporaryFolder tmpFolder = new TemporaryFolder();

    @Autowired
    protected String baseAddress;
    @Autowired
    private RepositoryObjectFactory repoObjFactory;
    @Autowired
    private RepositoryObjectLoader repoObjLoader;
    @Autowired
    private StorageLocationManager storageLocManager;
    @Autowired
    private RepositoryObjectTreeIndexer treeIndexer;
    @Autowired
    private BinaryTransferService transferService;
    @Autowired
    private Model queryModel;
    private File modelFile;

    private RepositoryInitializer repositoryInitializer;

    final PrintStream originalOut = System.out;
    final ByteArrayOutputStream out = new ByteArrayOutputStream();

    CommandLine migrationCommand;

    private String output;

    protected ContentRootObject rootObj;

    private StorageLocation defaultStorageLoc;

    @Before
    public void setUp() throws Exception {
        TestHelper.setContentBase("http://localhost:48085/rest");

        tmpFolder.create();
        modelFile = tmpFolder.newFile();
        System.setProperty("dcr.it.rcd.model_file", modelFile.getAbsolutePath());

        out.reset();
        System.setOut(new PrintStream(out));

        migrationCommand = new CommandLine(new MigrationCLI());

        // Set the application context path for the test environment
        Map<String, CommandLine> subs = migrationCommand.getSubcommands();
        CommandLine recalcCommand = subs.get("recalculate_digests");

        RecalculateDigestsCommand rcdCommand = (RecalculateDigestsCommand) recalcCommand.getCommand();
        Path contextPath = Paths.get("src", "test", "resources", "spring-test",
                "recalculate-digests-command-it.xml");
        rcdCommand.setApplicationContextPath(contextPath.toUri().toString());

        output = null;

        repositoryInitializer = new RepositoryInitializer();
        repositoryInitializer.setObjFactory(repoObjFactory);
        repositoryInitializer.initializeRepository();
        rootObj = repoObjLoader.getContentRootObject(RepositoryPaths.getContentRootPid());

        defaultStorageLoc = storageLocManager.getStorageLocationById("loc1");
    }

    @After
    public void cleanup() {
        System.setOut(originalOut);
        System.clearProperty("dcr.it.rcd.model_file");
    }

    @Test
    public void recalculateWithNoMetadataDigests() throws Exception {
        AdminUnit unit = repoObjFactory.createAdminUnit(null);
        CollectionObject coll = repoObjFactory.createCollectionObject(null);
        WorkObject work = repoObjFactory.createWorkObject(null);
        FileObject file = repoObjFactory.createFileObject(null);
        PID originalPid = DatastreamPids.getOriginalFilePid(file.getPid());
        createBinary(originalPid, "content");

        DepositRecord depRec = repoObjFactory.createDepositRecord(null);

        work.addMember(file);
        coll.addMember(work);
        unit.addMember(coll);
        rootObj.addMember(unit);

        PID collEventsPid = DatastreamPids.getMdEventsPid(coll.getPid());
        createBinary(collEventsPid, "events go here");

        PID workDescPid = DatastreamPids.getMdDescriptivePid(work.getPid());
        createBinary(workDescPid, "descriptive");

        PID techMdPid = DatastreamPids.getTechnicalMetadataPid(file.getPid());
        createBinary(techMdPid, "highly technical");

        PID manifestPid = DatastreamPids.getDepositManifestPid(depRec.getPid(), "manifest0");
        BinaryTransferOutcome manifestOut = transferContent(manifestPid, "manifested");
        depRec.addManifest(manifestOut.getDestinationUri(), "manifest0", "text/plain", null, null);

        treeIndexer.indexAll(baseAddress);

        RDFModelUtil.serializeModel(queryModel, modelFile);

        String[] countArgs = new String[] { "recalculate_digests", "--count" };
        executeExpectSuccess(countArgs);

        assertTrue("Incorrect number of binaries found",
                output.contains("Retrieved list of 4 binaries to update"));

        out.reset();

        String[] dryArgs = new String[] { "recalculate_digests", "-n" };
        executeExpectSuccess(dryArgs);

        assertTrue("Incorrect number of binaries found",
                output.contains("Retrieved list of 4 binaries to update"));
        assertTrue("Did not process collections events",
                output.contains(collEventsPid.getQualifiedId()));
        assertTrue("Did not process description binary",
                output.contains(workDescPid.getQualifiedId()));
        assertTrue("Did not process techmd binary",
                output.contains(techMdPid.getQualifiedId()));
        assertTrue("Did not process manifest binary",
                output.contains(manifestPid.getQualifiedId()));

        // Dry run, so no SHA1s should be set
        assertNoDigestPresent(originalPid);
        assertNoDigestPresent(collEventsPid);
        assertNoDigestPresent(workDescPid);
        assertNoDigestPresent(techMdPid);
        assertNoDigestPresent(manifestPid);

        out.reset();

        String[] args = new String[] { "recalculate_digests" };
        executeExpectSuccess(args);

        assertTrue("Incorrect number of binaries found",
                output.contains("Retrieved list of 4 binaries to update"));

        // Original should not be updated, but the rest should
        assertNoDigestPresent(originalPid);
        assertDigestPresent(collEventsPid);
        assertDigestPresent(workDescPid);
        assertDigestPresent(techMdPid);
        assertDigestPresent(manifestPid);
    }

    private BinaryObject createBinary(PID binPid, String content) throws Exception {
        BinaryTransferOutcome eventsOutcome = transferContent(binPid, content);
        return repoObjFactory.createOrUpdateBinary(binPid, eventsOutcome.getDestinationUri(),
                "somefile", "text/plain", null, null, null);
    }

    private BinaryTransferOutcome transferContent(PID binPid, String content) throws Exception {
        try (BinaryTransferSession session = transferService.getSession(defaultStorageLoc)) {
            return session.transfer(binPid, IOUtils.toInputStream("content", UTF_8));
        }
    }

    private void assertNoDigestPresent(PID binPid) {
        BinaryObject bin = repoObjLoader.getBinaryObject(binPid);
        assertNull("Expected no digest for " + binPid, bin.getSha1Checksum());
    }

    private void assertDigestPresent(PID binPid) {
        BinaryObject bin = repoObjLoader.getBinaryObject(binPid);
        assertNotNull("Expected SHA1 digest for " + binPid, bin.getSha1Checksum());
    }

    private void executeExpectSuccess(String[] args) {
        int result = migrationCommand.execute(args);
        output = out.toString();
        if (result != 0) {
            System.setOut(originalOut);
            log.error(output);
            fail("Expected command to result in success: " + String.join(" ", args));
        }
    }
}
