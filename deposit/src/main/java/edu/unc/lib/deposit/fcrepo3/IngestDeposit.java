package edu.unc.lib.deposit.fcrepo3;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.jdom.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Bag;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Resource;

import edu.unc.lib.deposit.work.AbstractDepositJob;
import edu.unc.lib.dl.fedora.JobForwardingJMSListener;
import edu.unc.lib.dl.fedora.ListenerJob;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.DepositConstants;
import edu.unc.lib.dl.util.JMSMessageUtil;
import edu.unc.lib.dl.util.JMSMessageUtil.FedoraActions;

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

	private int ingestObjectCount;

	private Set<String> ingestPids;

	private List<String> topLevelPids;

	private JobForwardingJMSListener listener;

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
	 * @param pid
	 * @param action
	 */
	@Override
	public void onEvent(Document message) {

		String action = JMSMessageUtil.getAction(message);
		if (!FedoraActions.INGEST.getName().equals(action))
			return;

		PID pid = new PID(JMSMessageUtil.getPid(message));
		ingestPids.remove(pid.getURI());

		addClicks(1);
	}

	/**
	 * Processes the structure of the deposit, storing the number of actions involved and retrieving an list of pids in
	 * the correct order for ingest
	 */
	private void processDepositStructure() {

		Model model = ModelFactory.createDefaultModel();
		File modelFile = new File(getDepositDirectory(), DepositConstants.MODEL_FILE);
		model.read(modelFile.toURI().toString());

		ingestPids = Collections.synchronizedSet(new LinkedHashSet<String>());
		topLevelPids = new ArrayList<String>();

		String depositPid = getDepositPID().getURI();
		Bag depositBag = model.getBag(depositPid);

		// Capture number of objects and depth first list of pids for individual objects to be ingested
		walkChildren(depositBag, ingestPids, true);

		// Store the number of objects being ingested now before it starts changing
		ingestObjectCount = ingestPids.size();

		// Add the deposit pid to the list
		ingestPids.add(depositPid);

		// Capture the top level pids
		walkChildren(depositBag, topLevelPids, false);

		// TODO capture structure for ordered sequences instead of just bags

		// Number of actions is the number of ingest objects plus deposit record
		setTotalClicks(ingestPids.size());

	}

	/**
	 * Walk the children of the given bag in depth first order, storing children to the ingest pid tracking list.
	 *
	 * @param bag
	 */
	private void walkChildren(Bag bag, Collection<String> pids, boolean recursive) {

		NodeIterator childIt = bag.iterator();
		while (childIt.hasNext()) {
			Resource childResource = (Resource) childIt.next();

			pids.add(childResource.getURI());

			if (recursive) {
				Bag childBag = childResource.getModel().getBag(childResource);
				walkChildren(childBag, pids, recursive);
			}
		}
	}

	@Override
	public void run() {

		// TODO resume deposit from the point it last left off at

		// Extract information about structure of the deposit
		processDepositStructure();

		// Register this job with the JMS listener prior to doing work
		listener.registerListener(this);

		// TODO update container
		// queue ingest of deposit record
		// queue object ingests, root to branches

		// listen to Fedora JMS to see when all objects are ingested
		try {
			while (ingestPids.size() > 0) {
				Thread.sleep(COMPLETE_CHECK_DELAY);
			}
		} catch (InterruptedException e) {
			log.info("Interrupted ingest of job {}", this.getJobUUID());
		}

		// Unregister self from the jms listener
		listener.unregisterListener(this);

		// TODO send confirmation email
	}

	public Set<String> getIngestPids() {
		return ingestPids;
	}

	public List<String> getTopLevelPids() {
		return topLevelPids;
	}

	public void setListener(JobForwardingJMSListener listener) {
		this.listener = listener;
	}

	public int getIngestObjectCount() {
		return ingestObjectCount;
	}

}
