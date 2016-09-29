package edu.unc.lib.deposit.fcrepo4;

import static edu.unc.lib.dl.test.TestHelpers.setField;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.tdb.TDBFactory;
import com.hp.hpl.jena.vocabulary.RDF;

import edu.unc.lib.dl.fcrepo4.DepositRecord;
import edu.unc.lib.dl.fcrepo4.Repository;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.rdf.DcElements;
import edu.unc.lib.dl.util.DepositConstants;
import edu.unc.lib.dl.util.DepositStatusFactory;
import edu.unc.lib.dl.util.PackagingType;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositField;

public class IngestDepositRecordJobTest {
	@Rule
	public final TemporaryFolder tmpFolder = new TemporaryFolder();
	
	@Mock
	private DepositStatusFactory depositStatusFactory;
	@Mock
	private Repository repository;
	
	private File depositsDirectory;
	
	private IngestDepositRecordJob job;
	
	private File aipsDir;
	
	private File depositDir;
	
	@Before
	public void setup() throws Exception {
		initMocks(this);

		depositsDirectory = tmpFolder.newFolder("deposits");

		String depositUUID = UUID.randomUUID().toString();
		File depositDir = new File(depositsDirectory, depositUUID);
		depositDir.mkdir();
	}
	
	private void initializeJob(String depositUUID, String packagePath, String n3File) throws Exception {
		depositDir = new File(depositsDirectory, depositUUID);
		depositDir.mkdir();
		FileUtils.copyDirectory(new File(packagePath), depositDir);

		Dataset dataset = TDBFactory.createDataset();

		job = new IngestDepositRecordJob();
		job.setDepositUUID(depositUUID);
		setField(job, "dataset", dataset);
		setField(job, "depositsDirectory", depositsDirectory);
		setField(job, "depositStatusFactory", depositStatusFactory);
		setField(job, "repository", repository);

		job.init();

		Model model = job.getWritableModel();
		model.read(new File(n3File).getAbsolutePath());
		job.closeModel();
		
		when(repository.getDepositRecordPath(anyString())).thenReturn("/deposits/" + depositUUID);

		aipsDir = new File(job.getDepositDirectory(), DepositConstants.AIPS_DIR);
	}
	
	@Test
	public void testProquestAggregateBag() throws Exception {
		String depositUUID = "55c262bf-9f15-4184-9979-3d8816d40103";

		initializeJob(depositUUID, "src/test/resources/ingest-bags/fcrepo4/proquest-bag",
				"src/test/resources/ingest-bags/fcrepo4/proquest-bag/everything.n3");
		
		Map<String, String> depositStatus = new HashMap<>();
		depositStatus.put(DepositField.fileName.name(), "proquest-bag");
		depositStatus.put(DepositField.packagingType.name(), PackagingType.PROQUEST_ETD.getUri());
		
		when(depositStatusFactory.get(eq(depositUUID))).thenReturn(depositStatus);

		job.run();
		
		// Verifying that the deposit record was created correctly
		Resource depositResc = getAIPResource(depositUUID);
		assertEquals(PackagingType.PROQUEST_ETD.getUri(), depositResc.getProperty(Cdr.depositPackageType).getString());
		assertEquals("Deposit record for proquest-bag", depositResc.getProperty(DcElements.title).getString());
		assertEquals(Cdr.DepositRecord, depositResc.getProperty(RDF.type).getObject());
	}
	
	@Test
	public void testWithManifests() throws Exception {
		String depositUUID = "8c1ba3ea-d3f5-4d6f-b8c2-1c7fcdc5fcf2";
		
		initializeJob(depositUUID, "src/test/resources/paths/valid-bag",
				"src/test/resources/ingest-bags/fcrepo4/valid-bag.n3");
		
		DepositRecord depositRecord = mock(DepositRecord.class);
		when(repository.createDepositRecord(anyString(), any(Model.class))).thenReturn(depositRecord);
		
		Map<String, String> depositStatus = new HashMap<>();
		depositStatus.put(DepositField.fileName.name(), "valid-bag");
		depositStatus.put(DepositField.packagingType.name(), PackagingType.BAGIT.getUri());
		when(depositStatusFactory.get(eq(depositUUID))).thenReturn(depositStatus);
		
		job.run();
		
		// Check that the deposit record model was given the correct properties
		Resource depositResc = getAIPResource(depositUUID);
		assertEquals(PackagingType.BAGIT.getUri(), depositResc.getProperty(Cdr.depositPackageType).getString());
		assertEquals(Cdr.DepositRecord, depositResc.getProperty(RDF.type).getObject());
		
		// Check that the deposit record was created
		verify(repository).createDepositRecord(eq(depositUUID), any(Model.class));
		
		// Check that all manifests were added to the record
		ArgumentCaptor<File> manifestCaptor = ArgumentCaptor.forClass(File.class);
		verify(depositRecord, times(2)).addManifest(manifestCaptor.capture());
		
		List<File> manifests = manifestCaptor.getAllValues();
		
		assertTrue(manifests.contains(new File(depositDir, "valid-bag/manifest-md5.txt")));
		assertTrue(manifests.contains(new File(depositDir, "valid-bag/bagit.txt")));
	}
	
	private Resource getAIPResource(String uuid) throws Exception {
		Model model = ModelFactory.createDefaultModel() ;
		model.read(new FileInputStream(new File(aipsDir, uuid + ".ttl")), null, "TURTLE");
		
		return model.getResource(new PID("uuid:" + uuid).getURI());
	}
}
