package edu.unc.lib.bag.validate;

import edu.unc.lib.bag.AbstractBagJob;

/**
 * Asserts that the destination container exists and is the right sort of container.
 * @author count0
 *
 */
public class ValidateDepositContainer extends AbstractBagJob {

	public ValidateDepositContainer(String bagDirectory, String depositId) {
		super(bagDirectory, depositId);
	}

	public ValidateDepositContainer() {
	}

	@Override
	public void run() {

	}

}
