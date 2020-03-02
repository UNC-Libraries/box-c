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

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.rdf.CdrDeposit;
import org.apache.commons.io.FileUtils;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.*;
import org.apache.jena.tdb.TDBFactory;
import org.apache.jena.vocabulary.RDF;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Path;

import static edu.unc.lib.dl.test.TestHelpers.setField;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class RegisterToLongleafJobTest extends AbstractDepositJobTest {

    private final static String LOC1_ID = "loc1";
    private final static String LOC2_ID = "loc2";
    private final static String LOC3_ID = "loc3";
    private final static String FILE_CONTENT1 = "Some content";
    private final static String FILE_CONTENT2 = "Other stuff";
    private final static String MD5 = "MD5 checksum";

    private RegisterToLongleafJob job;
    private PID depositPid;
    private Bag depBag;
    private Model model;
    private Path storageLocPath;
    private String outputPath;
    private String logPath;
    private String longleafScript;

    @Before
    public void init() throws Exception {
        Dataset dataset = TDBFactory.createDataset();

        job = new RegisterToLongleafJob();
        job.setJobUUID(jobUUID);
        job.setDepositUUID(depositUUID);
        job.setDepositDirectory(depositDir);
        setField(job, "dataset", dataset);

        outputPath = createOutputFile();
        logPath = createOutputFile();
        longleafScript = getLongleafScript(outputPath);
        job.setLongleafBaseCommand(longleafScript);
        job.setLongleafLogPath(logPath);

        job.init();

        // Get a writeable model
        model = job.getWritableModel();

        depositPid = job.getDepositPID();
        storageLocPath = tmpFolder.newFolder("storageLoc").toPath();
        depBag = model.createBag(depositPid.getRepositoryPath());
        depBag.addProperty(Cdr.storageLocation, LOC1_ID);
    }

    /**
     * Test that a single file in an ingested work is registered to longleaf
     */
    @Test
    public void registerSingleFileToLongleaf() throws Exception {
        Bag workBag = addContainerObject(depBag, Cdr.Work);
        Resource fileResc = addFileObject(workBag, FILE_CONTENT1, true);
        fileResc.addProperty(CdrDeposit.storageUri, LOC2_ID);
        workBag.addProperty(Cdr.primaryObject, fileResc);

        job.closeModel();

        job.run();

        String registrationArguments = "register -f " + LOC2_ID + " --checksums 'md5:" + MD5 + "' --force";

        File file = new File(outputPath);
        BufferedReader br = new BufferedReader(new FileReader(file));
        String output = br.readLine();

        assertEquals(registrationArguments, output);
    }

    /**
     * Test that longleaf registration is not triggered for works with no files
     */
    @Test
    public void registerWorkWithNoFilesToLongleaf() throws Exception {
        addContainerObject(depBag, Cdr.Work);

        job.closeModel();

        job.run();

        File file = new File(outputPath);
        BufferedReader br = new BufferedReader(new FileReader(file));
        String output = br.readLine();

        assertNull(output);
    }

    /**
     * Test that multiple files in an ingested work are registered to longleaf
     */
    @Test
    public void registerWorkWithMultipleFilesToLongleaf() throws Exception {
        Bag workBag = addContainerObject(depBag, Cdr.Work);
        Resource fileResc1 = addFileObject(workBag, FILE_CONTENT1, true);
        fileResc1.addProperty(CdrDeposit.storageUri, LOC2_ID);
        workBag.addProperty(Cdr.primaryObject, fileResc1);
        Resource fileResc2 = addFileObject(workBag, FILE_CONTENT2, true);
        fileResc2.addProperty(CdrDeposit.storageUri, LOC3_ID);

        job.closeModel();

        job.run();

        String registrationArguments1 = "register -f " + LOC2_ID + " --checksums 'md5:" + MD5 + "' --force";
        String registrationArguments2 = "register -f " + LOC3_ID + " --checksums 'md5:" + MD5 + "' --force";

        File file = new File(outputPath);
        BufferedReader br = new BufferedReader(new FileReader(file));
        String firstLine = br.readLine();
        String secondLine = br.readLine();

        assertEquals(registrationArguments1, firstLine);
        assertEquals(registrationArguments2, secondLine);
    }

    /**
     * Test that a mods file in an ingested work is registered to longleaf
     */
    @Test
    public void registerModsFilesToLongleaf() throws Exception {
        Bag workBag = addContainerObject(depBag, Cdr.Work);
        Resource fileResc = addFileObject(workBag, FILE_CONTENT1, true);
        fileResc.addProperty(CdrDeposit.descriptiveStorageUri, LOC3_ID);
        workBag.addProperty(Cdr.primaryObject, fileResc);

        job.closeModel();

        job.run();

        String registrationArguments = "register -f " + LOC3_ID + " --checksums 'md5:" + MD5 + "' --force";

        File file = new File(outputPath);
        BufferedReader br = new BufferedReader(new FileReader(file));
        String output = br.readLine();

        assertEquals(registrationArguments, output);
    }

    /**
     * Test that a file in a non-ingested work is not registered to longleaf
     */
    @Test
    public void registerFilesWhichHaveNotBeenIngestedToLongleaf() throws Exception {
        Bag workBag = addContainerObject(depBag, Cdr.Work);
        Resource fileResc = addFileObject(workBag, FILE_CONTENT1, true);
        workBag.addProperty(Cdr.primaryObject, fileResc);

        job.closeModel();

        job.run();

        File file = new File(outputPath);
        BufferedReader br = new BufferedReader(new FileReader(file));
        String output = br.readLine();

        assertNull(output);
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

    private String createOutputFile() throws Exception {
        File outputFile = File.createTempFile("output", ".txt");
        outputFile.deleteOnExit();

        return outputFile.getAbsolutePath();
    }

    private String getLongleafScript(String outputPath) throws Exception {
        String scriptContent = "#!/usr/bin/env bash\necho $@ >> " + outputPath;
        File longleafScript = File.createTempFile("longleaf", ".sh");

        FileUtils.write(longleafScript, scriptContent, "UTF-8");

        longleafScript.deleteOnExit();

        Runtime.getRuntime().exec("chmod u+x " + longleafScript.getAbsolutePath());

        return longleafScript.getAbsolutePath();
    }
}
