package edu.unc.lib.dl.cdr.sword.server.deposit;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import org.apache.abdera.Abdera;
import org.apache.abdera.model.Document;
import org.apache.abdera.model.Entry;
import org.apache.abdera.parser.Parser;
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
public class AtomPubEntryDepositHandlerTest {
	@Autowired
	private AtomPubEntryDepositHandler atomPubEntryDepositHandler = null;
	public AtomPubEntryDepositHandler getAtomPubEntryDepositHandler() {
		return atomPubEntryDepositHandler;
	}

	public void setAtomPubEntryDepositHandler(
			AtomPubEntryDepositHandler atomPubEntryDepositHandler) {
		this.atomPubEntryDepositHandler = atomPubEntryDepositHandler;
	}

	@Autowired
	private SwordConfigurationImpl swordConfiguration;

	public SwordConfigurationImpl getSwordConfiguration() {
		return swordConfiguration;
	}

	public void setSwordConfiguration(SwordConfigurationImpl swordConfiguration) {
		this.swordConfiguration = swordConfiguration;
	}

	@Test(expected = JedisDataException.class)
	public void testDoDepositBagit() throws SwordError, FileNotFoundException {
		File testPayload = FileUtils.tempCopy(new File("src/test/resources/dcDocument.xml"));
		Deposit d = new Deposit();
		d.setFile(testPayload);
		d.setMd5("ce812d38aec998c6f3a163994b81bb3a");
		d.setFilename("dcDocument.xml");
		d.setMimeType("application/xml");
		d.setSlug("atomPubEntryTest");
		d.setPackaging(PackagingType.ATOM.getUri());
		Parser parser = Abdera.getInstance().getParser();
		FileInputStream in = new FileInputStream("src/test/resources/atompubMODS.xml");
		Document<Entry> doc = parser.<Entry>parse(in);
		d.setEntry(doc.getRoot());
		
		PID dest = new PID("uuid:destination");
		getAtomPubEntryDepositHandler().doDeposit(dest, d, PackagingType.ATOM, getSwordConfiguration(),
				"test-depositor", "test-owner");
	}

}
