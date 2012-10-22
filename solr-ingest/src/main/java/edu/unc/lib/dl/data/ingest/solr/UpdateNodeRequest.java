/**
 * Copyright 2008 The University of North Carolina at Chapel Hill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.unc.lib.dl.data.ingest.solr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.message.ActionMessage;

public class UpdateNodeRequest implements ActionMessage {
	private static final long serialVersionUID = 1L;

	protected String messageID;
	protected long timeCreated = System.currentTimeMillis();
	protected long timeFinished;

	protected DocumentIndexingPackage documentIndexingPackage;
	
	protected UpdateNodeRequest parent;
	protected List<UpdateNodeRequest> children;

	protected ProcessingStatus status;
	protected AtomicInteger childrenPending;
	protected AtomicInteger childrenProcessed;

	public UpdateNodeRequest(String messageID, UpdateNodeRequest parent) {
		this.messageID = messageID;
		this.setParent(parent);
		this.children = null;
		
		childrenPending = new AtomicInteger(0);
		childrenProcessed = new AtomicInteger(0);
	}

	public UpdateNodeRequest(String messageID, UpdateNodeRequest parent, ProcessingStatus status) {
		this(messageID, parent);
		this.status = status;
	}

	public void addChild(UpdateNodeRequest node) {
		if (node == null)
			return;
		if (children == null)
			children = new ArrayList<UpdateNodeRequest>();
		
		this.childAdded();
		children.add(node);
	}
	
	public void removeChild(UpdateNodeRequest node) {
		if (node == null || children == null)
			return;
		
		this.childRemoved();
		children.remove(node);
	}
	
	public void remove() {
		if (parent == null)
			return;
		parent.removeChild(this);
	}
	
	private void childRemoved() {
		childrenPending.decrementAndGet();
		if (parent != null)
			parent.childRemoved();
	}
	
	protected void childAdded() {
		childrenPending.incrementAndGet();
		if (parent != null)
			parent.childAdded();
	}
	
	protected void childCompleted() {
		int value = childrenProcessed.incrementAndGet();
		if (value == childrenPending.get() && ProcessingStatus.INPROGRESS.equals(status)) {
			status = ProcessingStatus.FINISHED;
		}
		if (parent != null)
			parent.childCompleted();
	}
	
	public void requestCompleted() {
		if (ProcessingStatus.FINISHED.equals(status))
			return;
		
		if (ProcessingStatus.FAILED.equals(status)) {
			timeFinished = System.currentTimeMillis();
		} else {
			if (children == null || childrenProcessed.get() == childrenPending.get()) {
				status = ProcessingStatus.FINISHED;
				timeFinished = System.currentTimeMillis();
			} else {
				status = ProcessingStatus.INPROGRESS;
			}
		}
		parent.childCompleted();
	}

	/**
	 * Determines if the ancestor path of this object contains a message matching the provided message ID.
	 * 
	 * @param messageID
	 * @return
	 */
	public boolean hasAncestor(String messageID) {
		if (messageID == null)
			return false;
		for (UpdateNodeRequest parent = this.parent; parent != null; parent = parent.getParent()) {
			if (messageID.equals(parent.getMessageID()))
				return true;
		}
		return false;
	}

	/**
	 * Returns a list containing all the ancestors of this request object.
	 * 
	 * @return
	 */
	public List<UpdateNodeRequest> getAncestors() {
		List<UpdateNodeRequest> ancestors = new ArrayList<UpdateNodeRequest>();
		for (UpdateNodeRequest parent = this.parent; parent != null; parent = parent.getParent()) {
			ancestors.add(parent);
		}
		return ancestors;
	}

	/**
	 * Returns the UpdateNodeRequest object from the tree starting at this object with message ID matching the input.
	 * 
	 * @param messageID
	 * @return
	 */
	public UpdateNodeRequest getChild(String messageID) {
		if (messageID == null || children == null)
			return null;

		// Breadth first
		for (UpdateNodeRequest child : children) {
			if (messageID.equals(child.getMessageID()))
				return child;
		}
		UpdateNodeRequest match = null;
		// No matches, so check children's children
		for (UpdateNodeRequest child : children) {
			match = child.getChild(messageID);
			if (match != null)
				return match;
		}
		return null;
	}

	public int countChildren() {
		return countChildren(-1);
	}
	
	public int countChildren(int depth) {
		if (children == null)
			return 0;
		int count = children.size();
		if (depth != 1) {
			for (UpdateNodeRequest child : children) {
				count += child.countChildren(depth - 1);
			}
		}
		return count;
	}
	
	public Map<ProcessingStatus, Integer> countChildrenByStatus() {
		return countChildrenByStatus(-1);
	}
	
	public Map<ProcessingStatus, Integer> countChildrenByStatus(int depth) {
		Map<ProcessingStatus, Integer> counts = new HashMap<ProcessingStatus, Integer>(ProcessingStatus.values().length);
		for (ProcessingStatus statusValue: ProcessingStatus.values()) {
			counts.put(statusValue, 0);
		}
		return countChildrenByStatus(depth, counts);
	}
	
	public Map<ProcessingStatus, Integer> countChildrenByStatus(int depth, Map<ProcessingStatus, Integer> counts) {
		if (children == null)
			return counts;
		
		for (UpdateNodeRequest child : children) {
			// Increment the status count
			counts.put(child.getStatus(), counts.get(child.getStatus()) + 1);
			if (depth != 1) {
				child.countChildrenByStatus(depth - 1, counts);
			}
		}
		
		return counts;
	}

	public int getChildrenPending() {
		return childrenPending.get();
	}

	public int getChildrenProcessed() {
		return childrenProcessed.get();
	}

	@Override
	public String getMessageID() {
		return messageID;
	}

	@Override
	public String getTargetID() {
		return messageID;
	}

	@Override
	public String getTargetLabel() {
		return messageID;
	}

	@Override
	public void setTargetLabel(String targetLabel) {
	}

	@Override
	public String getAction() {
		return null;
	}

	@Override
	public String getNamespace() {
		return null;
	}

	@Override
	public String getQualifiedAction() {
		return null;
	}

	@Override
	public long getTimeCreated() {
		return this.timeCreated;
	}

	public long getTimeFinished() {
		return timeFinished;
	}

	public ProcessingStatus getStatus() {
		return status;
	}

	public void setStatus(ProcessingStatus status) {
		this.status = status;
	}

	public UpdateNodeRequest getParent() {
		return parent;
	}

	public void setParent(UpdateNodeRequest parent) {
		if (this.parent == parent)
			return;
		// Remove from previous parent
		if (this.parent != null)
			this.parent.removeChild(this);
		this.parent = parent;
		// Add to new parent
		if (parent != null) {
			parent.addChild(this);
		}
	}

	public List<UpdateNodeRequest> getChildren() {
		return children;
	}

	public DocumentIndexingPackage getDocumentIndexingPackage() {
		return documentIndexingPackage;
	}

	public void setDocumentIndexingPackage(DocumentIndexingPackage documentIndexingPackage) {
		this.documentIndexingPackage = documentIndexingPackage;
	}
}
