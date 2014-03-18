package edu.unc.lib.dl.util;

import static edu.unc.lib.dl.util.RedisWorkerConstants.DEPOSIT_SET;
import static edu.unc.lib.dl.util.RedisWorkerConstants.DEPOSIT_STATUS_PREFIX;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositField;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositState;

public class DepositStatusFactory {
	JedisPool jedisPool;
	
	public JedisPool getJedisPool() {
		return jedisPool;
	}

	public void setJedisPool(JedisPool jedisPool) {
		this.jedisPool = jedisPool;
	}

	public DepositStatusFactory() {}
	
	public Map<String, String> get(String depositUUID) {
		Jedis jedis = getJedisPool().getResource();
		Map<String, String> result = jedis.hgetAll(DEPOSIT_STATUS_PREFIX+depositUUID);
		getJedisPool().returnResource(jedis);
		return result;
	}
	
	public Set<Map<String, String>> getAll() {
		Set<Map<String, String>> result = new HashSet<Map<String, String>>();
		Jedis jedis = getJedisPool().getResource();
		Set<String> deposits = jedis.smembers(DEPOSIT_SET);
		for(String uuid : deposits) {
			result.add(jedis.hgetAll(DEPOSIT_STATUS_PREFIX+uuid));
		}
		getJedisPool().returnResource(jedis);
		return result;
	}

	/**
	 * Save deposit status, with the exception of incremented fields (jobs, octets, objects)
	 * @param status
	 */
	public void save(String depositUUID, Map<String, String> status) {
		Jedis jedis = getJedisPool().getResource();
		jedis.hmset(DEPOSIT_STATUS_PREFIX+depositUUID, status);
		jedis.sadd(DEPOSIT_SET, depositUUID);
		getJedisPool().returnResource(jedis);
	}
	
	/**
	 * Locks the given deposit for a designated supervisor. These
	 * are short term locks and should be released after every
	 * set of jobs are queued.
	 * @param depositUUID identify of the deposit
	 * @param owner identity of the supervisor
	 * @return true if lock acquired
	 */
	public boolean addSupervisorLock(String depositUUID, String owner) {
		Jedis jedis = getJedisPool().getResource();
		Long result = jedis.hsetnx(DEPOSIT_STATUS_PREFIX+depositUUID, DepositField.lock.name(), owner);
		getJedisPool().returnResource(jedis);
		return result == 1;
	}
	
	/**
	 * Unlocks the given deposit, allowing a new supervisor to manage it.
	 * @param depositUUID
	 */
	public void removeSupervisorLock(String depositUUID) {
		Jedis jedis = getJedisPool().getResource();
		jedis.hdel(DEPOSIT_STATUS_PREFIX+depositUUID, DepositField.lock.name());
		getJedisPool().returnResource(jedis);
	}
	
	public DepositState getState(String depositUUID) {
		DepositState result = null;
		Jedis jedis = getJedisPool().getResource();
		String state = jedis.hget(DEPOSIT_STATUS_PREFIX+depositUUID, DepositField.status.name());
		try {
			result = DepositState.valueOf(state);
		} catch(NullPointerException e) {
		} catch(IllegalArgumentException e) {
		}
		getJedisPool().returnResource(jedis);
		return result;
	}
	
	public void setState(String depositUUID, DepositState state) {
		Jedis jedis = getJedisPool().getResource();
		jedis.hset(DEPOSIT_STATUS_PREFIX+depositUUID, DepositField.status.name(), state.name());
		getJedisPool().returnResource(jedis);
	}
	
	public void incrIngestedOctets(String depositUUID, int amount) {
		Jedis jedis = getJedisPool().getResource();
		jedis.hincrBy(DEPOSIT_STATUS_PREFIX+depositUUID, DepositField.ingestedOctets.name(), amount);
		getJedisPool().returnResource(jedis);
	}
	
	public void incrIngestedObjects(String depositUUID, int amount) {
		Jedis jedis = getJedisPool().getResource();
		jedis.hincrBy(DEPOSIT_STATUS_PREFIX+depositUUID, DepositField.ingestedObjects.name(), amount);
		getJedisPool().returnResource(jedis);
	}

	public void fail(String depositUUID, Throwable e) {
		Jedis jedis = getJedisPool().getResource();
		jedis.hset(DEPOSIT_STATUS_PREFIX+depositUUID, DepositField.status.name(), DepositState.failed.name());
		jedis.hset(DEPOSIT_STATUS_PREFIX+depositUUID, DepositField.errorMessage.name(), e.getMessage());
		getJedisPool().returnResource(jedis);
	}
}
