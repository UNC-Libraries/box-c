package edu.unc.lib.dl.update;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.RandomAccessFile;

import org.apache.abdera.Abdera;
import org.apache.abdera.model.Document;
import org.apache.abdera.model.Entry;
import org.apache.abdera.parser.Parser;
import org.junit.Assert;
import org.junit.Test;

import edu.unc.lib.dl.agents.PersonAgent;
import edu.unc.lib.dl.fedora.AccessClient;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.fedora.types.MIMETypedStream;
import edu.unc.lib.dl.util.ContentModelHelper;

import static org.mockito.Mockito.*;

public class AtomPubMetadataUIPTest extends Assert {

	@Test
	public void noPrexistingData() throws Exception{
		InputStream entryPart = new FileInputStream(new File("src/test/resources/atompub/metadataUpdateMultipleDS.xml"));
		Abdera abdera = new Abdera();
		Parser parser = abdera.getParser();
		Document<Entry> entryDoc = parser.parse(entryPart);
		Entry entry = entryDoc.getRoot();
		
		AccessClient accessClient = mock(AccessClient.class);
		when(accessClient.getDatastreamDissemination(any(PID.class), anyString(), anyString())).thenReturn(null);
		
		PID pid = new PID("uuid:test");
		PersonAgent user = new PersonAgent("testuser", "testuser");
		
		AtomPubMetadataUIP uip = new AtomPubMetadataUIP(pid, user, UpdateOperation.REPLACE, entry);
		uip.storeOriginalDatastreams(accessClient);
		
		assertTrue(uip.getPID().getPid().equals(pid.getPid()));
		assertTrue(uip.getUser().getOnyen().equals(user.getOnyen()));
		assertTrue(uip.getMessage().equals("Creating collection"));
		
		assertEquals(0, uip.getOriginalData().size());
		assertEquals(3, uip.getIncomingData().size());
		assertEquals(0, uip.getModifiedData().size());
	}
	
	@Test
	public void prexistingData() throws Exception{
		InputStream entryPart = new FileInputStream(new File("src/test/resources/atompub/metadataUpdateMultipleDS.xml"));
		Abdera abdera = new Abdera();
		Parser parser = abdera.getParser();
		Document<Entry> entryDoc = parser.parse(entryPart);
		Entry entry = entryDoc.getRoot();
		
		PID pid = new PID("uuid:test");
		PersonAgent user = new PersonAgent("testuser", "testuser");
		
		AccessClient accessClient = mock(AccessClient.class);
		MIMETypedStream modsStream = new MIMETypedStream();
		RandomAccessFile raf = new RandomAccessFile("src/test/resources/testmods.xml", "r");
		byte[] bytes = new byte[(int)raf.length()];
		raf.read(bytes);
		modsStream.setStream(bytes);
		modsStream.setMIMEType("text/xml");
		when(accessClient.getDatastreamDissemination(any(PID.class), eq(ContentModelHelper.Datastream.MD_DESCRIPTIVE.getName()), anyString())).thenReturn(modsStream);
		when(accessClient.getDatastreamDissemination(any(PID.class), eq(ContentModelHelper.Datastream.RELS_EXT.getName()), anyString())).thenReturn(null);
		
		AtomPubMetadataUIP uip = new AtomPubMetadataUIP(pid, user, UpdateOperation.ADD, entry);
		assertEquals(0, uip.getOriginalData().size());
		uip.storeOriginalDatastreams(accessClient);
		
		assertTrue(uip.getPID().getPid().equals(pid.getPid()));
		assertTrue(uip.getUser().getOnyen().equals(user.getOnyen()));
		assertTrue(uip.getMessage().equals("Creating collection"));
		
		assertEquals(1, uip.getOriginalData().size());
		assertEquals(3, uip.getIncomingData().size());
		assertEquals(0, uip.getModifiedData().size());
	}
}
