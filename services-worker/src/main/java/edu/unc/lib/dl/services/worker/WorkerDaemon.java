package edu.unc.lib.dl.services.worker;

import net.greghaines.jesque.Job;
import net.greghaines.jesque.worker.Worker;
import net.greghaines.jesque.worker.WorkerEvent;
import net.greghaines.jesque.worker.WorkerListener;
import net.greghaines.jesque.worker.WorkerPool;

import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
import org.apache.commons.daemon.DaemonInitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class WorkerDaemon implements Daemon, WorkerListener {
	
	private static final Logger LOG = LoggerFactory.getLogger(WorkerDaemon.class);
	private AbstractApplicationContext appContext;
	
	public WorkerDaemon() {
	}

	@Override
	public void init(DaemonContext context) throws DaemonInitException, Exception {
		LOG.debug("Daemon initialized with arguments {}.", context.getArguments());
	}

	@Override
	public void start() throws Exception {
		LOG.info("Starting the services worker daemon");

		if (appContext == null) {
			appContext = new ClassPathXmlApplicationContext(new String[] { "service-context.xml" });
			appContext.registerShutdownHook();
		} else {
			appContext.refresh();
		}
		
		WorkerPool workerPool = appContext.getBean(WorkerPool.class);
		workerPool.getWorkerEventEmitter().addListener(this);
		workerPool.run();
	}

	@Override
	public void stop() throws Exception {
		LOG.info("Stopping the services worker daemon");
		
		WorkerPool workerPool = appContext.getBean(WorkerPool.class);
		workerPool.end(true);
		appContext.stop();
	}

	@Override
	public void destroy() {
		LOG.info("Destroying the services worker daemon");
	}
	
	@Override
	public void onEvent(WorkerEvent event, Worker worker, String queue, Job job, Object runner, Object result, Throwable t) {
		if (event == null || event == WorkerEvent.WORKER_POLL) {
			return;
		}
		
		LOG.info("onEvent event={}, worker={}, queue={}, job={}, runner={}, result={}, t={}", new Object[] { event, worker, queue, job, runner, result, t });
		
		if (t != null) {
			LOG.error("Worker error", t);
		}
		
	}

}
