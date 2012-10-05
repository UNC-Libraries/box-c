package edu.unc.lib.dl.search.solr.util;

import java.util.List;

import edu.unc.lib.dl.util.ContentModelHelper;

public enum ResourceType {
	Collection(1), Aggregate(3), Folder(2), Item(3);
	
	private int displayOrder;
	
	ResourceType(int displayOrder) {
		this.displayOrder = displayOrder;
	}
	
	public int getDisplayOrder(){
		return this.displayOrder;
	}
	
	public static ResourceType getResourceTypeByContentModels(List<String> contentModels) {
		if (contentModels.contains(ContentModelHelper.Model.COLLECTION.getPID().getURI())) {
			return Collection;
		}
		if (contentModels.contains(ContentModelHelper.Model.AGGREGATE_WORK.getPID().getURI())) {
			return Aggregate;
		}
		if (contentModels.contains(ContentModelHelper.Model.CONTAINER.getPID().getURI())) {
			return Folder;
		}
		if (contentModels.contains(ContentModelHelper.Model.SIMPLE.getPID().getURI())) {
			return Item;
		}
		return null;
	}
}
