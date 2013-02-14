package edu.unc.lib.dl.data.ingest.solr;

import java.lang.ref.WeakReference;
import java.util.Iterator;

public class UpdateNodeWithManagedChildrenRequest extends UpdateNodeRequest {
	private static final long serialVersionUID = 1L;

	public UpdateNodeWithManagedChildrenRequest(String messageID, UpdateNodeRequest parent) {
		super(messageID, parent);
	}

	protected void cleanChildren() {
		Iterator<WeakReference<UpdateNodeRequest>> childIt = this.children.iterator();
		while (childIt.hasNext()) {
			WeakReference<UpdateNodeRequest> child = childIt.next();
			if (child == null || child.get() == null)
				childIt.remove();
		}
	}
	
	@Override
	public void addChild(UpdateNodeRequest node) {
		super.addChild(node);
		this.cleanChildren();
	}
}
