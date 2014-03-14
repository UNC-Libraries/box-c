package edu.unc.lib.deposit.validate;

import edu.unc.lib.deposit.work.AbstractDepositJob;

public class ValidateRdfGraph extends AbstractDepositJob implements Runnable {

	public ValidateRdfGraph(String uuid, String bagDirectory, String depositId) {
		super(uuid, bagDirectory, depositId);
	}

	public ValidateRdfGraph() {
	}

	public void run() {
		// TODO Validate RDF Graph (no implementation)
		return;
	}

}
