package edu.unc.lib.boxc.deposit.api;

public class RedisWorkerConstants {
    public static final String DEPOSIT_STATUS_PREFIX = "deposit-status:";
    public static final String DEPOSIT_METRICS_PREFIX = "deposit-metrics:";
    public static final String DEPOSIT_MANIFEST_PREFIX = "deposit-manifest:";
    public static final String DEPOSIT_TO_JOBS_PREFIX = "deposit-to-jobs:";
    public static final String DEPOSIT_PIPELINE_STATE = "deposit-pipeline-state";
    public static final String DEPOSIT_PIPELINE_ACTION = "deposit-pipeline-action";
    public static final String JOB_STATUS_PREFIX = "job-status:";
    public static final String JOB_COMPLETED_OBJECTS = "job-completed-objects:";

    public static final String RUN_ENHANCEMENT_TREE_QUEUE = "enhance-tree";

    public static final String RESQUE_QUEUE_PREFIX = "resque:queue:";

    public static enum DepositField {
        uuid, state, actionRequest, contactName, depositorName, intSenderDescription,
        fileName, depositMethod, containerId,
        createTime, startTime, endTime, ingestedObjects, directory, lock, submitTime,
        depositorEmail, packagingType, packageProfile, metsType, permissionGroups, depositMd5, depositSlug,
        errorMessage, excludeDepositRecord, fileMimetype, priority, sourceUri,
        ingestInprogress, storageLocation, staffOnly, overrideTimestamps,
        // file server deposit specific fields
        createParentFolder, filesOnlyMode;
    }

    public static enum JobField {
        uuid, name, status, message, starttime, endtime, options, num, total;
    }

    public static enum JobStatus {
        queued, working, completed, failed, killed;
    }

    public static enum DepositState {
        unregistered, queued, running, paused, finished, cancelled, failed, quieted;
    }

    public static enum Priority {
        low, normal, high
    }

    /**
     * Deposit-level instructions that can be executed by a deposit supervisor.
     *
     * @author count0
     *
     */
    public static enum DepositAction {
        register, pause, resume, cancel, destroy;
    }

    public static enum DepositPipelineState {
        starting, active, quieted, stopped, shutdown;

        public static DepositPipelineState fromName(String name) {
            try {
                return valueOf(name);
            } catch (IllegalArgumentException | NullPointerException e) {
                return null;
            }
        }
    }

    public static enum DepositPipelineAction {
        quiet, unquiet, stop;

        public static DepositPipelineAction fromName(String name) {
            try {
                return valueOf(name);
            } catch (IllegalArgumentException | NullPointerException e) {
                return null;
            }
        }
    }

    private RedisWorkerConstants() {
    }
}
