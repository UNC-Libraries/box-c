package edu.unc.lib.dl.cdr.services.processing;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.jdom.Element;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.cdr.services.AbstractFedoraEnhancementService;
import edu.unc.lib.dl.cdr.services.Enhancement;
import edu.unc.lib.dl.cdr.services.ObjectEnhancementService;
import edu.unc.lib.dl.cdr.services.exception.EnhancementException;
import edu.unc.lib.dl.cdr.services.model.FailedObjectHashMap;
import edu.unc.lib.dl.cdr.services.model.PIDMessage;
import edu.unc.lib.dl.cdr.services.util.JMSMessageUtil;
import edu.unc.lib.dl.fedora.PID;

public class ServicesConductorInterruptTest extends Assert {
	
	protected static final Logger LOG = LoggerFactory.getLogger(ServicesConductorInterruptTest.class);
	
	protected ServicesConductor servicesConductor;
	protected ServicesThreadPoolExecutor executor;
	protected List<ObjectEnhancementService> delayServices;
	
	public static AtomicInteger inIsApplicable;
	public static AtomicInteger incompleteServices;
	public static AtomicInteger betweenApplicableAndEnhancement;
	public static AtomicInteger servicesCompleted;
	public static AtomicInteger inService;
	
	private boolean blockingService;
	
	public int parallelServices;
	public long delayServiceTime = 0;
	protected int numberTestMessages;
	
	public ServicesConductorInterruptTest(){
		
	}
	
	@Before
	public void setUp() throws Exception {
		delayServices = new ArrayList<ObjectEnhancementService>();
		DelayService delayService = new DelayService();
		delayServices.add(delayService);
		
		servicesConductor = new ServicesConductor();
		servicesConductor.setRecoverableDelay(0);
		servicesConductor.setUnexpectedExceptionDelay(0);
		servicesConductor.setMaxThreads(3);
		servicesConductor.setFailedPids(new FailedObjectHashMap());
		servicesConductor.setServices(delayServices);
		servicesConductor.init();
		
		this.executor = servicesConductor.getExecutor();
		inIsApplicable = new AtomicInteger(0);
		incompleteServices = new AtomicInteger(0);
		betweenApplicableAndEnhancement = new AtomicInteger(0);
		servicesCompleted = new AtomicInteger(0);
		inService = new AtomicInteger(0);
		numberTestMessages = 10;
		delayServiceTime = 300;
		parallelServices = 3;
		blockingService = false;
	}
	
	@Test
	public void abortPause() throws InterruptedException {
		delayServiceTime = 200;
		
		assertTrue(servicesConductor.isReady());
		servicesConductor.pause();
		
		blockingService = true;
		
		assertTrue(servicesConductor.isReady());
		assertTrue(servicesConductor.isIdle());
		int numberTestMessages = 10;
		//queue items while paused, make sure they aren't moving
		for (int i=0; i<numberTestMessages; i++){
			PIDMessage message = new PIDMessage("uuid:" + i, JMSMessageUtil.servicesMessageNamespace, 
					JMSMessageUtil.ServicesActions.APPLY_SERVICE_STACK.getName());
			message.setFilteredServices(delayServices);
			servicesConductor.add(message);
		}
		
		while (servicesConductor.getQueueSize() != numberTestMessages);

		assertTrue(servicesConductor.getLockedPids().size() == 0);
		
		//Unpause and let the first max threads number of messages start processing, then pause
		servicesConductor.resume();
		
		//Wait for isApplicables to finish so that services are paused midway
		while (inService.get() < servicesConductor.getMaxThreads());
		
		servicesConductor.pause();
		
		assertEquals(servicesConductor.getLockedPids().size(), servicesConductor.getMaxThreads());
		//assertEquals(incompleteServices.get(), servicesConductor.getMaxThreads());
		assertEquals(incompleteServices.get(), numberTestMessages - servicesConductor.getQueueSize());
		//Abort the currently active threads
		servicesConductor.abort();
		
		while (servicesConductor.getExecutor().isTerminating() || servicesConductor.getExecutor().isShutdown());
		
		executor = servicesConductor.getExecutor();
		//Verify that current threads died but that the remaining items are still ready to go
		assertTrue(servicesConductor.getLockedPids().size() == 0);
		assertTrue(servicesConductor.getQueueSize() == numberTestMessages - servicesConductor.getMaxThreads());
		//LOG.debug("Queue: " + executor.getQueue().size() + " Active:" + executor.getActiveCount());
		//assertTrue(executor.getQueue().size() + executor.getActiveCount() == numberTestMessages - servicesConductor.getMaxThreads());
		
		//Process remaining message queue, then shut down conductor
		blockingService = false;
		servicesConductor.resume();
		while (servicesConductor.getLockedPids().size() > 0 || servicesConductor.getQueueSize() > 0);
		
		assertEquals(servicesCompleted.get(), numberTestMessages - servicesConductor.getMaxThreads());
	}
	
	public class DelayService extends AbstractFedoraEnhancementService {
		
		public DelayService(){
			this.active = true;
		}
		
		@Override
		public List<PID> findCandidateObjects(int maxResults) throws EnhancementException {
			return null;
		}

		@Override
		public List<PID> findStaleCandidateObjects(int maxResults, String priorToDate) throws EnhancementException {
			return null;
		}

		@Override
		public Enhancement<Element> getEnhancement(PIDMessage pid) throws EnhancementException {
			return new DelayEnhancement(this, pid);
		}

		@Override
		public boolean isApplicable(PIDMessage pid) throws EnhancementException {
			incompleteServices.incrementAndGet();
			betweenApplicableAndEnhancement.incrementAndGet();
			LOG.debug("Completed isApplicable for " + pid.getPIDString());	
			return true;
		}

		@Override
		public boolean prefilterMessage(PIDMessage pid) throws EnhancementException {
			return true;
		}

		@Override
		public boolean isStale(PID pid) throws EnhancementException {
			return false;
		}
		
	}
	
	public class DelayEnhancement extends Enhancement<Element> {

		public DelayEnhancement(ObjectEnhancementService service, PIDMessage pid) {
			super(pid);
		}
		
		@Override
		public Element call() throws EnhancementException {
			LOG.debug("Call invoked for " + this.pid.getPIDString());
			betweenApplicableAndEnhancement.decrementAndGet();
			inService.incrementAndGet();
			try {
				while (blockingService){
					if (Thread.currentThread().isInterrupted())
						throw new InterruptedException();
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return null;
			}
			incompleteServices.decrementAndGet();
			servicesCompleted.incrementAndGet();
			return null;
		}
		
	}
}
