package edu.unc.lib.dl.event;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Date;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Premis;
import edu.unc.lib.dl.util.PremisEventBuilder;

public class PremisLoggerTest {
	private String depositUUID;
	private PID pid;
	private Resource eventType;
	private File file;
	private PremisLogger premis;
	private Date date;
	
	@Before
	public void setup() throws Exception {
		initMocks(this);
		
		depositUUID = UUID.randomUUID().toString();
		pid = new PID(depositUUID);
		eventType = Premis.VirusCheck;
		file = new File(depositUUID + ".ttl");
		premis = new PremisLogger(pid, file);
		date = new Date();
	}
	
	@Test
	public void testTripleCreation() {
		PremisEventBuilder builder = premis.buildEvent(eventType, date);
		
		assertTrue("Returned object is not a PremisLogger", premis instanceof PremisLogger);
		assertTrue("Returned object is not a PremisEventBuilder", builder instanceof PremisEventBuilder);	
	}
	
	@Test
	public void testTripleWrite() throws FileNotFoundException {
		String message = "Test event successfully added";
		String name = "ClamAV";
		String versionNumber = "3.2.1";
		
		Model objModel = premis.objectModel();
		Model premisBuilder = premis.buildEvent(eventType, date)
				.addEventDetail(message)
				.addSoftwareAgent(name, versionNumber)
				.create();
		
		Model writeModel = premis.modelMerge(objModel, premisBuilder);
		premis.writeEvent(writeModel);
		
		InputStream in = new FileInputStream(this.file);
		Model model = ModelFactory.createDefaultModel().read(in, null, "TURTLE");
		Resource resource = model.getResource(premis.cdrEventURI + premis.eventId);
		
		assertTrue("File doesn't exist", file.exists());
		assertEquals("Virus check property event not written to file", eventType, resource.getProperty(Premis.hasEventType).getObject());
		assertEquals("Virus check property detailed message not written to file", message, resource.getProperty(Premis.hasEventDetail).getObject().toString());
		assertEquals("Virus check property software agent not written to file", name+" ("+versionNumber+")", resource.getProperty(Premis.hasAgentName).getObject().toString());
	} 
}
