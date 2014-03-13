package edu.unc.lib.bag.validate;

import edu.unc.lib.workers.AbstractBagJob;

public class ValidateRdfGraph extends AbstractBagJob implements Runnable {

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
