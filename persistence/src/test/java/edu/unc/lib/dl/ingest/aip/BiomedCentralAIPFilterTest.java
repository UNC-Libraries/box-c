package edu.unc.lib.dl.ingest.aip;

import static org.mockito.Mockito.*;

import java.io.File;

import javax.annotation.Resource;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.output.XMLOutputter;
import org.jrdf.graph.Graph;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import edu.unc.lib.dl.agents.AgentFactory;
import edu.unc.lib.dl.agents.PersonAgent;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.ingest.sip.METSPackageSIP;
import edu.unc.lib.dl.ingest.sip.METSPackageSIPProcessor;
import edu.unc.lib.dl.schematron.SchematronValidator;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.util.DepositMethod;
import edu.unc.lib.dl.util.JRDFGraphUtil;
import edu.unc.lib.dl.util.PackagingType;
import edu.unc.lib.dl.xml.FOXMLJDOMUtil;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/service-context.xml" })
public class BiomedCentralAIPFilterTest extends Assert {
	private static Logger LOG = Logger.getLogger(BiomedCentralAIPFilterTest.class);
	
	@Resource
	private METSPackageSIPProcessor metsPackageSIPProcessor = null;
	private PersonAgent biomedAgent;
	@Resource
	private SchematronValidator schematronValidator;
	
	public BiomedCentralAIPFilterTest(){
		biomedAgent = new PersonAgent(new PID("uuid:biomed"), "Biomed Central", "biomedcentral");
	}
	
	@Test
	public void xmlParseTest() throws Exception{
		AgentFactory agentFactory = mock(AgentFactory.class);
		when(agentFactory.findPersonByOnyen(anyString(), anyBoolean())).thenReturn(biomedAgent);
		BiomedCentralAIPFilter filter = new BiomedCentralAIPFilter();
		filter.setAgentFactory(agentFactory);
		filter.init();
		
		File ingestPackage = new File("src/test/resources/biomedWithSupplements.zip");
		PID containerPID = new PID("uuid:container");
		METSPackageSIP sip = new METSPackageSIP(containerPID, ingestPackage, true);
		
		DepositRecord record = new DepositRecord(biomedAgent, biomedAgent, DepositMethod.SWORD13);
		record.setPackagingType(PackagingType.METS_DSPACE_SIP_2);
		
		RDFAwareAIPImpl aip = (RDFAwareAIPImpl)metsPackageSIPProcessor.createAIP(sip, record);
		
		filter.doFilter(aip);
		
		Graph graph = aip.getGraph();
		
		PID articlePID = JRDFGraphUtil.getPIDRelationshipSubject(graph, ContentModelHelper.CDRProperty.slug.getURI(), "1471-2458-11-702.pdf");
		PID aggregatePID = JRDFGraphUtil.getPIDRelationshipSubject(graph, ContentModelHelper.Relationship.contains.getURI(), articlePID);
		PID xmlPID = JRDFGraphUtil.getPIDRelationshipSubject(graph, ContentModelHelper.CDRProperty.slug.getURI(), "1471-2458-11-702.xml");
		PID s1PID = JRDFGraphUtil.getPIDRelationshipSubject(graph, ContentModelHelper.CDRProperty.slug.getURI(), "1471-2458-11-702-S1.PDF");
		PID s2PID = JRDFGraphUtil.getPIDRelationshipSubject(graph, ContentModelHelper.CDRProperty.slug.getURI(), "1471-2458-11-702-S2.PDF");
		PID s3PID = JRDFGraphUtil.getPIDRelationshipSubject(graph, ContentModelHelper.CDRProperty.slug.getURI(), "1471-2458-11-702-S3.PDF");
		assertNotNull(articlePID);
		assertNotNull(aggregatePID);
		assertNotNull(xmlPID);
		assertNotNull(s1PID);
		assertNotNull(s2PID);
		assertNotNull(s3PID);
		
		String defaultWebPID = JRDFGraphUtil.getRelationshipObjectURIs(graph, aggregatePID, ContentModelHelper.CDRProperty.defaultWebObject.getURI()).get(0).toString();
		assertTrue(defaultWebPID.equals(articlePID.getURI()));
		
		String supplementTitle = FOXMLJDOMUtil.getLabel(aip.getFOXMLDocument(s1PID));
		assertTrue("Technical assistance checklist. PDF of list of commonly provided technical assistance subjects.".equals(supplementTitle));
		supplementTitle = FOXMLJDOMUtil.getLabel(aip.getFOXMLDocument(s2PID));
		assertTrue("Survey. PDF of survey used for participating member (i.e., client) survey.".equals(supplementTitle));
		supplementTitle = FOXMLJDOMUtil.getLabel(aip.getFOXMLDocument(s3PID));
		assertTrue("Interview protocol. PDF of interview protocol used for staff interviews.".equals(supplementTitle));
		
		String allowIndexing = JRDFGraphUtil.getRelatedLiteralObject(graph, xmlPID, ContentModelHelper.CDRProperty.allowIndexing.getURI());
		assertTrue("no".equals(allowIndexing));
		
		assertTrue(aip.getDepositRecord().getPackagingSubType().equals("BiomedCentral"));
		
		//Make sure it passes dc filter
		DublinCoreCrosswalkFilter dcFilter = new DublinCoreCrosswalkFilter();
		dcFilter.doFilter(aip);
		
		//Make sure if passes mods test
		MODSValidationFilter modsFilter = new MODSValidationFilter();
		modsFilter.setSchematronValidator(schematronValidator);
		modsFilter.doFilter(aip);
		
		logXML(aip.getFOXMLDocument(aggregatePID));
	}
	
	@Test
	public void rejectedAgentTest() throws Exception{
		AgentFactory agentFactory = mock(AgentFactory.class);
		when(agentFactory.findPersonByOnyen(anyString(), anyBoolean())).thenReturn(biomedAgent);
		BiomedCentralAIPFilter filter = new BiomedCentralAIPFilter();
		filter.setAgentFactory(agentFactory);
		filter.init();
		
		File ingestPackage = new File("src/test/resources/dspaceMets.zip");
		PID containerPID = new PID("uuid:container");
		PersonAgent agent = new PersonAgent(new PID("notbiomed"), "notbiomed","notbiomed");
		METSPackageSIP sip = new METSPackageSIP(containerPID, ingestPackage, true);
		DepositRecord record = new DepositRecord(agent, agent, DepositMethod.SWORD13);
		record.setPackagingType(PackagingType.METS_DSPACE_SIP_2);
		RDFAwareAIPImpl aip = (RDFAwareAIPImpl)metsPackageSIPProcessor.createAIP(sip, record);
		
		filter.doFilter(aip);
		
		assertNull(aip.getDepositRecord().getPackagingSubType());
	}
	
	@Test
	public void rejectedPackageTypeTest() throws Exception{
		AgentFactory agentFactory = mock(AgentFactory.class);
		when(agentFactory.findPersonByOnyen(anyString(), anyBoolean())).thenReturn(biomedAgent);
		BiomedCentralAIPFilter filter = new BiomedCentralAIPFilter();
		filter.setAgentFactory(agentFactory);
		filter.init();
		
		File ingestPackage = new File("src/test/resources/simple.zip");
		PID containerPID = new PID("uuid:container");
		METSPackageSIP sip = new METSPackageSIP(containerPID, ingestPackage, true);
		DepositRecord record = new DepositRecord(biomedAgent, biomedAgent, DepositMethod.SWORD13);
		record.setPackagingType(PackagingType.SIMPLE_ZIP);
		RDFAwareAIPImpl aip = (RDFAwareAIPImpl)metsPackageSIPProcessor.createAIP(sip, record);
		
		filter.doFilter(aip);
		
		assertNull(aip.getDepositRecord().getPackagingSubType());
	}
	
	@Test
	public void noAggregateTest() throws Exception{
		AgentFactory agentFactory = mock(AgentFactory.class);
		when(agentFactory.findPersonByOnyen(anyString(), anyBoolean())).thenReturn(biomedAgent);
		BiomedCentralAIPFilter filter = new BiomedCentralAIPFilter();
		filter.setAgentFactory(agentFactory);
		filter.init();
		
		File ingestPackage = new File("src/test/resources/simple.zip");
		PID containerPID = new PID("uuid:container");
		METSPackageSIP sip = new METSPackageSIP(containerPID, ingestPackage, true);
		DepositRecord record = new DepositRecord(biomedAgent, biomedAgent, DepositMethod.SWORD13);
		record.setPackagingType(PackagingType.METS_DSPACE_SIP_1);
		RDFAwareAIPImpl aip = (RDFAwareAIPImpl)metsPackageSIPProcessor.createAIP(sip, record);
		
		filter.doFilter(aip);
		
		assertNull(aip.getDepositRecord().getPackagingSubType());
	}
	
	@Test
	public void invalidAIPTest() throws Exception{
		AgentFactory agentFactory = mock(AgentFactory.class);
		when(agentFactory.findPersonByOnyen(anyString(), anyBoolean())).thenReturn(biomedAgent);
		BiomedCentralAIPFilter filter = new BiomedCentralAIPFilter();
		filter.setAgentFactory(agentFactory);
		filter.init();
		
		DepositRecord record = new DepositRecord(biomedAgent, biomedAgent, DepositMethod.SWORD13);
		record.setPackagingType(PackagingType.METS_DSPACE_SIP_1);
		AIPImpl aip = new AIPImpl(record);
		
		try {
			filter.doFilter(aip);
			fail();
		} catch (AIPException e){
			//Excepted
		}
		
		assertNull(aip.getDepositRecord().getPackagingSubType());
	}
	
	@Test
	public void missingTitlesTest() throws Exception{
		AgentFactory agentFactory = mock(AgentFactory.class);
		when(agentFactory.findPersonByOnyen(anyString(), anyBoolean())).thenReturn(biomedAgent);
		BiomedCentralAIPFilter filter = new BiomedCentralAIPFilter();
		filter.setAgentFactory(agentFactory);
		filter.init();
		
		File ingestPackage = new File("src/test/resources/biomedMissingSupplementTitle.zip");
		PID containerPID = new PID("uuid:container");
		METSPackageSIP sip = new METSPackageSIP(containerPID, ingestPackage, true);
		
		DepositRecord record = new DepositRecord(biomedAgent, biomedAgent, DepositMethod.SWORD13);
		record.setPackagingType(PackagingType.METS_DSPACE_SIP_2);
		
		RDFAwareAIPImpl aip = (RDFAwareAIPImpl)metsPackageSIPProcessor.createAIP(sip, record);
		
		filter.doFilter(aip);
		
		Graph graph = aip.getGraph();
		
		PID articlePID = JRDFGraphUtil.getPIDRelationshipSubject(graph, ContentModelHelper.CDRProperty.slug.getURI(), "1471-2458-11-702.pdf");
		PID aggregatePID = JRDFGraphUtil.getPIDRelationshipSubject(graph, ContentModelHelper.Relationship.contains.getURI(), articlePID);
		PID xmlPID = JRDFGraphUtil.getPIDRelationshipSubject(graph, ContentModelHelper.CDRProperty.slug.getURI(), "1471-2458-11-702.xml");
		PID s1PID = JRDFGraphUtil.getPIDRelationshipSubject(graph, ContentModelHelper.CDRProperty.slug.getURI(), "1471-2458-11-702-S1.PDF");
		PID s2PID = JRDFGraphUtil.getPIDRelationshipSubject(graph, ContentModelHelper.CDRProperty.slug.getURI(), "1471-2458-11-702-S2.PDF");
		PID s3PID = JRDFGraphUtil.getPIDRelationshipSubject(graph, ContentModelHelper.CDRProperty.slug.getURI(), "1471-2458-11-702-S3.PDF");
		assertNotNull(articlePID);
		assertNotNull(aggregatePID);
		assertNotNull(xmlPID);
		assertNotNull(s1PID);
		assertNotNull(s2PID);
		assertNotNull(s3PID);
		
		String defaultWebPID = JRDFGraphUtil.getRelationshipObjectURIs(graph, aggregatePID, ContentModelHelper.CDRProperty.defaultWebObject.getURI()).get(0).toString();
		assertTrue(defaultWebPID.equals(articlePID.getURI()));
		
		String supplementTitle = FOXMLJDOMUtil.getLabel(aip.getFOXMLDocument(s1PID));
		assertTrue("Technical assistance checklist. PDF of list of commonly provided technical assistance subjects.".equals(supplementTitle));
		supplementTitle = FOXMLJDOMUtil.getLabel(aip.getFOXMLDocument(s2PID));
		assertTrue("1471-2458-11-702-S2.PDF".equals(supplementTitle));
		supplementTitle = FOXMLJDOMUtil.getLabel(aip.getFOXMLDocument(s3PID));
		assertTrue("1471-2458-11-702-S3.PDF".equals(supplementTitle));
		
		String allowIndexing = JRDFGraphUtil.getRelatedLiteralObject(graph, xmlPID, ContentModelHelper.CDRProperty.allowIndexing.getURI());
		assertTrue("no".equals(allowIndexing));
	}
	
	private void logXML(Document xml){
		XMLOutputter outputter = new XMLOutputter();
		LOG.debug(outputter.outputString(xml));
	}

	public METSPackageSIPProcessor getMetsPackageSIPProcessor() {
		return metsPackageSIPProcessor;
	}

	public void setMetsPackageSIPProcessor(METSPackageSIPProcessor metsPackageSIPProcessor) {
		this.metsPackageSIPProcessor = metsPackageSIPProcessor;
	}

	public SchematronValidator getSchematronValidator() {
		return schematronValidator;
	}

	public void setSchematronValidator(SchematronValidator schematronValidator) {
		this.schematronValidator = schematronValidator;
	}
}
