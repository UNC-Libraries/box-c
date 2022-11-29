package edu.unc.lib.boxc.deposit.impl.model;

import static edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DEPOSIT_TO_JOBS_PREFIX;
import static edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.JOB_COMPLETED_OBJECTS;
import static edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.JOB_STATUS_PREFIX;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.JobField;
import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.JobStatus;

/**
 * Work with job statuses
 * @author bbpennel
 *
 */
public class JobStatusFactory extends AbstractJedisFactory {

    public void started(String jobUUID, String depositUUID, Class<?> jobClass) {
        Map<String, String> status = new HashMap<String, String>();
        status.put(JobField.uuid.name(), jobUUID);
        status.put(JobField.name.name(), jobClass.getName());
        status.put(JobField.status.name(), JobStatus.working.name());
        status.put(JobField.starttime.name(),
                String.valueOf(System.currentTimeMillis()));
        status.put(JobField.num.name(), "0");

        connectWithRetries((jedis) -> {
            jedis.hmset(JOB_STATUS_PREFIX + jobUUID, status);
            jedis.rpush(DEPOSIT_TO_JOBS_PREFIX + depositUUID, jobUUID);
        });
    }

    public void interrupted(String jobUUID) {
        connectWithRetries((jedis) -> {
            jedis.hset(JOB_STATUS_PREFIX + jobUUID, JobField.status.name(), JobStatus.queued.name());
            jedis.hset(JOB_STATUS_PREFIX + jobUUID,
                    JobField.endtime.name(), String.valueOf(System.currentTimeMillis()));
        });
    }

    public void failed(String jobUUID, String message) {
        connectWithRetries((jedis) -> {
            jedis.hset(JOB_STATUS_PREFIX + jobUUID, JobField.status.name(), JobStatus.failed.name());
            jedis.hset(JOB_STATUS_PREFIX + jobUUID,
                    JobField.endtime.name(), String.valueOf(System.currentTimeMillis()));
            if (message != null) {
                jedis.hset(JOB_STATUS_PREFIX + jobUUID, JobField.message.name(), message);
            }
        });
    }

    public void failed(String jobUUID) {
        failed(jobUUID, null);
    }

    /**
     * Removes the all leftover partially completed jobs from the deposit
     *
     * @param depositUUID
     */
    public void clearStale(String depositUUID) {

        List<String> failed = getJobsByStatus(depositUUID, JobStatus.failed);
        List<String> queued = getJobsByStatus(depositUUID, JobStatus.queued);
        List<String> working = getJobsByStatus(depositUUID, JobStatus.working);

        List<String> uuids = new ArrayList<String>(failed);
        uuids.addAll(queued);
        uuids.addAll(working);

        connectWithRetries((jedis) -> {
            for (String uuid : uuids) {
                jedis.del(JOB_STATUS_PREFIX + uuid);
                jedis.lrem(DEPOSIT_TO_JOBS_PREFIX + depositUUID, 0, uuid);
                jedis.del(JOB_COMPLETED_OBJECTS + uuid);
            }
        });
    }

    public void completed(String jobUUID) {
        connectWithRetries((jedis) -> {
            jedis.hset(JOB_STATUS_PREFIX + jobUUID,
                    JobField.status.name(), JobStatus.completed.name());
            jedis.hset(JOB_STATUS_PREFIX + jobUUID,
                    JobField.endtime.name(),
                    String.valueOf(System.currentTimeMillis()));
        });
    }

    public void killed(String jobUUID) {
        connectWithRetries((jedis) -> {
            jedis.hset(JOB_STATUS_PREFIX + jobUUID,
                    JobField.status.name(), JobStatus.killed.name());
            jedis.hset(JOB_STATUS_PREFIX + jobUUID,
                    JobField.endtime.name(),
                    String.valueOf(System.currentTimeMillis()));
        });
    }

    public void incrCompletion(String jobUUID, int amount) {
        connectWithRetries((jedis) -> {
            jedis.hincrBy(JOB_STATUS_PREFIX + jobUUID,
                    JobField.num.name(), amount);
        });
    }

    public void setCompletion(String jobUUID, int amount) {
        connectWithRetries((jedis) -> {
            jedis.hset(JOB_STATUS_PREFIX + jobUUID,
                JobField.num.name(), Integer.toString(amount));
        });
    }

    public String getJobState(String uuid) {
        AtomicReference<String> result = new AtomicReference<>();
        connectWithRetries((jedis) -> {
            result.set(jedis.hget(JOB_STATUS_PREFIX + uuid,
                    JobField.status.name()));
        });
        return result.get();
    }

    public void setTotalCompletion(String jobUUID, int totalClicks) {
        connectWithRetries((jedis) -> {
            jedis.hset(JOB_STATUS_PREFIX + jobUUID, JobField.total.name(),
                    String.valueOf(totalClicks));
        });
    }

    public Map<String, String> get(String jobUUID) {
        AtomicReference<Map<String, String>> result = new AtomicReference<>();
        connectWithRetries((jedis) -> {
            result.set(jedis.hgetAll(JOB_STATUS_PREFIX + jobUUID));
        });
        return result.get();
    }

    /**
     * Checks if an object is already in the list of deposited objects for the current deposit
     *
     * @param depositId Id of the current deposit
     * @param objectId Id of the object to check
     * @return boolean
     */
    public boolean objectIsCompleted(String depositId, String objectId) {
        final AtomicBoolean result = new AtomicBoolean(false);
        connectWithRetries((jedis) -> {
            result.set(jedis.sismember(JOB_COMPLETED_OBJECTS + depositId, objectId));
        });
        return result.get();
    }

    /**
     * A list of the values of the current deposit's objects that have been completed
     *
     * @param depositId Id of the current deposit
     * @param objectId Id of the completed object
     */
    public void addObjectCompleted(String depositId, String objectId) {
        connectWithRetries((jedis) -> {
            jedis.sadd(JOB_COMPLETED_OBJECTS + depositId, objectId);
        });
    }

    /**
     * Retrieves the names of the jobs that have already succeeded for the
     * deposit.
     *
     * @param depositUUID
     *            the id of the deposit
     * @return a set of job class names
     */
    public List<String> getSuccessfulJobNames(String depositUUID) {
        List<String> result = new ArrayList<String>();
        connectWithRetries((jedis) -> {
            List<String> jobUUIDs = jedis.lrange(DEPOSIT_TO_JOBS_PREFIX + depositUUID, 0, -1);

            for (String jobUUID : jobUUIDs) {
                List<String> info = jedis.hmget(JOB_STATUS_PREFIX + jobUUID,
                                new String[] { JobField.status.name(), JobField.name.name() });

                if (JobStatus.completed.name().equals(info.get(0))) {
                    result.add(info.get(1));
                }
            }
        });
        return result;
    }

    /**
     * Delete all job status related to the deposit.
     *
     * @param depositUUID
     */
    public void deleteAll(String depositUUID) {
        connectWithRetries((jedis) -> {
            List<String> jobUUIDs = jedis.lrange(DEPOSIT_TO_JOBS_PREFIX + depositUUID, 0, -1);

            for (String jobUUID : jobUUIDs) {
                jedis.del(JOB_STATUS_PREFIX + jobUUID);
            }
            jedis.del(JOB_COMPLETED_OBJECTS + depositUUID);
            jedis.del(DEPOSIT_TO_JOBS_PREFIX + depositUUID);
        });
    }

    public String getWorkingJob(String depositUUID) {
        return getJobByStatus(depositUUID, JobStatus.working);
    }

    public String getJobByStatus(String depositUUID, JobStatus status) {
        AtomicReference<String> result = new AtomicReference<>();
        connectWithRetries((jedis) -> {
            List<String> jobUUIDs = jedis.lrange(DEPOSIT_TO_JOBS_PREFIX + depositUUID, 0, -1);

            for (String jobUUID : jobUUIDs) {
                String statusValue = jedis.hget(JOB_STATUS_PREFIX + jobUUID, JobField.status.name());

                if (status.name().equals(statusValue)) {
                    result.set(jobUUID);
                    return;
                }
            }
        });

        return result.get();
    }

    public List<String> getJobsByStatus(String depositUUID, JobStatus status) {
        List<String> result = new ArrayList<>();
        connectWithRetries((jedis) -> {
            List<String> jobUUIDs = jedis.lrange(DEPOSIT_TO_JOBS_PREFIX + depositUUID, 0, -1);

            for (String jobUUID : jobUUIDs) {
                String statusValue = jedis.hget(JOB_STATUS_PREFIX + jobUUID, JobField.status.name());

                if (status.name().equals(statusValue)) {
                    result.add(jobUUID);
                }
            }
        });

        return result;
    }

    /**
     * Expire all the job keys associated with this deposit in X seconds.
     *
     * @param depositUUID
     * @param seconds
     *            time until expire
     */
    public void expireKeys(String depositUUID, int seconds) {
        connectWithRetries((jedis) -> {
            List<String> jobUUIDs = jedis.lrange(DEPOSIT_TO_JOBS_PREFIX + depositUUID, 0, -1);

            for (String jobUUID : jobUUIDs) {
                jedis.expire(JOB_STATUS_PREFIX + jobUUID, seconds);
            }
            jedis.expire(JOB_COMPLETED_OBJECTS
                    + depositUUID, seconds);
            jedis.expire(DEPOSIT_TO_JOBS_PREFIX
                    + depositUUID, seconds);
        });
    }

    public Map<String, Map<String, String>> getAllJobs(String depositUUID) {
        Map<String, Map<String, String>> results = new LinkedHashMap<>();

        connectWithRetries((jedis) -> {
            List<String> jobUUIDs = jedis.lrange(DEPOSIT_TO_JOBS_PREFIX + depositUUID, 0, -1);

            for (String jobUUID : jobUUIDs) {
                Map<String, String> status = jedis.hgetAll(JOB_STATUS_PREFIX + jobUUID);
                results.put(jobUUID, status);
            }
        });

        return results;
    }
}
