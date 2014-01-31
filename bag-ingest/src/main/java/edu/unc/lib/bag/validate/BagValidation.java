package edu.unc.lib.bag.validate;

import edu.unc.lib.bag.AbstractBagJob;

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

	public BagValidation(String bagDirectory, String depositId) {
		super(bagDirectory, depositId);
	}

	@Override
	public void run() {
		
	}

}
