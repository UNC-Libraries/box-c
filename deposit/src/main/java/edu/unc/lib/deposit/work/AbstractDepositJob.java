package edu.unc.lib.deposit.work;

import static edu.unc.lib.dl.util.DepositConstants.DESCRIPTION_DIR;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.ReadWrite;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

import edu.unc.lib.dl.event.FilePremisLogger;
import edu.unc.lib.dl.event.PremisLogger;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.Repository;
import edu.unc.lib.dl.fcrepo4.RepositoryPathConstants;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.DepositConstants;
import edu.unc.lib.dl.util.DepositStatusFactory;
import edu.unc.lib.dl.util.JobStatusFactory;
import edu.unc.lib.dl.util.RDFModelUtil;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositState;

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

	protected Repository repository;

	// UUID for this deposit and its deposit record
	private String depositUUID;
	
	private PID depositPID;

	// UUID for this ingest job
	private String jobUUID;

	// Root directory where all deposits are stored
	@Autowired
	private File depositsDirectory;

	// Directory for this deposit
	private File depositDirectory;

	// Directory for local data files
	private File dataDirectory;

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
		this.setDepositUUID(depositUUID);
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
			dataset.end();
		}
	}

	public abstract void runJob();

	public String getDepositUUID() {
		return depositUUID;
	}

	public void setDepositUUID(String depositUUID) {
		this.depositUUID = depositUUID;
		this.depositPID = PIDs.get(RepositoryPathConstants.DEPOSIT_RECORD_BASE, depositUUID);
	}

	public PID getDepositPID() {
		return depositPID;
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

	public File getEventsDirectory() {
		return eventsDirectory;
	}

	public Repository getRepository() {
		return repository;
	}

	public void setRepository(Repository repository) {
		this.repository = repository;
	}

	/**
	 * Returns the manifest URIs for this deposit, or an empty list in case there are no manifests.
	 *
	 * @return
	 */
	public List<String> getManifestFileURIs() {
		List<String> filePaths = depositStatusFactory.getManifestURIs(getDepositUUID());
		return filePaths;
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
	 * Creates new PremisLogger object from which instances can build and write Premis events to a file
	 *
	 * @param pid
	 * @return PremisLogger object
	 */
	public PremisLogger getPremisLogger(PID pid) {
		File file = new File(depositDirectory, DepositConstants.EVENTS_DIR + "/" + pid.getUUID() + ".ttl");

		try {
			if (!file.exists()) {
				file.getParentFile().mkdirs();
			} 
			return new FilePremisLogger(pid, file, repository);
		} catch (Exception e) {
			failJob(e, "Unexpected problem with deposit events file {}.", file.getAbsoluteFile());
		}

		return null;
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
	
	protected void serializeObjectModel(PID pid, Model objModel) {
		File propertiesFile = new File(getSubdir(DepositConstants.AIPS_DIR), pid.getUUID() + ".ttl");
		
		try {
			RDFModelUtil.serializeModel(objModel, propertiesFile);
		} catch (IOException e) {
			failJob(e, "Failed to serialize properties for object {} to {}",
					pid, propertiesFile.getAbsolutePath());
		}
	}
}