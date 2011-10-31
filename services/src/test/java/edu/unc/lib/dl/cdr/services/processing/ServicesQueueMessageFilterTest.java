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
import edu.unc.lib.dl.cdr.services.model.PIDMessage;
import edu.unc.lib.dl.cdr.services.solr.SolrUpdateEnhancementService;
import edu.unc.lib.dl.cdr.services.techmd.TechnicalMetadataEnhancementService;
import edu.unc.lib.dl.cdr.services.util.JMSMessageUtil;

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
	public void fedoraMessage() throws Exception {
		Document doc = readFileAsString("ingestMessage.xml");
		PIDMessage message = new PIDMessage(doc, JMSMessageUtil.fedoraMessageNamespace);
		assertTrue(servicesMessageFilter.filter(message));
		assertTrue(message.getFilteredServices().size() > 0);
		int i = 0;
		for (ObjectEnhancementService service: message.getFilteredServices()){
			if (SolrUpdateEnhancementService.class.equals(service.getClass())){
				break;
			}
			i++;
		}
		if (i != message.getFilteredServices().size())
			fail();
		
		doc = readFileAsString("modifyDSMDDescriptive.xml");
		message = new PIDMessage(doc, JMSMessageUtil.fedoraMessageNamespace);
		assertFalse(servicesMessageFilter.filter(message));
		assertNull(message.getFilteredServices());
		
		doc = readFileAsString("modifyDSDataFile.xml");
		message = new PIDMessage(doc, JMSMessageUtil.fedoraMessageNamespace);
		assertTrue(servicesMessageFilter.filter(message));
		assertTrue(message.getFilteredServices().size() > 0);
		i = 0;
		for (ObjectEnhancementService service: message.getFilteredServices()){
			if (SolrUpdateEnhancementService.class.equals(service.getClass())){
				break;
			}
			i++;
		}
		if (i == message.getFilteredServices().size())
			fail();
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
