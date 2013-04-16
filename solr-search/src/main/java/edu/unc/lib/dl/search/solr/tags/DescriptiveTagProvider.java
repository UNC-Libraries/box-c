package edu.unc.lib.dl.search.solr.tags;

import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadata;
import edu.unc.lib.dl.search.solr.model.Datastream;
import edu.unc.lib.dl.search.solr.model.Tag;

public class DescriptiveTagProvider implements TagProvider {

	@Override
	public void addTags(BriefObjectMetadata record, AccessGroupSet accessGroups) {
		Datastream descr = record.getDatastreamObject("MD_DESCRIPTIVE");
		if(descr != null) {
			record.addTag(new Tag("described", "This object has descriptive metadata."));
		}
	}

}
