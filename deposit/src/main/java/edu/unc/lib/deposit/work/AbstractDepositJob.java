package edu.unc.lib.deposit.work;

import static edu.unc.lib.dl.util.DepositConstants.DESCRIPTION_DIR;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
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
import edu.unc.lib.dl.xml.JDOMNamespaceUtil;

/**
 * Constructed with deposit directory and deposit ID. Facilitates event logging
 * with standard success/failure states.
 * 
 * @author count0
 * 
 */
public abstract class AbstractDepositJob {
	private static final Logger log = LoggerFactory
			.getLogger(AbstractDepositJob.class);
	public static final String DEPOSIT_QUEUE = "Deposit";

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

	private final PremisEventLogger eventLog = new PremisEventLogger(this
			.getClass().getName());
	// Directory containing PREMIS event files for individual objects in this
	// deposit
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
		this.dataDirectory = new File(depositDirectory,
				DepositConstants.DATA_DIR);
		this.eventsDirectory = new File(depositDirectory,
				DepositConstants.EVENTS_DIR);
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

	public void setDepositStatusFactory(
			DepositStatusFactory depositStatusFactory) {
		this.depositStatusFactory = depositStatusFactory;
	}

	public Map<String, String> getDepositStatus() {
		Map<String, String> result = this.getDepositStatusFactory().get(
				depositUUID);
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

	public File getEventsDirectory() {
		return eventsDirectory;
	}

	public void recordDepositEvent(Type type, String messageformat,
			Object... args) {
		String message = MessageFormat.format(messageformat, args);
		Element event = getEventLog().logEvent(type, message,
				this.getDepositPID());
		log.debug("event recorded: {}", event);
		appendDepositEvent(getDepositPID(), event);
	}

	public void failJob(Type type, String message, String details) {
		log.debug("failed deposit: {}", message);
		Element event = getEventLog().logEvent(type, message,
				this.getDepositPID());
		event = PremisEventLogger.addDetailedOutcome(event, "failed", details,
				null);
		appendDepositEvent(getDepositPID(), event);
		throw new JobFailedException(message);
	}

	public void failJob(Throwable throwable, Type type, String messageformat,
			Object... args) {
		String message = MessageFormat.format(messageformat, args);
		log.debug("failed deposit: {}", message);
		Element event = getEventLog().logException(message, throwable);
		event = PremisEventLogger.addLinkingAgentIdentifier(event,
				"SIP Processing Job", this.getClass().getName(), "Software");
		appendDepositEvent(getDepositPID(), event);
		throw new JobFailedException(message, throwable);
	}

	protected void appendDepositEvent(PID pid, Element event) {
		File file = new File(depositDirectory, DepositConstants.EVENTS_DIR
				+ "/" + pid.getUUID() + ".xml");
		try {
			Document dom;
			if (!file.exists()) {
				file.getParentFile().mkdirs();
				file.createNewFile();
				dom = new Document();
				Element premis = new Element("premis",
						JDOMNamespaceUtil.PREMIS_V2_NS).addContent(PremisEventLogger.getObjectElement(pid));
				dom.setRootElement(premis);
			} else {
				dom = new SAXBuilder().build(file);
			}
			dom.getRootElement().addContent(event.detach());
			try (FileOutputStream out = new FileOutputStream(file, false)) {
				new XMLOutputter(Format.getPrettyFormat()).output(dom, out);
			}
		} catch (JDOMException | IOException e1) {
			throw new Error("Unexpected problem with deposit events file", e1);
		}
	}

	protected void saveModel(Model model, String filepath) {
		File arrangementFile = new File(this.getDepositDirectory(), filepath);
		try(FileOutputStream fos = new FileOutputStream(arrangementFile)) {
			model.write(fos, "N-TRIPLE");
		} catch (IOException e) {
			throw new Error("Cannot open file " + arrangementFile, e);
		}
	}

	protected void setTotalClicks(int totalClicks) {
		getJobStatusFactory().setTotalCompletion(this, totalClicks);
	}

	protected void addClicks(int clicks) {
		getJobStatusFactory().incrCompletion(this, clicks);
	}

	public File getSubdir(String subpath) {
		return new File(getDepositDirectory(), subpath);
	}
}
