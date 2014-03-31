package edu.unc.lib.deposit.work;

import static edu.unc.lib.dl.util.DepositConstants.DESCRIPTION_DIR;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.apache.commons.io.IOUtils;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.hp.hpl.jena.rdf.model.Model;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.DepositConstants;
import edu.unc.lib.dl.util.DepositStatusFactory;
import edu.unc.lib.dl.util.PremisEventLogger;
import edu.unc.lib.dl.util.PremisEventLogger.Type;
import edu.unc.lib.dl.util.RedisWorkerConstants.JobStatus;

/**
 * Constructed with deposit directory and deposit ID. 
 * Facilitates event logging with standard success/failure states.
 *
 * @author count0
 * 
 */
public abstract class AbstractDepositJob {
	private static final Logger log = LoggerFactory.getLogger(AbstractDepositJob.class);
	public static final String DEPOSIT_QUEUE = "Deposit";
	private static final int joinPollingSeconds = 5;

	@Autowired
	private JobStatusFactory jobStatusFactory;

	@Autowired
	private DepositStatusFactory depositStatusFactory;

	// UUID for this deposit and its deposit record
	private String depositUUID;

	// UUID for this ingest job
	private String jobUUID;

	// Root directory where all deposits are stored
	@Autowired
	private File depositsDirectory;

	// Directory for this deposit
	private File depositDirectory;

	// Directory for local data files
	private File dataDirectory;

	private final PremisEventLogger eventLog = new PremisEventLogger(this.getClass().getName());
	// PREMIS events file for the deposit
	private File eventsFile;
	// Directory containing PREMIS event files for individual objects in this deposit
	private File eventsDirectory;

	public AbstractDepositJob() {
	}

	public AbstractDepositJob(String uuid, String depositUUID) {
		log.debug("Deposit job created: job:{} deposit:{}", uuid, depositUUID);
		this.jobUUID = uuid;
		this.depositUUID = depositUUID;
	}

	@PostConstruct
	public void init() {
		this.depositDirectory = new File(depositsDirectory, depositUUID);
		this.dataDirectory = new File(depositDirectory, DepositConstants.DATA_DIR);
		this.eventsDirectory = new File(depositDirectory, DepositConstants.EVENTS_DIR);
		this.eventsFile = new File(depositDirectory, DepositConstants.EVENTS_FILE);
	}

	public String getDepositUUID() {
		return depositUUID;
	}

	public void setDepositUUID(String depositUUID) {
		this.depositUUID = depositUUID;
	}

	public PID getDepositPID() {
		return new PID("uuid:" + this.depositUUID);
	}

	public String getJobUUID() {
		return jobUUID;
	}

	public void setJobUUID(String uuid) {
		this.jobUUID = uuid;
	}

	protected JobStatusFactory getJobStatusFactory() {
		return jobStatusFactory;
	}

	public void setJobStatusFactory(JobStatusFactory jobStatusFactory) {
		this.jobStatusFactory = jobStatusFactory;
	}

	protected DepositStatusFactory getDepositStatusFactory() {
		return depositStatusFactory;
	}

	public void setDepositStatusFactory(DepositStatusFactory depositStatusFactory) {
		this.depositStatusFactory = depositStatusFactory;
	}

	public Map<String, String> getDepositStatus() {
		Map<String, String> result = this.getDepositStatusFactory().get(depositUUID);
		return Collections.unmodifiableMap(result);
	}

	public File getDescriptionDir() {
		return new File(getDepositDirectory(), DESCRIPTION_DIR);
	}

	public File getDepositDirectory() {
		return depositDirectory;
	}

	public void setDepositDirectory(File depositDirectory) {
		this.depositDirectory = depositDirectory;
	}

	public File getDataDirectory() {
		return dataDirectory;
	}

	public PremisEventLogger getEventLog() {
		return eventLog;
	}

	public File getEventsFile() {
		return eventsFile;
	}

	public File getEventsDirectory() {
		return eventsDirectory;
	}

	public void recordDepositEvent(Type type, String messageformat, Object... args) {
		String message = MessageFormat.format(messageformat, args);
		log.debug("event recorded: {}", message);
		Element event = getEventLog().logEvent(type, message, this.getDepositPID());
		appendDepositEvent(event);
	}

	public void failJob(Type type, String message, String details) {
		log.debug("failed deposit: {}", message);
		Element event = getEventLog().logEvent(type, message, this.getDepositPID());
		event = PremisEventLogger.addDetailedOutcome(event, "failed", details, null);
		appendDepositEvent(event);
		throw new JobFailedException(message);
	}

	public void failJob(Throwable throwable, Type type, String messageformat, Object... args) {
		String message = MessageFormat.format(messageformat, args);
		log.debug("failed deposit: {}", message);
		Element event = getEventLog().logException(message, throwable);
		event = PremisEventLogger.addLinkingAgentIdentifier(event, "SIP Processing Job", this.getClass().getName(), "Software");
		appendDepositEvent(event);
		throw new JobFailedException(message, throwable);
	}

	protected void appendDepositEvent(Element event) {
		File file = new File(depositDirectory, DepositConstants.EVENTS_FILE);
		FileLock lock = null;
		FileOutputStream out = null;
		try {
			file.createNewFile();
			@SuppressWarnings("resource")
			FileChannel channel = new RandomAccessFile(file, "rw").getChannel();
			// Get an exclusive lock on the whole file
			lock = channel.lock();
			out = new FileOutputStream(file, true);
			out.write("\n".getBytes());
			new XMLOutputter(Format.getPrettyFormat()).output(event, out);
			out.close();
		} catch (IOException e) {
			throw new Error(e);
		} finally {
			IOUtils.closeQuietly(out);
			try {
				lock.release();
			} catch (IOException e) {
				throw new Error(e);
			}
		}
	}

	protected void saveModel(Model model, String filepath) {
		File arrangementFile = new File(this.getDepositDirectory(), filepath);
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(arrangementFile);
			model.write(fos, "N-TRIPLE");
		} catch(IOException e) {
			throw new Error("Cannot open file "+arrangementFile, e);
		} finally {
			try {
				fos.close();
			} catch (IOException ignored) {}
		}
	}

	protected void setTotalClicks(int totalClicks) {
		getJobStatusFactory().setTotalCompletion(this, totalClicks);
	}

	protected void addClicks(int clicks) {
		getJobStatusFactory().incrCompletion(this, clicks);
	}

	/**
	 * Pauses the current thread while polling Redis until the listed jobs
	 * are completed, failed or killed.
	 * @param jobUUIDs
	 * @return true if all jobs completed successfully, false if any did not or on timeout.
	 * @throws InterruptedException
	 */
	public boolean joinAfterExecute(int maxSeconds, boolean failFast, String... jobUUIDs) {
		log.debug("job {} waiting for completion of {}", getJobUUID(), jobUUIDs);
		boolean allSuccess = true;
		Set<String> jobsRemaining = new HashSet<String>(Arrays.asList(jobUUIDs));
		long start = System.currentTimeMillis();
		sleep: do {
			if(System.currentTimeMillis() - start > maxSeconds*1000) {
				log.debug("job {} joining after timeout of {}", getJobUUID(), maxSeconds);
				allSuccess = false;
				break sleep;
			}
			try {
				Thread.sleep(1000*joinPollingSeconds);
			} catch (InterruptedException expected) {
			}
			Set<String> done = new HashSet<String>();
			for(String uuid : jobsRemaining) {
				String state = getJobStatusFactory().getJobState(uuid);
				if(state == null) continue; // job state not posted yet
				if(JobStatus.queued.name().equals(state)) continue;
				if(JobStatus.working.name().equals(state)) continue;
				done.add(uuid);
				if(JobStatus.failed.name().equals(state) || JobStatus.killed.name().equals(state)) {
					allSuccess = false;
					if(failFast) {
						log.debug("job {} will join after fast fail of {}", getJobUUID(), uuid);
						break sleep;
					}
				}
			}
			jobsRemaining.removeAll(done);
		} while(jobsRemaining.size() > 0);
		log.debug("job {} joining after completion of {}", getJobUUID(), jobUUIDs);
		return allSuccess;
	}
	
	public File getSubdir(String subpath) {
		return new File(getDepositDirectory(), subpath);
	}
}
