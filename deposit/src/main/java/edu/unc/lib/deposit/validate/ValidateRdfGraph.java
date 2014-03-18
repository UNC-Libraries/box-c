package edu.unc.lib.deposit.validate;

import edu.unc.lib.deposit.work.AbstractDepositJob;

public class ValidateRdfGraph extends AbstractDepositJob implements Runnable {

	public ValidateRdfGraph(String uuid, String depositUUID) {
		super(uuid, depositUUID);
	}

	public ValidateRdfGraph() {
	}

	public void run() {
		// TODO Validate RDF Graph (no implementation)
		return;
	}

}
