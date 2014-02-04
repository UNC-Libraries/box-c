package edu.unc.lib.dl.util;

public class RedisWorkerConstants {
	public static final String DEPOSIT_SET = "deposits";
	public static final String DEPOSIT_STATUS_PREFIX = "deposit-status:";
	public static final String DEPOSIT_TO_JOBS_PREFIX = "deposit-to-jobs:";
	public static final String JOB_STATUS_PREFIX = "job-status:";
	
	public static enum DepositField {
		uuid, status, contactName, contactEmail, intSenderIdentifier, intSenderDescription,
		extIdentifier, depositMethod, containerId, payLoadOctets, bagDate, startTime,
		endTime, ingestedOctets, ingestedObjects, bagDirectory;
	}

	public static enum JobField {
		uuid, name, status, message, starttime, endtime, options, num, total;
	}
	
	public static enum JobStatus {
		queued, working, completed, failed, killed;
	}
}
