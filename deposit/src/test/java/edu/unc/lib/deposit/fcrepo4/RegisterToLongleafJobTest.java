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
package edu.unc.lib.deposit.fcrepo4;

import static edu.unc.lib.dl.test.TestHelpers.setField;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Bag;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.tdb.TDBFactory;
import org.apache.jena.vocabulary.RDF;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;

import edu.unc.lib.dl.fcrepo4.BinaryObject;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.rdf.CdrDeposit;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositField;

public class RegisterToLongleafJobTest extends AbstractDepositJobTest {
    @Rule
    public final TemporaryFolder tmpFolder = new TemporaryFolder();

    private final static String FILE_SCHEME = "file:";
    private final static String LOC1_ID = "/some/path/loc1";
    private final static String LOC2_ID = "/some/path/loc2";
    private final static String LOC3_ID = "/some/path/loc3";
    private final static String FILE_CONTENT1 = "Some content";
    private final static String FILE_CONTENT2 = "Other stuff";
    private final static String MD5 = "MD5 checksum";

    private RegisterToLongleafJob job;
    private PID depositPid;
    private Bag depBag;
    private Model model;
    private Path storageLocPath;
    private String outputPath;
    private String longleafScript;
    private BinaryObject destinationObj;
    private URI desitnationUri;

    @Mock
    private RepositoryObjectLoader repoObjLoader;

    @Before
    public void init() throws Exception {
        tmpFolder.create();

        Dataset dataset = TDBFactory.createDataset();

        job = new RegisterToLongleafJob();
        job.setJobUUID(jobUUID);
        job.setDepositUUID(depositUUID);
        job.setDepositDirectory(depositDir);
        setField(job, "repoObjLoader", repoObjLoader);
        setField(job, "dataset", dataset);
        setField(job, "depositStatusFactory", depositStatusFactory);

        outputPath = tmpFolder.newFile().getPath();
        longleafScript = getLongleafScript(outputPath);
        job.setLongleafBaseCommand(longleafScript);

        job.init();

        // Get a writeable model
        model = job.getWritableModel();

        depositPid = job.getDepositPID();
        storageLocPath = tmpFolder.newFolder("storageLoc").toPath();
        depBag = model.createBag(depositPid.getRepositoryPath());
        depBag.addProperty(Cdr.storageLocation, LOC1_ID);

        // set up mock for premis uri
        destinationObj = mock(BinaryObject.class);
        when(repoObjLoader.getBinaryObject(any(PID.class))).thenReturn(destinationObj);
        desitnationUri = URI.create(FILE_SCHEME + LOC1_ID + "/event_log");
        when(destinationObj.getContentUri()).thenReturn(desitnationUri);

        Map<String, String> depositStatus = new HashMap<>();
        depositStatus.put(DepositField.excludeDepositRecord.name(), "false");
        when(depositStatusFactory.get(anyString())).thenReturn(depositStatus);
    }

    @Test
    public void registerSingleFileToLongleaf() throws Exception {
        Bag workBag = addContainerObject(depBag, Cdr.Work);
        Resource fileResc = addFileObject(workBag, FILE_CONTENT1, true);
        fileResc.addProperty(CdrDeposit.storageUri, FILE_SCHEME + LOC2_ID + "/original_file");
        workBag.addProperty(Cdr.primaryObject, fileResc);

        job.closeModel();

        job.run();

        String output = FileUtils.readFileToString(new File(outputPath), StandardCharsets.UTF_8);

        assertRegisterCalled(LOC1_ID + "/event_log", 3, output); // bag, work, file
        assertRegisterCalled(LOC2_ID + "/original_file --checksums \"md5:" + MD5 + "\"", 1, output);
    }

    @Test
    public void registerWorkWithNoFilesToLongleaf() throws Exception {
        addContainerObject(depBag, Cdr.Work);

        job.closeModel();

        job.run();

        String output = FileUtils.readFileToString(new File(outputPath), StandardCharsets.UTF_8);

        assertRegisterCalled(LOC1_ID + "/event_log", 2, output);
    }

    @Test
    public void registerWorkWithMultipleFilesToLongleaf() throws Exception {
        Bag workBag = addContainerObject(depBag, Cdr.Work);
        Resource fileResc1 = addFileObject(workBag, FILE_CONTENT1, true);
        fileResc1.addProperty(CdrDeposit.storageUri, FILE_SCHEME + LOC2_ID + "/original_file");
        workBag.addProperty(Cdr.primaryObject, fileResc1);
        Resource fileResc2 = addFileObject(workBag, FILE_CONTENT2, true);
        fileResc2.addProperty(CdrDeposit.storageUri, FILE_SCHEME + LOC3_ID);

        job.closeModel();

        job.run();

        String output = FileUtils.readFileToString(new File(outputPath), StandardCharsets.UTF_8);

        assertRegisterCalled(LOC1_ID + "/event_log", 4, output); // bag, work, file1, file2
        assertRegisterCalled(LOC2_ID + "/original_file --checksums \"md5:" + MD5 + "\"", 1, output);
        assertRegisterCalled(LOC3_ID, 1, output);
    }

    @Test
    public void registerWorkWithMetadataFilesToLongleaf() throws Exception {
        Bag workBag = addContainerObject(depBag, Cdr.Work);
        Resource fileResc = addFileObject(workBag, FILE_CONTENT1, true);
        fileResc.addProperty(CdrDeposit.storageUri, FILE_SCHEME + LOC2_ID + "/original_file");
        workBag.addProperty(Cdr.primaryObject, fileResc);
        fileResc.addProperty(CdrDeposit.fitsStorageUri, FILE_SCHEME + LOC3_ID + "/techmd_fits");
        fileResc.addProperty(CdrDeposit.descriptiveStorageUri, FILE_SCHEME + LOC3_ID + "/md_descriptive");

        job.closeModel();

        job.run();

        String output = FileUtils.readFileToString(new File(outputPath), StandardCharsets.UTF_8);

        assertRegisterCalled(LOC1_ID + "/event_log", 3, output); // bag, work, file
        assertRegisterCalled(LOC2_ID + "/original_file --checksums \"md5:" + MD5 + "\"", 1, output);
        assertRegisterCalled(LOC3_ID + "/techmd_fits", 1, output);
        assertRegisterCalled(LOC3_ID + "/md_descriptive", 1, output);
    }

    @Test
    public void registerModsFilesToLongleaf() throws Exception {
        Bag workBag = addContainerObject(depBag, Cdr.Work);
        Resource fileResc = addFileObject(workBag, FILE_CONTENT1, true);
        fileResc.addProperty(CdrDeposit.descriptiveStorageUri, FILE_SCHEME + LOC3_ID);
        workBag.addProperty(Cdr.primaryObject, fileResc);

        job.closeModel();

        job.run();

        String output = FileUtils.readFileToString(new File(outputPath), StandardCharsets.UTF_8);

        assertRegisterCalled(LOC1_ID + "/event_log", 3, output); // bag, work, file
        assertRegisterCalled(LOC3_ID, 1, output);
    }

    @Test
    public void registerFitsExtractFilesToLongleaf() throws Exception {
        Bag workBag = addContainerObject(depBag, Cdr.Work);
        Resource fileResc = addFileObject(workBag, FILE_CONTENT1, true);
        fileResc.addProperty(CdrDeposit.fitsStorageUri, FILE_SCHEME + LOC3_ID);
        workBag.addProperty(Cdr.primaryObject, fileResc);

        job.closeModel();

        job.run();

        String output = FileUtils.readFileToString(new File(outputPath), StandardCharsets.UTF_8);

        assertRegisterCalled(LOC1_ID + "/event_log", 3, output); // bag, work, file
        assertRegisterCalled(LOC3_ID, 1, output);
    }

    @Test
    public void registerFilesWithoutStorageUris() throws Exception {
        Bag workBag = addContainerObject(depBag, Cdr.Work);
        Resource fileResc = addFileObject(workBag, FILE_CONTENT1, true);
        workBag.addProperty(Cdr.primaryObject, fileResc);

        job.closeModel();

        job.run();

        String output = FileUtils.readFileToString(new File(outputPath), StandardCharsets.UTF_8);

        assertRegisterCalled(LOC1_ID + "/event_log", 3, output);
    }

    private Resource addFileObject(Bag parent, String content, boolean withFits) throws Exception {
        PID objPid = makePid();
        Resource objResc = model.getResource(objPid.getRepositoryPath());
        objResc.addProperty(RDF.type, Cdr.FileObject);

        File originalFile = storageLocPath.resolve(objPid.getId() + ".txt").toFile();
        FileUtils.writeStringToFile(originalFile, content, "UTF-8");
        objResc.addProperty(CdrDeposit.stagingLocation, originalFile.toPath().toUri().toString());
        objResc.addProperty(CdrDeposit.md5sum, MD5);

        parent.add(objResc);
        return objResc;
    }

    private Bag addContainerObject(Bag parent, Resource type) {
        PID objPid = makePid();
        Bag objBag = model.createBag(objPid.getRepositoryPath());
        objBag.addProperty(RDF.type, type);
        parent.add(objBag);

        return objBag;
    }

    private String getLongleafScript(String outputPath) throws Exception {
        String scriptContent = "#!/usr/bin/env bash\necho $@ >> " + outputPath;
        File longleafScript = File.createTempFile("longleaf", ".sh");

        FileUtils.write(longleafScript, scriptContent, "UTF-8");

        longleafScript.deleteOnExit();

        Set<PosixFilePermission> ownerExecutable = PosixFilePermissions.fromString("r-x------");
        Files.setPosixFilePermissions(longleafScript.toPath(), ownerExecutable);

        return longleafScript.getAbsolutePath();
    }

    private void assertRegisterCalled(String expectedMessage, int expectedCount, String output) {
        String lines[] = output.split("\n");

        int count = 0;
        for (int i = 0; i < lines.length; i++) {
            if (("register -f " + expectedMessage).equals(lines[i])) {
                count++;
            }
        }

        assertEquals(expectedCount, count);
    }
}
