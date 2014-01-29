package edu.unc.lib.dl.data.ingest.solr;

import java.util.ArrayList;
import java.util.List;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.IndexingActionType;

public class ChildSetRequest extends SolrUpdateRequest {
	private static final long serialVersionUID = 1L;
	private List<PID> children;
	
	public ChildSetRequest(String newParent, List<String> children, IndexingActionType action) {
		super(newParent, action);
		this.children = new ArrayList<PID>(children.size());
		for (String childString : children) {
			this.children.add(new PID(childString));
		}
	}

	public List<PID> getChildren() {
		return children;
	}

	public void setChlidren(List<PID> children) {
		this.children = children;
	}
}
