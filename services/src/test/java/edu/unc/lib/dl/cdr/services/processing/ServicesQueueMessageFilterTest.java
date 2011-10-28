package edu.unc.lib.dl.cdr.services.processing;

import javax.annotation.Resource;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import edu.unc.lib.dl.cdr.services.model.PIDMessage;
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
	public void fedoraMessage(){
		
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

	
}
