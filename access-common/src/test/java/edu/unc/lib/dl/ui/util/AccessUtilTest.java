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
package edu.unc.lib.dl.ui.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import edu.unc.lib.dl.search.solr.model.BriefObjectMetadataBean;

public class AccessUtilTest {

    private BriefObjectMetadataBean mdObject;

    private List<String> roleGroups;

    @Before
    public void init() {
        roleGroups = new ArrayList<>();

        mdObject = new BriefObjectMetadataBean();
        mdObject.setId("uuid:test");
        mdObject.setRoleGroup(roleGroups);
    }

    @Test
    public void testHasPatronRoleForPublicGroup() {
        roleGroups.add("canViewOriginals|everyone");

        assertTrue("Failed to determine object has full patron access",
                AccessUtil.hasPatronRoleForPublicGroup(mdObject));
    }

    @Test
    public void testDoesNotHaveFullPatronRoleForPublicGroup() {
        roleGroups.add("canViewMetadata|everyone");

        assertFalse("Object must not have full patron access",
                AccessUtil.hasPatronRoleForPublicGroup(mdObject));
    }

    @Test
    public void testHasPatronRoleForPublicGroupNoRoleGroups() {
        mdObject.setRoleGroup(null);

        assertFalse("Object must not have full patron access",
                AccessUtil.hasPatronRoleForPublicGroup(mdObject));
    }
//
//	private BriefObjectMetadata createMetadataObject() {
//		BriefObjectMetadataBean md = new BriefObjectMetadataBean();
//		md.setId("uuid:test");
//		md.setDatastream(Arrays.asList(Datastream.MD_TECHNICAL.getName(), Datastream.RELS_EXT.getName(),
//				Datastream.DATA_FILE.getName(), Datastream.IMAGE_JP2000.getName()));
//		md.setRoleGroup(Arrays.asList(UserRole.list.toString() + "|public", UserRole.patron.toString() + "|patron",
//				UserRole.curator.toString() + "|curator"));
//		return md;
//	}
//
//	@Test
//	public void permitAdminTest() {
//		BriefObjectMetadata md = this.createMetadataObject();
//
//		AccessGroupSet groups = new AccessGroupSet("public;admin");
//		AccessGroupConstants accessGroupConstants = new AccessGroupConstants();
//		accessGroupConstants.setADMIN_GROUP("admin");
//
//		assertTrue(AccessUtil.permitDatastreamAccess(groups, Datastream.DATA_FILE.name(), md));
//		assertTrue(AccessUtil.permitDatastreamAccess(groups, Datastream.IMAGE_JP2000.name(), md));
//		assertTrue(AccessUtil.permitDatastreamAccess(groups, Datastream.MD_TECHNICAL.name(), md));
//		assertTrue(AccessUtil.permitDatastreamAccess(groups, Datastream.RELS_EXT.getName(), md));
//	}
//
//	@Test
//	public void permitDerivativeTest() {
//		BriefObjectMetadata md = this.createMetadataObject();
//
//		AccessGroupSet groups = new AccessGroupSet("public");
//
//		assertFalse(AccessUtil.permitDatastreamAccess(groups, Datastream.IMAGE_JP2000.name(), md));
//
//		groups = new AccessGroupSet("public;patron");
//		assertTrue(AccessUtil.permitDatastreamAccess(groups, Datastream.IMAGE_JP2000.name(), md));
//
//		groups = new AccessGroupSet("public;curator");
//		assertTrue(AccessUtil.permitDatastreamAccess(groups, Datastream.IMAGE_JP2000.name(), md));
//	}
//
//	@Test
//	public void permitMDTechTest() {
//		BriefObjectMetadata md = this.createMetadataObject();
//
//		AccessGroupSet groups = new AccessGroupSet("public");
//
//		assertFalse(AccessUtil.permitDatastreamAccess(groups, "MD_TECHNICAL", md));
//
//		groups = new AccessGroupSet("public;patron");
//		assertFalse(AccessUtil.permitDatastreamAccess(groups, "MD_TECHNICAL", md));
//
//		groups = new AccessGroupSet("public;curator");
//		assertTrue(AccessUtil.permitDatastreamAccess(groups, "MD_TECHNICAL", md));
//	}
}
