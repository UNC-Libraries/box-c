package edu.unc.lib.bag.validate;

import edu.unc.lib.workers.AbstractBagJob;

/**
 * Orchestrates parallel validation jobs, including
 * virus scans, metadata validation, structural validation, container
 * check, and slug conflict check.
 * @author count0
 *
 */
public class BagValidation extends AbstractBagJob {

	public BagValidation() {
		super();
	}

	public BagValidation(String uuid, String bagDirectory, String depositId) {
		super(uuid, bagDirectory, depositId);
	}

	@Override
	public void run() {
		enqueueJob(VirusScanJob.class.getName());
		enqueueJob(ValidateMODS.class.getName());
		enqueueJob(ValidateDepositContainer.class.getName());
	}

}
