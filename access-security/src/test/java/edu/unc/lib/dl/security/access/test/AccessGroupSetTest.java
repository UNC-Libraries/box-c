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
package edu.unc.lib.dl.security.access.test;

import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Test;

import edu.unc.lib.dl.security.access.AccessGroupSet;

public class AccessGroupSetTest extends Assert {

	@Test
	public void testGroups(){
		String testGroup = "this:is:a:test:group";
		
		AccessGroupSet groups = new AccessGroupSet();
		assertEquals(groups.size(), 0);
		groups.add(testGroup);
		
		assertTrue(groups.contains(testGroup));
		assertFalse(groups.contains("this:is:not:a:group"));
		
		groups.add("another:group");
		groups.add("third:group");
		groups.add(testGroup);
		assertEquals(groups.size(), 3);
		
		String testGroupSlashes = "this\\:is\\:a\\:test\\:group";
		groups.add(testGroupSlashes);
		assertEquals(groups.size(), 4);
		
		ArrayList<String> groupList = new ArrayList<String>();
		groupList.add("nonmatching:group:1");
		groupList.add("nonmatching:group:2");
		groupList.add("nonmatching:group:3");
		assertFalse(groups.containsAny(groupList));
		
		groupList.add(testGroup);
		assertTrue(groups.containsAny(groupList));
		
		groups = new AccessGroupSet();
		groups.add(testGroupSlashes);
		assertFalse(groups.contains(testGroup));
		assertTrue(groups.contains(testGroupSlashes));
		
		groups = new AccessGroupSet(new String[]{"group:1","group:2","group:3","group:1"});
		assertEquals(groups.size(), 3);
	}
}
