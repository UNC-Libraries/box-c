package edu.unc.lib.workers;

import static edu.unc.lib.dl.util.RedisWorkerConstants.DEPOSIT_SET;
import static edu.unc.lib.dl.util.RedisWorkerConstants.DEPOSIT_STATUS_PREFIX;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import redis.clients.jedis.Jedis;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositField;

public class DepositStatusFactory {
	
	private Jedis jedis;

	public Jedis getJedis() {
		return jedis;
	}

	public void setJedis(Jedis jedis) {
		this.jedis = jedis;
	}

	public DepositStatusFactory() {}
	
	public Map<String, String> get(String depositUUID) {
		return jedis.hgetAll(DEPOSIT_STATUS_PREFIX+depositUUID);
	}
	
	public Set<Map<String, String>> getAll() {
		Set<Map<String, String>> result = new HashSet<Map<String, String>>();
		Set<String> deposits = jedis.smembers(DEPOSIT_SET);
		for(String uuid : deposits) {
			result.add(jedis.hgetAll(DEPOSIT_STATUS_PREFIX+uuid));
		}
		return result;
	}

	/**
	 * Save deposit status, with the exception of incremented fields (jobs, octets, objects)
	 * @param status
	 */
	public void save(String depositUUID, Map<String, String> status) {
		jedis.hmset(DEPOSIT_STATUS_PREFIX+depositUUID, status);
		jedis.sadd(DEPOSIT_SET, depositUUID);
	}
	
	public void incrIngestedOctets(String depositUUID, int amount) {
		jedis.hincrBy(DEPOSIT_STATUS_PREFIX+depositUUID, DepositField.ingestedOctets.name(), amount);
	}
	
	public void incrIngestedObjects(String depositUUID, int amount) {
		jedis.hincrBy(DEPOSIT_STATUS_PREFIX+depositUUID, DepositField.ingestedObjects.name(), amount);
	}
}
