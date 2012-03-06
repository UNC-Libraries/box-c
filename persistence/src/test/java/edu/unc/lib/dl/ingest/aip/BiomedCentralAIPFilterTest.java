package edu.unc.lib.dl.ingest.aip;

import static org.mockito.Mockito.*;

import java.io.File;
import java.io.IOException;

import javax.annotation.Resource;

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
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.util.DepositMethod;
import edu.unc.lib.dl.util.JRDFGraphUtil;
import edu.unc.lib.dl.util.PackagingType;
import edu.unc.lib.dl.xml.FOXMLJDOMUtil;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/service-context.xml" })
public class BiomedCentralAIPFilterTest extends Assert {
	
	@Resource
	private METSPackageSIPProcessor metsPackageSIPProcessor = null;
	
	@Test
	public void xmlParseTest() throws Exception{
		PersonAgent biomedAgent = new PersonAgent(new PID("uuid:biomed"), "Biomed Central", "biomedcentral");
		
		AgentFactory agentFactory = mock(AgentFactory.class);
		when(agentFactory.findPersonByOnyen(anyString(), anyBoolean())).thenReturn(biomedAgent);
		
		File ingestPackage = new File("src/test/resources/biomedWithSupplements.zip");
		
		PID containerPID = new PID("uuid:container");
		
		METSPackageSIP sip = new METSPackageSIP(containerPID, ingestPackage, true);
		DepositRecord record = new DepositRecord(biomedAgent, biomedAgent, DepositMethod.SWORD13);
		record.setPackagingType(PackagingType.METS_DSPACE_SIP_2);
		
		RDFAwareAIPImpl aip = (RDFAwareAIPImpl)metsPackageSIPProcessor.createAIP(sip, record);
		
		BiomedCentralAIPFilter filter = new BiomedCentralAIPFilter();
		filter.setAgentFactory(agentFactory);
		filter.init();
		
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
	}

	public METSPackageSIPProcessor getMetsPackageSIPProcessor() {
		return metsPackageSIPProcessor;
	}

	public void setMetsPackageSIPProcessor(METSPackageSIPProcessor metsPackageSIPProcessor) {
		this.metsPackageSIPProcessor = metsPackageSIPProcessor;
	}
}
