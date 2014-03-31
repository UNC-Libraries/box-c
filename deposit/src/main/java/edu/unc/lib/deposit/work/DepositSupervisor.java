package edu.unc.lib.deposit.work;

import java.io.File;
import java.text.MessageFormat;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import net.greghaines.jesque.Job;
import net.greghaines.jesque.client.Client;
import net.greghaines.jesque.worker.Worker;
import net.greghaines.jesque.worker.WorkerEvent;
import net.greghaines.jesque.worker.WorkerEventEmitter;
import net.greghaines.jesque.worker.WorkerListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import edu.unc.lib.deposit.fcrepo3.MakeFOXML;
import edu.unc.lib.deposit.normalize.BioMedCentralExtrasJob;
import edu.unc.lib.deposit.normalize.CDRMETS2N3BagJob;
import edu.unc.lib.deposit.normalize.DSPACEMETS2N3BagJob;
import edu.unc.lib.deposit.normalize.UnpackDepositJob;
import edu.unc.lib.deposit.validate.PackageIntegrityCheckJob;
import edu.unc.lib.deposit.validate.ValidateMODS;
import edu.unc.lib.deposit.validate.VirusScanJob;
import edu.unc.lib.dl.util.DepositConstants;
import edu.unc.lib.dl.util.DepositStatusFactory;
import edu.unc.lib.dl.util.PackagingType;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositField;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositState;

/**
 * Coordinates work on deposits via Redis and Resque. Responsible for putting
 * all work onto the queues. Coordinates with outside world via deposit status
 * keys.
 * 
 * @author count0
 * 
 */
public class DepositSupervisor implements WorkerListener {
	private static final Logger log = LoggerFactory
			.getLogger(DepositSupervisor.class);

	@Autowired
	private DepositStatusFactory depositStatusFactory;

	@Autowired
	private JobStatusFactory jobStatusFactory;

	private WorkerEventEmitter workerEventEmitter;

	public WorkerEventEmitter getWorkerEventEmitter() {
		return workerEventEmitter;
	}

	public void setWorkerEventEmitter(WorkerEventEmitter workerEventEmitter) {
		this.workerEventEmitter = workerEventEmitter;
	}

	@Autowired
	private Client jesqueClient;

	private Timer timer;

	private String id;

	@Autowired
	private File depositsDirectory;

	public DepositSupervisor() {
	}

	private static enum Queue {
		PREPARE;
	}

	@PostConstruct
	public void init() {
		this.workerEventEmitter.addListener(this);
		id = UUID.randomUUID().toString();
		timer = new Timer("DepositSupervisor Periodic Checks");
		timer.schedule(new TimerTask() {

			@Override
			public void run() {
				log.info("Checking for registered deposits.. ({})", id);
				for (Map<String, String> fields : depositStatusFactory.getAll()) {
					if (DepositState.registered.name().equals(
							fields.get(DepositField.status.name()))) {
						String uuid = fields.get(DepositField.uuid.name());
						log.info("Found new registered deposit: {}", uuid);
						if (depositStatusFactory.addSupervisorLock(uuid, id)) {
							try {
								log.info("Queued first job ({})", uuid);
								Job job = makeJob(
										PackageIntegrityCheckJob.class, uuid);
								jesqueClient.enqueue(Queue.PREPARE.name(), job);
								depositStatusFactory.setState(uuid,
										DepositState.queued);
							} finally {
								depositStatusFactory.removeSupervisorLock(uuid);
							}
						}
					}
				}
			}

		}, 10 * 1000, 10 * 1000);
	}
	
	@PreDestroy
	public void destroy() {
		this.timer.cancel();
	}
	
	public Job makeJob(Class jobClass, String depositUUID) {
		String uuid = UUID.randomUUID().toString();
		return new Job(jobClass.getName(), uuid, depositUUID);
	}

	/*
	 * Respond to job success with more job scheduling or finish
	 * 
	 * @see
	 * net.greghaines.jesque.worker.WorkerListener#onEvent(net.greghaines.jesque
	 * .worker.WorkerEvent, net.greghaines.jesque.worker.Worker,
	 * java.lang.String, net.greghaines.jesque.Job, java.lang.Object,
	 * java.lang.Object, java.lang.Exception)
	 */
	@Override
	public void onEvent(WorkerEvent event, Worker worker, String queue,
			Job job, Object runner, Object result, Exception ex) {
		if (WorkerEvent.WORKER_POLL != event)
			log.debug("WorkerEvent {}, {}, {}, {}, {}, {}, {}", new Object[] {
					event, worker, queue, job, runner, result, ex });

		String depositUUID;
		AbstractDepositJob j;

		// Job-level status logging
		switch (event) {
		case WORKER_ERROR:
			log.error("Worker threw an error: {}", ex);
		case WORKER_START:
		case WORKER_STOP:
		case WORKER_POLL:
		case JOB_PROCESS:
			return;
		default:
		}

		depositUUID = (String) job.getArgs()[1];
		j = (AbstractDepositJob) runner;

		switch (event) {
		case JOB_EXECUTE:
			jobStatusFactory.started(j);
			break;
		case JOB_SUCCESS:
			jobStatusFactory.completed(j);
			break;
		case JOB_FAILURE:
			if (j != null) {
				if (ex != null) {
					jobStatusFactory.failed(j, ex.getLocalizedMessage());
				} else {
					jobStatusFactory.failed(j);
				}
			} else {
				log.error("Job failure", ex);
			}
			break;
		default:
			break;
		}

		// Deposit-level actions
		switch (event) {
		case JOB_SUCCESS:
			try {
				Job nextJob = getNextJob(job, depositUUID);
				if (nextJob != null) {
					jesqueClient.enqueue(Queue.PREPARE.name(), nextJob);
				}
			} catch (DepositFailedException e) {
				depositStatusFactory.fail(depositUUID, e);
			}
			break;
		case JOB_FAILURE:
			depositStatusFactory.fail(depositUUID, ex);
		default:
			break;
		}
	}

	private Job getNextJob(Job job, String depositUUID)
			throws DepositFailedException {
		Map<String, String> status = this.depositStatusFactory.get(depositUUID);
		Set<String> successfulJobs = this.jobStatusFactory
				.getSuccessfulJobNames(depositUUID);
		log.debug("Got completed job names: {}", successfulJobs);

		// Package integrity check
		if (status.get(DepositField.depositMd5.name()) != null) {
			if (!successfulJobs.contains(PackageIntegrityCheckJob.class.getName())) {
				return makeJob(PackageIntegrityCheckJob.class, depositUUID);
			}
		}
		
		// Package may be unpacked
		String filename = status.get(DepositField.fileName.name());
		if (filename.toLowerCase().endsWith(".zip")) {
			if (!successfulJobs.contains(UnpackDepositJob.class.getName())) {
				return makeJob(UnpackDepositJob.class, depositUUID);
			}
		}

		// Deposit package type may be converted to N3
		String packagingType = status.get(DepositField.packagingType.name());
		if (!packagingType.equals(PackagingType.BAG_WITH_N3.getUri())) {
			Job conversion = null;
			// we need to add N3 packaging to this bag
			if (packagingType.equals(PackagingType.METS_CDR.getUri())) {
					conversion = makeJob(CDRMETS2N3BagJob.class, depositUUID);
			} else if (packagingType.equals(PackagingType.METS_DSPACE_SIP_1
					.getUri())
					|| packagingType.equals(PackagingType.METS_DSPACE_SIP_2
							.getUri())) {
					conversion = makeJob(DSPACEMETS2N3BagJob.class, depositUUID);
			}
			if (conversion == null) {
				String msg = MessageFormat
						.format("Cannot convert deposit package to N3 BagIt. No converter for this packaging type(s): {}",
								packagingType);
				throw new DepositFailedException(msg);
			} else if (!successfulJobs.contains(conversion.getClassName())) {
				return conversion;
			}
		}

		// BioMedCentral metadata may be extracted (if applicable)
		if ("BioMed Central".equals(status
				.get(DepositField.intSenderDescription.name()))) {
			if (!successfulJobs
					.contains(BioMedCentralExtrasJob.class.getName())) {
				return makeJob(BioMedCentralExtrasJob.class, depositUUID);
			}
		}

		// MODS validation
		File bagPath = new File(depositsDirectory, depositUUID);
		File descrFolder = new File(bagPath, DepositConstants.DESCRIPTION_DIR);
		if (descrFolder.exists()) {
			if (!successfulJobs.contains(ValidateMODS.class.getName())) {
				return makeJob(ValidateMODS.class, depositUUID);
			}
		}

		// Virus Scan
		if (!successfulJobs.contains(VirusScanJob.class.getName())) {
			return makeJob(VirusScanJob.class, depositUUID);
		}
		
		// Make FOXML
		if (!successfulJobs.contains(MakeFOXML.class.getName())) {
			return makeJob(MakeFOXML.class, depositUUID);
		}

		// TODO RDF Graph Validation

		return null;
	}

}
