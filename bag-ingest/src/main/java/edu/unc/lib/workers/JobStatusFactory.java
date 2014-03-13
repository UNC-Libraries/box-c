package edu.unc.lib.workers;

import static edu.unc.lib.dl.util.RedisWorkerConstants.DEPOSIT_TO_JOBS_PREFIX;
import static edu.unc.lib.dl.util.RedisWorkerConstants.JOB_STATUS_PREFIX;

import java.util.HashMap;
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
	
	public void started(AbstractBagJob job) {
		Map<String, String> status = new HashMap<String, String>();
		status.put(JobField.uuid.name(), job.getJobUUID());
		status.put(JobField.name.name(), job.getClass().getName());
		status.put(JobField.status.name(), JobStatus.working.name());
		status.put(JobField.starttime.name(), String.valueOf(System.currentTimeMillis()));
		status.put(JobField.num.name(), "0");
		Jedis jedis = getJedisPool().getResource();
		jedis.hmset(JOB_STATUS_PREFIX+job.getJobUUID(), status);
		jedis.sadd(DEPOSIT_TO_JOBS_PREFIX+job.getDepositPID().getUUID(), job.getJobUUID());
		getJedisPool().returnResource(jedis);
	}
	
	public void failed(AbstractBagJob job) {
		Jedis jedis = getJedisPool().getResource();
		jedis.hset(JOB_STATUS_PREFIX+job.getJobUUID(), JobField.status.name(), JobStatus.failed.name());
		jedis.hset(JOB_STATUS_PREFIX+job.getJobUUID(), JobField.endtime.name(), String.valueOf(System.currentTimeMillis()));
		getJedisPool().returnResource(jedis);
	}
	
	public void failed(AbstractBagJob job, String message) {
		failed(job);
		if(message != null) {
			Jedis jedis = getJedisPool().getResource();
			jedis.hset(JOB_STATUS_PREFIX+job.getJobUUID(), JobField.message.name(), message);
			getJedisPool().returnResource(jedis);
		}
	}
	
	public void completed(AbstractBagJob job) {
		Jedis jedis = getJedisPool().getResource();
		jedis.hset(JOB_STATUS_PREFIX+job.getJobUUID(), JobField.status.name(), JobStatus.completed.name());
		jedis.hset(JOB_STATUS_PREFIX+job.getJobUUID(), JobField.endtime.name(), String.valueOf(System.currentTimeMillis()));
		getJedisPool().returnResource(jedis);		
	}
	
	public void killed(AbstractBagJob job) {
		Jedis jedis = getJedisPool().getResource();
		jedis.hset(JOB_STATUS_PREFIX+job.getJobUUID(), JobField.status.name(), JobStatus.killed.name());
		jedis.hset(JOB_STATUS_PREFIX+job.getJobUUID(), JobField.endtime.name(), String.valueOf(System.currentTimeMillis()));
		getJedisPool().returnResource(jedis);		
	}
	
	public void incrCompletion(AbstractBagJob job, int amount) {
		Jedis jedis = getJedisPool().getResource();
		jedis.hincrBy(JOB_STATUS_PREFIX+job.getJobUUID(), JobField.num.name(), amount);
		getJedisPool().returnResource(jedis);
	}
	
	public String getJobState(String uuid) {
		Jedis jedis = getJedisPool().getResource();
		String result = jedis.hget(JOB_STATUS_PREFIX+uuid, JobField.status.name());
		getJedisPool().returnResource(jedis);
		return result;
	}

	public void setTotalCompletion(AbstractBagJob job, int totalClicks) {
		Jedis jedis = getJedisPool().getResource();
		jedis.hset(JOB_STATUS_PREFIX+job.getJobUUID(), JobField.total.name(), String.valueOf(totalClicks));
		getJedisPool().returnResource(jedis);
	}

}
