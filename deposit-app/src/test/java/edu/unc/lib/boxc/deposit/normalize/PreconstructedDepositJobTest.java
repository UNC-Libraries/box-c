package edu.unc.lib.boxc.deposit.normalize;

import static edu.unc.lib.boxc.common.test.TestHelpers.setField;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.io.FileNotFoundException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.DC;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositField;
import edu.unc.lib.boxc.deposit.fcrepo4.AbstractDepositJobTest;
import edu.unc.lib.boxc.deposit.impl.model.DepositDirectoryManager;
import edu.unc.lib.boxc.deposit.work.JobFailedException;

/**
 * @author bbpennel
 */
public class PreconstructedDepositJobTest extends AbstractDepositJobTest {
    private PreconstructedDepositJob job;
    private Map<String, String> status;

    @BeforeEach
    public void setup() throws Exception {
        status = new HashMap<>();
        when(depositStatusFactory.get(anyString())).thenReturn(status);

        job = new PreconstructedDepositJob();
        job.setDepositUUID(depositUUID);
        setField(job, "pidMinter", pidMinter);
        setField(job, "depositModelManager", depositModelManager);
        setField(job, "depositsDirectory", depositsDirectory);
        setField(job, "depositStatusFactory", depositStatusFactory);
        job.init();
    }

    @Test
    public void noSourceNoModelFilePrepopulatedModel() throws Exception {
        Model writeModel = job.getWritableModel();
        Resource preResc = writeModel.getResource(depositPid.getRepositoryPath());
        preResc.addLiteral(DC.title, "Test value");
        job.closeModel();

        job.run();

        Model model = job.getReadOnlyModel();
        assertEquals(1, model.listStatements().toList().size());
        Resource postResc = model.getResource(depositPid.getRepositoryPath());
        assertTrue(postResc.hasLiteral(DC.title, "Test value"));
    }

    @Test
    public void withExternalSourceNoModelFile() throws Exception {
        Path externalBasePath = tmpFolder.resolve("newFolder");
        Files.createDirectory(externalBasePath);
        DepositDirectoryManager extDirManager = new DepositDirectoryManager(depositPid, externalBasePath, true);
        status.put(DepositField.sourceUri.name(), extDirManager.getDepositDir().toUri().toString());
        Files.createFile(extDirManager.getPremisPath(depositPid, true));

        job.run();

        Model model = job.getReadOnlyModel();
        assertEquals(0, model.listStatements().toList().size());

        DepositDirectoryManager intDirManager = new DepositDirectoryManager(
                depositPid, depositsDirectory.toPath(), true, false);
        Path intPremisPath = intDirManager.getPremisPath(depositPid);
        assertTrue(Files.exists(intPremisPath));
    }

    @Test
    public void withInternalSourceNoModelFile() throws Exception {
        DepositDirectoryManager preDirManager = new DepositDirectoryManager(
                depositPid, depositsDirectory.toPath(), true);
        status.put(DepositField.sourceUri.name(), preDirManager.getDepositDir().toUri().toString());
        Files.createFile(preDirManager.getPremisPath(depositPid, true));

        job.run();

        Model model = job.getReadOnlyModel();
        assertEquals(0, model.listStatements().toList().size());

        DepositDirectoryManager postDirManager = new DepositDirectoryManager(
                depositPid, depositsDirectory.toPath(), true, false);
        Path intPremisPath = postDirManager.getPremisPath(depositPid);
        assertTrue(Files.exists(intPremisPath));
    }

    @Test
    public void noSourceWithModelFile() throws Exception {
        DepositDirectoryManager preDirManager = new DepositDirectoryManager(
                depositPid, depositsDirectory.toPath(), true);
        Model importModel = ModelFactory.createDefaultModel();
        Resource preResc = importModel.getResource(depositPid.getRepositoryPath());
        preResc.addLiteral(DC.title, "Import value");
        Writer writer = Files.newBufferedWriter(preDirManager.getModelPath());
        importModel.write(writer, "N3");

        job.run();

        Model model = job.getReadOnlyModel();
        assertEquals(1, model.listStatements().toList().size());
        Resource postResc = model.getResource(depositPid.getRepositoryPath());
        assertTrue(postResc.hasLiteral(DC.title, "Import value"));
    }

    @Test
    public void withSourceWithModelFile() throws Exception {
        Path externalBasePath = tmpFolder.resolve("testFolder");
        DepositDirectoryManager extDirManager = new DepositDirectoryManager(
                depositPid, externalBasePath, true);
        Model importModel = ModelFactory.createDefaultModel();
        Resource extResc = importModel.getResource(depositPid.getRepositoryPath());
        extResc.addLiteral(DC.title, "Import me");
        Writer writer = Files.newBufferedWriter(extDirManager.getModelPath());
        importModel.write(writer, "N3");

        status.put(DepositField.sourceUri.name(), extDirManager.getDepositDir().toUri().toString());
        Files.createFile(extDirManager.getPremisPath(depositPid, true));

        job.run();

        Model model = job.getReadOnlyModel();
        assertEquals(1, model.listStatements().toList().size());
        Resource postResc = model.getResource(depositPid.getRepositoryPath());
        assertTrue(postResc.hasLiteral(DC.title, "Import me"));

        DepositDirectoryManager intDirManager = new DepositDirectoryManager(
                depositPid, depositsDirectory.toPath(), true, false);
        Path intPremisPath = intDirManager.getPremisPath(depositPid);
        assertTrue(Files.exists(intPremisPath));
    }

    @Test
    public void noSourceWithModelFilePrepopulatedModel() throws Exception {
        Model writeModel = job.getWritableModel();
        Resource preResc = writeModel.getResource(depositPid.getRepositoryPath());
        preResc.addLiteral(DC.title, "Pre-existing Condition");
        job.closeModel();

        DepositDirectoryManager preDirManager = new DepositDirectoryManager(
                depositPid, depositsDirectory.toPath(), true);
        Model importModel = ModelFactory.createDefaultModel();
        Resource importResc = importModel.getResource(depositPid.getRepositoryPath());
        importResc.addLiteral(DC.title, "Imported Titles");
        Writer writer = Files.newBufferedWriter(preDirManager.getModelPath());
        importModel.write(writer, "N3");

        job.run();

        Model model = job.getReadOnlyModel();
        assertEquals(1, model.listStatements().toList().size());
        Resource postResc = model.getResource(depositPid.getRepositoryPath());
        assertTrue(postResc.hasLiteral(DC.title, "Imported Titles"));
    }

    @Test
    public void sourceDirDoesNotExistTest() throws Exception {
        Path externalBasePath = tmpFolder.resolve("testFolder");
        Files.createDirectory(externalBasePath);
        Files.delete(externalBasePath);
        DepositDirectoryManager extDirManager = new DepositDirectoryManager(depositPid, externalBasePath, true, false);
        status.put(DepositField.sourceUri.name(), extDirManager.getDepositDir().toUri().toString());

        try {
            job.run();
            fail();
        } catch (JobFailedException e) {
            assertTrue(e.getCause() instanceof FileNotFoundException);
        }
    }
}
