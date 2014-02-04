package edu.unc.lib.workers;

import java.util.HashMap;
import java.util.Map;

import static edu.unc.lib.dl.util.RedisWorkerConstants.JobField;
import static edu.unc.lib.dl.util.RedisWorkerConstants.JobStatus;
import static edu.unc.lib.dl.util.RedisWorkerConstants.DEPOSIT_TO_JOBS_PREFIX;
import static edu.unc.lib.dl.util.RedisWorkerConstants.JOB_STATUS_PREFIX;
import redis.clients.jedis.Jedis;

public class JobStatusFactory {
	Jedis jedis;
	
	public Jedis getJedis() {
		return jedis;
	}

	public void setJedis(Jedis jedis) {
		this.jedis = jedis;
	}
	
	public void started(AbstractBagJob job) {
		Map<String, String> status = new HashMap<String, String>();
		status.put(JobField.uuid.name(), job.getJobUUID());
		status.put(JobField.name.name(), job.getClass().getName());
		status.put(JobField.status.name(), JobStatus.working.name());
		status.put(JobField.starttime.name(), String.valueOf(System.currentTimeMillis()));
		status.put(JobField.num.name(), "0");
		this.jedis.hmset(JOB_STATUS_PREFIX+job.getJobUUID(), status);
		this.jedis.sadd(DEPOSIT_TO_JOBS_PREFIX+job.getDepositPID().getUUID(), job.getJobUUID());
	}
	
	public void failed(AbstractBagJob job) {
		this.jedis.hset(JOB_STATUS_PREFIX+job.getJobUUID(), JobField.status.name(), JobStatus.failed.name());
		this.jedis.hset(JOB_STATUS_PREFIX+job.getJobUUID(), JobField.endtime.name(), String.valueOf(System.currentTimeMillis()));
	}
	
	public void failed(AbstractBagJob job, String message) {
		failed(job);
		if(message != null) this.jedis.hset(JOB_STATUS_PREFIX+job.getJobUUID(), JobField.message.name(), message);
	}
	
	public void completed(AbstractBagJob job) {
		this.jedis.hset(JOB_STATUS_PREFIX+job.getJobUUID(), JobField.status.name(), JobStatus.completed.name());
		this.jedis.hset(JOB_STATUS_PREFIX+job.getJobUUID(), JobField.endtime.name(), String.valueOf(System.currentTimeMillis()));		
	}
	
	public void killed(AbstractBagJob job) {
		this.jedis.hset(JOB_STATUS_PREFIX+job.getJobUUID(), JobField.status.name(), JobStatus.killed.name());
		this.jedis.hset(JOB_STATUS_PREFIX+job.getJobUUID(), JobField.endtime.name(), String.valueOf(System.currentTimeMillis()));		
	}
	
	public void incrCompletion(AbstractBagJob job, int amount) {
		this.jedis.hincrBy(JOB_STATUS_PREFIX+job.getJobUUID(), JobField.num.name(), amount);
	}
}
