package edu.unc.lib.workers;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import javax.annotation.PostConstruct;

import net.greghaines.jesque.Job;
import net.greghaines.jesque.client.Client;
import net.greghaines.jesque.worker.Worker;
import net.greghaines.jesque.worker.WorkerEvent;
import net.greghaines.jesque.worker.WorkerListener;

import org.springframework.beans.factory.annotation.Autowired;

import edu.unc.lib.bag.normalize.NormalizeBag;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.DepositStatusFactory;
import edu.unc.lib.dl.util.RedisWorkerConstants;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositField;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositState;

/**
 * Coordinates work on deposits via Redis and Resque. Responsible for putting all work
 * onto the queues. Coordinates with outside world via deposit status keys.
 * @author count0
 *
 */
public class DepositSupervisor implements WorkerListener {
	@Autowired
	DepositStatusFactory depositStatusFactory;
	
	@Autowired
	JobStatusFactory jobStatusFactory;
	
	@Autowired
	Client jesqueClient;
	
	private Timer timer;
	
	private String id;

	public DepositSupervisor() {
	}
	
	private static enum Queue {
		PREPARE;
	}
	
	@PostConstruct
	public void init() {
		id = UUID.randomUUID().toString();
		timer = new Timer("DepositSupervisor Periodic Checks");
		timer.schedule(new TimerTask() {

			@Override
			public void run() {
				for(Map<String, String> fields : depositStatusFactory.getAll()) {
					if(DepositState.registered.name().equals(fields.get(DepositField.status.name()))) {
						String uuid = fields.get(DepositField.uuid.name());
						if(depositStatusFactory.addSupervisorLock(uuid, id)) {
							try {
								String bagPath = fields.get(DepositField.bagDirectory.name());
								PID depositId = new PID("uuid:"+fields.get(DepositField.uuid.name()));
								Job job = makeJob(NormalizeBag.class.getName(), bagPath, depositId);
								jesqueClient.enqueue(Queue.PREPARE.name(), job);
							} finally {
								depositStatusFactory.removeSupervisorLock(uuid);
							}
						}
					}
				}
			}
			
		}, 2*60*1000, 15*1000);
	}
	
	public Job makeJob(String jobClassName, String bagPath, PID depositPID) {
		String uuid = UUID.randomUUID().toString();
		return new Job(jobClassName, uuid, bagPath, depositPID);
	}

	@Override
	public void onEvent(WorkerEvent event, Worker worker, String queue,
			Job job, Object runner, Object result, Exception ex) {
		// TODO respond to job success with more job scheduling or finish
		
		
		
		
		
	}

}
