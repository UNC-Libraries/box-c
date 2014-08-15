package edu.unc.lib.dl.util;

import static edu.unc.lib.dl.util.RedisWorkerConstants.DEPOSIT_TO_JOBS_PREFIX;
import static edu.unc.lib.dl.util.RedisWorkerConstants.JOB_STATUS_PREFIX;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import edu.unc.lib.dl.util.RedisWorkerConstants.JobField;
import edu.unc.lib.dl.util.RedisWorkerConstants.JobStatus;

public class JobStatusFactory {

	private JedisPool jedisPool;

	public JedisPool getJedisPool() {
		return jedisPool;
	}

	public void setJedisPool(JedisPool jedisPool) {
		this.jedisPool = jedisPool;
	}

	public void started(String jobUUID, String depositUUID, Class<?> jobClass) {
		Map<String, String> status = new HashMap<String, String>();
		status.put(JobField.uuid.name(), jobUUID);
		status.put(JobField.name.name(), jobClass.getName());
		status.put(JobField.status.name(), JobStatus.working.name());
		status.put(JobField.starttime.name(),
				String.valueOf(System.currentTimeMillis()));
		status.put(JobField.num.name(), "0");

		Jedis jedis = getJedisPool().getResource();
		jedis.hmset(JOB_STATUS_PREFIX + jobUUID, status);
		jedis.rpush(DEPOSIT_TO_JOBS_PREFIX + depositUUID, jobUUID);
		getJedisPool().returnResource(jedis);
	}

	public void failed(String jobUUID) {
		Jedis jedis = getJedisPool().getResource();
		jedis.hset(JOB_STATUS_PREFIX + jobUUID, JobField.status.name(), JobStatus.failed.name());
		jedis.hset(JOB_STATUS_PREFIX + jobUUID,
				JobField.endtime.name(), String.valueOf(System.currentTimeMillis()));
		getJedisPool().returnResource(jedis);
	}

	public void interrupted(String jobUUID) {
		Jedis jedis = getJedisPool().getResource();
		jedis.hset(JOB_STATUS_PREFIX + jobUUID, JobField.status.name(), JobStatus.queued.name());
		jedis.hset(JOB_STATUS_PREFIX + jobUUID,
				JobField.endtime.name(), String.valueOf(System.currentTimeMillis()));
		getJedisPool().returnResource(jedis);
	}

	public void failed(String jobUUID, String message) {
		failed(jobUUID);
		if (message != null) {
			Jedis jedis = getJedisPool().getResource();
			jedis.hset(JOB_STATUS_PREFIX + jobUUID,
					JobField.message.name(), message);
			getJedisPool().returnResource(jedis);
		}
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

		Jedis jedis = getJedisPool().getResource();
		try {
			for (String uuid : uuids) {
				jedis.del(JOB_STATUS_PREFIX + uuid);
				jedis.lrem(DEPOSIT_TO_JOBS_PREFIX + depositUUID, 0, uuid);
			}
		} finally {
			getJedisPool().returnResource(jedis);
		}
	}

	public void completed(String jobUUID) {
		Jedis jedis = getJedisPool().getResource();
		jedis.hset(JOB_STATUS_PREFIX + jobUUID,
				JobField.status.name(), JobStatus.completed.name());
		jedis.hset(JOB_STATUS_PREFIX + jobUUID,
				JobField.endtime.name(),
				String.valueOf(System.currentTimeMillis()));
		getJedisPool().returnResource(jedis);
	}

	public void killed(String jobUUID) {
		Jedis jedis = getJedisPool().getResource();
		jedis.hset(JOB_STATUS_PREFIX + jobUUID,
				JobField.status.name(), JobStatus.killed.name());
		jedis.hset(JOB_STATUS_PREFIX + jobUUID,
				JobField.endtime.name(),
				String.valueOf(System.currentTimeMillis()));
		getJedisPool().returnResource(jedis);
	}

	public void incrCompletion(String jobUUID, int amount) {
		Jedis jedis = getJedisPool().getResource();
		jedis.hincrBy(JOB_STATUS_PREFIX + jobUUID,
				JobField.num.name(), amount);
		getJedisPool().returnResource(jedis);
	}

	public String getJobState(String uuid) {
		Jedis jedis = getJedisPool().getResource();
		String result = jedis.hget(JOB_STATUS_PREFIX + uuid,
				JobField.status.name());
		getJedisPool().returnResource(jedis);
		return result;
	}

	public void setTotalCompletion(String jobUUID, int totalClicks) {
		Jedis jedis = getJedisPool().getResource();
		jedis.hset(JOB_STATUS_PREFIX + jobUUID, JobField.total.name(),
				String.valueOf(totalClicks));
		getJedisPool().returnResource(jedis);
	}

	public Map<String, String> get(String jobUUID) {
		Jedis jedis = getJedisPool().getResource();
		Map<String, String> result = jedis.hgetAll(JOB_STATUS_PREFIX + jobUUID);
		getJedisPool().returnResource(jedis);
		return result;
	}

	/**
	 * Retrieves the names of the jobs that have already succeeded for the deposit.
	 * @param depositUUID the id of the deposit
	 * @return a set of job class names
	 */
	public List<String> getSuccessfulJobNames(String depositUUID) {
		List<String> result = new ArrayList<String>();
		Jedis jedis = getJedisPool().getResource();
		try {
			List<String> jobUUIDs = jedis.lrange(DEPOSIT_TO_JOBS_PREFIX + depositUUID, 0, -1);

			for (String jobUUID : jobUUIDs) {
				List<String> info = jedis.hmget(JOB_STATUS_PREFIX + jobUUID,
								new String[] { JobField.status.name(), JobField.name.name() });

				if (JobStatus.completed.name().equals(info.get(0))) {
					result.add(info.get(1));
				}
			}
		} finally {
			getJedisPool().returnResource(jedis);
		}
		return result;
	}

	/**
	 * Delete all job status related to the deposit.
	 * @param depositUUID
	 */
	public void deleteAll(String depositUUID) {
		Jedis jedis = getJedisPool().getResource();
		try {
			List<String> jobUUIDs = jedis.lrange(DEPOSIT_TO_JOBS_PREFIX + depositUUID, 0, -1);

			for (String jobUUID : jobUUIDs) {
				jedis.del(JOB_STATUS_PREFIX + jobUUID);
			}
			jedis.del(DEPOSIT_TO_JOBS_PREFIX + depositUUID);
		} finally {
			getJedisPool().returnResource(jedis);
		}
	}

	public String getWorkingJob(String depositUUID) {
		return getJobByStatus(depositUUID, JobStatus.working);
	}

	public String getJobByStatus(String depositUUID, JobStatus status) {
		Jedis jedis = getJedisPool().getResource();
		try {
			List<String> jobUUIDs = jedis.lrange(DEPOSIT_TO_JOBS_PREFIX + depositUUID, 0, -1);

			for (String jobUUID : jobUUIDs) {
				String statusValue = jedis.hget(JOB_STATUS_PREFIX + jobUUID, JobField.status.name());

				if (status.name().equals(statusValue)) {
					return jobUUID;
				}
			}
		} finally {
			getJedisPool().returnResource(jedis);
		}

		return null;
	}

	public List<String> getJobsByStatus(String depositUUID, JobStatus status) {
		Jedis jedis = getJedisPool().getResource();

		List<String> result = new ArrayList<String>();
		try {
			List<String> jobUUIDs = jedis.lrange(DEPOSIT_TO_JOBS_PREFIX + depositUUID, 0, -1);

			for (String jobUUID : jobUUIDs) {
				String statusValue = jedis.hget(JOB_STATUS_PREFIX + jobUUID, JobField.status.name());

				if (status.name().equals(statusValue)) {
					result.add(jobUUID);
				}
			}
		} finally {
			getJedisPool().returnResource(jedis);
		}

		return result;
	}

	/**
	 * Expire all the job keys associated with this deposit in X seconds.
	 * @param depositUUID
	 * @param statusKeysExpireSeconds time until expire
	 */
	public void expireKeys(String depositUUID, int seconds) {
		Jedis jedis = getJedisPool().getResource();
		try {
			List<String> jobUUIDs = jedis.lrange(DEPOSIT_TO_JOBS_PREFIX + depositUUID, 0, -1);

			for (String jobUUID : jobUUIDs) {
				jedis.expire(RedisWorkerConstants.JOB_STATUS_PREFIX+jobUUID, seconds);
			}
			jedis.expire(RedisWorkerConstants.DEPOSIT_TO_JOBS_PREFIX
					+ depositUUID, seconds);
		} finally {
			getJedisPool().returnResource(jedis);
		}
	}

	public Map<String, Map<String, String>> getAllJobs(String depositUUID) {
		Map<String, Map<String, String>> results = new LinkedHashMap<String, Map<String, String>>();

		Jedis jedis = getJedisPool().getResource();
		try {
			List<String> jobUUIDs = jedis.lrange(DEPOSIT_TO_JOBS_PREFIX + depositUUID, 0, -1);

			for (String jobUUID : jobUUIDs) {
				Map<String, String> status = jedis.hgetAll(JOB_STATUS_PREFIX + jobUUID);
				results.put(jobUUID, status);
			}
		} finally {
			getJedisPool().returnResource(jedis);
		}

		return results;
	}
}
