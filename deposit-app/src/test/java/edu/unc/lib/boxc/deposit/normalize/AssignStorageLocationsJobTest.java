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
package edu.unc.lib.boxc.deposit.normalize;

import static edu.unc.lib.boxc.common.test.TestHelpers.setField;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.apache.jena.rdf.model.Bag;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositField;
import edu.unc.lib.boxc.deposit.normalize.AssignStorageLocationsJob;
import edu.unc.lib.boxc.deposit.work.JobFailedException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.persist.api.storage.StorageLocation;
import edu.unc.lib.boxc.persist.api.storage.StorageLocationManager;
import edu.unc.lib.boxc.persist.api.storage.UnknownStorageLocationException;

/**
 * @author bbpennel
 *
 */
public class AssignStorageLocationsJobTest extends AbstractNormalizationJobTest {

    private final static String LOC_ID = "loc1";

    @Mock
    private StorageLocationManager locationManager;
    @Mock
    private StorageLocation storageLoc;

    private AssignStorageLocationsJob job;

    private Map<String, String> depositStatus;

    private PID destPid;
    private Model depositModel;
    private Bag depBag;

    @Before
    public void init() throws Exception {
        destPid = makePid();

        when(locationManager.getStorageLocation(destPid)).thenReturn(storageLoc);
        when(storageLoc.getId()).thenReturn(LOC_ID);

        job = new AssignStorageLocationsJob(jobUUID, depositUUID);
        setField(job, "locationManager", locationManager);
        setField(job, "depositModelManager", depositModelManager);
        setField(job, "depositsDirectory", depositsDirectory);
        setField(job, "depositStatusFactory", depositStatusFactory);
        setField(job, "jobStatusFactory", jobStatusFactory);
        job.init();

        depositModel = job.getWritableModel();
        depBag = depositModel.createBag(depositPid.getRepositoryPath());

        depositStatus = new HashMap<>();
        depositStatus.put(DepositField.containerId.name(), destPid.getId());

        when(depositStatusFactory.get(anyString())).thenReturn(depositStatus);
    }

    @Test
    public void singleFileDepositUsingDefaultStorageLocation() throws Exception {
        Bag workBag = addContainerObject(depBag, Cdr.Work);
        Resource fileResc = addObject(workBag, Cdr.FileObject);
        workBag.addProperty(Cdr.primaryObject, fileResc);
        job.closeModel();

        job.run();

        Model model = job.getReadOnlyModel();
        Bag postDepBag = model.getBag(depBag);
        Bag postWorkBag = model.getBag(workBag);
        Resource postFileResc = model.getResource(fileResc.getURI());

        assertTrue(postDepBag.hasProperty(Cdr.storageLocation, LOC_ID));
        assertTrue(postWorkBag.hasProperty(Cdr.storageLocation, LOC_ID));
        assertTrue(postFileResc.hasProperty(Cdr.storageLocation, LOC_ID));
    }

    @Test
    public void allObjectTypesDeposit() throws Exception {
        Bag collBag = addContainerObject(depBag, Cdr.Collection);
        Bag folderBag = addContainerObject(collBag, Cdr.Folder);
        Bag workBag = addContainerObject(folderBag, Cdr.Work);
        Resource fileResc = addObject(workBag, Cdr.FileObject);
        workBag.addProperty(Cdr.primaryObject, fileResc);
        job.closeModel();

        job.run();

        Model model = job.getReadOnlyModel();
        Bag postDepBag = model.getBag(depBag);
        Bag postCollBag = model.getBag(collBag);
        Bag postFolderBag = model.getBag(folderBag);
        Bag postWorkBag = model.getBag(workBag);
        Resource postFileResc = model.getResource(fileResc.getURI());

        assertTrue(postDepBag.hasProperty(Cdr.storageLocation, LOC_ID));
        assertTrue(postCollBag.hasProperty(Cdr.storageLocation, LOC_ID));
        assertTrue(postFolderBag.hasProperty(Cdr.storageLocation, LOC_ID));
        assertTrue(postWorkBag.hasProperty(Cdr.storageLocation, LOC_ID));
        assertTrue(postFileResc.hasProperty(Cdr.storageLocation, LOC_ID));
    }

    /*
     * Test that a object in a deposit without a resource type does not get the
     * storage location added, since this has historically meant it was a binary
     */
    @Test
    public void resourceWithNoTypeDeposit() throws Exception {
        Bag folderBag = addContainerObject(depBag, Cdr.Folder);
        Resource untypedResc = addObject(folderBag, null);
        job.closeModel();

        job.run();

        Model model = job.getReadOnlyModel();
        Bag postDepBag = model.getBag(depBag);
        Bag postFolderBag = model.getBag(folderBag);
        Resource postUntypedResc = model.getResource(untypedResc.getURI());

        assertTrue(postDepBag.hasProperty(Cdr.storageLocation, LOC_ID));
        assertTrue(postFolderBag.hasProperty(Cdr.storageLocation, LOC_ID));
        assertFalse(postUntypedResc.hasProperty(Cdr.storageLocation));
    }

    @Test(expected = UnknownStorageLocationException.class)
    public void depositNoDefaultStorageLocation() throws Exception {
        when(locationManager.getStorageLocation(destPid)).thenReturn(null);

        addContainerObject(depBag, Cdr.Folder);
        job.closeModel();

        job.run();
    }

    @Test
    public void depositWithProvidedStorageLocation() throws Exception {
        String loc2Id = "loc2";
        StorageLocation storageLoc2 = mock(StorageLocation.class);
        when(storageLoc2.getId()).thenReturn(loc2Id);

        when(locationManager.getStorageLocationById(loc2Id)).thenReturn(storageLoc2);
        when(locationManager.listAvailableStorageLocations(destPid))
                .thenReturn(asList(storageLoc, storageLoc2));

        // Provide a specific storage location to use which is different from the default
        depositStatus.put(DepositField.storageLocation.name(), loc2Id);

        Bag folderBag = addContainerObject(depBag, Cdr.Folder);
        job.closeModel();

        job.run();

        Model model = job.getReadOnlyModel();
        Bag postDepBag = model.getBag(depBag);
        Bag postFolderBag = model.getBag(folderBag);

        assertTrue(postDepBag.hasProperty(Cdr.storageLocation, loc2Id));
        assertTrue(postFolderBag.hasProperty(Cdr.storageLocation, loc2Id));
    }

    @Test(expected = UnknownStorageLocationException.class)
    public void depositWithInvalidStorageLocation() throws Exception {
        String providedLoc = "somestorage";

        // Provide a specific storage location to use which is different from the default
        depositStatus.put(DepositField.storageLocation.name(), providedLoc);

        addContainerObject(depBag, Cdr.Folder);
        job.closeModel();

        job.run();
    }

    @Test(expected = JobFailedException.class)
    public void depositWithProvidedStorageLocationNoAvailableForDest() throws Exception {
        String loc2Id = "loc2";
        StorageLocation storageLoc2 = mock(StorageLocation.class);
        when(storageLoc2.getId()).thenReturn(loc2Id);

        when(locationManager.getStorageLocationById(loc2Id)).thenReturn(storageLoc2);
        // The second storage location is not in the list of available locations
        when(locationManager.listAvailableStorageLocations(destPid))
                .thenReturn(asList(storageLoc));

        depositStatus.put(DepositField.storageLocation.name(), loc2Id);

        addContainerObject(depBag, Cdr.Folder);
        job.closeModel();

        job.run();
    }

    private Bag addContainerObject(Bag parent, Resource type) {
        PID objPid = makePid();
        Bag objBag = depositModel.createBag(objPid.getRepositoryPath());
        objBag.addProperty(RDF.type, type);
        parent.add(objBag);

        return objBag;
    }

    private Resource addObject(Bag parent, Resource type) {
        PID objPid = makePid();
        Resource objResc = depositModel.getResource(objPid.getRepositoryPath());
        if (type != null) {
            objResc.addProperty(RDF.type, type);
        }
        parent.add(objResc);

        return objResc;
    }
}
