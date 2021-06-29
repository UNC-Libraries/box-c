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

import static edu.unc.lib.boxc.common.test.TestHelpers.setField;
import static edu.unc.lib.dl.acl.util.AccessPrincipalConstants.AUTHENTICATED_PRINC;
import static edu.unc.lib.dl.acl.util.AccessPrincipalConstants.PUBLIC_PRINC;
import static edu.unc.lib.dl.acl.util.UserRole.none;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.apache.jena.rdf.model.Bag;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.junit.Before;
import org.junit.Test;

import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositField;

/**
 * @author lfarrell
 */
public class StaffOnlyPermissionJobTest extends AbstractDepositJobTest {
    private final static String LOC1_ID = "loc1";

    private StaffOnlyPermissionJob job;
    private Model model;
    private Bag depBag;

    @Before
    public void setup() throws Exception {
        job = new StaffOnlyPermissionJob();

        job.setJobUUID(jobUUID);
        job.setDepositUUID(depositUUID);
        job.setDepositDirectory(depositDir);
        setField(job, "depositModelManager", depositModelManager);
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

        Bag objBag = addWorkObject(depBag);
        job.closeModel();

        job.run();

        model = job.getReadOnlyModel();
        assertAssignedNoneRole(objBag);
        assertPrimaryObjectNotNoneRole(objBag);
    }

    @Test
    public void testNoStaffOnly() throws Exception {
        Map<String, String> depositStatus = new HashMap<>();
        depositStatus.put(DepositField.staffOnly.name(), "false");
        when(depositStatusFactory.get(eq(depositUUID))).thenReturn(depositStatus);

        Bag objBag = addWorkObject(depBag);
        job.closeModel();

        job.run();

        model = job.getReadOnlyModel();
        assertNotAssignedNoneRole(objBag);
        assertPrimaryObjectNotNoneRole(objBag);
    }

    @Test
    public void testStaffOnlyMultipleTopLevel() throws Exception {
        Map<String, String> depositStatus = new HashMap<>();
        depositStatus.put(DepositField.staffOnly.name(), "true");
        when(depositStatusFactory.get(eq(depositUUID))).thenReturn(depositStatus);

        Bag objBag = addWorkObject(depBag);
        Bag objBag2 = addWorkObject(depBag);

        job.closeModel();

        job.run();

        model = job.getReadOnlyModel();
        assertAssignedNoneRole(objBag);
        assertPrimaryObjectNotNoneRole(objBag);
        assertAssignedNoneRole(objBag2);
        assertPrimaryObjectNotNoneRole(objBag2);
    }

    private void assertAssignedNoneRole(Resource objResc) {
        assertTrue(objResc.hasProperty(none.getProperty(), AUTHENTICATED_PRINC));
        assertTrue(objResc.hasProperty(none.getProperty(), PUBLIC_PRINC));
    }

    private void assertNotAssignedNoneRole(Resource objResc) {
        assertFalse(objResc.hasProperty(none.getProperty(), AUTHENTICATED_PRINC));
        assertFalse(objResc.hasProperty(none.getProperty(), PUBLIC_PRINC));
    }

    private void assertPrimaryObjectNotNoneRole(Resource workResc) {
        Resource primary = workResc.getPropertyResourceValue(Cdr.primaryObject);
        assertFalse(primary.hasProperty(none.getProperty()));
    }

    private Bag addWorkObject(Bag parent) throws Exception {
        PID objPid = makePid();
        Bag objBag = model.createBag(objPid.getRepositoryPath());
        objBag.addProperty(RDF.type, Cdr.Work);

        Resource fileResc = addFileObject(objBag);
        objBag.addProperty(Cdr.primaryObject, fileResc);

        parent.add(objBag);
        return objBag;
    }

    private Resource addFileObject(Bag parent) throws Exception {
        PID objPid = makePid();
        Resource objResc = model.getResource(objPid.getRepositoryPath());
        objResc.addProperty(RDF.type, Cdr.FileObject);

        parent.add(objResc);
        return objResc;
    }
}