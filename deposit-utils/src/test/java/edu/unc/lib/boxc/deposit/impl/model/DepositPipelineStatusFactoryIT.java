package edu.unc.lib.boxc.deposit.impl.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositPipelineAction;
import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositPipelineState;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * @author bbpennel
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"/spring/jedis-context.xml"})
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
        factory.requestPipelineAction(DepositPipelineAction.stop);
        assertEquals(DepositPipelineAction.stop, factory.getPipelineAction());
    }
}
