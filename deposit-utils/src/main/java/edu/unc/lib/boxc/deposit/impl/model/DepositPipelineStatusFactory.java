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
package edu.unc.lib.boxc.deposit.impl.model;

import static edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DEPOSIT_PIPELINE_ACTION;
import static edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DEPOSIT_PIPELINE_STATE;

import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositPipelineAction;
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
     * @return action being requested against the deposit pipeline as a whole
     */
    public DepositPipelineAction getPipelineAction() {
        try (Jedis jedis = getJedisPool().getResource()) {
            String action = jedis.get(DEPOSIT_PIPELINE_ACTION);
            if (action == null) {
                return null;
            }
            return DepositPipelineAction.valueOf(action);
        }
    }

    /**
     * Request an action be taken on the deposit pipeline
     * @param action action to request
     */
    public void requestPipelineAction(DepositPipelineAction action) {
        try (Jedis jedis = getJedisPool().getResource()) {
            jedis.set(DEPOSIT_PIPELINE_ACTION, action.name());
        }
    }

    /**
     * Clear the requested pipeline action if there is one
     */
    public void clearPipelineActionRequest() {
        try (Jedis jedis = getJedisPool().getResource()) {
            jedis.del(DEPOSIT_PIPELINE_ACTION);
        }
    }

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
