package edu.unc.lib.bag;

import net.greghaines.jesque.Config;

import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import redis.clients.jedis.Jedis;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/service-context.xml" })
public class AbstractResqueIT {
	private static final Logger log = LoggerFactory.getLogger(AbstractResqueIT.class);

	@Autowired
	net.greghaines.jesque.Config config = null;

	public net.greghaines.jesque.Config getConfig() {
		return config;
	}

	public void setConfig(net.greghaines.jesque.Config config) {
		this.config = config;
	}

	public net.greghaines.jesque.client.Client getClient() {
		return client;
	}

	public void setClient(net.greghaines.jesque.client.Client client) {
		this.client = client;
	}

	@Autowired
	net.greghaines.jesque.client.Client client = null;

	/**
	 * Reset the Redis database using the supplied Config.
	 * 
	 * @param config
	 *            the location of the Redis server
	 */
	public void resetRedis() {
		final Jedis jedis = createJedis(getConfig());
		try {
			log.info("Resetting Redis for next test...");
			jedis.flushDB();
		} finally {
			jedis.quit();
		}
	}

	/**
	 * Create a connection to Redis from the given Config.
	 * 
	 * @param config
	 *            the location of the Redis server
	 * @return a new connection
	 */
	public static Jedis createJedis(final Config config) {
		if (config == null) {
			throw new IllegalArgumentException("config must not be null");
		}
		final Jedis jedis = new Jedis(config.getHost(), config.getPort(),
				config.getTimeout());
		if (config.getPassword() != null) {
			jedis.auth(config.getPassword());
		}
		jedis.select(config.getDatabase());
		return jedis;
	}

}
