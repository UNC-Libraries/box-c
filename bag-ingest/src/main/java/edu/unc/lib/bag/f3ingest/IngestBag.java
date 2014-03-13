package edu.unc.lib.bag.f3ingest;

import edu.unc.lib.workers.AbstractBagJob;

/**
 * Ingests the contents of the bag into the Fedora repository, along
 * with a deposit record. Also performs updates to the destination container.
 * @author count0
 *
 */
public class IngestBag extends AbstractBagJob {

	public IngestBag() {
		super();
	}

	public IngestBag(String uuid, String bagDirectory, String depositId) {
		super(uuid, bagDirectory, depositId);
	}

	public String execute() {
		// TODO update container
		// queue object ingests, root to branches
		// listen to Fedora JMS to see when all objects are ingested
		// send email
		return null;
	}

}
