package edu.unc.lib.boxc.deposit.impl.model;

import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author bbpennel
 */
public class DepositModelManagerTest {
    private static final String DEPOSIT_ID = "0f71ea28-b44e-41f4-a7d6-6666da8ad140";
    private static final String DEPOSIT2_ID = "7ab3c4d5-1234-5678-abcd-ef0123456789";
    private static final String SUBJECT_URI = "http://example.com/subject/1";
    private static final String PROPERTY_URI = "http://example.com/prop/label";
    private static final String VALUE = "test value";

    @TempDir
    public Path tmpFolder;

    private DepositModelManager manager;
    private PID depositPid;
    private PID deposit2Pid;

    @BeforeEach
    void setUp() {
        manager = DepositModelManager.inMemoryManager();
        depositPid = PIDs.get(DEPOSIT_ID);
        deposit2Pid = PIDs.get(DEPOSIT2_ID);
    }

    @AfterEach
    void tearDown() {
        manager.close();
    }

    @Test
    void testGetWriteModel_WrittenDataPersistedAfterCommit() {
        Model writeModel = manager.getWriteModel(depositPid);
        assertNotNull(writeModel, "Write model should not be null");
        addTriple(writeModel);
        manager.commit();

        Model readModel = manager.getReadModel(depositPid);
        assertTrue(containsTriple(readModel), "Written triple should be readable after commit");
        manager.end();
    }

    @Test
    void testGetWriteModel_MultipleDepositsIndependent() {
        Model model1 = manager.getWriteModel(depositPid);
        addTriple(model1);
        manager.commit();

        Model readModel2 = manager.getReadModel(deposit2Pid);
        assertFalse(containsTriple(readModel2), "Second deposit model should not contain data from first deposit");
        manager.end();
    }

    @Test
    void testGetReadModel_EmptyWhenNothingWritten() {
        Model readModel = manager.getReadModel(depositPid);
        assertNotNull(readModel, "Read model should not be null");
        assertTrue(readModel.isEmpty(), "New deposit model should be empty");
        manager.end();
    }

    @Test
    void testCommitNoArgs_WhenNotInTransaction_DoesNotThrow() {
        assertDoesNotThrow(() -> manager.commit(),
                "commit() should not throw when not in a transaction");
    }

    @Test
    void testCommitRunnable_PersistsChanges() {
        // Establish the named model, then get a reference while in a READ tx
        manager.getWriteModel(depositPid);
        manager.commit();

        // commit(Runnable) ends the read tx, starts write, runs runnable, commits, restarts read
        Model readModel = manager.getReadModel(depositPid);
        manager.commit(() -> addTriple(readModel));
        assertTrue(containsTriple(readModel), "Data should be persisted after commit with runnable");
        manager.end();

        Model readModel2 = manager.getReadModel(depositPid);
        assertTrue(containsTriple(readModel2), "Data should still be persisted in fresh model");
        manager.end();
    }

    @Test
    void testCommitRunnableNotInTx_PersistsChanges() {
        // Establish the named model
        manager.getWriteModel(depositPid);
        manager.commit();

        Model readModel = manager.getReadModel(depositPid);
        manager.end(); // end the read tx so we're not in a transaction before calling commit

        // commit(Runnable, false) does not assume or restore any surrounding transaction
        manager.commit(() -> addTriple(readModel), false);

        Model verifyModel = manager.getReadModel(depositPid);
        assertTrue(containsTriple(verifyModel), "Data should be persisted after commit(Runnable, false)");
        manager.end();
    }

    @Test
    void testCommitOrAbortFalse_CommitsChanges() {
        Model writeModel = manager.getWriteModel(depositPid);
        addTriple(writeModel);
        manager.commitOrAbort(false);

        Model readModel = manager.getReadModel(depositPid);
        assertTrue(containsTriple(readModel), "Data should be persisted when commitOrAbort is called with false");
        manager.end();
    }

    @Test
    void testCommitOrAbortTrue_AbortsChanges() {
        Model writeModel = manager.getWriteModel(depositPid);
        addTriple(writeModel);
        manager.commitOrAbort(true);

        Model readModel = manager.getReadModel(depositPid);
        assertFalse(containsTriple(readModel), "Data should NOT be persisted when commitOrAbort is called with true");
        manager.end();
    }

    @Test
    void testEnd_WhenNotInTransaction_DoesNotThrow() {
        assertDoesNotThrow(() -> manager.end(), "end() should not throw when not in a transaction");
    }

    @Test
    void testEnd_EndsActiveTransaction() {
        manager.getReadModel(depositPid);
        manager.end();
        // Should be able to start a fresh transaction after ending the previous one
        assertDoesNotThrow(() -> {
            manager.getReadModel(depositPid);
            manager.end();
        });
    }

    @Test
    void testRemoveModel_ModelIsEmpty() {
        Model writeModel = manager.getWriteModel(depositPid);
        addTriple(writeModel);
        manager.commit();

        manager.removeModel(depositPid);

        Model readModel = manager.getReadModel(depositPid);
        assertFalse(containsTriple(readModel), "Model should be empty after removal");
        manager.end();
    }

    @Test
    void testRemoveModel_OtherModelsUnaffected() {
        Model model1 = manager.getWriteModel(depositPid);
        addTriple(model1);
        manager.commit();

        Model model2 = manager.getWriteModel(deposit2Pid);
        addTriple(model2);
        manager.commit();

        manager.removeModel(depositPid);

        Model readModel2 = manager.getReadModel(deposit2Pid);
        assertTrue(containsTriple(readModel2), "Other deposit model should be unaffected by removal");
        manager.end();
    }

    @Test
    void testLoadDataset_PersistsDataAcrossReload() {
        try (DepositModelManager diskManager = new DepositModelManager(tmpFolder)) {
            Model writeModel = diskManager.getWriteModel(depositPid);
            addTriple(writeModel);
            diskManager.commit();
        }

        // Reload from the same path and verify data is still present
        try (DepositModelManager reloadedManager = new DepositModelManager(tmpFolder)) {
            reloadedManager.loadDataset();
            Model readModel = reloadedManager.getReadModel(depositPid);
            assertTrue(containsTriple(readModel), "Data written before reload should still be present after loadDataset");
            reloadedManager.end();
        }
    }

    @Test
    void testLoadDataset_CreatesDirectoryIfAbsent() {
        Path newDir = tmpFolder.resolve("new-dataset-dir");
        try (DepositModelManager diskManager = new DepositModelManager(newDir)) {
            assertTrue(newDir.toFile().exists(), "Dataset directory should be created by loadDataset");
        }
    }

    private void addTriple(Model model) {
        Resource subject = model.createResource(SUBJECT_URI);
        Property property = model.createProperty(PROPERTY_URI);
        subject.addProperty(property, VALUE);
    }

    private boolean containsTriple(Model model) {
        Resource subject = model.createResource(SUBJECT_URI);
        Property property = model.createProperty(PROPERTY_URI);
        return model.contains(subject, property, VALUE);
    }
}
