package edu.unc.lib.dl.cdr.sword.server.deposit;
import java.io.File;

import org.apache.abdera.Abdera;
import org.apache.abdera.model.Entry;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.swordapp.server.Deposit;
import org.swordapp.server.SwordError;

import redis.clients.jedis.exceptions.JedisDataException;
import edu.unc.lib.dl.cdr.sword.server.SwordConfigurationImpl;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.FileUtils;
import edu.unc.lib.dl.util.PackagingType;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/service-context.xml" })
public class METSDepositHandlerTest {
	@Autowired
	private METSDepositHandler metsDepositHandler = null;
	@Autowired
	private SwordConfigurationImpl swordConfiguration;

	public SwordConfigurationImpl getSwordConfiguration() {
		return swordConfiguration;
	}

	public void setSwordConfiguration(SwordConfigurationImpl swordConfiguration) {
		this.swordConfiguration = swordConfiguration;
	}

	public METSDepositHandler getMetsDepositHandler() {
		return metsDepositHandler;
	}

	public void setMetsDepositHandler(METSDepositHandler metsDepositHandler) {
		this.metsDepositHandler = metsDepositHandler;
	}

	@Test(expected = JedisDataException.class)
	public void testDoDepositZIP() throws SwordError {
		Deposit d = new Deposit();
		d.setFile(new File("src/test/resources/simple.zip"));
		d.setMd5("d2b88d292e2c47943231205ed36f6c94");
		d.setFilename("simple.zip");
		d.setMimeType("application/zip");
		d.setSlug("simpletest");
		d.setPackaging(PackagingType.SIMPLE_ZIP.getUri());
		Entry entry = Abdera.getInstance().getFactory().newEntry();
		d.setEntry(entry);
		
		PID dest = new PID("uuid:destination");
		getMetsDepositHandler().doDeposit(dest, d, PackagingType.METS_CDR, getSwordConfiguration(),
				"test-depositor", "test-owner");
	}
	
	@Test(expected = JedisDataException.class)
	public void testDoDepositMETSXML() throws SwordError {
		Deposit d = new Deposit();
		File testMETS = FileUtils.tempCopy(new File("src/test/resources/METS.xml"));
		d.setFile(testMETS);
		d.setMd5("d2b88d292e2c47943231205ed36f6c94");
		d.setFilename("METS.xml");
		d.setMimeType("application/zip");
		d.setSlug("simpletest");
		d.setPackaging(PackagingType.METS_CDR.getUri());
		Entry entry = Abdera.getInstance().getFactory().newEntry();
		d.setEntry(entry);
		
		PID dest = new PID("uuid:destination");
		getMetsDepositHandler().doDeposit(dest, d, PackagingType.METS_CDR, getSwordConfiguration(),
				"test-depositor", "test-owner");
	}

}
