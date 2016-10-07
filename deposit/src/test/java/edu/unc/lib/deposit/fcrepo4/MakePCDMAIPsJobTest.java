package edu.unc.lib.deposit.fcrepo4;

import static edu.unc.lib.dl.test.TestHelpers.setField;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.File;
import java.io.FileInputStream;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.tdb.TDBFactory;
import com.hp.hpl.jena.vocabulary.RDF;

import edu.unc.lib.dl.fcrepo4.ContentObject;
import edu.unc.lib.dl.fcrepo4.Repository;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.util.DepositConstants;
import edu.unc.lib.dl.util.DepositStatusFactory;
import edu.unc.lib.dl.util.JobStatusFactory;

public class MakePCDMAIPsJobTest {
	@Rule
	public final TemporaryFolder tmpFolder = new TemporaryFolder();

	@Mock
	private JobStatusFactory jobStatusFactory;
	@Mock
	private DepositStatusFactory depositStatusFactory;
	@Mock
	private Repository repository;

	private File depositsDirectory;
	
	private MakePCDMAIPsJob job;
	
	private File aipsDir;
	
	private String depositUri;
	
	@Before
	public void setup() throws Exception {
		initMocks(this);

		depositsDirectory = tmpFolder.newFolder("deposits");

		String depositUUID = UUID.randomUUID().toString();
		File depositDir = new File(depositsDirectory, depositUUID);
		depositDir.mkdir();
	}
	
	private void initializeJob(String depositUUID, String packagePath) throws Exception {
		File depositDirectory = new File(depositsDirectory, depositUUID);
		depositDirectory.mkdir();
		FileUtils.copyDirectory(new File(packagePath), depositDirectory);

		Dataset dataset = TDBFactory.createDataset();

		job = new MakePCDMAIPsJob();
		job.setDepositUUID(depositUUID);
		setField(job, "dataset", dataset);
		setField(job, "depositsDirectory", depositsDirectory);
		setField(job, "jobStatusFactory", jobStatusFactory);
		setField(job, "depositStatusFactory", depositStatusFactory);
		setField(job, "repository", repository);

		job.init();

		Model model = job.getWritableModel();
		model.read(new File(depositDirectory, "everything.n3").getAbsolutePath());
		job.closeModel();
		
		depositUri = "http://example.com/fcrepo/deposits/" + depositUUID;
		//when(repository.getDepositRecordPath(anyString())).thenReturn(depositUri);

		aipsDir = new File(job.getDepositDirectory(), DepositConstants.AIPS_DIR);
	}
	
	@Test
	public void testWithSourceMetadata() throws Exception {
		String depositUUID = "55c262bf-9f15-4184-9979-3d8816d40103";

		initializeJob(depositUUID, "src/test/resources/ingest-bags/fcrepo4/proquest-bag");
		
		// Setting up retrieval of a fedora path for the primary object of this aggregate
		PID primaryObjPid = new PID("uuid:8f8f587b-b109-4ff1-84b1-1206779faffb");
		PID aggrObjPid = new PID("uuid:fc39ce7d-b81b-4c2b-bf38-e2f18ebfd4dd");
		String primaryPath = aggrObjPid.getUUID() + "/members/" + primaryObjPid.getUUID();
		
		ContentObject aggrObject = mock(ContentObject.class);
		//when(aggrObject.getChildPath(eq(primaryObjPid))).thenReturn(primaryPath);
		when(repository.getContentObject(eq(aggrObjPid))).thenReturn(aggrObject);

		job.run();
		
		// Verify that all the objects 
		Resource aggrResc = getAIPResource("fc39ce7d-b81b-4c2b-bf38-e2f18ebfd4dd");
		assertEquals(Cdr.AggregateWork, aggrResc.getProperty(RDF.type).getObject());
		assertEquals(depositUri, aggrResc.getProperty(Cdr.originalDeposit).getResource().getURI());
		
		Resource primaryResc = getAIPResource("8f8f587b-b109-4ff1-84b1-1206779faffb");
		assertEquals(Cdr.FileObject, primaryResc.getProperty(RDF.type).getObject());
		assertEquals(depositUri, primaryResc.getProperty(Cdr.originalDeposit).getResource().getURI());
		
		Resource suppleResc = getAIPResource("90456591-8328-4256-b8e8-9fa000ae0dee");
		assertEquals(Cdr.FileObject, suppleResc.getProperty(RDF.type).getObject());
		
		Resource sourceMDResc = getAIPResource("bdf3ae03-f0c4-4097-bb3d-c9e5be5f4594");
		assertEquals(Cdr.SourceMetadata, sourceMDResc.getProperty(RDF.type).getObject());
		assertEquals("proquest", sourceMDResc.getProperty(Cdr.hasSourceMetadataProfile).getString());
		assertFalse("Source metadata should not have originalDeposit relation",
				sourceMDResc.hasProperty(Cdr.originalDeposit));
	}
	
	private Resource getAIPResource(String uuid) throws Exception {
		Model model = ModelFactory.createDefaultModel() ;
		model.read(new FileInputStream(new File(aipsDir, uuid + ".ttl")), null, "TURTLE");
		
		return model.getResource(new PID("uuid:" + uuid).getURI());
	}
}
