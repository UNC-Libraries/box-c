package edu.unc.lib.boxc.auth.fcrepo.model;

import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Test;

import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;

public class AccessGroupSetImplTest extends Assert {

    @Test
    public void testGroups() {
        String testGroup = "this:is:a:test:group";

        AccessGroupSetImpl groups = new AccessGroupSetImpl();
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

        groups = new AccessGroupSetImpl();
        groups.add(testGroupSlashes);
        assertFalse(groups.contains(testGroup));
        assertTrue(groups.contains(testGroupSlashes));

        groups = new AccessGroupSetImpl(new String[]{"group:1","group:2","group:3","group:1"});
        assertEquals(groups.size(), 3);
    }

    @Test
    public void accessGroupTests() {
        ArrayList<String> groups = new ArrayList<String>();
        groups.add("oddGroup");groups.add("testGroup1");
        groups.add("nonmatchingGroup");groups.add("edu:unc:lib:cdr:admin");

        AccessGroupSetImpl groupSet = new AccessGroupSetImpl();
        //groupSet.addAll(groups);

        String members = "testGroup1";
        groupSet = new AccessGroupSetImpl(members);
        Assert.assertTrue(groupSet.size() == 1);

        Assert.assertTrue(groupSet.contains("testGroup1"));

        members = "testGroup1;testGroup2;testGroup3;oddGroup";
        groupSet = new AccessGroupSetImpl(members);
        Assert.assertEquals(4, groupSet.size());
        Assert.assertTrue(groupSet.contains("testGroup1"));
        Assert.assertTrue(groupSet.contains("oddGroup"));

        members = "testGroup1:testGroupExtended;testGroup2";
        groupSet = new AccessGroupSetImpl(members);
        Assert.assertTrue(groupSet.size() == 2);

        members = ";testGroup1;";
        groupSet = new AccessGroupSetImpl(members);
        Assert.assertTrue(groupSet.size() == 1);

        members = "";
        groupSet = new AccessGroupSetImpl(members);
        Assert.assertTrue(groupSet.size() == 0);
        Assert.assertTrue(members.equals(groupSet.joinAccessGroups(";", "", false)));

        members = "edu:unc:lib:cdr:admin;edu:unc:lib:cdr:sfc";
        groupSet = new AccessGroupSetImpl(members);
        Assert.assertTrue(groupSet.size() == 2);

        Assert.assertTrue(members.equals(groupSet.joinAccessGroups(";", "", false)) ||
                "edu:unc:lib:cdr:sfc;edu:unc:lib:cdr:admin".equals(groupSet.joinAccessGroups(";", "", false)));

        groupSet.remove("edu:unc:lib:cdr:admin");
        Assert.assertTrue(groupSet.size() == 1);

    }
}
