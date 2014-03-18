package edu.unc.lib.deposit.fcrepo3;

import edu.unc.lib.deposit.work.AbstractDepositJob;

/**
 * Ingests the contents of the bag into the Fedora repository, along
 * with a deposit record. Also performs updates to the destination container.
 * @author count0
 *
 */
public class IngestBag extends AbstractDepositJob {

	public IngestBag() {
		super();
	}

	public IngestBag(String uuid, String depositUUID) {
		super(uuid, depositUUID);
	}

	public String execute() {
		// TODO update container
		// queue ingest of deposit record
		// queue object ingests, root to branches
		// listen to Fedora JMS to see when all objects are ingested
		// send email
		return null;
	}

}
