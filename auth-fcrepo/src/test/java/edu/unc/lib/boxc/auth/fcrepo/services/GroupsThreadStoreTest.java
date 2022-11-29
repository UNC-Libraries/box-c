package edu.unc.lib.boxc.auth.fcrepo.services;

import org.junit.Assert;
import org.junit.Test;

public class GroupsThreadStoreTest extends Assert {

    @Test
    public void nullGroupsTest() {
        GroupsThreadStore.storeGroups(null);
    }
}
