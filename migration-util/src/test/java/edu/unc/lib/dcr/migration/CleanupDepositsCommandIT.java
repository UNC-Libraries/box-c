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

import static edu.unc.lib.dcr.migration.MigrationConstants.toBxc3Uri;
import static edu.unc.lib.dl.fedora.PIDConstants.DEPOSITS_QUALIFIER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.jdom2.Document;
import org.junit.After;
import org.junit.AfterClass;
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

import edu.unc.lib.dcr.migration.fcrepo3.ContentModelHelper.ContentModel;
import edu.unc.lib.dcr.migration.fcrepo3.ContentModelHelper.Relationship;
import edu.unc.lib.dcr.migration.fcrepo3.DatastreamVersion;
import edu.unc.lib.dcr.migration.fcrepo3.FoxmlDocumentBuilder;
import edu.unc.lib.dcr.migration.fcrepo3.FoxmlDocumentHelpers;
import edu.unc.lib.dl.fcrepo4.CollectionObject;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectFactory;
import edu.unc.lib.dl.fcrepo4.RepositoryPIDMinter;
import edu.unc.lib.dl.fcrepo4.RepositoryPathConstants;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.persist.services.deposit.DepositDirectoryManager;
import edu.unc.lib.dl.persist.services.deposit.DepositModelManager;
import edu.unc.lib.dl.test.TestHelper;
import edu.unc.lib.dl.util.DepositStatusFactory;
import edu.unc.lib.dl.util.JobStatusFactory;
import picocli.CommandLine;
import redis.embedded.RedisServer;

/**
 * @author bbpennel
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextHierarchy({
    @ContextConfiguration("/spring-test/test-fedora-container.xml"),
    @ContextConfiguration("/spring-test/jedis-context.xml"),
    @ContextConfiguration("/spring-test/cdr-client-container.xml")
})
public class CleanupDepositsCommandIT extends AbstractTransformationIT {
    private static final Logger log = getLogger(CleanupDepositsCommandIT.class);

    private static final String BINARY_CONTENT = "Binary stuff";
    private static final String BINARY_MD5 = "74dec3bdb7c0cda2c3d501c145d73723";

    @Rule
    public final TemporaryFolder tmpFolder = new TemporaryFolder();

    private File tdbDir;
    private File depositBaseDir;

    private DepositModelManager modelManager;

    final PrintStream originalOut = System.out;
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private String output;

    CommandLine migrationCommand;

    @Autowired
    private DepositStatusFactory depositStatusFactory;
    @Autowired
    private JobStatusFactory jobStatusFactory;
    @Autowired
    private RepositoryPIDMinter pidMinter;
    @Autowired
    private RepositoryObjectFactory repoObjFactory;

    private CollectionObject destCollObj;

    private static final RedisServer redisServer;

    static {
        try {
            redisServer = new RedisServer(46380);
            redisServer.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @AfterClass
    public static void afterClass() throws Exception {
        redisServer.stop();
    }

    @Before
    public void setUp() throws Exception {
        TestHelper.setContentBase("http://localhost:48085/rest");
        resetOutput();

        datastreamsPath = tmpFolder.newFolder("datastreams").toPath();
        objectsPath = tmpFolder.newFolder("objects").toPath();

        tdbDir = tmpFolder.newFolder("tdb");
        depositBaseDir = tmpFolder.newFolder("deposits");
        File dbFile = tmpFolder.newFile("index_db");
        System.setProperty("dcr.deposit.dir", depositBaseDir.getAbsolutePath());
        System.setProperty("dcr.tdb.dir", tdbDir.getAbsolutePath());
        System.setProperty("dcr.migration.index.url", dbFile.toPath().toUri().toString());
        System.setProperty("dcr.it.tdr.ingestSource", tmpFolder.getRoot().getAbsolutePath());

        migrationCommand = new CommandLine(new MigrationCLI());

        Map<String, CommandLine> subs = migrationCommand.getSubcommands();
        CommandLine cleanupCommand = subs.get("cleanup_deposits");

        CleanupDepositsCommand cCommand = (CleanupDepositsCommand) cleanupCommand.getCommand();
        Path contextPath = Paths.get("src", "test", "resources", "spring-test",
                "jedis-context.xml");
        cCommand.setApplicationContextPath(contextPath.toUri().toString());

        output = null;
    }

    private void resetOutput() {
        out.reset();
        System.setOut(new PrintStream(out));
    }

    @After
    public void cleanup() {
        System.setOut(originalOut);
        System.clearProperty("dcr.tdb.dir");
        System.clearProperty("dcr.deposit.dir");
        if (modelManager != null) {
            modelManager.close();
        }
    }

    @Test
    public void cleanupNonExistentDeposit() throws Exception {
        String depositId = UUID.randomUUID().toString();
        PID depositPid = PIDs.get(DEPOSITS_QUALIFIER, depositId);
        String[] args = new String[] { "cleanup_deposits", depositId };
        executeExpectSuccess(args);

        assertTrue("Expected output state that model removed",
                output.contains("Removed deposit model"));
        assertTrue("Expected output state that directory not present",
                output.contains("No Deposit directory present"));
        assertTrue("Expected output state that status deleted",
                output.contains("Deleted deposit/job status details"));

        modelManager = new DepositModelManager(tdbDir.toString());
        Model depModel = modelManager.getReadModel(depositPid);
        assertTrue(depModel.isEmpty());

        assertEquals(0, depositBaseDir.list().length);

        assertTrue(jobStatusFactory.getAllJobs(depositId).isEmpty());
        assertTrue(depositStatusFactory.get(depositId).isEmpty());
    }

    @Test
    public void cleanupOneDepositWithMultiplePresent() throws Exception {
        destCollObj = repoObjFactory.createCollectionObject(null);

        setupTransformCommand();
        PID depositPid1 = populateDeposit();
        String depositId1 = depositPid1.getId();
        PID depositPid2 = populateDeposit();
        String depositId2 = depositPid2.getId();

        // Check that data is present prior cleanup
        modelManager = new DepositModelManager(tdbDir.toString());
        assertFalse(modelManager.getReadModel(depositPid1).isEmpty());
        modelManager.end();
        assertFalse(modelManager.getReadModel(depositPid2).isEmpty());
        modelManager.end();

        assertFalse(depositStatusFactory.get(depositId1).isEmpty());
        assertFalse(depositStatusFactory.get(depositId2).isEmpty());

        String[] args = new String[] { "cleanup_deposits", depositId1 };
        executeExpectSuccess(args);

        DepositDirectoryManager dirManager1 = new DepositDirectoryManager(
                depositPid1, depositBaseDir.toPath(), true, false);

        assertTrue("Expected output state that model removed",
                output.contains("Removed deposit model"));
        assertTrue("Expected output state that directory deleted",
                output.contains("Deleted deposit directory: " + dirManager1.getDepositDir()));
        assertEquals("Only expected 1 occurance of cleanup message",
                1, StringUtils.countMatches(output, "Deleted deposit directory"));
        assertTrue("Expected output state that status deleted",
                output.contains("Deleted deposit/job status details"));

        // Verify that data is only gone for the cleaned up deposit
        assertTrue(modelManager.getReadModel(depositPid1).isEmpty());
        modelManager.end();
        assertFalse(modelManager.getReadModel(depositPid2).isEmpty());
        modelManager.end();

        assertEquals("One deposit directory should still exist", 1, depositBaseDir.list().length);
        assertEquals(depositPid2.getId(), depositBaseDir.list()[0]);

        assertTrue(depositStatusFactory.get(depositId1).isEmpty());
        assertFalse(depositStatusFactory.get(depositId2).isEmpty());
    }

    @Test
    public void cleanupMultipleDeposits() throws Exception {
        destCollObj = repoObjFactory.createCollectionObject(null);

        setupTransformCommand();
        PID depositPid1 = populateDeposit();
        String depositId1 = depositPid1.getId();
        PID depositPid2 = populateDeposit();
        String depositId2 = depositPid2.getId();

        String[] args = new String[] { "cleanup_deposits", depositId1 + "," + depositId2 };
        executeExpectSuccess(args);

        DepositDirectoryManager dirManager1 = new DepositDirectoryManager(
                depositPid1, depositBaseDir.toPath(), true, false);
        DepositDirectoryManager dirManager2 = new DepositDirectoryManager(
                depositPid2, depositBaseDir.toPath(), true, false);

        assertEquals("Expected 2 occurances of cleanup message",
                2, StringUtils.countMatches(output, "Removed deposit model"));
        assertTrue("Expected output state that directory deleted",
                output.contains("Deleted deposit directory: " + dirManager1.getDepositDir()));
        assertTrue("Expected output state that directory deleted",
                output.contains("Deleted deposit directory: " + dirManager2.getDepositDir()));
        assertEquals("Expected 2 occurances of cleanup message",
                2, StringUtils.countMatches(output, "Deleted deposit directory"));
        assertEquals("Expected 2 occurances of cleanup message",
                2, StringUtils.countMatches(output, "Deleted deposit/job status details"));

        modelManager = new DepositModelManager(tdbDir.toString());
        // Verify that data is gone for both deposits
        assertTrue(modelManager.getReadModel(depositPid1).isEmpty());
        modelManager.end();
        assertTrue(modelManager.getReadModel(depositPid2).isEmpty());
        modelManager.end();

        assertEquals(0, depositBaseDir.list().length);

        assertTrue(depositStatusFactory.get(depositId1).isEmpty());
        assertTrue(depositStatusFactory.get(depositId2).isEmpty());
    }

    private void setupTransformCommand() {
        Map<String, CommandLine> subs = migrationCommand.getSubcommands();
        CommandLine transformCommand = subs.get("tc");

        TransformContentCommand tcCommand = (TransformContentCommand) transformCommand.getCommand();
        Path contextPath = Paths.get("src", "test", "resources", "spring-test",
                "cdr-client-container.xml");
        tcCommand.setApplicationContextPath(contextPath.toUri().toString());
    }

    private PID populateDeposit() throws Exception {
        PID filePid = pidMinter.mintContentPid();
        writeDatastreamFile(filePid, FoxmlDocumentHelpers.ORIGINAL_DS, BINARY_CONTENT);
        DatastreamVersion ds0 = new DatastreamVersion(BINARY_MD5,
                FoxmlDocumentHelpers.ORIGINAL_DS, "0",
                FoxmlDocumentBuilder.DEFAULT_CREATED_DATE,
                Integer.toString(BINARY_CONTENT.length()),
                "text/plain",
                null);

        Model fileModel = createModelWithTypes(filePid, ContentModel.SIMPLE);

        Document foxml2 = new FoxmlDocumentBuilder(filePid, "file")
                .relsExtModel(fileModel)
                .withDatastreamVersion(ds0)
                .build();
        serializeFoxml(filePid, foxml2);

        addPremisLog(filePid);

        PID folderPid = pidMinter.mintContentPid();
        Model folderModel = createModelWithTypes(folderPid, ContentModel.CONTAINER);
        Resource folderResc = folderModel.getResource(toBxc3Uri(folderPid));
        Resource fileResc = folderModel.getResource(toBxc3Uri(filePid));
        folderResc.addProperty(Relationship.contains.getProperty(), fileResc);

        serializeBasicObject(folderPid, "folder", folderModel);

        indexFiles();

        String[] args = new String[] {
                "--redis-port", "46380",
                "tc", folderPid.getId(),
                "--deposit-into", destCollObj.getPid().getId()};
        executeExpectSuccess(args);

        assertTrue("Expected one transformation successful",
                output.contains(" 2/2 "));

        PID depositPid = extractDepositPid(output);
        resetOutput();
        return depositPid;
    }

    private PID extractDepositPid(String output) {
        String fieldText = "Populating deposit: ";
        int start = output.indexOf(fieldText);
        if (start == -1) {
            fail("No deposit id output");
        }
        start += fieldText.length();
        int end = output.indexOf("\n", start);
        String id = output.substring(start, end);
        return PIDs.get(RepositoryPathConstants.DEPOSIT_RECORD_BASE, id);
    }

    private void indexFiles() {
        String[] indexArgs = new String[] { "pi", "populate",
                objectsPath.toString(),
                datastreamsPath.toString(),
                "-l" };
        migrationCommand.execute(indexArgs);
    }

    private void serializeBasicObject(PID pid, String title, Model model) throws IOException {
        Document foxml = new FoxmlDocumentBuilder(pid, title)
                .relsExtModel(model)
                .build();
        serializeFoxml(pid, foxml);
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
