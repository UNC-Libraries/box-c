package edu.unc.lib.dl.search.solr.model;

import java.util.List;

import edu.unc.lib.dl.acl.util.AccessGroupSet;

public class IdListRequest extends SimpleIdRequest {
	private List<String> ids;
	
	public IdListRequest(List<String> ids, List<String> resultFields, AccessGroupSet accessGroups) {
		super(resultFields, accessGroups);
		this.ids = ids;
	}

	public List<String> getIds() {
		return ids;
	}

	public void setIds(List<String> ids) {
		this.ids = ids;
	}
}
