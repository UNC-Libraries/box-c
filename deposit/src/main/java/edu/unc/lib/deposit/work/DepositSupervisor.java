package edu.unc.lib.deposit.work;

import java.text.MessageFormat;
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

import edu.unc.lib.deposit.normalize.BioMedCentralExtrasJob;
import edu.unc.lib.deposit.normalize.CDRMETS2N3BagJob;
import edu.unc.lib.deposit.normalize.DSPACEMETS2N3BagJob;
import edu.unc.lib.deposit.validate.PackageIntegrityCheckJob;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.DepositStatusFactory;
import edu.unc.lib.dl.util.PackagingType;
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
								String bagPath = fields.get(DepositField.directory.name());
								PID depositId = new PID("uuid:"+fields.get(DepositField.uuid.name()));
								Job job = makeJob(PackageIntegrityCheckJob.class.getName(), bagPath, depositId);
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
		//if(WorkerEvent.JOB_EXECUTE) {
			// TODO set start time if not set
		//}
		
		
		// See if we need package conversion to N3
		String packagingType = "";
		if(!packagingType.equals(PackagingType.BAG_WITH_N3.getUri())) {
			String convertJob = null;
			// we need to add N3 packaging to this bag
			if(packagingType.equals(PackagingType.METS_CDR.getUri())) {
				convertJob = CDRMETS2N3BagJob.class.getName();
			} else if(packagingType.equals(PackagingType.METS_DSPACE_SIP_1.getUri())
					|| packagingType.equals(PackagingType.METS_DSPACE_SIP_2.getUri())) {
				convertJob = DSPACEMETS2N3BagJob.class.getName();
			}
			if(convertJob == null) {
				String msg = MessageFormat.format("Cannot convert deposit package to N3 BagIt. No converter for this packaging type(s): {}", packagingType);
				//failDeposit(Type.NORMALIZATION, "Cannot convert deposit to N3 BagIt package.", msg);
			}
		}
		
		// enqueue for additional BIOMED job if applicable
//				if("BioMed Central".equals(bag.getBagInfoTxt().getInternalSenderDescription())) {
//					return BioMedCentralExtrasJob.class.getName();
//				}
		
		
		
		
		
	}

}
