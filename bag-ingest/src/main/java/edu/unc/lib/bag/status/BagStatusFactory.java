package edu.unc.lib.bag.status;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class BagStatusFactory {
	private static final Logger log = LoggerFactory.getLogger(BagStatusFactory.class);
	private static final String statusKey = "bag-status";
	private String ingestedOctetsPrefix = "bag-octets:";
	private String ingestedObjectsPrefix = "bag-objects:";
	
	private Jedis jedis;

	public Jedis getJedis() {
		return jedis;
	}

	public void setJedis(Jedis jedis) {
		this.jedis = jedis;
	}

	public BagStatusFactory() {}
	
	public BagStatus get(String depositUUID) {
		BagStatus result = null;
		String json = jedis.hget(statusKey, depositUUID);
		result = makeStatus(depositUUID, json);
		return result;
	}
	
	public Set<BagStatus> getAll() {
		Set<BagStatus> result = new HashSet<BagStatus>();
		Map<String, String> uuid2json = jedis.hgetAll(statusKey);
		for(Entry<String, String> entry : uuid2json.entrySet()) {
			result.add(makeStatus(entry.getKey(), entry.getValue()));	
		}
		return result;
	}

	private BagStatus makeStatus(String depositUUID, String json) {
		BagStatus result = null;
		ObjectMapper om = new ObjectMapper();
		try {
			result = om.readValue(json, BagStatus.class);
			result.setUuid(depositUUID);
		} catch (JsonParseException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		addIncrementedFields(result);
		return result;
	}
	
	private void addIncrementedFields(BagStatus result) {
		String depositUUID = result.getUuid();
		String octets = jedis.get(ingestedOctetsPrefix+depositUUID);
		String objects = jedis.get(ingestedObjectsPrefix+depositUUID);
		if(octets != null) result.setIngestedOctets(Integer.valueOf(octets));
		if(objects != null) result.setIngestedObjects(Integer.valueOf(objects));
	}

	/**
	 * Save bag status, with the exception of incremented fields (jobs, octets, objects)
	 * @param status
	 */
	public void saveStatus(BagStatus status) {
		ObjectMapper om = new ObjectMapper();
		try {
			String json = om.writeValueAsString(status);
			jedis.hset(statusKey, status.getUuid(), json);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
	}
	
	public void addFailedJob(String depositUUID, String jobLabel, String errorMessage) {
		
	}
	
	public void addCompletedJob(String depositUUID, String jobLabel) {
		
	}
	
	public void incrIngestedOctets(String depositUUID, int amount) {
		jedis.incrBy(ingestedOctetsPrefix+depositUUID, amount);
	}
	
	public void incrIngestedObjects(String depositUUID, int amount) {
		jedis.incrBy(ingestedObjectsPrefix+depositUUID, amount);
	}
}
