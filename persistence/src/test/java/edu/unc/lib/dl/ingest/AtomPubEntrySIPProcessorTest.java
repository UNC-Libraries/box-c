package edu.unc.lib.dl.ingest;

import static edu.unc.lib.dl.util.FileUtils.tempCopy;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import javax.annotation.Resource;

import org.apache.abdera.Abdera;
import org.apache.abdera.model.Document;
import org.apache.abdera.model.Entry;
import org.apache.abdera.parser.Parser;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import edu.unc.lib.dl.agents.Agent;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.ingest.aip.ArchivalInformationPackage;
import edu.unc.lib.dl.ingest.aip.DepositRecord;
import edu.unc.lib.dl.ingest.aip.RDFAwareAIPImpl;
import edu.unc.lib.dl.ingest.sip.AtomPubEntrySIP;
import edu.unc.lib.dl.ingest.sip.AtomPubEntrySIPProcessor;
import edu.unc.lib.dl.schematron.SchematronValidator;
import edu.unc.lib.dl.services.AgentManager;
import edu.unc.lib.dl.util.AtomPubMetadataParserUtil;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.util.DepositMethod;
import edu.unc.lib.dl.util.JRDFGraphUtil;
import edu.unc.lib.dl.util.TripleStoreQueryService;
import edu.unc.lib.dl.util.ContentModelHelper.Datastream;
import edu.unc.lib.dl.xml.FOXMLJDOMUtil;

import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/service-context.xml" })
public class AtomPubEntrySIPProcessorTest extends Assert {
	@Resource
	private AtomPubEntrySIPProcessor sipProcessor;
	@Resource
	private SchematronValidator schematronValidator;
	@Resource
	private TripleStoreQueryService tripleStoreQueryService;
	
	@Test
	public void atomPubMODSMetadataOnlyIngest() throws Exception {
		when(tripleStoreQueryService.lookupRepositoryPath(any(PID.class))).thenReturn("/uuid:parent");
		
		File testFile = tempCopy(new File("src/test/resources/atompub/metadataUpdateMODS.xml"));
		
		Agent user = AgentManager.getAdministrativeGroupAgentStub();
		DepositRecord record = new DepositRecord(user, user, DepositMethod.Unspecified);
		
		InputStream entryPart = new FileInputStream(testFile);
		Abdera abdera = new Abdera();
		Parser parser = abdera.getParser();
		Document<Entry> entryDoc = parser.parse(entryPart);
		Entry entry = entryDoc.getRoot();
		
		PID containerPID = new PID("uuid:parent");
		
		AtomPubEntrySIP sip = new AtomPubEntrySIP(containerPID, entry);
		
		assertEquals(containerPID.getPid(), sip.getContainerPID().getPid());
		
		ArchivalInformationPackage aip = this.sipProcessor.createAIP(sip, record);
		
		assertEquals("There should be pid", 1, aip.getPIDs().size());
		
		PID pid = aip.getPIDs().iterator().next();
		
		org.jdom.Document doc = aip.getFOXMLDocument(pid);
		XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
		outputter.output(doc, System.out);
		Element ds = FOXMLJDOMUtil.getDatastream(doc, Datastream.MD_DESCRIPTIVE.getName());
		
		assertNotNull("There must be a MODS entry", ds);
		
		RDFAwareAIPImpl rdfaip = (RDFAwareAIPImpl)aip;
		String slug = JRDFGraphUtil.getRelatedLiteralObject(rdfaip.getGraph(), pid, ContentModelHelper.CDRProperty.slug.getURI());
		assertEquals("Slug did not match", "SOHP_Test", slug);
	}
	
	@Test
	public void atomPubDCMetadataOnlyIngest() throws Exception {
		when(tripleStoreQueryService.lookupRepositoryPath(any(PID.class))).thenReturn("/uuid:parent");
		
		File testFile = tempCopy(new File("src/test/resources/atompub/metadataDC.xml"));
		
		Agent user = AgentManager.getAdministrativeGroupAgentStub();
		DepositRecord record = new DepositRecord(user, user, DepositMethod.Unspecified);
		
		InputStream entryPart = new FileInputStream(testFile);
		Abdera abdera = new Abdera();
		Parser parser = abdera.getParser();
		Document<Entry> entryDoc = parser.parse(entryPart);
		Entry entry = entryDoc.getRoot();
		
		PID containerPID = new PID("uuid:parent");
		
		AtomPubEntrySIP sip = new AtomPubEntrySIP(containerPID, entry);
		
		ArchivalInformationPackage aip = this.sipProcessor.createAIP(sip, record);
		
		assertEquals("There should be pid", 1, aip.getPIDs().size());
		
		PID pid = aip.getPIDs().iterator().next();
		
		org.jdom.Document doc = aip.getFOXMLDocument(pid);
		Element ds = FOXMLJDOMUtil.getDatastream(doc, Datastream.MD_DESCRIPTIVE.getName());
		
		assertNotNull("There must be a MODS entry", ds);
		assertNull("There must not be a dcterms datastreams", FOXMLJDOMUtil.getDatastream(doc, AtomPubMetadataParserUtil.ATOM_DC_DATASTREAM));
		
		RDFAwareAIPImpl rdfaip = (RDFAwareAIPImpl)aip;
		String slug = JRDFGraphUtil.getRelatedLiteralObject(rdfaip.getGraph(), pid, ContentModelHelper.CDRProperty.slug.getURI());
		assertEquals("Slug " + slug + " did not match", "Title", slug);
	}
	
	@Test
	public void atomPubMultipleDSIngest() throws Exception {
		when(tripleStoreQueryService.lookupRepositoryPath(any(PID.class))).thenReturn("/uuid:parent");
		
		File testFile = tempCopy(new File("src/test/resources/atompub/metadataUpdateMultipleDS.xml"));
		
		Agent user = AgentManager.getAdministrativeGroupAgentStub();
		DepositRecord record = new DepositRecord(user, user, DepositMethod.Unspecified);
		
		InputStream entryPart = new FileInputStream(testFile);
		Abdera abdera = new Abdera();
		Parser parser = abdera.getParser();
		Document<Entry> entryDoc = parser.parse(entryPart);
		Entry entry = entryDoc.getRoot();
		
		PID containerPID = new PID("uuid:parent");
		
		AtomPubEntrySIP sip = new AtomPubEntrySIP(containerPID, entry);
		sip.setInProgress(true);
		
		assertEquals(containerPID.getPid(), sip.getContainerPID().getPid());
		
		ArchivalInformationPackage aip = this.sipProcessor.createAIP(sip, record);
		
		assertEquals("There should be pid", 1, aip.getPIDs().size());
		
		PID pid = aip.getPIDs().iterator().next();
		
		org.jdom.Document doc = aip.getFOXMLDocument(pid);
		aip.getEventLogger().addEvent(pid, new Element("event"));
		((RDFAwareAIPImpl)aip).prepareIngest();
		XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
		outputter.output(doc, System.out);
		Element ds = FOXMLJDOMUtil.getDatastream(doc, Datastream.MD_DESCRIPTIVE.getName());
		
		assertNotNull("There must be a MODS entry", ds);
		
		RDFAwareAIPImpl rdfaip = (RDFAwareAIPImpl)aip;
		String slug = JRDFGraphUtil.getRelatedLiteralObject(rdfaip.getGraph(), pid, ContentModelHelper.CDRProperty.slug.getURI());
		assertEquals("Slug did not match", "SOHP_Test", slug);
	}
	
	@Test
	public void suggestedSlugIngest() throws Exception {
		when(tripleStoreQueryService.lookupRepositoryPath(any(PID.class))).thenReturn("/uuid:parent");
		
		File testFile = tempCopy(new File("src/test/resources/atompub/metadataUpdateMODS.xml"));
		
		Agent user = AgentManager.getAdministrativeGroupAgentStub();
		DepositRecord record = new DepositRecord(user, user, DepositMethod.Unspecified);
		
		InputStream entryPart = new FileInputStream(testFile);
		Abdera abdera = new Abdera();
		Parser parser = abdera.getParser();
		Document<Entry> entryDoc = parser.parse(entryPart);
		Entry entry = entryDoc.getRoot();
		
		PID containerPID = new PID("uuid:parent");
		
		AtomPubEntrySIP sip = new AtomPubEntrySIP(containerPID, entry);
		sip.setSuggestedSlug("test_slug");
		
		ArchivalInformationPackage aip = this.sipProcessor.createAIP(sip, record);
		
		assertEquals("There should be pid", 1, aip.getPIDs().size());
		
		PID pid = aip.getPIDs().iterator().next();
		
		RDFAwareAIPImpl rdfaip = (RDFAwareAIPImpl)aip;
		String slug = JRDFGraphUtil.getRelatedLiteralObject(rdfaip.getGraph(), pid, ContentModelHelper.CDRProperty.slug.getURI());
		assertEquals("Slug did not match suggested", "test_slug", slug);
	}
	
	public AtomPubEntrySIPProcessor getSipProcessor() {
		return sipProcessor;
	}
	
	public void setSipProcessor(AtomPubEntrySIPProcessor sipProcessor) {
		this.sipProcessor = sipProcessor;
	}
	
	public SchematronValidator getSchematronValidator() {
		return schematronValidator;
	}
	
	public void setSchematronValidator(SchematronValidator schematronValidator) {
		this.schematronValidator = schematronValidator;
	}

	public TripleStoreQueryService getTripleStoreQueryService() {
		return tripleStoreQueryService;
	}

	public void setTripleStoreQueryService(TripleStoreQueryService tripleStoreQueryService) {
		this.tripleStoreQueryService = tripleStoreQueryService;
	}
}
