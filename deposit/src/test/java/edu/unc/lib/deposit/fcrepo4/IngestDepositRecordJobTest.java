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

import static edu.unc.lib.dl.persist.services.storage.StorageLocationTestHelper.LOC1_ID;
import static edu.unc.lib.dl.test.TestHelpers.setField;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.net.URI;
import java.nio.file.Paths;
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
import org.mockito.Captor;
import org.mockito.Mock;

import edu.unc.lib.dl.event.PremisLogger;
import edu.unc.lib.dl.fcrepo4.DepositRecord;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectFactory;
import edu.unc.lib.dl.fcrepo4.RepositoryPathConstants;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.persist.api.storage.StorageLocation;
import edu.unc.lib.dl.persist.api.storage.StorageLocationManager;
import edu.unc.lib.dl.persist.api.transfer.BinaryTransferService;
import edu.unc.lib.dl.persist.api.transfer.BinaryTransferSession;
import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.rdf.CdrDeposit;
import edu.unc.lib.dl.util.DepositConstants;
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
    @Mock
    private PremisLogger premisLogger;
    @Captor
    private ArgumentCaptor<URI> uriCaptor;

    @Mock
    private BinaryTransferService binaryTransferService;
    @Mock
    private StorageLocationManager storageLocationManager;
    @Mock
    private StorageLocation storageLocation;
    @Mock
    private BinaryTransferSession mockTransferSession;

    @Before
    public void setup() throws Exception {
        when(repoObjFactory.createDepositRecord(any(PID.class), any(Model.class)))
                .thenReturn(depositRecord);
        PID eventPid = makePid("content");
        when(pidMinter.mintPremisEventPid(any(PID.class))).thenReturn(eventPid);

        when(premisEventBuilder.addAuthorizingAgent(anyString())).thenReturn(premisEventBuilder);

        when(storageLocationManager.getStorageLocationById(anyString())).thenReturn(storageLocation);
        when(binaryTransferService.getSession(any(StorageLocation.class))).thenReturn(mockTransferSession);
    }

    private void initializeJob(String depositUUID, String packagePath, String n3File) throws Exception {
        depositPid = PIDs.get(RepositoryPathConstants.DEPOSIT_RECORD_BASE + "/" + depositUUID);
        when(depositRecord.getPid()).thenReturn(depositPid);
        when(depositRecord.getPremisLog()).thenReturn(premisLogger);

        depositDir = new File(depositsDirectory, depositUUID);
        depositDir.mkdir();
        FileUtils.copyDirectory(new File(packagePath), depositDir);

        File eventsDir = new File(depositDir, DepositConstants.EVENTS_DIR);
        eventsDir.mkdir();
        FileUtils.writeStringToFile(new File(eventsDir, depositPid.getUUID() + ".nt"), "loggin", "UTF-8");

        Dataset dataset = TDBFactory.createDataset();

        job = new IngestDepositRecordJob();
        job.setDepositUUID(depositUUID);
        setField(job, "dataset", dataset);
        setField(job, "depositsDirectory", depositsDirectory);
        setField(job, "depositStatusFactory", depositStatusFactory);
        job.setPremisLoggerFactory(premisLoggerFactory);
        setField(job, "pidMinter", pidMinter);
        setField(job, "repoObjFactory", repoObjFactory);
        setField(job, "transferService", binaryTransferService);
        setField(job, "locationManager", storageLocationManager);

        job.init();

        Model model = job.getWritableModel();
        model.read(new File(n3File).getAbsolutePath());
        job.closeModel();
    }

    @Test
    public void testWithManifests() throws Exception {

        initializeJob(depositUUID, "src/test/resources/paths/valid-bag",
                "src/test/resources/ingest-bags/fcrepo4/valid-bag.n3");
        Model depositModel = job.getWritableModel();

        Map<String, String> depositStatus = new HashMap<>();
        depositStatus.put(DepositField.fileName.name(), "valid-bag");
        depositStatus.put(DepositField.packagingType.name(), PackagingType.BAGIT.getUri());
        when(depositStatusFactory.get(eq(depositUUID))).thenReturn(depositStatus);

        Resource resc = depositModel.getResource(depositPid.getRepositoryPath());
        URI manifestUri1 = Paths.get(depositDir.getAbsolutePath(), "manifest-md5.txt").toUri();
        URI manifestUri2 = Paths.get(depositDir.getAbsolutePath(), "bagit.txt").toUri();
        resc.addProperty(CdrDeposit.storageUri, manifestUri1.toString());
        resc.addProperty(CdrDeposit.storageUri, manifestUri2.toString());
        resc.addProperty(Cdr.storageLocation, LOC1_ID);

        job.closeModel();

        job.run();

        // Check that the deposit record model was given the correct properties
        Resource depositResc = getAIPResource();
        assertEquals(PackagingType.BAGIT.getUri(), depositResc.getProperty(Cdr.depositPackageType).getString());
        assertEquals(Cdr.DepositRecord, depositResc.getProperty(RDF.type).getObject());

        // Check that the deposit record was created
        verify(repoObjFactory).createDepositRecord(eq(depositPid), any(Model.class));

        // Check that all manifests were added to the record
        verify(depositRecord, times(2)).addManifest(uriCaptor.capture(), anyString());

        List<URI> manifests = uriCaptor.getAllValues();

        assertTrue(manifests.contains(manifestUri1));
        assertTrue(manifests.contains(manifestUri2));
    }

    private Resource getAIPResource() throws Exception {
        ArgumentCaptor<Model> depositRecordModelCaptor = ArgumentCaptor.forClass(Model.class);
        verify(repoObjFactory).createDepositRecord(any(PID.class), depositRecordModelCaptor.capture());
        Model recordModel = depositRecordModelCaptor.getValue();

        return recordModel.getResource(depositPid.getRepositoryPath());
    }
}
