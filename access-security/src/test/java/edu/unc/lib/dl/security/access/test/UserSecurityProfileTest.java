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

import edu.unc.lib.dl.fedora.AccessControlCategory;
import edu.unc.lib.dl.security.access.AccessType;
import edu.unc.lib.dl.security.access.UserSecurityProfile;

public class UserSecurityProfileTest extends Assert {

	@Test
	public void accessGroupTests(){
		UserSecurityProfile user = new UserSecurityProfile();

		Assert.assertNull(user.getAccessGroups());

		ArrayList<String> groups = new ArrayList<String>();
		groups.add("oddGroup");groups.add("testGroup1");
		groups.add("nonmatchingGroup");groups.add("edu:unc:lib:cdr:admin");

		String members = null;
		user.setAccessGroups(members);
		Assert.assertTrue(user.getAccessGroups().size() == 0);
		Assert.assertTrue("".equals(user.getAccessGroups().joinAccessGroups(";", "", false)));

		Assert.assertTrue(user.getIsMemeberOf() == null);

		members = "testGroup1";
		user.setAccessGroups(members);
		Assert.assertTrue(user.getAccessGroups().size() == 1);

		Assert.assertTrue(user.getAccessGroups().contains("testGroup1"));

		members = "testGroup1;testGroup2;testGroup3;oddGroup";
		user.setAccessGroups(members);
		Assert.assertTrue(user.getAccessGroups().size() == 4);
		Assert.assertTrue(user.getAccessGroups().contains("testGroup1"));
		Assert.assertTrue(user.getAccessGroups().contains("oddGroup"));

		Assert.assertTrue(user.getAccessGroups().containsAny(groups));

		Assert.assertTrue(user.getIsMemeberOf().equals(members));

		members = "testGroup1:testGroupExtended;testGroup2";
		user.setAccessGroups(members);
		Assert.assertTrue(user.getAccessGroups().size() == 2);

		members = ";testGroup1;";
		user.setAccessGroups(members);
		Assert.assertTrue(user.getAccessGroups().size() == 1);

		members = "";
		user.setAccessGroups(members);
		Assert.assertTrue(user.getAccessGroups().size() == 0);
		Assert.assertFalse(user.getAccessGroups().containsAny(groups));
		Assert.assertTrue(members.equals(user.getAccessGroups().joinAccessGroups(";", "", false)));

		members = "edu:unc:lib:cdr:admin;edu:unc:lib:cdr:sfc";
		user.setAccessGroups(members);
		Assert.assertTrue(user.getAccessGroups().size() == 2);
		Assert.assertTrue(user.getAccessGroups().containsAny(groups));

		Assert.assertTrue(user.getIsMemeberOf().equals(members));

		Assert.assertTrue(members.equals(user.getAccessGroups().joinAccessGroups(";", "", false)) ||
				"edu:unc:lib:cdr:sfc;edu:unc:lib:cdr:admin".equals(user.getAccessGroups().joinAccessGroups(";", "", false)));

		user.getAccessGroups().remove("edu:unc:lib:cdr:admin");
		Assert.assertTrue(user.getAccessGroups().size() == 1);

	}

	@Test
	public void datastreamAccessCacheTests(){
		UserSecurityProfile user = new UserSecurityProfile();

		Assert.assertNotNull(user.getDatastreamAccessCache());
		Assert.assertEquals(user.getDatastreamAccessCache().size(), 0);

		String id = "id";
		user.getDatastreamAccessCache().put(id, AccessControlCategory.Original);
		Assert.assertEquals(user.getDatastreamAccessCache().size(), 1);
		Assert.assertTrue(user.getDatastreamAccessCache().contains(id, AccessControlCategory.Original));
		Assert.assertFalse(user.getDatastreamAccessCache().contains(id, AccessControlCategory.Administrative));

		id = "uuid:12344578";
		user.getDatastreamAccessCache().put(id, AccessControlCategory.Metadata);
		user.getDatastreamAccessCache().put(id, AccessControlCategory.Derivative);
		user.getDatastreamAccessCache().put(id, AccessControlCategory.Original);

		Assert.assertEquals(user.getDatastreamAccessCache().size(), 2);
		Assert.assertEquals(user.getDatastreamAccessCache().get(id).size(), 3);
		Assert.assertTrue(user.getDatastreamAccessCache().contains(id, AccessControlCategory.Original));
		Assert.assertFalse(user.getDatastreamAccessCache().contains(id, AccessControlCategory.Administrative));

		user.getDatastreamAccessCache().get(id).remove(AccessControlCategory.Derivative);
		Assert.assertEquals(user.getDatastreamAccessCache().get(id).size(), 2);

		id = null;
		user.getDatastreamAccessCache().put(id, AccessControlCategory.Metadata);
		Assert.assertEquals(user.getDatastreamAccessCache().size(), 3);
		user.getDatastreamAccessCache().remove(id);
		Assert.assertEquals(user.getDatastreamAccessCache().size(), 2);

		id = "id";
		user.getDatastreamAccessCache().get(id).remove(AccessControlCategory.Original);
		Assert.assertEquals(user.getDatastreamAccessCache().get(id).size(), 0);
		Assert.assertFalse(user.getDatastreamAccessCache().contains(id, AccessControlCategory.Original));
	}
}
