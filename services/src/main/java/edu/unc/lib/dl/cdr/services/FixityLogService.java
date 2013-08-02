package edu.unc.lib.dl.cdr.services;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.unc.lib.dl.cdr.services.processing.ServiceConductor;
import edu.unc.lib.dl.cdr.services.processing.ServicesThreadPoolExecutor;
import edu.unc.lib.dl.services.FixityLogTask;
import edu.unc.lib.dl.services.FixityLogTaskFactory;

public class FixityLogService implements ServiceConductor {
	
	private static final Log LOG = LogFactory.getLog(FixityLogService.class);
	private static final String identifier = "FixityLogService";

	protected ServicesThreadPoolExecutor<FixityLogTask> executor = null;
	private FixityLogTaskFactory fixityLogTaskFactory = null;
	private Timer pollingTimer = null;

	public void init() {
		initializeExecutor();
		
		pollingTimer = new Timer();
		pollingTimer.schedule(new ExecuteTask(), 0, 1000 * 60);
	}
	
	private void initializeExecutor() {
		this.executor = new ServicesThreadPoolExecutor<FixityLogTask>(1, "FixityLog");
	}

	public void destroy() {
		this.shutdown();
		this.pollingTimer.cancel();
	}

	
	// Periodically create and execute a new FixityLogTask instance
	
	class ExecuteTask extends TimerTask {
		public void run() {
			executeFixityLogTask();
		}
	}
	
	private void executeFixityLogTask() {
		LOG.debug("Creating and executing fixity log task");
		
		FixityLogTask task = this.fixityLogTaskFactory.createTask();
		this.executor.execute(task);
	}
	
	
	// Accessors
	
	public FixityLogTaskFactory getFixityLogTaskFactory() {
		return fixityLogTaskFactory;
	}

	public void setFixityLogTaskFactory(FixityLogTaskFactory fixityLogTaskFactory) {
		this.fixityLogTaskFactory = fixityLogTaskFactory;
	}
	
	
	// ServiceConductor implementation methods

	@Override
	public String getIdentifier() {
		return identifier;
	}

	@Override
	public void pause() {
		this.executor.pause();
	}

	@Override
	public void resume() {
		this.executor.resume();
	}

	@Override
	public boolean isPaused() {
		return this.executor.isPaused();
	}

	@Override
	public boolean isEmpty() {
		return this.executor.getQueue().isEmpty();
	}

	@Override
	public boolean isIdle() {
		return this.executor.isPaused() || this.executor.getAllRunningAndQueued().isEmpty();
	}

	@Override
	public void shutdown() {
		this.executor.shutdownNow();
	}

	@Override
	public void shutdownNow() {
		this.executor.shutdownNow();
	}

	@Override
	public void abort() {
		this.executor.pause();
		this.executor.shutdownNow();
		try {
			this.executor.awaitTermination(5, TimeUnit.MINUTES);
		} catch (InterruptedException ignored) {
		}
		initializeExecutor();
		this.executor.pause();
	}

	@Override
	public void restart() {
		if (this.executor == null || this.executor.isShutdown() || this.executor.isTerminated())
			initializeExecutor();
	}

	@Override
	public int getActiveThreadCount() {
		return this.executor.getActiveCount();
	}

}
