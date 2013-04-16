package edu.unc.lib.dl.search.solr.tags;

import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadata;

public interface TagProvider {
	public void addTags(BriefObjectMetadata record, AccessGroupSet accessGroups); 
}
