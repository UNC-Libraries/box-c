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

import static edu.unc.lib.dl.acl.util.AccessPrincipalConstants.AUTHENTICATED_PRINC;
import static edu.unc.lib.dl.acl.util.AccessPrincipalConstants.PUBLIC_PRINC;
import static edu.unc.lib.dl.acl.util.UserRole.none;
import static edu.unc.lib.dl.test.TestHelpers.setField;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import edu.unc.lib.dl.persist.api.storage.StorageLocationManager;
import edu.unc.lib.dl.persist.services.storage.StorageLocationTestHelper;
import edu.unc.lib.dl.rdf.Cdr;
import org.apache.jena.rdf.model.Bag;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.junit.Before;
import org.junit.Test;

import edu.unc.lib.dl.util.RedisWorkerConstants.DepositField;
import edu.unc.lib.dl.fedora.PID;

/**
 * @author lfarrell
 */
public class StaffOnlyPermissionJobTest extends AbstractDepositJobTest {
    private final static String LOC1_ID = "loc1";

    private StaffOnlyPermissionJob job;
    private Model model;
    private StorageLocationManager locationManager;
    private Bag depBag;
    private StorageLocationTestHelper locTestHelper;

    @Before
    public void setup() throws Exception {
        locTestHelper = new StorageLocationTestHelper();
        locationManager = locTestHelper.createLocationManager(null);

        job = new StaffOnlyPermissionJob();

        job.setJobUUID(jobUUID);
        job.setDepositUUID(depositUUID);
        job.setDepositDirectory(depositDir);
        setField(job, "locationManager", locationManager);
        setField(job, "dataset", dataset);
        setField(job, "depositsDirectory", depositsDirectory);
        setField(job, "depositStatusFactory", depositStatusFactory);
        setField(job, "jobStatusFactory", jobStatusFactory);
        job.init();

        model = job.getWritableModel();
        depBag = model.createBag(depositPid.getRepositoryPath());
        depBag.addProperty(Cdr.storageLocation, LOC1_ID);
    }

    @Test
    public void testStaffOnly() throws Exception {
        Map<String, String> depositStatus = new HashMap<>();
        depositStatus.put(DepositField.staffOnly.name(), "true");
        when(depositStatusFactory.get(eq(depositUUID))).thenReturn(depositStatus);

        PID objPid = makePid();
        Bag objBag = model.createBag(objPid.getRepositoryPath());
        objBag.addProperty(RDF.type, Cdr.Work);

        Resource fileResc = addFileObject(objBag);
        objBag.addProperty(Cdr.primaryObject, fileResc);

        depBag.add(objBag);
        job.closeModel();

        job.run();

        model = job.getReadOnlyModel();
        Resource roles = model.getResource(objPid.getRepositoryPath());

        assertTrue(roles.hasProperty(none.getProperty(), AUTHENTICATED_PRINC));
        assertTrue(roles.hasProperty(none.getProperty(), PUBLIC_PRINC));

        assertFalse(fileResc.hasProperty(none.getProperty()));
    }

    @Test
    public void testNoStaffOnly() throws Exception {
        Map<String, String> depositStatus = new HashMap<>();
        depositStatus.put(DepositField.staffOnly.name(), "false");
        when(depositStatusFactory.get(eq(depositUUID))).thenReturn(depositStatus);

        PID objPid = makePid();
        Bag objBag = model.createBag(objPid.getRepositoryPath());
        objBag.addProperty(RDF.type, Cdr.Work);

        Resource fileResc = addFileObject(objBag);
        objBag.addProperty(Cdr.primaryObject, fileResc);

        depBag.add(objBag);
        job.closeModel();

        job.run();

        model = job.getReadOnlyModel();
        Resource roles = model.getResource(objPid.getRepositoryPath());

        assertFalse(roles.hasProperty(none.getProperty()));
        assertFalse(roles.hasProperty(none.getProperty()));

        assertFalse(fileResc.hasProperty(none.getProperty()));
    }

    private Resource addFileObject(Bag parent) throws Exception {
        PID objPid = makePid();
        Resource objResc = model.getResource(objPid.getRepositoryPath());
        objResc.addProperty(RDF.type, Cdr.FileObject);

        parent.add(objResc);
        return objResc;
    }
}