package edu.unc.lib.boxc.deposit.impl.model;

import static org.slf4j.LoggerFactory.getLogger;

import java.net.SocketTimeoutException;
import java.util.function.Consumer;

import org.slf4j.Logger;

import edu.unc.lib.boxc.model.api.exceptions.RepositoryException;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisConnectionException;

/**
 * Abstract client for interacting with redis data.
 *
 * @author bbpennel
 */
public class AbstractJedisFactory {
    private static final Logger log = getLogger(AbstractJedisFactory.class);

    protected JedisPool jedisPool;
    private int socketTimeoutRetries = 5;
    private long socketTimeoutDelay = 15000l;

    /**
     * Connect to redis and perform the provided block of code. If the connection
     * is interrupted, the block will be retried.
     *
     * @param block
     */
    protected void connectWithRetries(Consumer<Jedis> block) {
        int socketTimeoutRetriesRemaining = socketTimeoutRetries;
        while (true) {
            try (Jedis jedis = getJedisPool().getResource()) {
                block.accept(jedis);
                return;
            } catch (JedisConnectionException e) {
                if (!(e.getCause() instanceof SocketTimeoutException)) {
                    throw e;
                }
                if (socketTimeoutRetriesRemaining-- <= 0) {
                    throw e;
                } else {
                    log.warn("Jedis connection interrupted, retrying: {}", e.getMessage());
                    try {
                        Thread.sleep(socketTimeoutDelay);
                    } catch (InterruptedException e1) {
                        throw new RepositoryException(e1);
                    }
                }
            }
        }
    }

    public JedisPool getJedisPool() {
        return jedisPool;
    }

    public void setJedisPool(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    public void setSocketTimeoutRetries(int socketTimeoutRetries) {
        this.socketTimeoutRetries = socketTimeoutRetries;
    }

    public void setSocketTimeoutDelay(long socketTimeoutDelay) {
        this.socketTimeoutDelay = socketTimeoutDelay;
    }
}
