package edu.unc.lib.deposit.normalize;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SourcePath2N3BagJob extends AbstractMETS2N3BagJob {
	private static final Logger LOG = LoggerFactory.getLogger(SourcePath2N3BagJob.class);
	public SourcePath2N3BagJob() {
		super();
	}

	public SourcePath2N3BagJob(String uuid, String depositUUID) {
		super(uuid, depositUUID);
	}

	@Override
	public void runJob() {
	}
	
}
