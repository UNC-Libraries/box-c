package edu.unc.lib.bag;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.text.MessageFormat;

import net.greghaines.jesque.Job;
import net.greghaines.jesque.client.Client;

import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.PremisEventLogger;
import edu.unc.lib.dl.util.PremisEventLogger.Type;
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
public abstract class AbstractBagJob implements Runnable {
	private static final Logger log = LoggerFactory.getLogger(AbstractBagJob.class);
	private static final String DEPOSIT_QUEUE = "Deposit";
	private File bagDirectory;
	private PID depositPID;
	private BagFactory bagFactory = new BagFactory();
	public BagFactory getBagFactory() {
		return bagFactory;
	}

	@Autowired
	Client jesqueClient = null;
	public Client getJesqueClient() {
		return jesqueClient;
	}

	public void setJesqueClient(Client jesqueClient) {
		this.jesqueClient = jesqueClient;
	}
	
	private String defaultNextJob = null;
	public String getDefaultNextJob() {
		return defaultNextJob;
	}

	public void setDefaultNextJob(String defaultNextJob) {
		this.defaultNextJob = defaultNextJob;
	}
	
	public void enqueueDefaultNextJob() {
		if(this.defaultNextJob != null) {
			Job job = new Job(this.defaultNextJob, getBagDirectory().getAbsolutePath(), this.getDepositPID().getURI());
			jesqueClient.enqueue(DEPOSIT_QUEUE, job);
		}
	}
	
	public void enqueueNextJob(String jobName) {
		if(jobName != null) {
			Job job = new Job(jobName, getBagDirectory().getAbsolutePath(), this.getDepositPID().getURI());
			jesqueClient.enqueue(DEPOSIT_QUEUE, job);
		}
	}

	private PremisEventLogger eventLog = new PremisEventLogger(this.getClass().getName());
	private File eventsFile;

	public AbstractBagJob(String bagDirectory, String depositId) {
		log.debug("Bag job created: {} {}", bagDirectory, depositId);
		this.bagDirectory = new File(bagDirectory);
		this.eventsFile = new File(bagDirectory, "events.xml");
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
	
	public void recordEvent(Type type, String messageformat, Object... args) {
		String message = MessageFormat.format(messageformat, args);
		Element event = getEventLog().logEvent(type, message, this.getDepositPID());
		appendDepositEvent(event);
	}
	
	public void failDeposit(Type type, String message, String details) {
		Element event = getEventLog().logEvent(type, message, this.getDepositPID());
		event = PremisEventLogger.addDetailedOutcome(event, "failed", details, null);
		appendDepositEvent(event);
		Throwable e = new DepositFailedException(message);
		throw new RuntimeException(e);
	}
	
	public void failDeposit(Throwable throwable, Type type, String messageformat, Object... args) {
		log.debug(messageformat, args);
		String message = MessageFormat.format(messageformat, args);
		Element event = getEventLog().logException(message, throwable);
		event = PremisEventLogger.addLinkingAgentIdentifier(event, "SIP Processing Job", this.getClass().getName(), "Software");
		appendDepositEvent(event); 
		Throwable e = new DepositFailedException(message, throwable);
		throw new RuntimeException(e);
	}
	
	protected void appendDepositEvent(Element event) {
			File file = new File(bagDirectory, "events.xml");
	        FileLock lock = null;
	        PrintWriter out = null;
	        try {
	        	file.createNewFile();
	        	@SuppressWarnings("resource")
				FileChannel channel = new RandomAccessFile(file, "rw").getChannel();
	        	// Get an exclusive lock on the whole file
	            lock = channel.lock();
			    out = new PrintWriter(new BufferedWriter(new FileWriter(file, true)));
			    out.append("\n");
			    new XMLOutputter(Format.getPrettyFormat()).output(event, out);
			    out.close();
	        } catch(IOException e) {
	        	throw new Error(e);
	        } finally {
	        	out.close();
	            try {
					lock.release();
				} catch (IOException e) {
					throw new Error(e);
				}
	        }
	}
	
	protected Bag loadBag() {
		Bag bag = getBagFactory().createBag(getBagDirectory());
		return bag;
	}

	protected void saveBag(gov.loc.repository.bagit.Bag bag) {
		if(eventsFile.exists())	bag.addFileAsTag(eventsFile);
		bag = bag.makeComplete(new UpdateCompleter(getBagFactory()));
		try {
			FileSystemWriter writer = new FileSystemWriter(getBagFactory());
			writer.setTagFilesOnly(true);
			writer.write(bag, getBagDirectory());
			bag.close();
		} catch (IOException e) {
			failDeposit(e, Type.NORMALIZATION, "Unable to write to deposit bag");
		}
	}
}
