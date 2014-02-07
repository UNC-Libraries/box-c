package edu.unc.lib.bag.f3ingest;

import edu.unc.lib.workers.AbstractBagJob;

public class IngestObject extends AbstractBagJob implements Runnable {

	public IngestObject() {
		super();
	}

	public IngestObject(String uuid, String bagDirectory, String depositId, String pid) {
		super(uuid, bagDirectory, depositId);
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}

}
