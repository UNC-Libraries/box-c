package edu.unc.lib.dl.ui.util;

import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.Permission;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadata;
import edu.unc.lib.dl.util.ContentModelHelper.Datastream;

public class AccessUtil {

	public static boolean permitDatastreamAccess(AccessGroupSet groups, String datastream,
			BriefObjectMetadata metadata) {
		return AccessUtil.permitDatastreamAccess(groups, Datastream.getDatastream(datastream), metadata);
	}
	
	public static boolean permitDatastreamAccess(AccessGroupSet groups, Datastream datastream,
			BriefObjectMetadata metadata) {
		if (groups == null || datastream == null || metadata == null)
			return false;

		if (!metadata.getDatastreamObjects().contains(datastream.getName()))
			return false;

		return metadata.getAccessControlBean().hasPermission(groups,
				Permission.getPermissionByDatastreamCategory(datastream.getCategory()));
	}
}
