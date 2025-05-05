package edu.unc.lib.boxc.deposit.normalize;

import static edu.unc.lib.boxc.common.test.TestHelpers.setField;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants;
import edu.unc.lib.boxc.deposit.impl.model.DepositDirectoryManager;
import edu.unc.lib.boxc.deposit.impl.model.DepositModelHelpers;
import edu.unc.lib.boxc.deposit.work.JobFailedException;
import edu.unc.lib.boxc.model.api.rdf.CdrDeposit;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import org.apache.jena.rdf.model.Bag;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author bbpennel
 */
public class WorkFormToBagJobTest extends AbstractNormalizationJobTest {
    private WorkFormModsTransformer modsTransformer;
    private WorkFormToBagJob job;
    private DepositDirectoryManager depositDirectoryManager;
    private Map<String, String> depositStatus;

    private Path uploadStagingPath;

    @BeforeEach
    public void setup() throws Exception {
        uploadStagingPath = tmpFolder.resolve("staging");
        Files.createDirectories(uploadStagingPath);

        depositStatus = new HashMap<>();
        when(depositStatusFactory.get(anyString())).thenReturn(depositStatus);

        depositDirectoryManager = new DepositDirectoryManager(depositPid, depositsDirectory.toPath(), true);
        modsTransformer = new WorkFormModsTransformer();

        job = new WorkFormToBagJob(jobUUID, depositUUID);
        setField(job, "depositModelManager", depositModelManager);
        setField(job, "depositsDirectory", depositsDirectory);
        setField(job, "depositStatusFactory", depositStatusFactory);
        setField(job, "premisLoggerFactory", premisLoggerFactory);
        setField(job, "pidMinter", pidMinter);
        setField(job, "modsTransformer", modsTransformer);
        job.setUploadStagingPath(uploadStagingPath);
        job.init();
    }

    @Test
    public void testWithGenericMinimal() throws Exception {
        var dataPath = depositDirectoryManager.getDataDir().resolve("formData.json");
        Files.copy(Paths.get("src/test/resources/form_submissions/generic_minimal.json"), dataPath);

        createStagedFile("ingest-5277341109740044922.tmp");
        var dataFilePath = depositDirectoryManager.getDataDir().resolve("ingest-5277341109740044922.tmp");

        depositStatus.put(RedisWorkerConstants.DepositField.sourceUri.name(), dataPath.toUri().toString());

        job.run();

        Model model = job.getReadOnlyModel();
        Bag depositBag = model.getBag(job.getDepositPID().getRepositoryPath());
        assertEquals(1, depositBag.size());

        Bag workBag = model.getBag((Resource) depositBag.iterator().next());
        assertEquals(1, workBag.size());

        Resource fileResource = (Resource) workBag.iterator().next();
        assertEquals("test.json", fileResource.getProperty(CdrDeposit.label).getLiteral().getString());
        Resource originalResc = DepositModelHelpers.getDatastream(fileResource);
        assertEquals(dataFilePath.toUri().toString(),
                originalResc.getProperty(CdrDeposit.stagingLocation).getLiteral().getString());

        // Ensure MODS exists and was populated for the work object
        var workPid = PIDs.get(workBag.getURI());
        var modsPath = depositDirectoryManager.getModsPath(workPid, false);
        assertTrue(Files.exists(modsPath));
        assertTrue(Files.size(modsPath) > 0);
    }

    @Test
    public void testWithGenericMinimalWithMissingFile() throws Exception {
        var dataPath = depositDirectoryManager.getDataDir().resolve("formData.json");
        Files.copy(Paths.get("src/test/resources/form_submissions/generic_minimal.json"), dataPath);

        depositStatus.put(RedisWorkerConstants.DepositField.sourceUri.name(), dataPath.toUri().toString());

        var e = assertThrows(JobFailedException.class, () -> job.run());
        assertTrue(e.getMessage().contains("Failed to move staged file to deposit data directory"));
    }

    @Test
    public void testWithNoTitle() throws Exception {
        var dataPath = depositDirectoryManager.getDataDir().resolve("formData.json");
        var formData = new WorkFormData();
        formData.setTitle("");
        var files = new WorkFormData.FileInfo();
        files.setTmp("test.json");
        files.setOriginalName("test.json");
        formData.setFile(Collections.singletonList(files));
        ObjectMapper mapper = new ObjectMapper();
        Files.writeString(dataPath, mapper.writeValueAsString(formData));

        depositStatus.put(RedisWorkerConstants.DepositField.sourceUri.name(), dataPath.toUri().toString());

        var e = assertThrows(JobFailedException.class, () -> job.run());
        assertTrue(e.getMessage().contains("Form data is missing title"));
    }

    @Test
    public void testWithNoFiles() throws Exception {
        var dataPath = depositDirectoryManager.getDataDir().resolve("formData.json");
        var formData = new WorkFormData();
        formData.setTitle("Test title");
        formData.setFile(Collections.emptyList());
        ObjectMapper mapper = new ObjectMapper();
        Files.writeString(dataPath, mapper.writeValueAsString(formData));

        depositStatus.put(RedisWorkerConstants.DepositField.sourceUri.name(), dataPath.toUri().toString());

        var e = assertThrows(JobFailedException.class, () -> job.run());
        assertTrue(e.getMessage().contains("Form data is missing files"));
    }

    @Test
    public void testWithInvalidJson() throws Exception {
        var dataPath = depositDirectoryManager.getDataDir().resolve("formData.json");
        Files.writeString(dataPath, "{ what in the world is this }");

        depositStatus.put(RedisWorkerConstants.DepositField.sourceUri.name(), dataPath.toUri().toString());

        var e = assertThrows(JobFailedException.class, () -> job.run());
        assertTrue(e.getMessage().contains("Failed to deserialize form JSON"));
    }

    private void createStagedFile(String filename) throws Exception {
        var stagedFile = uploadStagingPath.resolve(filename);
        Files.createDirectories(stagedFile.getParent());
        Files.createFile(stagedFile);
    }
}
