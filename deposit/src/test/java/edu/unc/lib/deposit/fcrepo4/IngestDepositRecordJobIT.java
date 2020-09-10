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
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.FileUtils.writeStringToFile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.net.URI;
import java.nio.file.Paths;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.Bag;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import edu.unc.lib.deposit.work.JobInterruptedException;
import edu.unc.lib.dl.fcrepo4.BinaryObject;
import edu.unc.lib.dl.fcrepo4.DepositRecord;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectFactory;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.model.AgentPids;
import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.rdf.CdrDeposit;
import edu.unc.lib.dl.rdf.Premis;
import edu.unc.lib.dl.rdf.Prov;
import edu.unc.lib.dl.util.PackagingType;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositField;
import edu.unc.lib.dl.util.SoftwareAgentConstants.SoftwareAgent;

/**
 * @author bbpennel
 *
 */
public class IngestDepositRecordJobIT extends AbstractFedoraDepositJobIT {
    private static final Logger log = getLogger(IngestDepositRecordJobIT.class);

    private static final String DEPOSITOR_NAME = "boxy_depositor";

    private static final String MANIFEST_BODY1 = "Manifested";
    private static final String MANIFEST_BODY2 = "Things";

    private IngestDepositRecordJob job;

    @Autowired
    private RepositoryObjectFactory repoObjFactory;
    @Autowired
    private RepositoryObjectLoader repoObjLoader;

    @Before
    public void init() throws Exception {

        job = new IngestDepositRecordJob();
        job.setJobUUID(jobUUID);
        job.setDepositUUID(depositUUID);
        job.setDepositDirectory(depositDir);
        job.setPremisLoggerFactory(premisLoggerFactory);
        setField(job, "pidMinter", pidMinter);
        setField(job, "depositModelManager", depositModelManager);
        setField(job, "depositsDirectory", depositsDirectory);
        setField(job, "depositStatusFactory", depositStatusFactory);
        setField(job, "jobStatusFactory", jobStatusFactory);
        setField(job, "repoObjLoader", repoObjLoader);
        setField(job, "repoObjFactory", repoObjFactory);
        setField(job, "transferService", binaryTransferService);
        setField(job, "locationManager", storageLocationManager);
        job.init();

        Model model = job.getWritableModel();
        Resource depositResc = model.getResource(depositPid.getRepositoryPath());
        depositResc.addProperty(Cdr.storageLocation, LOC1_ID);
        job.closeModel();
    }

    @Test
    public void depositWithManifests() throws Exception {
        depositStatusFactory.set(depositUUID, DepositField.packagingType, PackagingType.BAGIT.getUri());
        depositStatusFactory.set(depositUUID, DepositField.packageProfile, "no profile");
        depositStatusFactory.set(depositUUID, DepositField.depositorName, DEPOSITOR_NAME);

        Model model = job.getWritableModel();
        Bag depBag = model.createBag(depositPid.getRepositoryPath());
        URI manifestUri1 = Paths.get(depositDir.getAbsolutePath(), "manifest-md5.txt").toUri();
        URI manifestUri2 = Paths.get(depositDir.getAbsolutePath(), "bagit.txt").toUri();
        writeStringToFile(new File(manifestUri1), MANIFEST_BODY1, UTF_8);
        writeStringToFile(new File(manifestUri2), MANIFEST_BODY2, UTF_8);
        depBag.addProperty(CdrDeposit.storageUri, manifestUri1.toString());
        depBag.addProperty(CdrDeposit.storageUri, manifestUri2.toString());
        job.closeModel();

        job.run();

        DepositRecord record = repoObjLoader.getDepositRecord(depositPid);
        List<PID> manifestPids = record.listManifests();

        assertEquals(2, manifestPids.size());

        PID manifest1Pid = getPidBySuffix(manifestPids, "manifest-md5.txt");
        PID manifest2Pid = getPidBySuffix(manifestPids, "bagit.txt");

        BinaryObject manifest1Binary = repoObjLoader.getBinaryObject(manifest1Pid);
        BinaryObject manifest2Binary = repoObjLoader.getBinaryObject(manifest2Pid);

        assertEquals(MANIFEST_BODY1, IOUtils.toString(manifest1Binary.getBinaryStream(), UTF_8));
        assertEquals(MANIFEST_BODY2, IOUtils.toString(manifest2Binary.getBinaryStream(), UTF_8));

        // Verify that the ingestion event was created
        Model premisModel = record.getPremisLog().getEventsModel();
        List<Resource> eventRescs = premisModel.listResourcesWithProperty(Prov.generated).toList();
        assertEquals(1, eventRescs.size());
        Resource ingestEvent = eventRescs.get(0);
        assertTrue(ingestEvent.hasProperty(RDF.type, Premis.Ingestion));
        assertTrue(ingestEvent.hasLiteral(Premis.note, "ingested as format: "
                + PackagingType.BAGIT.getUri() + " with profile no profile"));

        Resource execAgent = ingestEvent.getProperty(Premis.hasEventRelatedAgentExecutor).getResource();
        assertEquals(AgentPids.forSoftware(SoftwareAgent.depositService).getRepositoryPath(),
                execAgent.getURI());

        Resource authgent = ingestEvent.getProperty(Premis.hasEventRelatedAgentAuthorizor).getResource();
        assertEquals(AgentPids.forPerson(DEPOSITOR_NAME).getRepositoryPath(),
                authgent.getURI());
    }

    @Test
    public void interruptTest() throws Exception {
        depositStatusFactory.set(depositUUID, DepositField.packagingType, PackagingType.BAGIT.getUri());
        depositStatusFactory.set(depositUUID, DepositField.packageProfile, "no profile");
        depositStatusFactory.set(depositUUID, DepositField.depositorName, DEPOSITOR_NAME);

        Model model = job.getWritableModel();
        Bag depBag = model.createBag(depositPid.getRepositoryPath());
        URI manifestUri1 = Paths.get(depositDir.getAbsolutePath(), "manifest-md5.txt").toUri();
        URI manifestUri2 = Paths.get(depositDir.getAbsolutePath(), "bagit.txt").toUri();
        writeStringToFile(new File(manifestUri1), MANIFEST_BODY1, UTF_8);
        writeStringToFile(new File(manifestUri2), MANIFEST_BODY2, UTF_8);
        depBag.addProperty(CdrDeposit.storageUri, manifestUri1.toString());
        depBag.addProperty(CdrDeposit.storageUri, manifestUri2.toString());
        job.closeModel();

        AtomicBoolean gotJobInterrupted = new AtomicBoolean(false);
        AtomicReference<Exception> otherException = new AtomicReference<>();
        Thread thread = new Thread(() -> {
            try {
                job.run();
            } catch (JobInterruptedException e) {
                gotJobInterrupted.set(true);
            } catch (Exception e) {
                otherException.set(e);
            }
        });
        thread.start();

        // Wait random amount of time and then interrupt thread if still alive
        Thread.sleep(50 + (long) new Random().nextFloat() * 600);
        if (thread.isAlive()) {
            thread.interrupt();
            thread.join();

            if (gotJobInterrupted.get()) {
                // success
            } else {
                if (otherException.get() != null) {
                    throw otherException.get();
                }
            }
        } else {
            log.warn("Job completed before interruption");
        }
    }

    // Repeat interruption test due to randomness of interruption timing
    @Test
    public void interruptTest2() throws Exception {
        interruptTest();
    }
    @Test
    public void interruptTest3() throws Exception {
        interruptTest();
    }

    private PID getPidBySuffix(List<PID> pids, String suffix) {
        return pids.stream()
                .filter(p -> p.getRepositoryPath().endsWith(suffix))
                .findFirst()
                .orElse(null);
    }
}
