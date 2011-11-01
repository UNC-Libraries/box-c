package edu.unc.lib.dl.cdr.services.processing;

import java.io.InputStreamReader;

import javax.annotation.Resource;

import org.jdom.Document;
import org.jdom.input.SAXBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import edu.unc.lib.dl.cdr.services.ObjectEnhancementService;
import edu.unc.lib.dl.cdr.services.imaging.ImageEnhancementService;
import edu.unc.lib.dl.cdr.services.imaging.ThumbnailEnhancementService;
import edu.unc.lib.dl.cdr.services.model.PIDMessage;
import edu.unc.lib.dl.cdr.services.solr.SolrUpdateEnhancementService;
import edu.unc.lib.dl.cdr.services.techmd.TechnicalMetadataEnhancementService;
import edu.unc.lib.dl.cdr.services.util.JMSMessageUtil;
import edu.unc.lib.dl.util.ContentModelHelper;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/service-context-unit.xml" })
public class ServicesQueueMessageFilterTest extends Assert {

	@Resource
	private ServicesQueueMessageFilter servicesMessageFilter;
	
	@Before
   public void setUp() throws Exception {
	}
	
	@Test
	public void serviceMessage(){
		PIDMessage message = new PIDMessage("cdr:test", JMSMessageUtil.servicesMessageNamespace, 
				JMSMessageUtil.ServicesActions.APPLY_SERVICE.getName(), "");
		assertFalse(servicesMessageFilter.filter(message));
		message.setServiceName(TechnicalMetadataEnhancementService.class.getName());
		assertTrue(servicesMessageFilter.filter(message));
		message.setServiceName(null);
		assertFalse(servicesMessageFilter.filter(message));
		message.setServiceName("does.not.exist.Service");
		assertFalse(servicesMessageFilter.filter(message));
		message.setServiceName("");
		assertFalse(servicesMessageFilter.filter(message));
		//Full stack run
		message = new PIDMessage("cdr:test", JMSMessageUtil.servicesMessageNamespace, 
				JMSMessageUtil.ServicesActions.APPLY_SERVICE_STACK.getName());
		assertTrue(servicesMessageFilter.filter(message));
		message = new PIDMessage("cdr:test", JMSMessageUtil.servicesMessageNamespace, 
				JMSMessageUtil.ServicesActions.APPLY_SERVICE_STACK.getName(), "");
		assertTrue(servicesMessageFilter.filter(message));
	}
	
	@Test
	public void nullMessage(){
		assertFalse(servicesMessageFilter.filter(null));
		String pid = null;
		try {
			servicesMessageFilter.filter(new PIDMessage(pid, null, null));
			assertTrue(false);
		} catch (IllegalArgumentException e){
			assertTrue(true);
		}
		PIDMessage emptyMessage = new PIDMessage("", "", "");
		assertFalse(servicesMessageFilter.filter(emptyMessage));
	}
	
	@Test
	public void fedoraObjectMessages() throws Exception {
		
		//Ingest object message, should partially pass, not pass solr
		Document doc = readFileAsString("ingestMessage.xml");
		PIDMessage message = new PIDMessage(doc, JMSMessageUtil.fedoraMessageNamespace);
		assertTrue(servicesMessageFilter.filter(message));
		assertTrue(message.getFilteredServices().size() > 0);
		assertFalse(message.filteredServicesContains(SolrUpdateEnhancementService.class));
		assertTrue(message.filteredServicesContains(TechnicalMetadataEnhancementService.class));
		assertTrue(message.filteredServicesContains(ImageEnhancementService.class));
		assertTrue(message.filteredServicesContains(ThumbnailEnhancementService.class));
		
		//Purge object message, fail
		message.setAction(JMSMessageUtil.FedoraActions.PURGE_OBJECT.getName());
		assertFalse(servicesMessageFilter.filter(message));
	}
	
	@Test
	public void fedoraDatastreamMessages() throws Exception {
		//Change md descript datastream, should not pass filters
		Document doc = readFileAsString("modifyDSMDDescriptive.xml");
		PIDMessage message = new PIDMessage(doc, JMSMessageUtil.fedoraMessageNamespace);
		assertFalse(servicesMessageFilter.filter(message));
		assertNull(message.getFilteredServices());
		
		message.setAction(JMSMessageUtil.FedoraActions.PURGE_DATASTREAM.getName());
		assertFalse(servicesMessageFilter.filter(message));
		
		message.setAction(JMSMessageUtil.FedoraActions.ADD_DATASTREAM.getName());
		assertFalse(servicesMessageFilter.filter(message));
		
		//Change data file, should pass
		doc = readFileAsString("modifyDSDataFile.xml");
		message = new PIDMessage(doc, JMSMessageUtil.fedoraMessageNamespace);
		assertTrue(servicesMessageFilter.filter(message));
		assertTrue(message.getFilteredServices().size() > 0);
		assertTrue(message.filteredServicesContains(SolrUpdateEnhancementService.class));
		assertTrue(message.filteredServicesContains(TechnicalMetadataEnhancementService.class));
		assertTrue(message.filteredServicesContains(ImageEnhancementService.class));
		assertTrue(message.filteredServicesContains(ThumbnailEnhancementService.class));
		
		message.setAction(JMSMessageUtil.FedoraActions.PURGE_DATASTREAM.getName());
		assertFalse(servicesMessageFilter.filter(message));
		
		message.setAction(JMSMessageUtil.FedoraActions.ADD_DATASTREAM.getName());
		assertTrue(servicesMessageFilter.filter(message));
		assertTrue(message.filteredServicesContains(SolrUpdateEnhancementService.class));
		assertTrue(message.filteredServicesContains(TechnicalMetadataEnhancementService.class));
		assertTrue(message.filteredServicesContains(ImageEnhancementService.class));
		assertTrue(message.filteredServicesContains(ThumbnailEnhancementService.class));
	}
	
	@Test
	public void fedoraRelationMessages() throws Exception {
		//Add relation tests
		Document doc = readFileAsString("addRelSourceData.xml");
		PIDMessage message = new PIDMessage(doc, JMSMessageUtil.fedoraMessageNamespace);
		assertTrue(servicesMessageFilter.filter(message));
		assertTrue(message.filteredServicesContains(SolrUpdateEnhancementService.class));
		assertFalse(message.filteredServicesContains(TechnicalMetadataEnhancementService.class));
		assertFalse(message.filteredServicesContains(ImageEnhancementService.class));
		assertTrue(message.filteredServicesContains(ThumbnailEnhancementService.class));
		
		message.setAction(JMSMessageUtil.FedoraActions.PURGE_RELATIONSHIP.getName());
		assertTrue(servicesMessageFilter.filter(message));
		assertTrue(message.filteredServicesContains(SolrUpdateEnhancementService.class));
		assertFalse(message.filteredServicesContains(TechnicalMetadataEnhancementService.class));
		assertFalse(message.filteredServicesContains(ImageEnhancementService.class));
		assertTrue(message.filteredServicesContains(ThumbnailEnhancementService.class));
		
		message.setRelation(ContentModelHelper.CDRProperty.hasSurrogate.getURI().toString());
		message.setAction(JMSMessageUtil.FedoraActions.ADD_RELATIONSHIP.getName());
		assertTrue(servicesMessageFilter.filter(message));
		assertTrue(message.filteredServicesContains(SolrUpdateEnhancementService.class));
		assertFalse(message.filteredServicesContains(TechnicalMetadataEnhancementService.class));
		assertFalse(message.filteredServicesContains(ImageEnhancementService.class));
		assertTrue(message.filteredServicesContains(ThumbnailEnhancementService.class));
		
		message.setAction(JMSMessageUtil.FedoraActions.PURGE_RELATIONSHIP.getName());
		assertTrue(servicesMessageFilter.filter(message));
		assertTrue(message.filteredServicesContains(SolrUpdateEnhancementService.class));
		assertFalse(message.filteredServicesContains(TechnicalMetadataEnhancementService.class));
		assertFalse(message.filteredServicesContains(ImageEnhancementService.class));
		assertTrue(message.filteredServicesContains(ThumbnailEnhancementService.class));
		
		message.setRelation(ContentModelHelper.CDRProperty.techData.getURI().toString());
		message.setAction(JMSMessageUtil.FedoraActions.ADD_RELATIONSHIP.getName());
		assertFalse(servicesMessageFilter.filter(message));
		
		message.setAction(JMSMessageUtil.FedoraActions.PURGE_RELATIONSHIP.getName());
		assertFalse(servicesMessageFilter.filter(message));
		
		message.setRelation(ContentModelHelper.CDRProperty.thumb.getURI().toString());
		message.setAction(JMSMessageUtil.FedoraActions.ADD_RELATIONSHIP.getName());
		assertFalse(servicesMessageFilter.filter(message));
		
		message.setAction(JMSMessageUtil.FedoraActions.PURGE_RELATIONSHIP.getName());
		assertFalse(servicesMessageFilter.filter(message));
	}
	
	@Test
	public void unacceptableValidMessages(){
		
	}

	public ServicesQueueMessageFilter getServicesMessageFilter() {
		return servicesMessageFilter;
	}

	public void setServicesMessageFilter(ServicesQueueMessageFilter servicesMessageFilter) {
		this.servicesMessageFilter = servicesMessageFilter;
	}

	private Document readFileAsString(String filePath) throws Exception {
		return new SAXBuilder().build(new InputStreamReader(this.getClass().getResourceAsStream(filePath)));
	}
}
