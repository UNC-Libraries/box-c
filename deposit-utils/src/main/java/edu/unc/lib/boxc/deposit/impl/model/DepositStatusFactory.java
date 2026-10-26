package edu.unc.lib.boxc.deposit.impl.model;

import static edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DEPOSIT_MANIFEST_PREFIX;
import static edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DEPOSIT_STATUS_PREFIX;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositField;
import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositState;

/**
 * Deposit/redis interactions
 * @author bbpennel
 *
 */
public class DepositStatusFactory extends AbstractJedisFactory {
    private static final String RELEASE_SUPERVISOR_LOCK_SCRIPT = """
            if redis.call('hget', KEYS[1], ARGV[1]) == ARGV[2] then
                return redis.call('hdel', KEYS[1], ARGV[1])
            end
            return 0
            """;
    private static final Logger log = LoggerFactory
            .getLogger(DepositStatusFactory.class);

    public DepositStatusFactory() {
    }


    public Map<String, String> get(String depositUUID) {
        AtomicReference<Map<String, String>> result = new AtomicReference<>();
        connectWithRetries((jedis) -> {
            result.set(jedis.hgetAll(DEPOSIT_STATUS_PREFIX + depositUUID));
        });
        return result.get();
    }

    public Set<Map<String, String>> getAll() {
        Set<Map<String, String>> result = new HashSet<>();
        connectWithRetries((jedis) -> {
            Set<String> keys = jedis.keys(DEPOSIT_STATUS_PREFIX + "*");
            if (keys != null) {
                for (String key : keys) {
                    result.add(jedis.hgetAll(key));
                }
            }
        });
        return result;
    }

    /**
     * Save deposit status, with the exception of incremented fields (jobs,
     * octets, objects)
     *
     * @param status
     */
    public void save(String depositUUID, Map<String, String> status) {
        connectWithRetries((jedis) -> {
            jedis.hmset(DEPOSIT_STATUS_PREFIX + depositUUID, status);
        });
    }

    /**
     * Set a single deposit field.
     *
     * @param depositUUID
     * @param field
     * @param value
     */
    public void set(String depositUUID, DepositField field, String value) {
        connectWithRetries((jedis) -> {
            jedis.hset(DEPOSIT_STATUS_PREFIX + depositUUID, field.name(), value);
        });
    }

    /**
     * Locks the given deposit for a designated supervisor. These are short term
     * locks and should be released after every set of jobs is queued.
     *
     * @param depositUUID
     *            identify of the deposit
     * @param lockToken
     *            unique token identifying this lock holder
     * @return true if lock acquired
     */
    public boolean addSupervisorLock(String depositUUID, String lockToken) {
        final AtomicBoolean acquired = new AtomicBoolean(false);
        connectWithRetries((jedis) -> {
            Long result = jedis.hsetnx(DEPOSIT_STATUS_PREFIX + depositUUID,
                    DepositField.lock.name(), lockToken);
            acquired.set(result == 1);
        });
        return acquired.get();
    }

    /**
     * Releases the lock only when it is still owned by the given token.
     *
     * @param depositUUID
     *            identity of the deposit
     * @param lockToken
     *            unique token identifying this lock holder
     * @return true if the lock was removed
     */
    public boolean removeSupervisorLock(String depositUUID, String lockToken) {
        final AtomicBoolean removed = new AtomicBoolean(false);
        connectWithRetries((jedis) -> {
            Object result = jedis.eval(RELEASE_SUPERVISOR_LOCK_SCRIPT,
                    java.util.List.of(DEPOSIT_STATUS_PREFIX + depositUUID),
                    java.util.List.of(DepositField.lock.name(), lockToken));
            removed.set(Long.valueOf(1).equals(result));
        });
        return removed.get();
    }

    /**
     * Removes a lock left behind by an interrupted supervisor during startup recovery.
     * Normal message processing must use {@link #removeSupervisorLock(String, String)}.
     */
    public void forceRemoveSupervisorLock(String depositUUID) {
        connectWithRetries((jedis) -> jedis.hdel(DEPOSIT_STATUS_PREFIX + depositUUID, DepositField.lock.name()));
    }

    public DepositState getState(String depositUUID) {
        final AtomicReference<DepositState> result = new AtomicReference<>();
        connectWithRetries((jedis) -> {
            String state = jedis.hget(DEPOSIT_STATUS_PREFIX + depositUUID, DepositField.state.name());
            if (state == null) {
                log.debug("No state was found for deposit {}", depositUUID);
                return;
            }
            result.set(DepositState.valueOf(state));
        });
        return result.get();
    }

    public boolean isResumedDeposit(String depositUUID) {
        final AtomicBoolean result = new AtomicBoolean(false);
        connectWithRetries((jedis) -> {
            String value = jedis.hget(DEPOSIT_STATUS_PREFIX + depositUUID,
                    DepositField.ingestInprogress.name());
            result.set(Boolean.parseBoolean(value));
        });
        return result.get();
    }

    public void setIngestInprogress(String depositUUID, boolean value) {
        connectWithRetries((jedis) -> {
            jedis.hset(DEPOSIT_STATUS_PREFIX + depositUUID,
                    DepositField.ingestInprogress.name(), Boolean.toString(value));
        });
    }

    public void setState(String depositUUID, DepositState state) {
        connectWithRetries((jedis) -> {
            jedis.hset(DEPOSIT_STATUS_PREFIX + depositUUID, DepositField.state.name(),
                    state.name());
        });
    }

    public void incrIngestedObjects(String depositUUID, int amount) {
        connectWithRetries((jedis) -> {
            jedis.hincrBy(DEPOSIT_STATUS_PREFIX + depositUUID, DepositField.ingestedObjects.name(), amount);
        });
    }

    public void fail(String depositUUID, String message) {
        connectWithRetries((jedis) -> {
            jedis.hset(DEPOSIT_STATUS_PREFIX + depositUUID, DepositField.state.name(), DepositState.failed.name());
            if (message != null) {
                jedis.hset(DEPOSIT_STATUS_PREFIX + depositUUID, DepositField.errorMessage.name(), message);
            }
        });
    }

    public void fail(String depositUUID) {
        fail(depositUUID, null);
    }

    public void deleteField(String depositUUID, DepositField field) {
        connectWithRetries((jedis) -> {
            jedis.hdel(DEPOSIT_STATUS_PREFIX + depositUUID, field.name());
        });
    }

    /**
     * Expire the deposit status key after given interval.
     *
     * @param depositUUID
     * @param seconds
     *            time until expire
     */
    public void expireKeys(String depositUUID, int seconds) {
        connectWithRetries((jedis) -> {
            jedis.expire(DEPOSIT_STATUS_PREFIX + depositUUID, seconds);
            jedis.expire(DEPOSIT_MANIFEST_PREFIX + depositUUID, seconds);
        });
    }

    /**
     * Queue a deposit for processing. This will set the deposit state to queued
     * and add it to the deposit queue.
     *
     * @param depositUUID
     *            the UUID of the deposit to queue
     */
    public void queueDeposit(String depositUUID) {
        connectWithRetries((jedis) -> {
            if (jedis.zscore(RedisWorkerConstants.DEPOSIT_QUEUE, depositUUID) == null) {
                jedis.hset(DEPOSIT_STATUS_PREFIX + depositUUID, DepositField.state.name(),
                        DepositState.queued.name());
                jedis.zadd(RedisWorkerConstants.DEPOSIT_QUEUE, System.nanoTime(), depositUUID);
            } else {
                log.warn("Deposit {} already exists in the deposit queue", depositUUID);
            }
        });
    }

    /**
     * Pop the first deposit off the queue
     * @return id of the first deposit in the queue, or null if the queue is empty
     */
    public String getFirstQueuedDeposit() {
        final AtomicReference<String> result = new AtomicReference<>();
        connectWithRetries((jedis) -> {
            var items = jedis.zpopmin(RedisWorkerConstants.DEPOSIT_QUEUE);
            if (items != null) {
                result.set(items.getElement());
            }
        });
        return result.get();
    }
}
