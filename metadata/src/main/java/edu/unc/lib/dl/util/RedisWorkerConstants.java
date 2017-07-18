/**
 * Copyright 2008 The University of North Carolina at Chapel Hill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.unc.lib.dl.util;

public class RedisWorkerConstants {
    public static final String DEPOSIT_STATUS_PREFIX = "deposit-status:";
    public static final String DEPOSIT_METRICS_PREFIX = "deposit-metrics:";
    public static final String DEPOSIT_MANIFEST_PREFIX = "deposit-manifest:";
    public static final String DEPOSIT_TO_JOBS_PREFIX = "deposit-to-jobs:";
    public static final String JOB_STATUS_PREFIX = "job-status:";
    public static final String BULK_UPDATE_PREFIX = "bulk-update:";
    public static final String BULK_RESUME_PREFIX = "bulk-resume:";
    public static final String BULK_UPDATE_QUEUE = "bulk-md-update";
    public static final String OPERATION_METRICS_PREFIX = "operation-metrics:";

    public static final String RUN_ENHANCEMENT_TREE_QUEUE = "enhance-tree";

    public static final String RESQUE_QUEUE_PREFIX = "resque:queue:";

    public static enum DepositField {
        uuid, state, actionRequest, contactName, depositorName, intSenderIdentifier, intSenderDescription,
        fileName, resubmitDirName, resubmitFileName, isResubmit, depositMethod, containerId, payLoadOctets,
        createTime, startTime, endTime, ingestedOctets, ingestedObjects, directory, lock, submitTime,
        depositorEmail, packagingType, packageProfile, metsType, permissionGroups, depositMd5, depositSlug,
        errorMessage, stackTrace, excludeDepositRecord, publishObjects, fileMimetype, priority, sourcePath,
        extras, ingestInprogress;
    }

    public static enum JobField {
        uuid, name, status, message, starttime, endtime, options, num, total;
    }

    public static enum JobStatus {
        queued, working, completed, failed, killed;
    }

    public static enum DepositState {
        unregistered, queued, running, paused, finished, cancelled, failed;
    }

    public static enum Priority {
        normal, high
    }

    /**
     * Deposit-level instructions that can be executed by a deposit supervisor.
     * 
     * @author count0
     *
     */
    public static enum DepositAction {
        register, pause, resume, cancel, destroy, resubmit;
    }

    private RedisWorkerConstants() {
    }
}
