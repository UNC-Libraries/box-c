package edu.unc.lib.dl.cdr.sword.server.deposit;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.reset;

import java.io.File;
import java.io.IOException;

import org.apache.abdera.Abdera;
import org.apache.abdera.model.Entry;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.swordapp.server.Deposit;
import org.swordapp.server.SwordError;

import edu.unc.lib.dl.cdr.sword.server.SwordConfigurationImpl;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.DepositStatusFactory;
import edu.unc.lib.dl.util.PackagingType;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/service-context.xml" })
public class CDRMETSDepositHandlerTest {
	@Mock
	private DepositStatusFactory depositStatusFactory;
	@Before
	public void setup() {
	    // Initialize mocks created above
	    MockitoAnnotations.initMocks(this);
	}
	
	@Rule
	public TemporaryFolder tmpDir = new TemporaryFolder();
	
	@InjectMocks
	@Autowired
	private CDRMETSDepositHandler metsDepositHandler;
	
	@Autowired
	private SwordConfigurationImpl swordConfiguration;
	
	@SuppressWarnings("unchecked")
	@Test
	public void testDoDepositBagit() throws SwordError, IOException {
		Deposit d = new Deposit();
		File testPayload = tmpDir.newFile("simple.zip");
		FileUtils.copyFile(new File("src/test/resources/simple.zip"), testPayload);
		d.setFile(testPayload);
		d.setMd5("d2b88d292e2c47943231205ed36f6c94");
		d.setFilename("simple.zip");
		d.setMimeType("application/zip");
		d.setSlug("metsbagittest");
		d.setPackaging(PackagingType.METS_CDR.getUri());
		Entry entry = Abdera.getInstance().getFactory().newEntry();
		d.setEntry(entry);
		
		reset(depositStatusFactory);
		
		PID dest = new PID("uuid:destination");
		metsDepositHandler.doDeposit(dest, d, PackagingType.METS_CDR, swordConfiguration,
				"test-depositor", "test-owner");

		verify(depositStatusFactory, atLeastOnce()).save(anyString(), anyMap());
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testDoDepositBagitFilename() throws SwordError, IOException {
		Deposit d = new Deposit();
		File testPayload = tmpDir.newFile("simple.tmp");
		FileUtils.copyFile(new File("src/test/resources/simple.zip"), testPayload);
		d.setFile(testPayload);
		d.setMd5("d2b88d292e2c47943231205ed36f6c94");
		d.setFilename("simple.zip");
		d.setMimeType("application/zip");
		d.setSlug("metsbagittest");
		d.setPackaging(PackagingType.METS_CDR.getUri());
		Entry entry = Abdera.getInstance().getFactory().newEntry();
		d.setEntry(entry);
		
		reset(depositStatusFactory);
		
		PID dest = new PID("uuid:destination");
		metsDepositHandler.doDeposit(dest, d, PackagingType.METS_CDR, swordConfiguration,
				"test-depositor", "test-owner");

		verify(depositStatusFactory, atLeastOnce()).save(anyString(), anyMap());
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testDoDepositMETSXML() throws SwordError, IOException {
		Deposit d = new Deposit();
		File testPayload = tmpDir.newFile("METS.xml");
		FileUtils.copyFile(new File("src/test/resources/METS.xml"), testPayload);
		d.setFile(testPayload);
		d.setMd5("d2b88d292e2c47943231205ed36f6c94");
		d.setFilename("METS.xml");
		d.setMimeType("application/xml");
		d.setSlug("metsxmltest");
		d.setPackaging(PackagingType.METS_CDR.getUri());
		Entry entry = Abdera.getInstance().getFactory().newEntry();
		d.setEntry(entry);

		reset(depositStatusFactory);
		
		PID dest = new PID("uuid:destination");
		metsDepositHandler.doDeposit(dest, d, PackagingType.METS_CDR, swordConfiguration,
				"test-depositor", "test-owner");

		verify(depositStatusFactory, atLeastOnce()).save(anyString(), anyMap());
	}

}
