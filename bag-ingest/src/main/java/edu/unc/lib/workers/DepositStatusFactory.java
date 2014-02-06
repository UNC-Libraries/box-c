package edu.unc.lib.workers;

import static edu.unc.lib.dl.util.RedisWorkerConstants.DEPOSIT_SET;
import static edu.unc.lib.dl.util.RedisWorkerConstants.DEPOSIT_STATUS_PREFIX;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositField;

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
}
