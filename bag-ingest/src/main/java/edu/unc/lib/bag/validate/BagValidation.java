package edu.unc.lib.bag.validate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.util.PremisEventLogger.Type;
import edu.unc.lib.workers.AbstractBagJob;

/**
 * Orchestrates parallel validation jobs, including
 * virus scans, metadata validation, structural validation, container
 * check, and slug conflict check.
 * @author count0
 *
 */
public class BagValidation extends AbstractBagJob {
	private static final Logger log = LoggerFactory.getLogger(BagValidation.class);

	public BagValidation() {
		super();
	}

	public BagValidation(String uuid, String bagDirectory, String depositId) {
		super(uuid, bagDirectory, depositId);
	}

	@Override
	public void run() {
		log.debug("starting validation jobs");
		String[] jobs = new String[4];
		jobs[0] = enqueueJob(VirusScanJob.class.getName());
		jobs[1] = enqueueJob(ValidateMODS.class.getName());
		jobs[2] = enqueueJob(ValidateDepositContainer.class.getName());
		jobs[3] = enqueueJob(ValidateRdfGraph.class.getName());
		boolean success = joinAfterExecute(60*60*24, true, jobs);
		if(!success) {
			failJob(Type.VALIDATION, "A validation job has failed and deposit cannot proceed", null);
		}
		enqueueDefaultNextJob();
	}

}
