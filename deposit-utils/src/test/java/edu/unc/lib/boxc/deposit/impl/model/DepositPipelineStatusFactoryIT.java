package edu.unc.lib.boxc.deposit.impl.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositPipelineAction;
import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositPipelineState;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPooled;

/**
 * @author bbpennel
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration({"/spring/jedis-context.xml"})
public class DepositPipelineStatusFactoryIT {
    private DepositPipelineStatusFactory factory;
    @Autowired
    private JedisPooled jedisPooled;

    @BeforeEach
    public void init() {
        factory = new DepositPipelineStatusFactory();
        factory.setJedisPooled(jedisPooled);
        jedisPooled.flushAll();
    }

    @AfterEach
    public void cleanup() {
        jedisPooled.flushAll();
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
