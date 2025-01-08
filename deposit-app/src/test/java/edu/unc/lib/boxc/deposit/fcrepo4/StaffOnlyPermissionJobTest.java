package edu.unc.lib.boxc.deposit.fcrepo4;

import static edu.unc.lib.boxc.common.test.TestHelpers.setField;
import static edu.unc.lib.boxc.auth.api.AccessPrincipalConstants.AUTHENTICATED_PRINC;
import static edu.unc.lib.boxc.auth.api.AccessPrincipalConstants.PUBLIC_PRINC;
import static edu.unc.lib.boxc.auth.api.UserRole.none;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.apache.jena.rdf.model.Bag;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositField;
import edu.unc.lib.boxc.deposit.fcrepo4.StaffOnlyPermissionJob;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.rdf.Cdr;

/**
 * @author lfarrell
 */
public class StaffOnlyPermissionJobTest extends AbstractDepositJobTest {
    private final static String LOC1_ID = "loc1";

    private StaffOnlyPermissionJob job;
    private Model model;
    private Bag depBag;

    @BeforeEach
    public void setup() throws Exception {
        job = new StaffOnlyPermissionJob();

        job.setJobUUID(jobUUID);
        job.setDepositUUID(depositUUID);
        setField(job, "depositsDirectory", depositsDirectory);
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