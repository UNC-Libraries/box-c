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
import edu.unc.lib.dl.util.PackagingType;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/service-context.xml" })
public class DSPACEMETSDepositHandlerTest {
	@Autowired
	private DSPACEMETSDepositHandler metsDepositHandler = null;
	@Autowired
	private SwordConfigurationImpl swordConfiguration;

	public SwordConfigurationImpl getSwordConfiguration() {
		return swordConfiguration;
	}

	public void setSwordConfiguration(SwordConfigurationImpl swordConfiguration) {
		this.swordConfiguration = swordConfiguration;
	}

	public DSPACEMETSDepositHandler getMetsDepositHandler() {
		return metsDepositHandler;
	}

	public void setMetsDepositHandler(DSPACEMETSDepositHandler metsDepositHandler) {
		this.metsDepositHandler = metsDepositHandler;
	}
	
	@Test(expected = JedisDataException.class)
	public void testDoDepositMETSBiomed() throws SwordError {
		Deposit d = new Deposit();
		File test = new File("src/test/resources/biomedWithSupplements.zip");
		d.setFile(test);
		d.setMd5("7ca5899e938e385c4ad61087bd834a0e");
		d.setFilename("biomedWithSupplements.zip");
		d.setMimeType("application/zip");
		d.setSlug("biomedtest");
		d.setPackaging(PackagingType.METS_DSPACE_SIP_1.getUri());
		Entry entry = Abdera.getInstance().getFactory().newEntry();
		d.setEntry(entry);
		
		PID dest = new PID("uuid:destination");
		getMetsDepositHandler().doDeposit(dest, d, PackagingType.METS_DSPACE_SIP_1, getSwordConfiguration(),
				"test-depositor", "test-owner");
	}

}
