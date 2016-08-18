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
import edu.unc.lib.dl.util.SoftwareAgentConstants;
import edu.unc.lib.dl.util.SoftwareAgentConstants.SoftwareAgent;

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
		file = File.createTempFile(depositUUID, ".ttl");
		premis = new PremisLogger(pid, file);
		date = new Date();
		SoftwareAgentConstants.setCdrVersion("4.0-SNAPSHOT");
	}
	
	@Test
	public void testEventbuilderCreation() {
		PremisEventBuilder builder = premis.buildEvent(eventType, date);
		
		assertTrue("Returned object is not a PremisLogger", premis instanceof PremisLogger);
		assertTrue("Returned object is not a PremisEventBuilder", builder instanceof PremisEventBuilder);	
	}
	
	@Test
	public void testTripleWrite() throws FileNotFoundException {
		String message = "Test event successfully added";
		String detailedNote = "No viruses found";
		
		Resource premisBuilder = premis.buildEvent(eventType, date)
				.addEventDetail(message)
				.addEventDetailOutcomeNote(detailedNote)
				.addSoftwareAgent(SoftwareAgent.clamav.getFullname())
				.addAuthorizingAgent(SoftwareAgent.depositService.getFullname())
				.create();
		
		premis.writeEvent(premisBuilder);
		
		InputStream in = new FileInputStream(this.file);
		Model model = ModelFactory.createDefaultModel().read(in, null, "TURTLE");
		Resource resource = model.getResource(premis.cdrEventURI + premis.eventId);
		
		this.file.deleteOnExit();
		
		assertTrue("File doesn't exist", file.exists());
		assertEquals("Virus check property event not written to file", eventType, resource.getProperty(Premis.hasEventType).getObject());
		assertEquals("Virus check property message not written to file", message, resource.getProperty(Premis.hasEventDetail).getObject().toString());
		assertEquals("Virus check property detailed note not written to file", detailedNote, resource.getProperty(Premis.hasEventOutcomeDetailNote).getObject().toString());
		assertEquals("Virus check property depositing agent not written to file", SoftwareAgent.clamav.getFullname(), resource.getProperty(Premis.hasEventRelatedAgentExecutor)
				.getProperty(Premis.hasAgentName).getObject().toString());
		assertEquals("Virus check property authorizing agent not written to file", SoftwareAgent.depositService.getFullname(), resource.getProperty(Premis.hasEventRelatedAgentAuthorizor)
				.getProperty(Premis.hasAgentName).getObject().toString());
	} 
}
