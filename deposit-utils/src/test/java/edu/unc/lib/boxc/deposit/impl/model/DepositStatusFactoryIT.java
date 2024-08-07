package edu.unc.lib.boxc.deposit.impl.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositAction;
import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositField;
import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositState;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 *
 * @author harring
 *
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration({"/spring/jedis-context.xml"})
public class DepositStatusFactoryIT {

    private DepositStatusFactory factory;
    @Autowired
    private JedisPool jedisPool;
    private Jedis jedisResource;

    @BeforeEach
    public void init() {
        factory = new DepositStatusFactory();
        factory.setJedisPool(jedisPool);
        jedisResource = jedisPool.getResource();
        jedisResource.flushAll();
    }

    @AfterEach
    public void cleanup() {
        jedisResource.flushAll();
        jedisResource.close();
    }

    @Test
    public void testAddRemoveSupervisorLock() {
        final String uuid = UUID.randomUUID().toString();
        String owner1 = "owner1";
        String owner2 = "owner2";
        assertTrue(factory.addSupervisorLock(uuid, owner1));
        assertFalse(factory.addSupervisorLock(uuid, owner2));
        factory.removeSupervisorLock(uuid);
        assertTrue(factory.addSupervisorLock(uuid, owner2));
    }

    @Test
    public void testSetStateGetState() {
        final String uuid = UUID.randomUUID().toString();
        factory.setState(uuid, DepositState.queued);
        assertEquals(DepositState.queued, factory.getState(uuid));
    }

    @Test
    public void testSetGetDeleteField() {
        final String uuid = UUID.randomUUID().toString();
        factory.set(uuid, DepositField.contactName, "Boxy");
        factory.set(uuid, DepositField.fileName, "boxys_file.txt");

        Map<String,String> status = factory.get(uuid);
        assertEquals(status.size(), 2);
        assertEquals("Boxy", status.get(DepositField.contactName.toString()));
        assertEquals("boxys_file.txt", status.get(DepositField.fileName.toString()));

        factory.deleteField(uuid, DepositField.fileName);
        status = factory.get(uuid);
        assertNull(status.get(DepositField.fileName.toString()));
        assertEquals(status.size(), 1);

        final String uuid2 = UUID.randomUUID().toString();
        factory.set(uuid2, DepositField.depositorName, "FriendOfBoxy");

        Set<Map<String,String>> statuses = factory.getAll();
        assertEquals(statuses.size(), 2);
    }

    @Test
    public void testRequestClearAction() {
        final String uuid = UUID.randomUUID().toString();
        factory.requestAction(uuid, DepositAction.pause);
        Map<String,String> status = factory.get(uuid);
        assertEquals(DepositAction.pause.toString(), status.get(DepositField.actionRequest.name()));
        factory.clearActionRequest(uuid);
        status = factory.get(uuid);
        assertNull(status.get(DepositField.actionRequest.name()));
    }

    @Test
    public void testExpireFail() throws InterruptedException {
        final String uuid = UUID.randomUUID().toString();
        factory.set(uuid, DepositField.contactName, "Boxy");
        final String uuid2 = UUID.randomUUID().toString();
        factory.set(uuid2, DepositField.depositorName, "FriendOfBoxy");

        //delete the uuid status by expiring its key
        factory.expireKeys(uuid, 1);
        Thread.sleep(1000);
        Set<Map<String,String>> statuses = factory.getAll();
        assertEquals(statuses.size(), 1);

        factory.fail(uuid2, "Boxy is sad");
        assertEquals("Boxy is sad", factory.get(uuid2).get(DepositField.errorMessage.name()));
    }

    @Test
    public void testInProgressIsResumed() {
        final String uuid = UUID.randomUUID().toString();
        factory.setIngestInprogress(uuid, true);
        assertTrue(factory.isResumedDeposit(uuid));
    }

}
