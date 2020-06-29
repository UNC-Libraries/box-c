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
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.io.FileUtils.writeStringToFile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.DC;
import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoResponse;
import org.jdom2.Document;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import edu.unc.lib.dcr.migration.deposit.AbstractDepositRecordTransformationIT;
import edu.unc.lib.dcr.migration.fcrepo3.ContentModelHelper.CDRProperty;
import edu.unc.lib.dcr.migration.fcrepo3.ContentModelHelper.ContentModel;
import edu.unc.lib.dcr.migration.fcrepo3.DatastreamVersion;
import edu.unc.lib.dcr.migration.fcrepo3.FoxmlDocumentBuilder;
import edu.unc.lib.dl.fcrepo4.BinaryObject;
import edu.unc.lib.dl.fcrepo4.DepositRecord;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;
import edu.unc.lib.dl.fcrepo4.RepositoryPIDMinter;
import edu.unc.lib.dl.fcrepo4.RepositoryPathConstants;
import edu.unc.lib.dl.fcrepo4.RepositoryPaths;
import edu.unc.lib.dl.fedora.NotFoundException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.rdf.Ldp;
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
public class TransformDepositRecordsCommandIT extends AbstractDepositRecordTransformationIT {
    private static final String MANIFEST_NAME = "DATA_MANIFEST0";
    private static final String MANIFEST_CONTENT = "content for m0";
    private static final String DEPOSITOR = "the_depositor";

    final PrintStream originalOut = System.out;
    final ByteArrayOutputStream out = new ByteArrayOutputStream();

    private PID bxc3Pid;

    @Autowired
    private RepositoryObjectLoader repoObjLoader;
    @Autowired
    private FcrepoClient fcrepoClient;

    @Before
    public void setUp() throws Exception {
        TestHelper.setContentBase("http://localhost:48085/rest");

        out.reset();
        System.setOut(new PrintStream(out));

        datastreamsPath = tmpFolder.newFolder("datastreams").toPath();
        objectsPath = tmpFolder.newFolder("objects").toPath();

        bxc3Pid = new RepositoryPIDMinter().mintDepositRecordPid();

        File dbFile = tmpFolder.newFile("index_db");
        System.setProperty("dcr.migration.index.url", dbFile.toPath().toUri().toString());
        System.setProperty("dcr.it.tdr.ingestSource", tmpFolder.getRoot().getAbsolutePath());
    }

    @After
    public void cleanup() {
        System.setOut(originalOut);
        System.clearProperty("dcr.migration.index.url");
        System.clearProperty("dcr.it.tdr.ingestSource");
    }

    @Test
    public void transformDepositRecordsLowerCasePID() throws Exception {
        CommandLine migrationCommand = new CommandLine(new MigrationCLI());

        File pidListFile = setupDepositRecord(migrationCommand,
                "Deposit Record with Manifest");

        String[] args = new String[] { "tdr", pidListFile.getAbsolutePath(),
                "-s", "loc1" };
        int result = migrationCommand.execute(args);

        assertEquals("Incorrect exit status", 0, result);
        String output = out.toString();
        assertTrue("Expected one transformation successful",
                output.contains(" 1/1 "));
        assertTrue("Expected transformation completed message",
                output.contains("Finished transformation"));


        DepositRecord depRec = repoObjLoader.getDepositRecord(bxc3Pid);
        assertTrue(depRec.getResource().hasProperty(DC.title, "Deposit Record with Manifest"));
        assertTrue(depRec.getResource().hasLiteral(Cdr.depositedOnBehalfOf, DEPOSITOR));

        assertPremisTransformed(depRec);

        assertManifestPopulated(depRec);
    }

    @Test
    public void transformDepositRecordsUpperCasePID() throws Exception {
        String bxc3PidUpperCase = UUID.randomUUID().toString().toUpperCase();
        bxc3Pid = PIDs.get(RepositoryPathConstants.DEPOSIT_RECORD_BASE, bxc3PidUpperCase);
        CommandLine migrationCommand = new CommandLine(new MigrationCLI());

        File pidListFile = setupDepositRecord(migrationCommand,
                "Deposit Record with Manifest");

        String[] args = new String[] { "tdr", pidListFile.getAbsolutePath(),
                "-s", "loc1" };
        int result = migrationCommand.execute(args);

        assertEquals("Incorrect exit status", 0, result);
        String output = out.toString();
        assertTrue("Expected one transformation successful",
                output.contains(" 1/1 "));
        assertTrue("Expected transformation completed message",
                output.contains("Finished transformation"));


        PID bxc5Pid = PIDs.get(RepositoryPathConstants.DEPOSIT_RECORD_BASE, bxc3PidUpperCase.toLowerCase());
        DepositRecord depRec = repoObjLoader.getDepositRecord(bxc5Pid);
        assertTrue(depRec.getResource().hasProperty(DC.title, "Deposit Record with Manifest"));
        assertTrue(depRec.getResource().hasLiteral(Cdr.depositedOnBehalfOf, DEPOSITOR));

        assertPremisTransformed(depRec);

        assertManifestPopulated(depRec);
    }

    private File setupDepositRecord(CommandLine migrationCommand, String title) throws Exception {
        // Set the application context path for the test environment
        Map<String, CommandLine> subs = migrationCommand.getSubcommands();
        CommandLine transformCommand = subs.get("tdr");

        TransformDepositRecordsCommand tdrCommand = (TransformDepositRecordsCommand) transformCommand.getCommand();
        Path contextPath = Paths.get("src", "test", "resources", "spring-test",
                "transform-deposit-record-command-it.xml");
        tdrCommand.setApplicationContextPath(contextPath.toUri().toString());

        // Setup the object to be ingested
        Model bxc3Model = createModelWithTypes(bxc3Pid, ContentModel.DEPOSIT_RECORD);
        Resource resc = bxc3Model.getResource(toBxc3Uri(bxc3Pid));
        resc.addLiteral(CDRProperty.depositedOnBehalfOf.getProperty(), DEPOSITOR);

        writeDatastreamFile(bxc3Pid, MANIFEST_NAME, MANIFEST_CONTENT);
        DatastreamVersion manifest0 = new DatastreamVersion(null,
                MANIFEST_NAME, "0",
                FoxmlDocumentBuilder.DEFAULT_CREATED_DATE,
                Integer.toString(MANIFEST_CONTENT.length()),
                "text/xml",
                null);

        Document foxml = new FoxmlDocumentBuilder(bxc3Pid, title)
                .relsExtModel(bxc3Model)
                .withDatastreamVersion(manifest0)
                .build();
        serializeFoxml(bxc3Pid, foxml);

        addPremisLog(bxc3Pid);

        // Index files
        String[] indexArgs = new String[] { "pi", "populate",
                objectsPath.toString(),
                datastreamsPath.toString(),
                "-l" };
        migrationCommand.execute(indexArgs);

        // Setup list file
        File pidListFile = tmpFolder.newFile("deposit_rec_pids.txt");
        writeStringToFile(pidListFile, bxc3Pid.getId(), UTF_8);

        return pidListFile;
    }

    @Test
    public void transformDepositRecordsGeneratedIds() throws Exception {
        CommandLine migrationCommand = new CommandLine(new MigrationCLI());

        String title = "Deposit Record Generated ID " + System.currentTimeMillis();
        File pidListFile = setupDepositRecord(migrationCommand,
                title);

        String[] args = new String[] { "tdr", pidListFile.getAbsolutePath(),
                "-g",
                "-s", "loc1" };
        int result = migrationCommand.execute(args);
        assertEquals("Incorrect exit status", 0, result);

        String output = out.toString();
        assertTrue("Expected one transformation successful",
                output.contains(" 1/1 "));
        assertTrue("Expected transformation completed message",
                output.contains("Finished transformation"));

        try {
            repoObjLoader.getDepositRecord(bxc3Pid);
            fail("Must not find deposit record at the original pid with generated ids flag");
        } catch(NotFoundException e) {
            // expected
        }

        // Retrieve all the deposit records in the test repository to find the new one
        List<DepositRecord> depRecs;
        URI depRecsBaseUri = URI.create(RepositoryPaths.getDepositRecordBase());
        try (FcrepoResponse resp = fcrepoClient.get(depRecsBaseUri).perform()) {
            Model depBaseModel = RDFModelUtil.createModel(resp.getBody());
            depRecs = depBaseModel.listObjectsOfProperty(Ldp.contains).toList().stream()
                .map(o -> PIDs.get(o.asResource().getURI()))
                .map(repoObjLoader::getDepositRecord)
                .collect(Collectors.toList());
        }

        // Find the correct deposit record by title
        DepositRecord depRec = depRecs.stream()
                .filter(d -> d.getResource().hasLiteral(DC.title, title))
                .findFirst()
                .get();

        assertTrue(depRec.getResource().hasLiteral(Cdr.depositedOnBehalfOf, DEPOSITOR));

        assertPremisTransformed(depRec);

        assertManifestPopulated(depRec);
    }

    private void assertManifestPopulated(DepositRecord depRec) throws Exception {
        List<BinaryObject> binList = depRec.listManifests().stream()
                .map(repoObjLoader::getBinaryObject)
                .collect(toList());

        assertEquals("Incorrect number of manifests", 1, binList.size());

        BinaryObject manifest0Bin = getManifestByName(binList, MANIFEST_NAME);
        assertManifestDetails(DEFAULT_CREATED_DATE,
                "text/xml",
                MANIFEST_CONTENT,
                manifest0Bin);
    }
}
