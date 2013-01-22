package edu.unc.lib.dl.acl.util;

import org.junit.Assert;
import org.junit.Test;

public class GroupsThreadStoreTest extends Assert {

	@Test
	public void nullGroupsTest() {
		GroupsThreadStore.storeGroups(null);
	}
}
