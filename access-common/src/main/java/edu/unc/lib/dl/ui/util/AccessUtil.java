package edu.unc.lib.dl.ui.util;

import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.Permission;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadata;
import edu.unc.lib.dl.search.solr.model.Datastream;

public class AccessUtil {

	public static boolean permitDatastreamAccess(AccessGroupSet groups, Datastream datastream,
			BriefObjectMetadata metadata) {
		if (groups == null || datastream == null || metadata == null)
			return false;

		if (!metadata.getDatastreamObjects().contains(datastream.getName()))
			return false;

		return metadata.getAccessControlBean().hasPermission(groups,
				Permission.getPermissionByDatastreamCategory(datastream.getDatastreamCategory()));
	}
}
