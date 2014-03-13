package edu.unc.lib.workers;

import static edu.unc.lib.dl.util.BagConstants.DESCRIPTION_DIR;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.hp.hpl.jena.rdf.model.Model;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.BagConstants;
import edu.unc.lib.dl.util.DepositStatusFactory;
import edu.unc.lib.dl.util.PremisEventLogger;
import edu.unc.lib.dl.util.PremisEventLogger.Type;
import edu.unc.lib.dl.util.RedisWorkerConstants.JobStatus;
import gov.loc.repository.bagit.Bag;
import gov.loc.repository.bagit.BagFactory;
import gov.loc.repository.bagit.transformer.impl.UpdateCompleter;
import gov.loc.repository.bagit.writer.impl.FileSystemWriter;

/**
 * Constructed with bag directory and deposit ID.
 * Facilitates event logging with standard success/failure states.
 * @author count0
 *
 */
public abstract class AbstractBagJob {
	private static final Logger log = LoggerFactory.getLogger(AbstractBagJob.class);
	public static final String DEPOSIT_QUEUE = "Deposit";
	private static final int joinPollingSeconds = 5;
	private File bagDirectory;
	private PID depositPID;
	private Bag bag;
	
	public Bag getBag() {
		if(bag == null) {
			loadBag();
		}
		return bag;
	}

	private String jobUUID;
	public String getJobUUID() {
		return jobUUID;
	}

	public void setJobUUID(String uuid) {
		this.jobUUID = uuid;
	}

	@Autowired
	private JobStatusFactory jobStatusFactory;
	
	public JobStatusFactory getJobStatusFactory() {
		return jobStatusFactory;
	}

	public void setJobStatusFactory(JobStatusFactory jobStatusFactory) {
		this.jobStatusFactory = jobStatusFactory;
	}

	private DepositStatusFactory depositStatusFactory;
	public DepositStatusFactory getDepositStatusFactory() {
		return depositStatusFactory;
	}

	public void setBagStatusFactory(DepositStatusFactory depositStatusFactory) {
		this.depositStatusFactory = depositStatusFactory;
	}

	private BagFactory bagFactory = new BagFactory();
	public BagFactory getBagFactory() {
		return bagFactory;
	}
	
	public File getDescriptionDir() {
		return new File(getBagDirectory(), DESCRIPTION_DIR);
	}

	private PremisEventLogger eventLog = new PremisEventLogger(this.getClass().getName());
	private File eventsFile;

	public AbstractBagJob(String uuid, String bagDirectory, String depositId) {
		log.debug("Bag job created: {} {}", bagDirectory, depositId);
		this.jobUUID = uuid;
		this.bagDirectory = new File(bagDirectory);
		this.eventsFile = new File(bagDirectory, BagConstants.EVENTS_FILE);
		this.depositPID = new PID(depositId);
	}
	
	public AbstractBagJob() {}
	
	public PID getDepositPID() {
		return depositPID;
	}

	public void setDepositPID(PID depositPID) {
		this.depositPID = depositPID;
	}

	public File getBagDirectory() {
		return bagDirectory;
	}

	public void setBagDirectory(File bagDirectory) {
		this.bagDirectory = bagDirectory;
	}

	public PremisEventLogger getEventLog() {
		return eventLog;
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
			File file = new File(bagDirectory, BagConstants.EVENTS_FILE);
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
	        } catch(IOException e) {
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
	
	private void loadBag() {
		bag = getBagFactory().createBag(getBagDirectory());
	}

	protected void saveBag(gov.loc.repository.bagit.Bag bag) {
		if(eventsFile.exists())	bag.addFileAsTag(eventsFile);
		// TODO serialize object-level events streams
		bag = bag.makeComplete(new UpdateCompleter(getBagFactory()));
		try {
			FileSystemWriter writer = new FileSystemWriter(getBagFactory());
			writer.setTagFilesOnly(true);
			writer.write(bag, getBagDirectory());
			bag.close();
		} catch (IOException e) {
			failJob(e, Type.NORMALIZATION, "Unable to write to deposit bag");
		}
	}

	protected void saveModel(Model model, String tagfilepath) {
		File arrangementFile = new File(this.getBagDirectory(), tagfilepath);
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
}
