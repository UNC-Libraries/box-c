package edu.unc.lib.deposit.fcrepo3;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ws.client.WebServiceTransportException;

import com.hp.hpl.jena.rdf.model.Bag;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

import edu.unc.lib.deposit.work.AbstractDepositJob;
import edu.unc.lib.deposit.work.DepositGraphUtils;
import edu.unc.lib.deposit.work.JobInterruptedException;
import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.GroupsThreadStore;
import edu.unc.lib.dl.fedora.AccessClient;
import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.fedora.FedoraTimeoutException;
import edu.unc.lib.dl.fedora.JobForwardingJMSListener;
import edu.unc.lib.dl.fedora.ListenerJob;
import edu.unc.lib.dl.fedora.ManagementClient;
import edu.unc.lib.dl.fedora.ManagementClient.Format;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.fedora.ServiceException;
import edu.unc.lib.dl.util.ContentModelHelper.Relationship;
import edu.unc.lib.dl.util.DepositConstants;
import edu.unc.lib.dl.util.DepositException;
import edu.unc.lib.dl.util.DepositStatusFactory;
import edu.unc.lib.dl.util.JMSMessageUtil;
import edu.unc.lib.dl.util.JMSMessageUtil.FedoraActions;
import edu.unc.lib.dl.util.PremisEventLogger;
import edu.unc.lib.dl.util.PremisEventLogger.Type;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositField;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositState;
import edu.unc.lib.dl.xml.FOXMLJDOMUtil;

/**
 * Ingests the contents of the deposit into the Fedora repository, along with a deposit record. Also performs updates to
 * the destination container.
 *
 * @author bbpennel
 * @author count0
 *
 */
public class IngestDeposit extends AbstractDepositJob implements Runnable, ListenerJob {

	private static final Logger log = LoggerFactory.getLogger(IngestDeposit.class);

	private static long COMPLETE_CHECK_DELAY = 500L;

	@Autowired
	private JobForwardingJMSListener listener;

	@Autowired
	private ManagementClient client;

	@Autowired
	private AccessClient accessClient;

	private int ingestObjectCount;

	private Queue<String> ingestPids;

	private Collection<String> ingestsAwaitingConfirmation;

	private List<String> topLevelPids;

	private PID destinationPID;

	// Flag indicating whether to ingest the deposit record object to the repository
	private boolean excludeDepositRecord;

	private File foxmlDirectory;

	private Map<String, String> depositStatus;

	public IngestDeposit() {
		super();
	}

	public IngestDeposit(String uuid, String depositUUID) {
		super(uuid, depositUUID);
	}

	/**
	 * Listener event fired when an object has been ingested. If the ingested object belonged to this job, then mark that
	 * ingest as completed
	 *
	 * @param message Fedora APIM message
	 */
	@Override
	public void onEvent(Document message) {

		String action = JMSMessageUtil.getAction(message);
		if (!FedoraActions.INGEST.getName().equals(action))
			return;

		PID pid = new PID(JMSMessageUtil.getPid(message));

		boolean result = ingestsAwaitingConfirmation.remove(pid.getURI());
		if (result) {
			addClicks(1);
			getDepositStatusFactory().addConfirmedPID(getDepositUUID(), pid.getPid());
			log.debug("Notified that {} has finished ingesting as part of deposit {}",
					pid.getPid(), this.getDepositUUID());
		}
	}

	/**
	 * Processes the structure of the deposit, storing the number of actions involved and retrieving an list of pids in
	 * the correct order for ingest
	 */
	private void processDepositStructure() {

		// Store reference to the foxml directory
		foxmlDirectory = new File(getDepositDirectory(), DepositConstants.FOXML_DIR);

		// Retrieve the pid of the container this object is being ingested to
		destinationPID = new PID(depositStatus.get(DepositField.containerId.name()));

		excludeDepositRecord = Boolean.parseBoolean(depositStatus.get(DepositField.excludeDepositRecord.name()));

		Model model = ModelFactory.createDefaultModel();
		File modelFile = new File(getDepositDirectory(), DepositConstants.MODEL_FILE);
		model.read(modelFile.toURI().toString());

		ingestPids = new ArrayDeque<String>();
		topLevelPids = new ArrayList<String>();
		ingestsAwaitingConfirmation = Collections.synchronizedSet(new HashSet<String>());

		String depositPid = getDepositPID().getURI();
		Bag depositBag = model.getBag(depositPid);

		// Capture number of objects and depth first list of pids for individual objects to be ingested
		DepositGraphUtils.walkChildrenDepthFirst(depositBag, ingestPids, true);

		// Store the number of objects being ingested, excluding the deposit record
		ingestObjectCount = ingestPids.size();

		// Add the deposit pid to the list
		if (!excludeDepositRecord) {
			ingestPids.add(depositPid);
		}

		// Number of actions is the number of ingest objects plus deposit record
		setTotalClicks(ingestPids.size());

		// Deposit is restarting from part way through, reduce set of items for ingest
		boolean resuming = getDepositStatusFactory().isResumedDeposit(getDepositUUID());
		if (resuming) {
			ingestObjectCount -= removeAlreadyIngested();
		}

		// Capture the top level pids
		DepositGraphUtils.walkChildrenDepthFirst(depositBag, topLevelPids, false);

		// TODO capture structure for ordered sequences instead of just bags
	}

	/**
	 * Removes any pids confirmed or uploaded and present in fedora from the list of pids for ingest
	 */
	private int removeAlreadyIngested() {
		int numberRemoved = 0;

		// Prevent reingest of all items already confirmed to have been ingested.
		Set<String> confirmedSet = getDepositStatusFactory().getConfirmedUploads(getDepositUUID());
		for (String confirmed : confirmedSet) {
			ingestPids.remove(new PID(confirmed).getURI());
			numberRemoved++;
		}

		// Check for any items that were uploaded but not confirmed, and check to see if they made it in
		Set<String> unconfirmedSet = getDepositStatusFactory().getUnconfirmedUploads(getDepositUUID());
		for (String unconfirmed : unconfirmedSet) {
			PID unconfirmedPID = new PID(unconfirmed);
			try {
				if (accessClient.getObjectProfile(unconfirmedPID, null) != null) {
					ingestPids.remove(unconfirmedPID.getURI());
					// Update status to indicate this item was actually confirmed
					addClicks(1);
					getDepositStatusFactory().addConfirmedPID(getDepositUUID(), unconfirmedPID.getPid());
					numberRemoved++;
				}
			} catch (FedoraException e) {
				// Object wasn't found, so ingest must have failed. Should be retained for ingest
			} catch (ServiceException e) {
				log.debug("Unexpected failure while checking for ingest of {}", unconfirmedPID, e);
			}
		}

		return numberRemoved;
	}

	@Override
	public void run() {

		depositStatus = getDepositStatus();

		try {
			// set up permission groups for forwarding
			String groups = depositStatus.get(DepositField.permissionGroups.name());
			AccessGroupSet ags = new AccessGroupSet(groups);
			GroupsThreadStore.storeGroups(ags);
			GroupsThreadStore.storeUsername(depositStatus.get(DepositField.depositorName.name()));

			// Extract information about structure of the deposit
			processDepositStructure();

			// Register this job with the JMS listener prior to doing work
			listener.registerListener(this);

			DepositStatusFactory statusFactory = getDepositStatusFactory();

			// Begin ingest of individual objects in the deposit
			String ingestPid = null;
			try {

				// Ingest all deposit objects and record, start listening for them
				while ((ingestPid = ingestPids.poll()) != null) {

					addTopLevelToContainer(ingestPid);

					// Register pid as needing ingest confirmation
					ingestsAwaitingConfirmation.add(ingestPid);

					ingestObject(ingestPid);

					statusFactory.addUploadedPID(getDepositUUID(), new PID(ingestPid).getPid());
					statusFactory.incrIngestedObjects(getDepositUUID(), 1);

					// Verify that the job has not been interrupted before continuing
					DepositState state = statusFactory.getState(getDepositUUID());
					if (!DepositState.running.equals(state)) {
						throw new JobInterruptedException("State for job " + getDepositUUID()
								+ " is no longer running, interrupting");
					}
				}
			} catch (DepositException e) {
				failJob(e, Type.INGESTION, "Ingest of object {0} failed", ingestPid);
				return;
			}

			// listen to Fedora JMS to see when all objects are ingested
			try {
				while (ingestsAwaitingConfirmation.size() > 0) {
					Thread.sleep(COMPLETE_CHECK_DELAY);
				}

				log.debug("Finished waiting for children of {} to be ingested", this.getDepositUUID());
			} catch (InterruptedException e) {
				log.info("Interrupted ingest of job {}", this.getJobUUID());
				return;
			}

			updateDestinationEvents();
		} finally {
			GroupsThreadStore.clearGroups();
			GroupsThreadStore.clearUsername();
			// Unregister self from the jms listener
			listener.unregisterListener(this);
		}

	}

	/**
	 * Adds the given objects pid to the destination container if the pid is at the top level of the ingest
	 *
	 * @param pid
	 * @throws DepositException
	 */
	private void addTopLevelToContainer(String pid) throws DepositException {
		if (!topLevelPids.contains(pid))
			return;

		try {
			client.addObjectRelationship(destinationPID, Relationship.contains.getURI().toString(), new PID(pid));
		} catch (FedoraTimeoutException e) {
			throw e;
		} catch (FedoraException e) {
			throw new DepositException("Failed to add object " + pid + " to destination " + destinationPID.getPid(), e);
		} catch (ServiceException e) {
			// If there was a web service transport error, make sure Fedora is still available
			if (e.getCause() instanceof Exception && ((Exception) e.getCause()) instanceof WebServiceTransportException) {
				if (!client.isRepositoryAvailable()) {
					// Exception was because Fedora became unavailable, so rethrow as a timeout exception
					throw new FedoraTimeoutException("Failed to connect to Fedora while adding object " + pid
							+ " to its parent");
				}
			}

			throw e;
		}
	}

	/**
	 * Ingests an object and its referenced files into Fedora
	 *
	 * @param ingestPid
	 * @throws DepositException
	 */
	private void ingestObject(String ingestPid) throws DepositException {

		PID pid = new PID(ingestPid);
		File foxml = new File(foxmlDirectory, pid.getUUID() + ".xml");

		// Load objects foxml
		SAXBuilder builder = new SAXBuilder();
		Document foxmlDoc;
		try {
			foxmlDoc = builder.build(foxml);
		} catch (Exception e) {
			throw new DepositException("Failed to parse FOXML for object " + pid.getPid(), e);
		}

		// Upload files included in this ingest and updates file references
		uploadIngestFiles(foxmlDoc, pid);

		// Ingest the object's FOXML
		try {

			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

			XMLOutputter xmlOutput = new XMLOutputter();
			xmlOutput.output(foxmlDoc, outputStream);

			log.debug("Ingesting foxml for {}", ingestPid);
			client.ingestRaw(outputStream.toByteArray(), Format.FOXML_1_1, getDepositUUID());

		} catch (FedoraTimeoutException e) {
			log.info("Fedora ingest timed out, awaiting ingest confirmation and proceeding with the remainder of the deposit: "
					+ e.getLocalizedMessage());
		} catch (Exception e) {
			throw new DepositException("Failed to ingest object " + pid.getPid() + " into Fedora.", e);
		}
		// TODO increment ingestedOctets

	}

	/**
	 * Uploads locally held files and PREMIS referenced by an objects FOXML. As a side effect, updates the FOXML
	 * document's file references to point to the uploaded file paths in Fedora instead of the local file paths.
	 *
	 * @param foxml
	 * @param pid
	 * @throws DepositException
	 */
	private void uploadIngestFiles(Document foxml, PID pid) throws DepositException {

		for (Element cLocation : FOXMLJDOMUtil.getFileLocators(foxml)) {
			String ref = cLocation.getAttributeValue("REF");
			String newref = null;
			try {
				URI uri = new URI(ref);
				// Upload local file reference
				if (uri.getScheme() == null || uri.getScheme().contains("file")) {
					try {
						String path = uri.getPath();
						File file = getDepositDirectory().toPath().resolve(path).toFile();

						// Make sure the file was inside the deposit directory
						if (!file.toPath().toAbsolutePath().startsWith(getDepositDirectory().toPath().toAbsolutePath())) {
							throw new ServiceException("File path was outside the deposit directory");
						}

						if (!file.exists()) {
							throw new IOException("File not found: " + ref);
						}

						log.debug("uploading " + file.getPath());
						newref = client.upload(file);

						cLocation.setAttribute("REF", newref);
					} catch (FedoraTimeoutException e) {
						log.warn("Connection to Fedora lost while ingesting {}, halting ingest", ref);
						throw e;
					} catch (IOException e) {
						throw new DepositException("Data file missing: " + ref, e);
					} catch (ServiceException e) {
						throw new DepositException("Problem uploading file: " + ref, e);
					}
				} else if (uri.getScheme().contains("premisEvents")) {
					// Upload PREMIS
					try {
						File file = new File(getEventsDirectory(), ref.substring(ref.indexOf(":") + 1));

						Document premis = new SAXBuilder().build(file);
						getEventLog().logEvent(PremisEventLogger.Type.INGESTION, "ingested as PID:" + pid.getPid(), pid);
						getEventLog().appendLogEvents(pid, premis.getRootElement());

						log.debug("uploading " + file.getPath());
						newref = client.upload(premis);
						cLocation.setAttribute("REF", newref);
					} catch (Exception e) {
						throw new DepositException("There was a problem uploading ingest events" + ref, e);
					}
				} else {
					continue;
				}
			} catch (URISyntaxException e) {
				throw new DepositException("Bad URI syntax for file ref", e);
			}
			log.debug("uploaded " + ref + " to Fedora " + newref + " for " + pid);
		}

	}

	/**
	 * Updates the destination container event log to include this ingest
	 */
	private void updateDestinationEvents() {

		// Record ingest event on parent
		PremisEventLogger destinationPremis = new PremisEventLogger(getDepositStatus().get(DepositField.depositorName));

		destinationPremis.logEvent(PremisEventLogger.Type.INGESTION,
				"added " + ingestObjectCount + " child object(s) to this container", destinationPID);
		try {
			client.writePremisEventsToFedoraObject(destinationPremis, destinationPID);
		} catch (FedoraException e) {
			log.error("Failed to update PREMIS events after completing ingest to " + destinationPID.getPid(), e);
		}

	}

	public Queue<String> getIngestPids() {
		return ingestPids;
	}

	public List<String> getTopLevelPids() {
		return topLevelPids;
	}

	public Collection<String> getIngestsAwaitingConfirmation() {
		return ingestsAwaitingConfirmation;
	}

	public void setListener(JobForwardingJMSListener listener) {
		this.listener = listener;
	}

	public int getIngestObjectCount() {
		return ingestObjectCount;
	}

}
