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
