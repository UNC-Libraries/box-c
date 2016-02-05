package edu.unc.lib.deposit;

import static edu.unc.lib.dl.test.TestHelpers.setField;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.File;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.Mockito;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

import edu.unc.lib.dl.fedora.ManagementClient;
import edu.unc.lib.dl.util.DepositStatusFactory;
import edu.unc.lib.dl.util.JobStatusFactory;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositField;
import edu.unc.lib.staging.FileResolver;
import edu.unc.lib.staging.Stages;

public class CleanupDepositJobTest {

	private static final URI CLEAN_FOLDERS_STAGE_URI = URI.create("tag:cdr.lib.unc.edu,2013:/clean_folders_stage/");
	private static final URI CLEAN_FILES_STAGE_URI = URI.create("tag:cdr.lib.unc.edu,2013:/clean_files_stage/");
	private static final URI NOOP_STAGE_URI = URI.create("tag:cdr.lib.unc.edu,2013:/noop_stage/");
	private static final URI CLEAN_EXTRAS_STAGE_URI = URI.create("tag:cdr.lib.unc.edu,2013:/clean_extras_stage/");

	@Rule
	public TemporaryFolder tmpDir = new TemporaryFolder();
	private File depositsDir;
	private File stagesDir;
	private File cleanDepositsStagingFolder;
	private File cleanFoldersStagingFolder;
	private File cleanFilesStagingFolder;
	private File cleanExtrasStagingFolder;
	private File noopStagingFolder;

	private File modifiedStagesConfig;

	@Mock
	private JobStatusFactory jobStatusFactory;
	@Mock
	private DepositStatusFactory depositStatusFactory;
	@Mock
	private ManagementClient client;
	@Mock
	private Dataset dataset;

	private Map<String, String> depositStatus;

	private CleanupDepositJob job;

	private Stages stages;

	@Before
	public void setup() throws Exception {
		initMocks(this);
		depositStatus = new HashMap<>();
		when(depositStatusFactory.get(anyString())).thenReturn(depositStatus);
		depositStatus.put(DepositField.permissionGroups.name(), "group");

		depositsDir = tmpDir.newFolder("deposits");
		stagesDir = tmpDir.newFolder("stages");

		File templateStageDirectory = new File("src/test/resources/cleanupStage");
		cleanDepositsStagingFolder = new File(stagesDir, "clean_deposits_stage");
		cleanFoldersStagingFolder = new File(stagesDir, "clean_folders_stage");
		cleanFilesStagingFolder = new File(stagesDir, "clean_files_stage");
		cleanExtrasStagingFolder = new File(stagesDir, "clean_extras_stage");
		noopStagingFolder = new File(stagesDir, "noop_stage");
		FileUtils.copyDirectory(templateStageDirectory, cleanDepositsStagingFolder);
		FileUtils.copyDirectory(templateStageDirectory, cleanFoldersStagingFolder);
		FileUtils.copyDirectory(templateStageDirectory, cleanFilesStagingFolder);
		FileUtils.copyDirectory(templateStageDirectory, noopStagingFolder);
		FileUtils.copyDirectory(templateStageDirectory, cleanExtrasStagingFolder);

		// load stages config
		modifiedStagesConfig = tmpDir.newFile("stagesConfig.json");
		stages = new Stages("{}", new FileResolver());
		URI stagingConfigUri = new File("src/test/resources/cleanup_stages.json").toURI();
		stages.addRepositoryConfigURL(stagingConfigUri.toString());

		// add mappings
		stages.setStorageMapping(CLEAN_FOLDERS_STAGE_URI, cleanFoldersStagingFolder.toURI());
		stages.setStorageMapping(CLEAN_FILES_STAGE_URI, cleanFilesStagingFolder.toURI());
		stages.setStorageMapping(NOOP_STAGE_URI, noopStagingFolder.toURI());
		stages.setStorageMapping(CLEAN_EXTRAS_STAGE_URI, cleanExtrasStagingFolder.toURI());

		// save mappings
		FileUtils.writeStringToFile(modifiedStagesConfig, stages.getLocalConfig());

		createJob("bd5ff703-9c2e-466b-b4cc-15bbfd03c8ae", "/cleanupDeposit");
	}

	private void createJob(String depositUuid, String depositDirectoryPath) throws Exception {
		// Clone the deposit directory
		File depositFolder = new File(depositsDir, depositUuid);
		File originalDepositDirectory = new File(getClass().getResource(depositDirectoryPath).toURI());
		FileUtils.copyDirectory(originalDepositDirectory, depositFolder);

		Model model = ModelFactory.createDefaultModel();
		File modelFile = new File(depositFolder, "everything.n3");
		model.read(modelFile.toURI().toString());
		Model spyModel = spy(model);
		doReturn(spyModel).when(spyModel).begin();

		when(dataset.getNamedModel(anyString())).thenReturn(spyModel);

		job = new CleanupDepositJob("jobuuid", depositUuid);
		job.setStages(stages);
		// job.setDepositDirectory(depositFolder);

		setField(job, "depositsDirectory", depositsDir);
		setField(job, "jobStatusFactory", jobStatusFactory);
		setField(job, "depositStatusFactory", depositStatusFactory);
		setField(job, "dataset", dataset);

		depositStatus.put(DepositField.containerId.name(), "uuid:destination");
		depositStatus.put(DepositField.excludeDepositRecord.name(), "false");

		job.init();
	}

	@Test
	public void testCommonPathStageCleanup() throws InterruptedException {

		Thread jobThread = new Thread(job);
		jobThread.start();

		// Start processing with a timelimit to prevent infinite wait in case of failure
		jobThread.join();

		// noop policy
		assertTrue(new File(noopStagingFolder, "project/folderA/ingested").exists());
		assertTrue(new File(noopStagingFolder, "project/folderA/leftover").exists());
		assertTrue(new File(noopStagingFolder, "project/folderB/ingested").exists());
		assertTrue(new File(noopStagingFolder, "project/folderB/also_ingested").exists());

		// clean files policy
		assertFalse(new File(cleanFilesStagingFolder, "project/folderA/ingested").exists());
		assertTrue(new File(cleanFilesStagingFolder, "project/folderA/leftover").exists());
		assertFalse(new File(cleanFilesStagingFolder, "project/folderB/ingested").exists());
		assertFalse(new File(cleanFilesStagingFolder, "project/folderB/also_ingested").exists());
		assertTrue(new File(cleanFilesStagingFolder, "project/folderB").exists());

		// clean folders policy
		assertFalse(new File(cleanFoldersStagingFolder, "project/folderA/ingested").exists());
		assertTrue(new File(cleanFoldersStagingFolder, "project/folderA/leftover").exists());
		assertFalse(new File(cleanFoldersStagingFolder, "project/folderB").exists());
		
		// clean up extra files
		assertFalse(new File(cleanExtrasStagingFolder, "project").exists());

		// deposit folder destroyed
		assertFalse(job.getDepositDirectory().exists());

		// project folder not cleaned
		assertTrue(new File(cleanDepositsStagingFolder, "project").exists());

		// keys have been set to expire
		verify(depositStatusFactory, times(1)).expireKeys(Mockito.anyString(), Mockito.anyInt());
		verify(jobStatusFactory, times(1)).expireKeys(Mockito.anyString(), Mockito.anyInt());
	}

	@Test
	public void testCleanupNoDepositStagingFolder() throws InterruptedException {
		// no deposit staging folder is set

		Thread jobThread = new Thread(job);
		jobThread.start();

		// Start processing with a timelimit to prevent infinite wait in case of failure
		jobThread.join(10000L);

		// clean deposit policy
		assertTrue(new File(cleanDepositsStagingFolder, "project").exists());
	}

}
