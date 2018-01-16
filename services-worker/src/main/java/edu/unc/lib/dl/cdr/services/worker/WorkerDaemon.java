package edu.unc.lib.dl.cdr.services.worker;

import java.util.Map;

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

import edu.unc.lib.dl.fedora.ManagementClient;

public class WorkerDaemon implements Daemon, WorkerListener {

	private static final Logger LOG = LoggerFactory.getLogger(WorkerDaemon.class);
	private AbstractApplicationContext appContext;
	private ManagementClient managementClient;

	public WorkerDaemon() {
	}

	@Override
	public void init(DaemonContext context) throws DaemonInitException, Exception {
		LOG.debug("Daemon initialized with arguments {}.", context.getArguments());
	}

	@Override
	public void start() throws Exception {
		long backoffDelay = 10000;
		int backoffAttempts = 1;
		int maxBackoffAttempts = 5;

		LOG.info("Starting the services worker daemon");

		while (backoffAttempts <= maxBackoffAttempts) {
			if (appContext == null) {
    				appContext = new ClassPathXmlApplicationContext(new String[] { "service-context.xml" });
    				appContext.registerShutdownHook();
			} else {
				appContext.refresh();
			}

			if (managementClient.isRepositoryAvailable()) {
				Map<String, WorkerPool> workerPools = appContext.getBeansOfType(WorkerPool.class);
				for (WorkerPool workerPool : workerPools.values()) {
					workerPool.getWorkerEventEmitter().addListener(this);
					workerPool.run();
				}
				
				break;
			} else {
				LOG.warn("Unable to connect to fedora. Retrying starting Worker Daemon. "
					+ "Retry attempt" + backoffAttempts);

				try {
					Thread.sleep(backoffDelay * backoffAttempts);
				} catch (InterruptedException e) {
					return;
				}

				if (backoffAttempts == 5) {
					LOG.error("Services worker daemon not started. Could not connect to Fedora after " + backoffAttempts + " attempts");
				}

				backoffAttempts++;
			}
		}
	}

	@Override
	public void stop() throws Exception {
		LOG.info("Stopping the services worker daemon");

		Map<String, WorkerPool> workerPools = appContext.getBeansOfType(WorkerPool.class);
		for (WorkerPool workerPool : workerPools.values()) {
			workerPool.end(true);
		}
		appContext.stop();
	}

	@Override
	public void destroy() {
		LOG.info("Destroying the services worker daemon");
	}

	private static final String onEventLogMessage = "onEvent event={}, worker={}, queue={}, job={}, runner={}, result={}, t={}";

	@Override
	public void onEvent(WorkerEvent event, Worker worker, String queue, Job job, Object runner, Object result, Throwable t) {
		if (event == null || event == WorkerEvent.WORKER_POLL) {
			return;
		}

		if (t != null) {
			LOG.error("Throwable caused worker event " + event, t);
		}

		Object[] params = new Object[] { event, worker, queue, job, runner, result, t };

		if (event == WorkerEvent.WORKER_ERROR || event == WorkerEvent.JOB_FAILURE) {
			LOG.error(onEventLogMessage, params);
		} else if (event == WorkerEvent.WORKER_START || event == WorkerEvent.WORKER_STOP) {
			LOG.info(onEventLogMessage, params);
		} else {
			LOG.debug(onEventLogMessage, params);
		}
	}

}
