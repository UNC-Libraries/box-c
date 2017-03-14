/**
 * Copyright 2016 The University of North Carolina at Chapel Hill
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

import static org.junit.Assert.*;
import java.util.List;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import edu.unc.lib.dl.util.RedisWorkerConstants.DepositState;
import redis.clients.jedis.JedisPool;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"/spring-test/cdr-client-container.xml"})
public class DepositStatusFactoryIT {
	
	private DepositStatusFactory factory;
	@Autowired
	private JedisPool jedisPool;
	
	@Before
	public void init() {
		factory = new DepositStatusFactory();
		factory.setJedisPool(jedisPool);
	}

	@Test
	public void testAddThenGetManifest() {
		final String uuid = Integer.toString(new Random().nextInt(99999));
		final String filename1 = "bagit.txt";
		final String filename2 = "manifest-md5.txt";
		
		factory.addManifest(uuid, filename1);
		factory.addManifest(uuid,  filename2);
		List<String >filenames = factory.getManifestURIs(uuid);
		
		assertTrue(filenames.size() == 2);
		assertEquals(filename1, filenames.get(0));
		assertEquals(filename2, filenames.get(1));
	}
	
	@Test
	public void testAddRemoveSupervisorLock() {
		final String uuid = Integer.toString(new Random().nextInt(99999));
		String owner1 = "owner1";
		String owner2 = "owner2";
		assertTrue(factory.addSupervisorLock(uuid, owner1));
		assertFalse(factory.addSupervisorLock(uuid, owner2));
		factory.removeSupervisorLock(uuid);
		assertTrue(factory.addSupervisorLock(uuid, owner2));
	}
	
	@Test
	public void testSetStateGetState() {
		final String uuid = Integer.toString(new Random().nextInt(99999));
		factory.setState(uuid, DepositState.queued);
		assertEquals(DepositState.queued, factory.getState(uuid));
	}

}
