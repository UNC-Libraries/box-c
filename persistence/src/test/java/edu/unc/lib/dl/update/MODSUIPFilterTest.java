package edu.unc.lib.dl.update;

import static org.mockito.Mockito.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.RandomAccessFile;

import javax.annotation.Resource;

import org.apache.abdera.Abdera;
import org.apache.abdera.model.Document;
import org.apache.abdera.model.Entry;
import org.apache.abdera.parser.Parser;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import edu.unc.lib.dl.agents.PersonAgent;
import edu.unc.lib.dl.fedora.AccessClient;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.fedora.types.MIMETypedStream;
import edu.unc.lib.dl.schematron.SchematronValidator;
import edu.unc.lib.dl.util.ContentModelHelper;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/service-context.xml" })
public class MODSUIPFilterTest extends Assert {
	@Resource
	private SchematronValidator schematronValidator;
	
	@Test
	public void addMODSToObjectWithoutMODS() throws Exception {
		InputStream entryPart = new FileInputStream(new File("src/test/resources/atompub/metadataUpdateMODS.xml"));
		Abdera abdera = new Abdera();
		Parser parser = abdera.getParser();
		Document<Entry> entryDoc = parser.parse(entryPart);
		Entry entry = entryDoc.getRoot();
		
		AccessClient accessClient = mock(AccessClient.class);
		when(accessClient.getDatastreamDissemination(any(PID.class), anyString(), anyString())).thenReturn(null);

		PID pid = new PID("uuid:test");
		PersonAgent user = new PersonAgent("testuser", "testuser");

		AtomPubMetadataUIP uip = new AtomPubMetadataUIP(pid, user, UpdateOperation.ADD, entry);
		uip.storeOriginalDatastreams(accessClient);

		assertFalse(uip.getOriginalData().containsKey(ContentModelHelper.Datastream.MD_DESCRIPTIVE.getName()));
		assertFalse(uip.getModifiedData().containsKey(ContentModelHelper.Datastream.MD_DESCRIPTIVE.getName()));
		assertTrue(uip.getIncomingData().containsKey(ContentModelHelper.Datastream.MD_DESCRIPTIVE.getName()));

		MODSUIPFilter filter = new MODSUIPFilter();
		filter.setSchematronValidator(schematronValidator);
		filter.doFilter(uip);

		assertFalse(uip.getOriginalData().containsKey(ContentModelHelper.Datastream.MD_DESCRIPTIVE.getName()));
		assertTrue(uip.getModifiedData().containsKey(ContentModelHelper.Datastream.MD_DESCRIPTIVE.getName()));
		assertTrue(uip.getIncomingData().containsKey(ContentModelHelper.Datastream.MD_DESCRIPTIVE.getName()));
	}

	@Test
	public void addMODSToObjectWithMODS() throws Exception {
		InputStream entryPart = new FileInputStream(new File("src/test/resources/atompub/metadataUpdateMODS.xml"));
		Abdera abdera = new Abdera();
		Parser parser = abdera.getParser();
		Document<Entry> entryDoc = parser.parse(entryPart);
		Entry entry = entryDoc.getRoot();

		AccessClient accessClient = mock(AccessClient.class);
		MIMETypedStream modsStream = new MIMETypedStream();
		RandomAccessFile raf = new RandomAccessFile("src/test/resources/testmods.xml", "r");
		byte[] bytes = new byte[(int) raf.length()];
		raf.read(bytes);
		modsStream.setStream(bytes);
		modsStream.setMIMEType("text/xml");
		when(
				accessClient.getDatastreamDissemination(any(PID.class),
						eq(ContentModelHelper.Datastream.MD_DESCRIPTIVE.getName()), anyString())).thenReturn(modsStream);

		PID pid = new PID("uuid:test");
		PersonAgent user = new PersonAgent("testuser", "testuser");

		AtomPubMetadataUIP uip = new AtomPubMetadataUIP(pid, user, UpdateOperation.ADD, entry);
		uip.storeOriginalDatastreams(accessClient);

		assertTrue(uip.getOriginalData().containsKey(ContentModelHelper.Datastream.MD_DESCRIPTIVE.getName()));
		assertFalse(uip.getModifiedData().containsKey(ContentModelHelper.Datastream.MD_DESCRIPTIVE.getName()));
		assertTrue(uip.getIncomingData().containsKey(ContentModelHelper.Datastream.MD_DESCRIPTIVE.getName()));

		int originalChildrenCount = uip.getOriginalData().get(ContentModelHelper.Datastream.MD_DESCRIPTIVE.getName())
				.getChildren().size();
		int incomingChildrenCount = uip.getIncomingData().get(ContentModelHelper.Datastream.MD_DESCRIPTIVE.getName())
				.getChildren().size();

		MODSUIPFilter filter = new MODSUIPFilter();
		filter.setSchematronValidator(schematronValidator);
		filter.doFilter(uip);
		
		assertEquals(originalChildrenCount,
				uip.getOriginalData().get(ContentModelHelper.Datastream.MD_DESCRIPTIVE.getName()).getChildren().size());
		assertEquals(incomingChildrenCount,
				uip.getIncomingData().get(ContentModelHelper.Datastream.MD_DESCRIPTIVE.getName()).getChildren().size());
		assertEquals(incomingChildrenCount + originalChildrenCount,
				uip.getModifiedData().get(ContentModelHelper.Datastream.MD_DESCRIPTIVE.getName()).getChildren().size());	
	}

	@Test
	public void replaceMODSToObjectWithoutMODS() throws Exception {
		InputStream entryPart = new FileInputStream(new File("src/test/resources/atompub/metadataUpdateMODS.xml"));
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

		assertFalse(uip.getOriginalData().containsKey(ContentModelHelper.Datastream.MD_DESCRIPTIVE.getName()));
		assertFalse(uip.getModifiedData().containsKey(ContentModelHelper.Datastream.MD_DESCRIPTIVE.getName()));
		assertTrue(uip.getIncomingData().containsKey(ContentModelHelper.Datastream.MD_DESCRIPTIVE.getName()));

		MODSUIPFilter filter = new MODSUIPFilter();
		filter.setSchematronValidator(schematronValidator);
		filter.doFilter(uip);

		assertFalse(uip.getOriginalData().containsKey(ContentModelHelper.Datastream.MD_DESCRIPTIVE.getName()));
		assertTrue(uip.getModifiedData().containsKey(ContentModelHelper.Datastream.MD_DESCRIPTIVE.getName()));
		assertTrue(uip.getIncomingData().containsKey(ContentModelHelper.Datastream.MD_DESCRIPTIVE.getName()));
	}
	
	@Test
	public void replaceMODSOnObjectWithMODS() throws Exception {
		InputStream entryPart = new FileInputStream(new File("src/test/resources/atompub/metadataUpdateMODS.xml"));
		Abdera abdera = new Abdera();
		Parser parser = abdera.getParser();
		Document<Entry> entryDoc = parser.parse(entryPart);
		Entry entry = entryDoc.getRoot();

		AccessClient accessClient = mock(AccessClient.class);
		MIMETypedStream modsStream = new MIMETypedStream();
		RandomAccessFile raf = new RandomAccessFile("src/test/resources/testmods.xml", "r");
		byte[] bytes = new byte[(int) raf.length()];
		raf.read(bytes);
		modsStream.setStream(bytes);
		modsStream.setMIMEType("text/xml");
		when(
				accessClient.getDatastreamDissemination(any(PID.class),
						eq(ContentModelHelper.Datastream.MD_DESCRIPTIVE.getName()), anyString())).thenReturn(modsStream);

		PID pid = new PID("uuid:test");
		PersonAgent user = new PersonAgent("testuser", "testuser");

		AtomPubMetadataUIP uip = new AtomPubMetadataUIP(pid, user, UpdateOperation.REPLACE, entry);
		uip.storeOriginalDatastreams(accessClient);

		//Original data is not loaded for replace operations
		assertFalse(uip.getOriginalData().containsKey(ContentModelHelper.Datastream.MD_DESCRIPTIVE.getName()));
		assertFalse(uip.getModifiedData().containsKey(ContentModelHelper.Datastream.MD_DESCRIPTIVE.getName()));
		assertTrue(uip.getIncomingData().containsKey(ContentModelHelper.Datastream.MD_DESCRIPTIVE.getName()));

		int incomingChildrenCount = uip.getIncomingData().get(ContentModelHelper.Datastream.MD_DESCRIPTIVE.getName())
				.getChildren().size();

		MODSUIPFilter filter = new MODSUIPFilter();
		filter.setSchematronValidator(schematronValidator);
		filter.doFilter(uip);
		
		
		for (Object elementObject: uip.getModifiedData().get(ContentModelHelper.Datastream.MD_DESCRIPTIVE.getName()).getChildren()){
			Element element = (Element)elementObject;
			System.out.println(element.getName());
		}

		XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
		outputter.output(uip.getModifiedData().get(ContentModelHelper.Datastream.MD_DESCRIPTIVE.getName()), System.out);
		
		assertEquals(incomingChildrenCount,
				uip.getIncomingData().get(ContentModelHelper.Datastream.MD_DESCRIPTIVE.getName()).getChildren().size());
		assertEquals(incomingChildrenCount,
				uip.getModifiedData().get(ContentModelHelper.Datastream.MD_DESCRIPTIVE.getName()).getChildren().size());
		//Assert that the new modified object isn't the incoming object
		assertFalse(uip.getModifiedData().get(ContentModelHelper.Datastream.MD_DESCRIPTIVE.getName())
				.equals(uip.getIncomingData().get(ContentModelHelper.Datastream.MD_DESCRIPTIVE.getName())));
		
	}
	
	@Test
	public void invalidMODS() throws Exception {
		SAXBuilder builder = new SAXBuilder();
		
		InputStream modsStream = new FileInputStream(new File("src/test/resources/invalidMODS.xml"));
		org.jdom.Document modsDoc = builder.build(modsStream);
		org.jdom.Element modsElement = modsDoc.detachRootElement();

		PID pid = new PID("uuid:test");
		PersonAgent user = new PersonAgent("testuser", "testuser");
		MetadataUIP uip = new MetadataUIP(pid, user, UpdateOperation.ADD);
		uip.getIncomingData().put(ContentModelHelper.Datastream.MD_DESCRIPTIVE.getName(), modsElement);
		
		MODSUIPFilter filter = new MODSUIPFilter();
		filter.setSchematronValidator(schematronValidator);
		try {
			filter.doFilter(uip);
			fail();
		} catch (UIPException expected){
			assertEquals("MODS failed to validate to schema", expected.getMessage());
		}
	}
	
	@Test
	public void wrongUIPType() throws UIPException{
		ContentUIP uip = mock(ContentUIP.class);
		
		MODSUIPFilter filter = new MODSUIPFilter();
		filter.doFilter(uip);
		
		verify(uip, times(0)).getIncomingData();
		verify(uip, times(0)).getModifiedData();
	}
	
	@Test
	public void nullUIP() throws UIPException{
		MODSUIPFilter filter = new MODSUIPFilter();
		filter.doFilter(null);
		//Passes if no exception
	}

	public SchematronValidator getSchematronValidator() {
		return schematronValidator;
	}

	public void setSchematronValidator(SchematronValidator schematronValidator) {
		this.schematronValidator = schematronValidator;
	}
}
