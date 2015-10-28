package edu.unc.lib.deposit.work;

import java.io.File;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import net.greghaines.jesque.Job;
import net.greghaines.jesque.client.Client;
import net.greghaines.jesque.meta.QueueInfo;
import net.greghaines.jesque.meta.dao.QueueInfoDAO;
import net.greghaines.jesque.worker.Worker;
import net.greghaines.jesque.worker.WorkerEvent;
import net.greghaines.jesque.worker.WorkerListener;
import net.greghaines.jesque.worker.WorkerPool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import edu.unc.lib.deposit.CleanupDepositJob;
import edu.unc.lib.deposit.PrepareResubmitJob;
import edu.unc.lib.deposit.fcrepo3.IngestDeposit;
import edu.unc.lib.deposit.fcrepo3.MakeFOXML;
import edu.unc.lib.deposit.normalize.BioMedToN3BagJob;
import edu.unc.lib.deposit.normalize.CDRMETS2N3BagJob;
import edu.unc.lib.deposit.normalize.Proquest2N3BagJob;
import edu.unc.lib.deposit.normalize.Simple2N3BagJob;
import edu.unc.lib.deposit.normalize.UnpackDepositJob;
import edu.unc.lib.deposit.normalize.VocabularyEnforcementJob;
import edu.unc.lib.deposit.validate.PackageIntegrityCheckJob;
import edu.unc.lib.deposit.validate.ValidateFileAvailabilityJob;
import edu.unc.lib.deposit.validate.ValidateMODS;
import edu.unc.lib.deposit.validate.VirusScanJob;
import edu.unc.lib.dl.fedora.FedoraTimeoutException;
import edu.unc.lib.dl.reporting.ActivityMetricsClient;
import edu.unc.lib.dl.util.DepositConstants;
import edu.unc.lib.dl.util.DepositStatusFactory;
import edu.unc.lib.dl.util.JobStatusFactory;
import edu.unc.lib.dl.util.PackagingType;
import edu.unc.lib.dl.util.RedisWorkerConstants;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositAction;
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
	private static final Logger LOG = LoggerFactory
			.getLogger(DepositSupervisor.class);

	@Autowired
	private DepositStatusFactory depositStatusFactory;

	@Autowired
	private JobStatusFactory jobStatusFactory;
	
	@Autowired
	private ActivityMetricsClient metricsClient;

	@Autowired
	private WorkerPool depositWorkerPool;
	
	@Autowired
	private WorkerPool cdrMetsDepositWorkerPool;
	
	@Autowired
	private QueueInfoDAO queueDAO;
	
	@Autowired
	private DepositEmailHandler depositEmailHandler;
	
	@Autowired
	private DepositMessageHandler depositMessageHandler;

	public net.greghaines.jesque.Config getJesqueConfig() {
		return jesqueConfig;
	}

	public void setJesqueConfig(net.greghaines.jesque.Config jesqueConfig) {
		this.jesqueConfig = jesqueConfig;
	}

	private Client makeJesqueClient() {
		Client result = new net.greghaines.jesque.client.ClientImpl(getJesqueConfig());
		return result;
	}

	@Autowired
	private net.greghaines.jesque.Config jesqueConfig;

	private Timer timer;

	private final String id;

	@Autowired
	private File depositsDirectory;

	private int cleanupDelaySeconds;

	private int unavailableDelaySeconds;

	public int getCleanupDelaySeconds() {
		return cleanupDelaySeconds;
	}

	public void setCleanupDelaySeconds(int cleanupDelaySeconds) {
		this.cleanupDelaySeconds = cleanupDelaySeconds;
	}

	public int getUnavailableDelaySeconds() {
		return unavailableDelaySeconds;
	}

	public void setUnavailableDelaySeconds(int unavailableDelaySeconds) {
		this.unavailableDelaySeconds = unavailableDelaySeconds;
	}

	public DepositSupervisor() {
		id = UUID.randomUUID().toString();
	}

	private static enum Queue {
		PREPARE, DELAYED_PREPARE, CDRMETSCONVERT;
	}

	@PostConstruct
	public void init() {
		LOG.info("Initializing DepositSupervisor timer and starting Jesque worker pool");
		depositWorkerPool.getWorkerEventEmitter().addListener(this);
		cdrMetsDepositWorkerPool.getWorkerEventEmitter().addListener(this);
	}

	public void start() {
		// Repopulate the queue
		requeueAll();
		
		LOG.info("Starting deposit checks and worker pool");
		if (timer != null)
			return;
		timer = new Timer("DepositSupervisor " + id);
		timer.schedule(new TimerTask() {

			@Override
			public void run() {

				try {
					// Scan for actions and trigger them
					for (Map<String, String> fields : depositStatusFactory.getAll()) {
						
						String requestedActionName = fields.get(DepositField.actionRequest.name());
						String uuid = fields.get(DepositField.uuid.name());

						if (DepositAction.register.name().equals(requestedActionName)) {
							
							LOG.info("Registering job {}", uuid);
							
							if (depositStatusFactory.addSupervisorLock(uuid, id)) {
								try {
									queueNewDeposit(uuid);
								} finally {
									depositStatusFactory.removeSupervisorLock(uuid);
								}
							}

						} else if (DepositAction.pause.name().equals(requestedActionName)) {

							LOG.info("Pausing job {}", uuid);

							depositStatusFactory.setState(uuid, DepositState.paused);
							depositStatusFactory.clearActionRequest(uuid);

						} else if (DepositAction.resume.name().equals(requestedActionName)) {

							LOG.info("Resuming job {}", uuid);

							if (depositStatusFactory.addSupervisorLock(uuid, id)) {
								try {
									resumeDeposit(uuid, fields);
								} finally {
									depositStatusFactory.removeSupervisorLock(uuid);
								}
							}
							
						} else if (DepositAction.resubmit.name().equals(requestedActionName)) {
							
							LOG.info("Resubmitting job {}", uuid);
							
							if (depositStatusFactory.addSupervisorLock(uuid, id)) {
								try {
									resubmitDeposit(uuid, fields);
								} finally {
									depositStatusFactory.removeSupervisorLock(uuid);
								}
							}
							
						}
						
					}
				} catch (Throwable t) {
					LOG.error("Encountered an exception while checking for action requests", t);
				}
			}

		}, 1000, 1000);

		if (depositWorkerPool.isShutdown()) {
			throw new Error("Cannot start deposit workers, already shutdown.");
		} else if (depositWorkerPool.isPaused()) {
			LOG.info("Unpausing deposit workers");
			this.depositWorkerPool.togglePause(false);
		} else {
			LOG.info("Starting deposit workers");
			depositWorkerPool.run();
		}
		
		if (cdrMetsDepositWorkerPool.isShutdown()) {
			throw new Error("Cannot start deposit workers, already shutdown.");
		} else if (cdrMetsDepositWorkerPool.isPaused()) {
			LOG.info("Unpausing deposit workers");
			this.cdrMetsDepositWorkerPool.togglePause(false);
		} else {
			LOG.info("Starting deposit workers");
			cdrMetsDepositWorkerPool.run();
		}
	}
	
	private Map<String, Set<String>> getQueuedDepositsWithJobs() {
		Map<String, Set<String>> depositMap = new HashMap<>();
		addQueuedDeposits(RedisWorkerConstants.DEPOSIT_PREPARE_QUEUE, depositMap);
		addQueuedDeposits(RedisWorkerConstants.DEPOSIT_DELAYED_QUEUE, depositMap);
		addQueuedDeposits(RedisWorkerConstants.DEPOSIT_CDRMETS_QUEUE, depositMap);
		return depositMap;
	}
	
	private void addQueuedDeposits(String queueName, Map<String, Set<String>> depositMap) {
		QueueInfo info = queueDAO.getQueueInfo(queueName, 0, 0);
		
		for (Job job : info.getJobs()) {
			String depositId = (String) job.getArgs()[1];
			
			Set<String> jobs = depositMap.get(depositId);
			if (jobs == null) {
				jobs = new HashSet<>();
				depositMap.put(depositId, jobs);
			}
			jobs.add(job.getClassName());
		}
	}

	/**
	 * Add jobs previously running or queued back to the queue
	 */
	private void requeueAll() {
		
		Map<String, Set<String>> depositSet = getQueuedDepositsWithJobs();
		Set<Map<String, String>> depositStatuses = depositStatusFactory.getAll();

		LOG.info("Repopulating the deposit queue, {} items in backlog", depositStatuses.size());

		// Requeue the previously running jobs from where they left off first
		for (Map<String, String> fields : depositStatuses) {
			if (DepositState.running.name().equals(fields.get(DepositField.state.name()))) {
				String uuid = fields.get(DepositField.uuid.name());

				// Job may have been locked to a particular supervisor depend on when it was interrupted
				depositStatusFactory.removeSupervisorLock(uuid);
				// Inform supervisor to resume this deposit from where it left off
				if (depositSet.containsKey(uuid)) {
					// If the job is queued but the job it is waiting on is a cleanup, then it is finished
					if (depositSet.get(uuid).contains(CleanupDepositJob.class.getName())) {
						depositStatusFactory.setState(uuid, DepositState.finished);
					} else {
						LOG.debug("Skipping resumption of deposit {} because it already is in the queue", uuid);
					}
				} else {
					depositStatusFactory.setActionRequest(uuid, DepositAction.resume);
				}
			}
		}

		// Requeue the "queued" jobs next
		for (Map<String, String> fields : depositStatuses) {
			if (DepositState.queued.name().equals(fields.get(DepositField.state.name()))) {
				String uuid = fields.get(DepositField.uuid.name());

				depositStatusFactory.removeSupervisorLock(uuid);
				// Re-register as a new deposit
				if (depositSet.containsKey(uuid)) {
					if (depositSet.get(uuid).contains(CleanupDepositJob.class.getName())) {
						depositStatusFactory.setState(uuid, DepositState.finished);
					} else {
						LOG.debug("Skipping resumption of queued deposit {} because it already is in the queue", uuid);
					}
				} else {
					List<String> successfulJobs = jobStatusFactory.getSuccessfulJobNames(uuid);
					if (successfulJobs != null && successfulJobs.size() > 0) {
						// Queued but had already performed some jobs, so this is a resumption rather than new deposit
						depositStatusFactory.setActionRequest(uuid, DepositAction.resume);
					} else {
						depositStatusFactory.setActionRequest(uuid, DepositAction.register);
					}
				}
			}
		}
	}

	public void stop() {
		LOG.info("Stopping the Deposit Supervisor");
		depositWorkerPool.togglePause(true); // take no new jobs
		if (timer != null) { // stop registering new deposits
			this.timer.cancel();
			this.timer.purge();
			this.timer = null;
		}
		depositWorkerPool.end(false); // cancel running jobs without interrupting
		LOG.info("Stopped the Deposit Supervisor");
	}

	public DepositStatusFactory getDepositStatusFactory() {
		return depositStatusFactory;
	}

	public void setDepositStatusFactory(
			DepositStatusFactory depositStatusFactory) {
		this.depositStatusFactory = depositStatusFactory;
	}

	public JobStatusFactory getJobStatusFactory() {
		return jobStatusFactory;
	}

	public void setJobStatusFactory(JobStatusFactory jobStatusFactory) {
		this.jobStatusFactory = jobStatusFactory;
	}

	public File getDepositsDirectory() {
		return depositsDirectory;
	}

	public void setDepositsDirectory(File depositsDirectory) {
		this.depositsDirectory = depositsDirectory;
	}

	@PreDestroy
	public void shutdown() {
		stop();
	}

	public Job makeJob(@SuppressWarnings("rawtypes") Class jobClass,
			String depositUUID) {
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
			Job job, Object runner, Object result, Throwable t) {
		if (WorkerEvent.WORKER_POLL != event)
			LOG.debug("WorkerEvent {}, {}, {}, {}, {}, {}, {}", new Object[] {
					event, worker, queue, job, runner, result, t });

		String depositUUID;
		AbstractDepositJob j;

		// Job-level status logging
		switch (event) {
			case WORKER_ERROR:
				LOG.error("Worker threw an error: {}", t);
			case WORKER_START:
			case WORKER_STOP:
			case WORKER_POLL:
			case JOB_PROCESS:
				return;
			default:
		}

		depositUUID = (String) job.getArgs()[1];
		Map<String, String> status = this.depositStatusFactory.get(depositUUID);

		j = (AbstractDepositJob) runner;

		switch (event) {
			case JOB_EXECUTE:
				jobStatusFactory.started(j.getJobUUID(), j.getDepositUUID(), j.getClass());
				break;
			case JOB_SUCCESS:
				jobStatusFactory.completed(j.getJobUUID());
				LOG.debug("Registering {} as completed for {}", j.getClass().getName(), depositUUID);
				break;
			case WORKER_ERROR:
			case JOB_FAILURE:
				String jobUUID = (String) job.getArgs()[0];
				if (j != null) {
					jobUUID = j.getJobUUID();
				}

				if (t != null && t instanceof JobInterruptedException) {
					LOG.debug("Job {} in deposit {} was interrupted", jobUUID, depositUUID);
					jobStatusFactory.interrupted(jobUUID);
					return;
				}
				
				if (t != null && t instanceof FedoraTimeoutException) {
					LOG.warn("Fedora timed out for job {} in deposit {}, will resume after delay", jobUUID, depositUUID);
					jobStatusFactory.failed(jobUUID);
					resumeDeposit(depositUUID, status, getUnavailableDelaySeconds() * 1000);
					return;
				}
				
				if (t != null) {
					LOG.error("Job " + jobUUID + " in deposit " + depositUUID + " failed with exception", t);
					
					if (t instanceof JobFailedException) {
						String details = ((JobFailedException) t).getDetails();
						if (details != null) {
							LOG.error("Details for failed job " + jobUUID + " in deposit " + depositUUID + ": " + details);
						}
					}
				} else {
					LOG.error("Job " + jobUUID + " in deposit " + depositUUID + " failed");
				}
				
				if (t instanceof JobFailedException) {
					jobStatusFactory.failed(jobUUID, t.getLocalizedMessage());
					depositStatusFactory.fail(depositUUID, t.getLocalizedMessage());
				} else {
					jobStatusFactory.failed(jobUUID);
					String serviceName = job.getClassName().substring(job.getClassName().lastIndexOf('.') + 1);
					depositStatusFactory.fail(depositUUID, "Failed while performing service " + serviceName);
				}
				metricsClient.incrFailedDepositJob(job.getClassName());
				
				depositEmailHandler.sendDepositResults(depositUUID);
				
				return;
			default:
				break;
		}

		// Now that state from the previous job is recorded, prevent further processing if interrupted
		if (isJobPaused(status)) {
			LOG.debug("Job {} has been paused", depositUUID);
			return;
		}
		
		if (CleanupDepositJob.class.getName().equals(job.getClassName())) {
			LOG.debug("Job {} is cleanup job, deposit state will expire", depositUUID);
			return;
		}

		// Deposit-level actions
		List<String> successfulJobs = this.jobStatusFactory
				.getSuccessfulJobNames(depositUUID);

		switch (event) {
			case JOB_EXECUTE:
				if (!DepositState.running.name().equals(status.get(DepositField.state.name()))) {
					depositStatusFactory.setState(depositUUID, DepositState.running);
				}
				break;
			case JOB_SUCCESS:
				try {
					queueNextJob(job, depositUUID, status, successfulJobs);
				} catch (DepositFailedException e) {
					LOG.error("Failed to enqueue next job for deposit " + depositUUID, e);
					depositStatusFactory.fail(depositUUID);
				}
				break;
			default:
				break;
		}
	}

	private Job getNextJob(Job job, String depositUUID, Map<String, String> status, List<String> successfulJobs)
			throws DepositFailedException {
		LOG.debug("Got completed job names: {}", successfulJobs);
		
		// Resubmit deposit
		if (status.get(DepositField.resubmitFileName.name()) != null) {
			if (!successfulJobs.contains(PrepareResubmitJob.class.getName())) {
				return makeJob(PrepareResubmitJob.class, depositUUID);
			}
		}

		// Package integrity check
		if (status.get(DepositField.depositMd5.name()) != null) {
			if (!successfulJobs.contains(PackageIntegrityCheckJob.class
					.getName())) {
				return makeJob(PackageIntegrityCheckJob.class, depositUUID);
			}
		}

		// Package may be unpacked
		String filename = status.get(DepositField.fileName.name());
		String packagingType = status.get(DepositField.packagingType.name());
		if (filename != null && filename.toLowerCase().endsWith(".zip") &&
				!PackagingType.SIMPLE_OBJECT.getUri().equals(packagingType)) {
			if (!successfulJobs.contains(UnpackDepositJob.class.getName())) {
				return makeJob(UnpackDepositJob.class, depositUUID);
			}
		}

		// Deposit package type may be converted to N3
		if (!packagingType.equals(PackagingType.BAG_WITH_N3.getUri())) {
			Job conversion = null;
			// we need to add N3 packaging to this bag
			if (packagingType.equals(PackagingType.METS_CDR.getUri())) {
				conversion = makeJob(CDRMETS2N3BagJob.class, depositUUID);
			} else if (packagingType.equals(PackagingType.METS_DSPACE_SIP_1.getUri())
					|| packagingType.equals(PackagingType.METS_DSPACE_SIP_2.getUri())) {
				conversion = makeJob(BioMedToN3BagJob.class, depositUUID);
			} else if (packagingType.equals(PackagingType.PROQUEST_ETD.getUri())) {
				conversion = makeJob(Proquest2N3BagJob.class, depositUUID);
			} else if (packagingType.equals(PackagingType.SIMPLE_OBJECT.getUri())) {
				conversion = makeJob(Simple2N3BagJob.class, depositUUID);
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

		// Perform vocabulary enforcement for package types that retain the original metadata
		if ((packagingType.equals(PackagingType.METS_DSPACE_SIP_1.getUri())
				|| packagingType.equals(PackagingType.METS_DSPACE_SIP_2.getUri())
				|| packagingType.equals(PackagingType.PROQUEST_ETD.getUri()))
				&& !successfulJobs.contains(VocabularyEnforcementJob.class.getName())) {
			return makeJob(VocabularyEnforcementJob.class, depositUUID);
		}

		// MODS validation
		File bagPath = new File(depositsDirectory, depositUUID);
		File descrFolder = new File(bagPath, DepositConstants.DESCRIPTION_DIR);
		if (descrFolder.exists()) {
			if (!successfulJobs.contains(ValidateMODS.class.getName())) {
				return makeJob(ValidateMODS.class, depositUUID);
			}
		}

		// Validate file availability
		if (!successfulJobs.contains(ValidateFileAvailabilityJob.class.getName())) {
			return makeJob(ValidateFileAvailabilityJob.class, depositUUID);
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

		// Ingest
		if (!successfulJobs.contains(IngestDeposit.class.getName())) {
			return makeJob(IngestDeposit.class, depositUUID);
		}

		return null;
	}

	private boolean isJobPaused(Map<String, String> status) {
		return DepositState.paused.name().equals(status.get(DepositField.state.name()));
	}

	private void queueNextJob(Job job, String depositUUID, Map<String, String> status, List<String> successfulJobs)
			throws DepositFailedException {
		queueNextJob(job, depositUUID, status, successfulJobs, 0);
	}

	private void queueNextJob(Job job, String depositUUID, Map<String, String> status, List<String> successfulJobs,
			long delay)
			throws DepositFailedException {
		Job nextJob = getNextJob(job, depositUUID, status, successfulJobs);
		if (nextJob != null) {
			LOG.info("Queuing next job {} for deposit {}", nextJob.getClassName(), depositUUID);
			Client c = makeJesqueClient();
			if (delay > 0)
				c.delayedEnqueue(Queue.DELAYED_PREPARE.name(), nextJob, System.currentTimeMillis() + delay);
			else {
				if(CDRMETS2N3BagJob.class.getName().equals(nextJob.getClassName())) {
					c.enqueue(Queue.CDRMETSCONVERT.name(), nextJob);
				} else {
					c.enqueue(Queue.PREPARE.name(), nextJob);
				}
			}
			c.end();
		} else {
			depositStatusFactory.setState(depositUUID, DepositState.finished);
			metricsClient.incrFinishedDeposit();
			
			depositEmailHandler.sendDepositResults(depositUUID);
			depositMessageHandler.sendDepositMessage(depositUUID);

			Client c = makeJesqueClient();
			// schedule cleanup job after the configured delay
			long schedule = System.currentTimeMillis() + 1000 * this.getCleanupDelaySeconds();
			Job cleanJob = makeJob(CleanupDepositJob.class, depositUUID);
			LOG.info("Queuing {} for deposit {}",
					cleanJob.getClassName(), depositUUID);
			c.delayedEnqueue(Queue.DELAYED_PREPARE.name(), cleanJob, schedule);
		}
	}

	private void queueNewDeposit(String uuid) {
		LOG.info("Queuing first job for deposit {}", uuid);

		Job job = makeJob(PackageIntegrityCheckJob.class, uuid);

		depositStatusFactory.setState(uuid, DepositState.queued);
		depositStatusFactory.clearActionRequest(uuid);

		Client c = makeJesqueClient();
		c.enqueue(Queue.PREPARE.name(), job);
		c.end();
	}

	private void resumeDeposit(String uuid, Map<String, String> status) {
		resumeDeposit(uuid, status, 0);
	}

	private void resumeDeposit(String uuid, Map<String, String> status, long delay) {
		try {
			depositStatusFactory.clearActionRequest(uuid);

			// Clear out the previous failed job if there was one
			jobStatusFactory.clearStale(uuid);
			depositStatusFactory.deleteField(uuid, DepositField.errorMessage);
			
			// since we already checked for queued jobs at startup, only check when resuming from a paused state
			boolean enqueueNext = true;
			if (DepositState.paused.name().equals(status.get(DepositField.state.name()))) {
				Map<String, Set<String>> depositSet = getQueuedDepositsWithJobs();
				enqueueNext = !depositSet.containsKey(uuid);
			}

			if (enqueueNext) {
				List<String> successfulJobs = jobStatusFactory.getSuccessfulJobNames(uuid);
				queueNextJob(null, uuid, status, successfulJobs, delay);
			} else {
				LOG.info("Resuming {} from paused state.  A job is already queued so no new jobs will be enqueued", uuid);
			}

			depositStatusFactory.setState(uuid, DepositState.queued);
		} catch (DepositFailedException e) {
			LOG.error("Failed to resume deposit " + uuid, e);
			depositStatusFactory.fail(uuid);
		}
	}

	private void resubmitDeposit(String uuid, Map<String, String> status) {
		try {
			depositStatusFactory.clearActionRequest(uuid);
			
			if (status.get(DepositField.resubmitFileName.name()) == null) {
				throw new DepositFailedException("Can't resubmit a deposit without a new ingest file.");
			}

			// Clear out all jobs for this deposit
			jobStatusFactory.deleteAll(uuid);
			depositStatusFactory.deleteField(uuid, DepositField.errorMessage);

			queueNextJob(null, uuid, status, Arrays.<String>asList(), new Long(0));

			depositStatusFactory.setState(uuid, DepositState.queued);
		} catch (DepositFailedException e) {
			LOG.error("Failed to resubmit deposit " + uuid, e);
			depositStatusFactory.fail(uuid);
		}
	}
	
}
