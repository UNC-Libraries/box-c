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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import edu.unc.lib.dl.util.RedisWorkerConstants.DepositPipelineAction;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositPipelineState;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * @author bbpennel
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"/spring-test/cdr-client-container.xml"})
public class DepositPipelineStatusFactoryIT {

    private DepositPipelineStatusFactory factory;
    @Autowired
    private JedisPool jedisPool;
    private Jedis jedisResource;

    @Before
    public void init() {
        factory = new DepositPipelineStatusFactory();
        factory.setJedisPool(jedisPool);
        jedisResource = jedisPool.getResource();
        jedisResource.flushAll();
    }

    @After
    public void cleanup() {
        jedisResource.close();
    }

    @Test
    public void clearSetAndGetState() {
        factory.clearPipelineState();
        assertNull(factory.getPipelineState());
        factory.setPipelineState(DepositPipelineState.active);
        assertEquals(DepositPipelineState.active, factory.getPipelineState());
        factory.setPipelineState(DepositPipelineState.quieted);
        assertEquals(DepositPipelineState.quieted, factory.getPipelineState());
    }

    @Test
    public void requestGetAndClearAction() {
        factory.clearPipelineActionRequest();
        assertNull(factory.getPipelineAction());
        factory.requestPipelineAction(DepositPipelineAction.quietNow);
        assertEquals(DepositPipelineAction.quietNow, factory.getPipelineAction());
    }
}
