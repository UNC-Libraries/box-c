package edu.unc.lib.deposit.fcrepo3;

import static edu.unc.lib.deposit.work.DepositGraphUtils.dprop;
import static edu.unc.lib.dl.util.ContentModelHelper.Datastream.DATA_FILE;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ws.client.WebServiceTransportException;

import com.hp.hpl.jena.rdf.model.Bag;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

import edu.unc.lib.deposit.work.AbstractDepositJob;
import edu.unc.lib.deposit.work.DepositGraphUtils;
import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.GroupsThreadStore;
import edu.unc.lib.dl.fedora.AccessClient;
import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.fedora.FedoraTimeoutException;
import edu.unc.lib.dl.fedora.JobForwardingJMSListener;
import edu.unc.lib.dl.fedora.ListenerJob;
import edu.unc.lib.dl.fedora.ManagementClient;
import edu.unc.lib.dl.fedora.ManagementClient.Format;
import edu.unc.lib.dl.fedora.ObjectExistsException;
import edu.unc.lib.dl.fedora.ObjectIntegrityException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.fedora.ServiceException;
import edu.unc.lib.dl.fedora.types.Datastream;
import edu.unc.lib.dl.ingest.IngestException;
import edu.unc.lib.dl.reporting.ActivityMetricsClient;
import edu.unc.lib.dl.services.DigitalObjectManager;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.util.ContentModelHelper.DepositRelationship;
import edu.unc.lib.dl.util.ContentModelHelper.Relationship;
import edu.unc.lib.dl.util.DepositConstants;
import edu.unc.lib.dl.util.DepositException;
import edu.unc.lib.dl.util.DepositStatusFactory;
import edu.unc.lib.dl.util.JMSMessageUtil;
import edu.unc.lib.dl.util.JMSMessageUtil.FedoraActions;
import edu.unc.lib.dl.util.PremisEventLogger;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositField;
import edu.unc.lib.dl.util.TripleStoreQueryService;
import edu.unc.lib.dl.xml.FOXMLJDOMUtil;

/**
 * Ingests the contents of the deposit into the Fedora repository, along with a deposit record. Also performs updates to
 * the destination container.
 *
 * @author bbpennel
 * @author count0
 *
 */
public class IngestDeposit extends AbstractDepositJob implements ListenerJob {

	private static final Logger log = LoggerFactory.getLogger(IngestDeposit.class);

	private static long COMPLETE_CHECK_DELAY = 500L;

	private static long CONNECT_EXCEPTION_DELAY = 30000L;

	@Autowired
	private JobForwardingJMSListener listener;

	@Autowired
	private ManagementClient client;

	@Autowired
	private DigitalObjectManager digitalObjectManager;

	@Autowired
	private AccessClient accessClient;

	@Autowired
	private TripleStoreQueryService tsqs;

	@Autowired
	private ActivityMetricsClient metricsClient;

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
		if (!FedoraActions.INGEST.getName().equals(action)) {
			return;
		}

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

		Model model = getReadOnlyModel();

		ingestPids = new ArrayDeque<>();
		topLevelPids = new ArrayList<>();
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

		closeModel();

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
	public void runJob() {

		depositStatus = getDepositStatus();

		try {
			// set up permission groups for forwarding
			String groups = depositStatus.get(DepositField.permissionGroups.name());
			AccessGroupSet ags = new AccessGroupSet(groups);
			GroupsThreadStore.storeGroups(ags);
			GroupsThreadStore.storeUsername(depositStatus.get(DepositField.depositorName.name()));

			// When ingesting, assume that an "object exists" exception is confirmation
			// that the object exists, rather than an error.
			boolean confirmExisting;

			if (Boolean.parseBoolean(depositStatus.get(DepositField.isResubmit.name()))) {
				confirmExisting = true;
			} else {
				confirmExisting = false;
			}

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

					ingestObject(ingestPid, confirmExisting);

					statusFactory.addUploadedPID(getDepositUUID(), new PID(ingestPid).getPid());
					statusFactory.incrIngestedObjects(getDepositUUID(), 1);

					// Verify that the job has not been interrupted before continuing
					verifyRunning();
				}
			} catch (DepositException e) {
				failJob(e, e.getLocalizedMessage(), ingestPid);
				return;
			}

			// listen to Fedora JMS to see when all objects are ingested
			try {
				while (ingestsAwaitingConfirmation.size() > 0) {
					verifyRunning();
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
		if (!topLevelPids.contains(pid)) {
			return;
		}

		while (true) {
			try {
				digitalObjectManager.addChildrenToContainer(destinationPID, Arrays.asList(new PID(pid)));
				return;
			} catch (FedoraTimeoutException e) {
				throw e;
			} catch (FedoraException | IngestException e) {
				throw new DepositException("Failed to add object " + pid + " to destination " + destinationPID.getPid(), e);
			} catch (ServiceException e) {
				waitIfConnectionLostOrRethrow(e);
			}
		}
	}

	/**
	 * Ingests an object and its referenced files into Fedora.
	 * <p>
	 * If confirmExisting is true, we will consider an exception from Fedora
	 * telling us the object already exists to be confirmation that it is already
	 * ingested and remove it from the list of ingests awaiting confirmation.
	 * Otherwise, we will rethrow such exceptions.
	 *
	 * @param ingestPid
	 * @param confirmExisting
	 * @throws DepositException
	 */
	private void ingestObject(String ingestPid, boolean confirmExisting) throws DepositException {

		PID pid = new PID(ingestPid);
		File foxml = new File(foxmlDirectory, pid.getUUID() + ".xml");

		// Load objects foxml
		SAXBuilder builder = new SAXBuilder();
		Document foxmlDoc;
		try {
			foxmlDoc = builder.build(foxml);
		} catch (Exception e) {
			throw new DepositException("Failed to parse FOXML for object " + pid.getPid() + ".", e);
		}

		// Add ingestion event to PREMIS log
		Element ingestEvent = getEventLog().logEvent(PremisEventLogger.Type.INGESTION, "ingested as PID:" + pid.getPid(),
				pid);
		appendDepositEvent(pid, ingestEvent);

		// Upload files included in this ingest and updates file references
		uploadIngestFiles(foxmlDoc, pid);

		// Ingest the object's FOXML
		try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

			XMLOutputter xmlOutput = new XMLOutputter();
			xmlOutput.output(foxmlDoc, outputStream);

			while (true) {
				try {
					log.debug("Ingesting foxml for {}", ingestPid);
					client.ingestRaw(outputStream.toByteArray(), Format.FOXML_1_1, getDepositUUID());

					// Record FOXML throughput metrics
					metricsClient.incrDepositFileThroughput(getDepositUUID(), foxml.length());

					return;
				} catch (ServiceException e) {
					waitIfConnectionLostOrRethrow(e);
				}
			}

		} catch (FedoraTimeoutException e) {
			log.info("Fedora ingest timed out, awaiting ingest confirmation and proceeding with the remainder of the deposit: "
					+ e.getLocalizedMessage());
		} catch (ObjectExistsException e) {
			if (confirmExisting || isDuplicateOkay(pid)) {
				if (ingestsAwaitingConfirmation.remove(ingestPid)) {
					addClicks(1);
				}
			} else {
				throw new DepositException("Object " + pid.getPid() + " already exists in the repository.", e);
			}
		} catch (ObjectIntegrityException e) {
			throw new DepositException("Checksum mismatch for object " + pid.getPid() + ".", e);
		} catch (Exception e) {
			throw new DepositException("Failed to ingest object " + pid.getPid() + " into Fedora.", e);
		}
		// TODO increment ingestedOctets

	}

	private boolean isDuplicateOkay(PID pid) {
		// Get the deposit ID for the repository copy of pid
		List<String> deposits = tsqs.fetchBySubjectAndPredicate(pid, Relationship.originalDeposit.toString());

		// Ensure that the deposit id as record by fedora matches the current deposit or is not present
		if (deposits != null && !deposits.contains(this.getDepositPID().getURI())) {
			return false;
		}

		Model model = getReadOnlyModel();
		try {
			Resource objectResc = model.getResource(pid.getURI());

			Property stagingLocation = dprop(model, DepositRelationship.stagingLocation);
			if (!objectResc.hasProperty(stagingLocation)) {
				// No staging location, no file, no reason to check further
				return true;
			}

			// Get information for copy in the repository
			Datastream ds = client.getDatastream(pid, DATA_FILE.getName());

			// Confirm that incoming file is the same size as the one in the repository
			Property filesizeProperty = dprop(model, DepositRelationship.size);
			if (objectResc.hasProperty(filesizeProperty)) {
				long incomingSize = Long.parseLong(objectResc.getProperty(filesizeProperty).getString());

				if (incomingSize != ds.getSize() && !(ds.getSize() == -1 && incomingSize == 0)) {
					// File sizes didn't match, so this is not the correct file
					return false;
				}
			}

			// If a checksum is available, make sure it matches the one in the repository
			Property md5sum = dprop(model, DepositRelationship.md5sum);
			if (objectResc.hasProperty(md5sum)) {
				String incomingChecksum = objectResc.getProperty(md5sum).getString();
				return ds.getChecksum().equals(incomingChecksum);
			}

			return true;
		} catch (FedoraException e1) {
			log.debug("Failed to get datastream info while checking on duplicate for {}", pid, e1);
		} finally {
			closeModel();
		}

		return false;
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

					String path = uri.getPath();
					File file = getDepositDirectory().toPath().resolve(path).toFile();

					boolean isManifest = cLocation.getParentElement().getAttributeValue("ID")
							.startsWith(ContentModelHelper.Datastream.DATA_MANIFEST.getName());

					// Make sure the file was inside the deposit directory, unless it is from a deposit record manifest
					if (!isManifest && !file.toPath().toAbsolutePath().startsWith(getDepositDirectory().toPath().toAbsolutePath())) {
						throw new DepositException("File path was outside the deposit directory: " + file.toPath().toAbsolutePath());
					}

					repeatUpload: while (true) {
						try {
							if (!file.exists()) {
								throw new IOException("File not found: " + ref);
							}

							log.debug("uploading " + file.getPath());
							newref = client.upload(file);

							cLocation.setAttribute("REF", newref);

							// Record throughput metrics
							metricsClient.incrDepositFileThroughput(getDepositUUID(), file.length());

							break repeatUpload;
						} catch (FedoraTimeoutException e) {
							log.warn("Connection to Fedora lost while ingesting {}, halting ingest", ref);
							throw e;
						} catch (IOException e) {
							throw new DepositException("Data file missing: " + ref, e);
						} catch (ServiceException e) {
							waitIfConnectionLostOrRethrow(e);
						}
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

		while (true) {
			try {
				client.writePremisEventsToFedoraObject(destinationPremis, destinationPID);
				return;
			} catch (FedoraException e) {
				log.error("Failed to update PREMIS events after completing ingest to " + destinationPID.getPid(), e);
				return;
			} catch (ServiceException e) {
				waitIfConnectionLostOrRethrow(e);
			}
		}
	}

	/**
	 * If the given exception indicates that it was caused by a lost connection, then wait until
	 * the repository is available again.  Otherwise, rethrow the exception
	 *
	 * @param e
	 * @throws ServiceException
	 */
	private void waitIfConnectionLostOrRethrow(ServiceException e) throws ServiceException {

		Throwable rootCause = e.getRootCause();
		if (rootCause instanceof ConnectException || rootCause instanceof WebServiceTransportException) {

			while (true) {
				log.warn("Unable to connect to Fedora repository, waiting before retrying.");
				try {
					Thread.sleep(CONNECT_EXCEPTION_DELAY);
				} catch (InterruptedException e1) {
					throw new ServiceException("Attempt to reconnect to Fedora was interrupted.");
				}

				verifyRunning();
				if (client.isRepositoryAvailable()) {
					return;
				}
			}
		} else {
			throw e;
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
