package edu.unc.lib.boxc.auth.fcrepo.services;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class GroupsThreadStoreTest extends Assertions {

    @Test
    public void nullGroupsTest() {
        GroupsThreadStore.storeGroups(null);
    }
}
