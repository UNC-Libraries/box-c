package edu.unc.lib.boxc.auth.fcrepo.model;

import java.util.ArrayList;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;

public class AccessGroupSetImplTest extends Assertions {

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
        assertTrue(groupSet.size() == 1);

        assertTrue(groupSet.contains("testGroup1"));

        members = "testGroup1;testGroup2;testGroup3;oddGroup";
        groupSet = new AccessGroupSetImpl(members);
        assertEquals(4, groupSet.size());
        assertTrue(groupSet.contains("testGroup1"));
        assertTrue(groupSet.contains("oddGroup"));

        members = "testGroup1:testGroupExtended;testGroup2";
        groupSet = new AccessGroupSetImpl(members);
        assertTrue(groupSet.size() == 2);

        members = ";testGroup1;";
        groupSet = new AccessGroupSetImpl(members);
        assertTrue(groupSet.size() == 1);

        members = "";
        groupSet = new AccessGroupSetImpl(members);
        assertTrue(groupSet.size() == 0);
        assertTrue(members.equals(groupSet.joinAccessGroups(";", "", false)));

        members = "edu:unc:lib:cdr:admin;edu:unc:lib:cdr:sfc";
        groupSet = new AccessGroupSetImpl(members);
        assertTrue(groupSet.size() == 2);

        assertTrue(members.equals(groupSet.joinAccessGroups(";", "", false)) ||
                "edu:unc:lib:cdr:sfc;edu:unc:lib:cdr:admin".equals(groupSet.joinAccessGroups(";", "", false)));

        groupSet.remove("edu:unc:lib:cdr:admin");
        assertTrue(groupSet.size() == 1);

    }
}
