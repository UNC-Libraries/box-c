package edu.unc.lib.dl.update;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import org.apache.abdera.Abdera;
import org.apache.abdera.model.Document;
import org.apache.abdera.model.Entry;
import org.apache.abdera.parser.Parser;
import org.junit.Assert;
import org.junit.Test;

import edu.unc.lib.dl.agents.Agent;
import edu.unc.lib.dl.agents.PersonAgent;
import edu.unc.lib.dl.fedora.AccessClient;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.services.DigitalObjectManager;
import edu.unc.lib.dl.util.ContentModelHelper.Datastream;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class FedoraObjectUIPProcessorTest extends Assert {

	@Test
	public void addMODSToObjectWithoutMODS() throws Exception {
		AccessClient accessClient = mock(AccessClient.class);
		when(accessClient.getDatastreamDissemination(any(PID.class), anyString(), anyString())).thenReturn(null);
		
		DigitalObjectManager digitalObjectManager = mock(DigitalObjectManager.class);
		when(digitalObjectManager.addOrReplaceDatastream(any(PID.class), any(Datastream.class), any(File.class),
				anyString(), any(Agent.class), anyString()));
		
		FedoraObjectUIPProcessor uipProcessor = new FedoraObjectUIPProcessor();
		uipProcessor.setAccessClient(accessClient);
		uipProcessor.setDigitalObjectManager(digitalObjectManager);
		
		
		InputStream entryPart = new FileInputStream(new File("src/test/resources/atompub/metadataUpdateMODS.xml"));
		Abdera abdera = new Abdera();
		Parser parser = abdera.getParser();
		Document<Entry> entryDoc = parser.parse(entryPart);
		Entry entry = entryDoc.getRoot();
		
		PID pid = new PID("uuid:test");
		PersonAgent user = new PersonAgent("testuser", "testuser");
		
		AtomPubMetadataUIP uip = new AtomPubMetadataUIP(pid, user, UpdateOperation.ADD, entry);
		uipProcessor.process(uip);
		
		assertEquals(1, uip.getModifiedData().size());
		assertEquals(1, uip.getIncomingData().size());
		
	}
}
