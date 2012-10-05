package edu.unc.lib.dl.data.ingest.solr.util;

import java.util.Collection;

import edu.unc.lib.dl.util.ContentModelHelper;

public class ValueMappingsUtil {

	public static String getResourceType(Collection<String> contentModels) {
		if (contentModels.contains(ContentModelHelper.Model.COLLECTION.getPID().getURI())) {
			return "Collection";
		}
		if (contentModels.contains(ContentModelHelper.Model.AGGREGATE_WORK.getPID().getURI())) {
			return "Aggregate";
		}
		if (contentModels.contains(ContentModelHelper.Model.CONTAINER.getPID().getURI())) {
			return "Folder";
		}
		if (contentModels.contains(ContentModelHelper.Model.SIMPLE.getPID().getURI())) {
			return "Item";
		}
		return null;
	}
}
