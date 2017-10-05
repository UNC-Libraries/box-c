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
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.tdb.TDBFactory;
import org.apache.jena.vocabulary.RDF;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import edu.unc.lib.dl.fcrepo4.DepositRecord;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.PremisEventObject;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectFactory;
import edu.unc.lib.dl.fcrepo4.RepositoryPathConstants;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.rdf.DcElements;
import edu.unc.lib.dl.util.PackagingType;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositField;

/**
 *
 * @author bbpennel
 *
 */
public class IngestDepositRecordJobTest extends AbstractDepositJobTest {

    @Mock
    private DepositRecord depositRecord;

    private IngestDepositRecordJob job;

    @Mock
    private RepositoryObjectFactory repoObjFactory;

    @Before
    public void setup() throws Exception {
        when(repoObjFactory.createDepositRecord(any(PID.class), any(Model.class)))
                .thenReturn(depositRecord);
        PID eventPid = makePid("content");
        when(pidMinter.mintPremisEventPid(any(PID.class))).thenReturn(eventPid);
        when(depositRecord.addPremisEvents(anyListOf(PremisEventObject.class))).thenReturn(depositRecord);

        when(premisEventBuilder.addAuthorizingAgent(anyString())).thenReturn(premisEventBuilder);
    }

    private void initializeJob(String depositUUID, String packagePath, String n3File) throws Exception {
        depositPid = PIDs.get(RepositoryPathConstants.DEPOSIT_RECORD_BASE + "/" + depositUUID);

        depositDir = new File(depositsDirectory, depositUUID);
        depositDir.mkdir();
        FileUtils.copyDirectory(new File(packagePath), depositDir);

        Dataset dataset = TDBFactory.createDataset();

        job = new IngestDepositRecordJob();
        job.setDepositUUID(depositUUID);
        setField(job, "dataset", dataset);
        setField(job, "depositsDirectory", depositsDirectory);
        setField(job, "depositStatusFactory", depositStatusFactory);
        job.setPremisLoggerFactory(premisLoggerFactory);
        setField(job, "pidMinter", pidMinter);
        setField(job, "repoObjFactory", repoObjFactory);

        job.init();

        Model model = job.getWritableModel();
        model.read(new File(n3File).getAbsolutePath());
        job.closeModel();
    }

    @Test
    public void testProquestAggregateBag() throws Exception {

        initializeJob(depositUUID, "src/test/resources/ingest-bags/fcrepo4/proquest-bag",
                "src/test/resources/ingest-bags/fcrepo4/proquest-bag/everything.n3");

        Map<String, String> depositStatus = new HashMap<>();
        depositStatus.put(DepositField.fileName.name(), "proquest-bag");
        depositStatus.put(DepositField.packagingType.name(), PackagingType.PROQUEST_ETD.getUri());

        when(depositStatusFactory.get(eq(depositUUID))).thenReturn(depositStatus);

        job.run();

        // Verifying that the deposit record was created correctly
        Resource depositResc = getAIPResource();
        assertEquals(PackagingType.PROQUEST_ETD.getUri(), depositResc.getProperty(Cdr.depositPackageType).getString());
        assertEquals("Deposit record for proquest-bag", depositResc.getProperty(DcElements.title).getString());
        assertEquals(Cdr.DepositRecord, depositResc.getProperty(RDF.type).getObject());

        verify(depositRecord).addPremisEvents(anyListOf(PremisEventObject.class));
    }

    @Test
    public void testWithManifests() throws Exception {

        initializeJob(depositUUID, "src/test/resources/paths/valid-bag",
                "src/test/resources/ingest-bags/fcrepo4/valid-bag.n3");

        Map<String, String> depositStatus = new HashMap<>();
        depositStatus.put(DepositField.fileName.name(), "valid-bag");
        depositStatus.put(DepositField.packagingType.name(), PackagingType.BAGIT.getUri());
        when(depositStatusFactory.get(eq(depositUUID))).thenReturn(depositStatus);
        List<String> manifestPaths = Arrays.asList(
                Paths.get(depositDir.getAbsolutePath(), "manifest-md5.txt").toString(),
                Paths.get(depositDir.getAbsolutePath(), "bagit.txt").toString());
        when(depositStatusFactory.getManifestURIs(eq(depositUUID))).thenReturn(manifestPaths);

        job.run();

        // Check that the deposit record model was given the correct properties
        Resource depositResc = getAIPResource();
        assertEquals(PackagingType.BAGIT.getUri(), depositResc.getProperty(Cdr.depositPackageType).getString());
        assertEquals(Cdr.DepositRecord, depositResc.getProperty(RDF.type).getObject());

        // Check that the deposit record was created
        verify(repoObjFactory).createDepositRecord(eq(depositPid), any(Model.class));

        // Check that all manifests were added to the record
        ArgumentCaptor<File> manifestCaptor = ArgumentCaptor.forClass(File.class);
        verify(depositRecord, times(2)).addManifest(manifestCaptor.capture(), anyString());

        List<File> manifests = manifestCaptor.getAllValues();

        assertTrue(manifests.contains(new File(depositDir, "manifest-md5.txt")));
        assertTrue(manifests.contains(new File(depositDir, "bagit.txt")));
    }

    private Resource getAIPResource() throws Exception {
        ArgumentCaptor<Model> depositRecordModelCaptor = ArgumentCaptor.forClass(Model.class);
        verify(repoObjFactory).createDepositRecord(any(PID.class), depositRecordModelCaptor.capture());
        Model recordModel = depositRecordModelCaptor.getValue();

        return recordModel.getResource(depositPid.getRepositoryPath());
    }
}
