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

import edu.unc.lib.dl.acl.util.AccessGroupSet;

public class AccessGroupSetTest extends Assert {

    @Test
    public void testGroups() {
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

    @Test
    public void accessGroupTests() {
        ArrayList<String> groups = new ArrayList<String>();
        groups.add("oddGroup");groups.add("testGroup1");
        groups.add("nonmatchingGroup");groups.add("edu:unc:lib:cdr:admin");

        AccessGroupSet groupSet = new AccessGroupSet();
        //groupSet.addAll(groups);

        String members = "testGroup1";
        groupSet = new AccessGroupSet(members);
        Assert.assertTrue(groupSet.size() == 1);

        Assert.assertTrue(groupSet.contains("testGroup1"));

        members = "testGroup1;testGroup2;testGroup3;oddGroup";
        groupSet = new AccessGroupSet(members);
        Assert.assertEquals(4, groupSet.size());
        Assert.assertTrue(groupSet.contains("testGroup1"));
        Assert.assertTrue(groupSet.contains("oddGroup"));

        Assert.assertTrue(groupSet.containsAny(groups));

        members = "testGroup1:testGroupExtended;testGroup2";
        groupSet = new AccessGroupSet(members);
        Assert.assertTrue(groupSet.size() == 2);

        members = ";testGroup1;";
        groupSet = new AccessGroupSet(members);
        Assert.assertTrue(groupSet.size() == 1);

        members = "";
        groupSet = new AccessGroupSet(members);
        Assert.assertTrue(groupSet.size() == 0);
        Assert.assertFalse(groupSet.containsAny(groups));
        Assert.assertTrue(members.equals(groupSet.joinAccessGroups(";", "", false)));

        members = "edu:unc:lib:cdr:admin;edu:unc:lib:cdr:sfc";
        groupSet = new AccessGroupSet(members);
        Assert.assertTrue(groupSet.size() == 2);
        Assert.assertTrue(groupSet.containsAny(groups));

        Assert.assertTrue(members.equals(groupSet.joinAccessGroups(";", "", false)) ||
                "edu:unc:lib:cdr:sfc;edu:unc:lib:cdr:admin".equals(groupSet.joinAccessGroups(";", "", false)));

        groupSet.remove("edu:unc:lib:cdr:admin");
        Assert.assertTrue(groupSet.size() == 1);

    }
}
