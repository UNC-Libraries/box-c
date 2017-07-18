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

import static edu.unc.lib.dl.util.RedisWorkerConstants.DEPOSIT_MANIFEST_PREFIX;
import static edu.unc.lib.dl.util.RedisWorkerConstants.DEPOSIT_STATUS_PREFIX;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.util.RedisWorkerConstants.DepositAction;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositField;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositState;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * Deposit/redis interactions
 * @author bbpennel
 *
 */
public class DepositStatusFactory {
    private static final Logger log = LoggerFactory
            .getLogger(DepositStatusFactory.class);

    JedisPool jedisPool;

    public DepositStatusFactory() {
    }

    public Map<String, String> get(String depositUUID) {
        try (Jedis jedis = getJedisPool().getResource()) {
            return jedis.hgetAll(DEPOSIT_STATUS_PREFIX + depositUUID);
        }
    }

    public Set<Map<String, String>> getAll() {
        Set<Map<String, String>> result = new HashSet<>();
        try (Jedis jedis = getJedisPool().getResource()) {
            Set<String> keys = jedis.keys(DEPOSIT_STATUS_PREFIX + "*");
            if (keys != null) {
                for (String key : keys) {
                    result.add(jedis.hgetAll(key));
                }
            }
        }
        return result;
    }

    public void addManifest(String depositUUID, String value) {
        try (Jedis jedis = getJedisPool().getResource()) {
            jedis.rpush(DEPOSIT_MANIFEST_PREFIX + depositUUID, value);
        }

    }

    public List<String> getManifestURIs(String depositUUID) {
        try (Jedis jedis = getJedisPool().getResource()) {
            return jedis.lrange(DEPOSIT_MANIFEST_PREFIX + depositUUID, 0, -1);
        }
    }

    /**
     * Save deposit status, with the exception of incremented fields (jobs,
     * octets, objects)
     * 
     * @param status
     */
    public void save(String depositUUID, Map<String, String> status) {
        try (Jedis jedis = getJedisPool().getResource()) {
            jedis.hmset(DEPOSIT_STATUS_PREFIX + depositUUID, status);
        }
    }

    /**
     * Set a single deposit field.
     * 
     * @param status
     */
    public void set(String depositUUID, DepositField field, String value) {
        try (Jedis jedis = getJedisPool().getResource()) {
            jedis.hset(DEPOSIT_STATUS_PREFIX + depositUUID, field.name(), value);
        }
    }

    /**
     * Locks the given deposit for a designated supervisor. These are short term
     * locks and should be released after every set of jobs is queued.
     * 
     * @param depositUUID
     *            identify of the deposit
     * @param owner
     *            identity of the supervisor
     * @return true if lock acquired
     */
    public boolean addSupervisorLock(String depositUUID, String owner) {
        try (Jedis jedis = getJedisPool().getResource()) {
            Long result = jedis.hsetnx(DEPOSIT_STATUS_PREFIX + depositUUID,
                    DepositField.lock.name(), owner);
            return result == 1;
        }
    }

    /**
     * Unlocks the given deposit, allowing a new supervisor to manage it.
     * 
     * @param depositUUID
     */
    public void removeSupervisorLock(String depositUUID) {
        try (Jedis jedis = getJedisPool().getResource()) {
            jedis.hdel(DEPOSIT_STATUS_PREFIX + depositUUID, DepositField.lock.name());
        }
    }

    public DepositState getState(String depositUUID) {
        try (Jedis jedis = getJedisPool().getResource()) {
            String state = jedis.hget(DEPOSIT_STATUS_PREFIX + depositUUID, DepositField.state.name());
            if (state == null) {
                log.debug("No state was found for deposit {}", depositUUID);
                return null;
            }
            return DepositState.valueOf(state);
        }
    }

    public boolean isResumedDeposit(String depositUUID) {
        try (Jedis jedis = getJedisPool().getResource()) {
            String value = jedis.hget(DEPOSIT_STATUS_PREFIX + depositUUID,
                    DepositField.ingestInprogress.name());
            return Boolean.parseBoolean(value);
        }
    }

    public void setIngestInprogress(String depositUUID, boolean value) {
        try (Jedis jedis = getJedisPool().getResource()) {
            jedis.hset(DEPOSIT_STATUS_PREFIX + depositUUID,
                    DepositField.ingestInprogress.name(), Boolean.toString(value));
        }
    }

    public void setState(String depositUUID, DepositState state) {
        try (Jedis jedis = getJedisPool().getResource()) {
            jedis.hset(DEPOSIT_STATUS_PREFIX + depositUUID, DepositField.state.name(),
                    state.name());
        }
    }

    public void setActionRequest(String depositUUID, DepositAction action) {
        try (Jedis jedis = getJedisPool().getResource()) {
            jedis.hset(DEPOSIT_STATUS_PREFIX + depositUUID, DepositField.actionRequest.name(), action.name());
        }
    }

    public void fail(String depositUUID, String message) {
        try (Jedis jedis = getJedisPool().getResource()) {
            jedis.hset(DEPOSIT_STATUS_PREFIX + depositUUID, DepositField.state.name(), DepositState.failed.name());
            if (message != null) {
                jedis.hset(DEPOSIT_STATUS_PREFIX + depositUUID, DepositField.errorMessage.name(), message);
            }
        }
    }

    public void fail(String depositUUID) {
        fail(depositUUID, null);
    }

    public void deleteField(String depositUUID, DepositField field) {
        try (Jedis jedis = getJedisPool().getResource()) {
            jedis.hdel(DEPOSIT_STATUS_PREFIX + depositUUID, field.name());
        }
    }

    public void requestAction(String depositUUID, DepositAction action) {
        try (Jedis jedis = getJedisPool().getResource()) {
            jedis.hset(DEPOSIT_STATUS_PREFIX + depositUUID, DepositField.actionRequest.name(),
                    action.name());
        }
    }

    public void clearActionRequest(String depositUUID) {
        try (Jedis jedis = getJedisPool().getResource()) {
            jedis.hdel(DEPOSIT_STATUS_PREFIX + depositUUID, DepositField.actionRequest.name());
        }
    }

    /**
     * Expire the deposit status key after given interval.
     * 
     * @param depositUUID
     * @param seconds
     *            time until expire
     */
    public void expireKeys(String depositUUID, int seconds) {
        try (Jedis jedis = getJedisPool().getResource()) {
            jedis.expire(DEPOSIT_STATUS_PREFIX + depositUUID, seconds);
            jedis.expire(DEPOSIT_MANIFEST_PREFIX + depositUUID, seconds);
        }
    }

    public void setJedisPool(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    private JedisPool getJedisPool() {
        return jedisPool;
    }
}
