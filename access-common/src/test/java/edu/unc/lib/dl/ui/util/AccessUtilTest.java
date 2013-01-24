package edu.unc.lib.dl.ui.util;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import edu.unc.lib.dl.acl.util.AccessGroupConstants;
import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.UserRole;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadata;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadataBean;
import edu.unc.lib.dl.util.ContentModelHelper.Datastream;

public class AccessUtilTest extends Assert {

	private BriefObjectMetadata createMetadataObject() {
		BriefObjectMetadataBean md = new BriefObjectMetadataBean();
		md.setId("uuid:test");
		md.setDatastream(Arrays.asList(Datastream.MD_TECHNICAL.getName(), Datastream.RELS_EXT.getName(),
				Datastream.DATA_FILE.getName(), Datastream.IMAGE_JP2000.getName()));
		md.setRoleGroup(Arrays.asList(UserRole.list.toString() + "|public", UserRole.patron.toString() + "|patron",
				UserRole.curator.toString() + "|curator"));
		return md;
	}

	@Test
	public void permitAdminTest() {
		BriefObjectMetadata md = this.createMetadataObject();

		AccessGroupSet groups = new AccessGroupSet("public;admin");
		AccessGroupConstants accessGroupConstants = new AccessGroupConstants();
		accessGroupConstants.setADMIN_GROUP("admin");

		assertTrue(AccessUtil.permitDatastreamAccess(groups, Datastream.DATA_FILE.name(), md));
		assertTrue(AccessUtil.permitDatastreamAccess(groups, Datastream.IMAGE_JP2000.name(), md));
		assertTrue(AccessUtil.permitDatastreamAccess(groups, Datastream.MD_TECHNICAL.name(), md));
		assertTrue(AccessUtil.permitDatastreamAccess(groups, Datastream.RELS_EXT.getName(), md));
	}

	@Test
	public void permitDerivativeTest() {
		BriefObjectMetadata md = this.createMetadataObject();

		AccessGroupSet groups = new AccessGroupSet("public");

		assertFalse(AccessUtil.permitDatastreamAccess(groups, Datastream.IMAGE_JP2000.name(), md));

		groups = new AccessGroupSet("public;patron");
		assertTrue(AccessUtil.permitDatastreamAccess(groups, Datastream.IMAGE_JP2000.name(), md));

		groups = new AccessGroupSet("public;curator");
		assertTrue(AccessUtil.permitDatastreamAccess(groups, Datastream.IMAGE_JP2000.name(), md));
	}

	@Test
	public void permitMDTechTest() {
		BriefObjectMetadata md = this.createMetadataObject();

		AccessGroupSet groups = new AccessGroupSet("public");

		assertFalse(AccessUtil.permitDatastreamAccess(groups, "MD_TECHNICAL", md));

		groups = new AccessGroupSet("public;patron");
		assertFalse(AccessUtil.permitDatastreamAccess(groups, "MD_TECHNICAL", md));

		groups = new AccessGroupSet("public;curator");
		assertTrue(AccessUtil.permitDatastreamAccess(groups, "MD_TECHNICAL", md));
	}
}
