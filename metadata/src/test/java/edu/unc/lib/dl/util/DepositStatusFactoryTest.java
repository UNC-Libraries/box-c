package edu.unc.lib.dl.util;

import static org.junit.Assert.*;
import java.util.List;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;

import redis.clients.jedis.JedisPool;

public class DepositStatusFactoryTest {
	
	private DepositStatusFactory factory;
	private JedisPool jedisPool;
	private final String uuid = Integer.toString(new Random().nextInt(99999));
	private List<String> filenames;
	private String filename1 = "bagit.txt";
	private String filename2 = "manifest-md5.txt";
	
	@Before
	public void setup() {
		factory = new DepositStatusFactory();
		jedisPool = new JedisPool("localhost", 6379);
		factory.setJedisPool(jedisPool);
	}

	@Test
	public void testAddThenGetManifest() {
		factory.addManifest(uuid, filename1);
		factory.addManifest(uuid,  filename2);
		filenames = factory.getManifestURIs(uuid);
		assertTrue(filenames.size() == 2);
		assertEquals(filename1, filenames.get(0));
		assertEquals(filename2, filenames.get(1));
	}

}
