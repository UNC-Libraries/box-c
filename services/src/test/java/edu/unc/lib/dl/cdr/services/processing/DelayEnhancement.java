package edu.unc.lib.dl.cdr.services.processing;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.cdr.services.Enhancement;
import edu.unc.lib.dl.cdr.services.ObjectEnhancementService;
import edu.unc.lib.dl.cdr.services.exception.EnhancementException;
import edu.unc.lib.dl.fedora.PID;

public class DelayEnhancement extends Enhancement<Element> {
	protected static final Logger LOG = LoggerFactory.getLogger(DelayEnhancement.class);
	
	public static AtomicInteger inIsApplicable;
	public static AtomicInteger incompleteServices;
	public static AtomicInteger betweenApplicableAndEnhancement;
	public static AtomicInteger servicesCompleted;
	public static AtomicInteger inService;
	
	public static Object blockingObject;
	public static AtomicBoolean flag;
	
	public static void init() {
		inIsApplicable = new AtomicInteger(0);
		incompleteServices = new AtomicInteger(0);
		betweenApplicableAndEnhancement = new AtomicInteger(0);
		servicesCompleted = new AtomicInteger(0);
		inService = new AtomicInteger(0);
		
		blockingObject = new Object();
		flag = new AtomicBoolean(true);
	}
	
	public DelayEnhancement(ObjectEnhancementService service, PID pid) {
		super(pid);
	}
	
	@Override
	public Element call() throws EnhancementException {
		LOG.debug("Call invoked for " + this.pid.getPid());
		betweenApplicableAndEnhancement.decrementAndGet();
		inService.incrementAndGet();
		while (flag.get()){
			synchronized(blockingObject){
				try {
					LOG.debug("Delay block begins");
					blockingObject.wait();
					LOG.debug("Delay block is over");
				} catch (InterruptedException e){
					LOG.debug("Delay block interrupted");
					Thread.currentThread().interrupt();
					return null;
				}
			}
		}
		incompleteServices.decrementAndGet();
		servicesCompleted.incrementAndGet();
		return null;
	}
	
}