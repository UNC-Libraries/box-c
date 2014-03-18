package edu.unc.lib.deposit;

import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import redis.clients.jedis.Jedis;
import edu.unc.lib.deposit.work.JedisFactory;
import edu.unc.lib.deposit.work.SpringJobFactory;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/service-context.xml" })
public class AbstractResqueIT {
	private static final Logger log = LoggerFactory.getLogger(AbstractResqueIT.class);

	@Autowired
	SpringJobFactory springJobFactory = null;
	
	public SpringJobFactory getSpringJobFactory() {
		return springJobFactory;
	}

	@Autowired
	net.greghaines.jesque.Config config = null;

	public net.greghaines.jesque.Config getConfig() {
		return config;
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
		final Jedis jedis = JedisFactory.createJedis(getConfig());
		try {
			log.info("Resetting Redis for next test...");
			//Set<String> delete = new HashSet<String>();
			//delete.add(RedisWorkerConstants.DEPOSIT_SET);
			//delete.addAll(jedis.keys(RedisWorkerConstants.DEPOSIT_STATUS_PREFIX+"*"));
			//delete.addAll(jedis.keys(RedisWorkerConstants.DEPOSIT_TO_JOBS_PREFIX+"*"));
			//delete.addAll(jedis.keys(RedisWorkerConstants.JOB_STATUS_PREFIX+"*"));
			//jedis.del(delete.toArray(new String[] {}));
			jedis.flushDB();
		} finally {
			jedis.quit();
		}
	}

}
