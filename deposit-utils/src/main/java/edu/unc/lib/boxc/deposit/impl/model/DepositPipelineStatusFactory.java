package edu.unc.lib.boxc.deposit.impl.model;

import static edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DEPOSIT_PIPELINE_STATE;

import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositPipelineState;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * Service for interactions with the state of the deposit pipeline
 *
 * @author bbpennel
 */
public class DepositPipelineStatusFactory {

    private JedisPool jedisPool;

    /**
     * Set the state of the deposit pipeline
     *
     * @param state new state for the pipeline
     */
    public void setPipelineState(DepositPipelineState state) {
        try (Jedis jedis = getJedisPool().getResource()) {
            if (state == null) {
                jedis.del(DEPOSIT_PIPELINE_STATE);
            } else {
                jedis.set(DEPOSIT_PIPELINE_STATE, state.name());
            }
        }
    }

    /**
     * @return state of the deposit pipeline
     */
    public DepositPipelineState getPipelineState() {
        try (Jedis jedis = getJedisPool().getResource()) {
            String state = jedis.get(DEPOSIT_PIPELINE_STATE);
            return DepositPipelineState.fromName(state);
        }
    }

    /**
     * Clear the pipeline state
     */
    public void clearPipelineState() {
        try (Jedis jedis = getJedisPool().getResource()) {
            jedis.del(DEPOSIT_PIPELINE_STATE);
        }
    }

    public void setJedisPool(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    private JedisPool getJedisPool() {
        return jedisPool;
    }
}
