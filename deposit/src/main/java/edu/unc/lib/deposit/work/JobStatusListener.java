package edu.unc.lib.deposit.work;

import net.greghaines.jesque.Job;
import net.greghaines.jesque.worker.Worker;
import net.greghaines.jesque.worker.WorkerEvent;
import net.greghaines.jesque.worker.WorkerEventEmitter;
import net.greghaines.jesque.worker.WorkerListener;

public class JobStatusListener implements WorkerListener {

	private WorkerEventEmitter workerEventEmitter;
	
	public WorkerEventEmitter getWorkerEventEmitter() {
		return workerEventEmitter;
	}

	public void setWorkerEventEmitter(WorkerEventEmitter workerEventEmitter) {
		this.workerEventEmitter = workerEventEmitter;
	}
	
	JobStatusFactory jobStatusFactory = null;

	public JobStatusFactory getJobStatusFactory() {
		return jobStatusFactory;
	}

	public void setJobStatusFactory(JobStatusFactory jobStatusFactory) {
		this.jobStatusFactory = jobStatusFactory;
	}

	public JobStatusListener() {
	}
	
	public void init() {
		if(workerEventEmitter != null) this.workerEventEmitter.addListener(this);
	}
	
	public void destroy() {
		if(workerEventEmitter != null) workerEventEmitter.removeListener(this);
	}

	@Override
	public void onEvent(WorkerEvent event, Worker worker, String queue,
			Job job, Object runner, Object result, Exception ex) {
		if (runner instanceof AbstractDepositJob) {
			AbstractDepositJob j = (AbstractDepositJob)runner;
			switch (event) {
			case JOB_EXECUTE:
				getJobStatusFactory().started(j);
			case JOB_SUCCESS:
				getJobStatusFactory().completed(j);
			case JOB_FAILURE:
				if(ex != null) {
					getJobStatusFactory().failed(j, ex.getLocalizedMessage());
				} else {
					getJobStatusFactory().failed(j);
				}
			default:
				break;
			}
		}

	}

}
