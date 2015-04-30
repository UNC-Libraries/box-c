package edu.unc.lib.deposit.work;

import static edu.unc.lib.dl.util.DepositConstants.DESCRIPTION_DIR;
import static edu.unc.lib.dl.util.RedisWorkerConstants.DepositField.manifestURI;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.ReadWrite;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.DepositConstants;
import edu.unc.lib.dl.util.DepositStatusFactory;
import edu.unc.lib.dl.util.JobStatusFactory;
import edu.unc.lib.dl.util.PremisEventLogger;
import edu.unc.lib.dl.util.PremisEventLogger.Type;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositState;
import edu.unc.lib.dl.xml.JDOMNamespaceUtil;

/**
 * Constructed with deposit directory and deposit ID. Facilitates event logging
 * with standard success/failure states.
 *
 * @author count0
 *
 */
public abstract class AbstractDepositJob implements Runnable {
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

	@Autowired
	private Dataset dataset;

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

	@Override
	public final void run() {
		try {
			runJob();
			if (dataset.isInTransaction()) {
				dataset.commit();
			}
		} catch (Throwable e) {
			if (dataset.isInTransaction()) {
				dataset.abort();
			}
			throw e;
		} finally {
			if (dataset.isInTransaction()) {
				dataset.end();
			}
		}
	}

	public abstract void runJob();

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

	public File getDepositsDirectory() {
		return depositsDirectory;
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

	/**
	 * Returns the file where the manifest for this deposit is stored. If no manifest was set, then null is returned
	 *
	 * @return
	 */
	public File getManifestFile() {
		String path = getDepositStatus().get(manifestURI.name());
		if (path == null)
			return null;
		return new File(path);
	}

	public void recordDepositEvent(Type type, String messageformat, Object... args) {
		String message = MessageFormat.format(messageformat, args);
		Element event = getEventLog().logEvent(type, message,
				this.getDepositPID());
		log.debug("event recorded: {}", event);
		appendDepositEvent(getDepositPID(), event);
	}

	public void failJob(String message, String details) {
		log.debug("failed deposit: {}", message);
		throw new JobFailedException(message, details);
	}

	public void failJob(Throwable throwable, String messageformat, Object... args) {
		String message = MessageFormat.format(messageformat, args);
		log.debug("failed deposit: {}", message, throwable);
		throw new JobFailedException(message, throwable);
	}

	protected void verifyRunning() {
		DepositState state = getDepositStatusFactory().getState(getDepositUUID());

		if (!DepositState.running.equals(state)) {
			throw new JobInterruptedException("State for job " + getDepositUUID()
					+ " is no longer running, interrupting");
		}
	}

	/**
	 * Returns the PREMIS events file for the given PID
	 *
	 * @param pid
	 * @return
	 */
	protected File getEventsFile(PID pid) {
		return createOrAppendToEventsFile(pid, null);
	}

	/**
	 * Appends an event to the PREMIS log for the given pid. If the log does not exist, it is created
	 *
	 * @param pid
	 * @param event
	 */
	protected void appendDepositEvent(PID pid, Element event) {
		createOrAppendToEventsFile(pid, event);
	}

	/**
	 * Appends an event to the PREMIS document for the given PID, creating the document if it does not already exist or
	 * is corrupt.
	 *
	 * @param pid
	 * @param event
	 * @return the premis document file
	 */
	private File createOrAppendToEventsFile(PID pid, Element event) {
		File file = new File(depositDirectory, DepositConstants.EVENTS_DIR + "/" + pid.getUUID() + ".xml");

		try {
			Document dom;
			if (!file.exists()) {
				file.getParentFile().mkdirs();
				dom = createNewEventsFile(pid, file);
			} else {
				// Not appending anything, so return before attempting to load existing file
				if (event == null)
					return file;

				try {
					dom = new SAXBuilder().build(file);
				} catch (JDOMException e) {
					log.warn("Failed to parse existing events file for {}, backing up corrupt log and creating a new log", e);
					try {
						Files.move(file.toPath(), Paths.get(file.getAbsolutePath() + ".backup." + System.currentTimeMillis()));
					} catch (IOException e2) {
						failJob(e2, "Failed to backup corrupt log file for object {}.", pid);
					}
					dom = createNewEventsFile(pid, file);
				}
			}

			if (event != null) {
				dom.getRootElement().addContent(event.detach());
			}

			try (FileOutputStream out = new FileOutputStream(file, false)) {
				new XMLOutputter(Format.getPrettyFormat()).output(dom, out);
			}

			return file;
		} catch (IOException e) {
			failJob(e, "Unexpected problem with deposit events file {}.", file.getAbsoluteFile());
		}

		return null;
	}

	/**
	 * Creates a new PREMIS event log file for the given PID using the provided file instance
	 *
	 * @param pid
	 * @param file
	 * @return Returns the document representing the created PREMIS event log.
	 * @throws IOException
	 */
	private Document createNewEventsFile(PID pid, File file) throws IOException {
		file.createNewFile();
		Document dom = new Document();
		Element premis = new Element("premis", JDOMNamespaceUtil.PREMIS_V2_NS).addContent(PremisEventLogger
				.getObjectElement(pid));
		dom.setRootElement(premis);

		return dom;
	}

	public Model getWritableModel() {
		String uri = getDepositPID().getURI();
		this.dataset.begin(ReadWrite.WRITE);
		if (!this.dataset.containsNamedModel(uri)) {
			this.dataset.addNamedModel(uri, ModelFactory.createDefaultModel());
		}
		return this.dataset.getNamedModel(uri).begin();
	}

	public Model getReadOnlyModel() {
		String uri = getDepositPID().getURI();
		this.dataset.begin(ReadWrite.READ);
		return this.dataset.getNamedModel(uri).begin();
	}

	public void closeModel() {
		if (dataset.isInTransaction()) {
			dataset.commit();
			dataset.end();
		}
	}

	public void destroyModel() {
		String uri = getDepositPID().getURI();
		if (!dataset.isInTransaction()) {
			getWritableModel();
		}
		if (this.dataset.containsNamedModel(uri)) {
			this.dataset.removeNamedModel(uri);
		}
	}

	protected void setTotalClicks(int totalClicks) {
		getJobStatusFactory().setTotalCompletion(getJobUUID(), totalClicks);
	}

	protected void addClicks(int clicks) {
		getJobStatusFactory().incrCompletion(getJobUUID(), clicks);
	}

	public File getSubdir(String subpath) {
		return new File(getDepositDirectory(), subpath);
	}
}
