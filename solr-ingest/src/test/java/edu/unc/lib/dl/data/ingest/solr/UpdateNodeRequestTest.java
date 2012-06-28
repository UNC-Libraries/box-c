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
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

public class UpdateNodeRequestTest extends Assert {

	@Test
	public void branchingChildrenPendingTest() {
		UpdateNodeRequest root = new UpdateNodeRequest("root", null);
		
		int nestDepth = 6;
		UpdateNodeRequest parent = root;
		for (int i = 0; i < nestDepth; i++) {
			UpdateNodeRequest node = new UpdateNodeRequest("" + i, parent);
			parent = node;
		}
		
		assertEquals(nestDepth, root.getChildrenPending());
		
		UpdateNodeRequest branch = new UpdateNodeRequest("branch", parent);
		
		UpdateNodeRequest leaf = new UpdateNodeRequest("leaf", branch);
		
		UpdateNodeRequest sideLeaf = new UpdateNodeRequest("sideLeaf", parent);
		
		assertEquals(nestDepth + 3, root.getChildrenPending());
		assertEquals(0, root.getChildrenProcessed());
		
		assertEquals(3, parent.getChildrenPending());
		assertEquals(1, branch.getChildrenPending());
		assertEquals(0, leaf.getChildrenPending());
		assertEquals(0, sideLeaf.getChildrenPending());
	}
	
	@Test
	public void branchingChildrenProcessedTest() {
		UpdateNodeRequest root = new UpdateNodeRequest("root", null);
		
		int nestDepth = 6;
		UpdateNodeRequest parent = root;
		for (int i = 0; i < nestDepth; i++) {
			UpdateNodeRequest node = new UpdateNodeRequest("" + i, parent);
			parent = node;
		}
		
		UpdateNodeRequest branch = new UpdateNodeRequest("branch", parent);
		
		UpdateNodeRequest leaf = new UpdateNodeRequest("leaf", branch);
		
		UpdateNodeRequest sideLeaf = new UpdateNodeRequest("sideLeaf", parent);
		
		sideLeaf.requestCompleted();
		
		assertEquals(nestDepth + 1, sideLeaf.getAncestors().size());
		for (UpdateNodeRequest ancestor: sideLeaf.getAncestors()) {
			assertEquals(1, ancestor.getChildrenProcessed());
		}
		
		assertEquals(0, branch.getChildrenProcessed());
		assertEquals(0, leaf.getChildrenProcessed());
		
		leaf.requestCompleted();
		for (UpdateNodeRequest ancestor: parent.getAncestors()) {
			assertEquals(2, ancestor.getChildrenProcessed());
		}
		assertEquals(1, branch.getChildrenProcessed());
		assertEquals(0, leaf.getChildrenProcessed());
	}
	
	@Test
	public void parentStatusTest() {
		UpdateNodeRequest root = new UpdateNodeRequest("root", null, ProcessingStatus.FINISHED);
		
		UpdateNodeRequest n1 = new UpdateNodeRequest("1", root);
		UpdateNodeRequest n2 = new UpdateNodeRequest("2", n1);
		UpdateNodeRequest n3 = new UpdateNodeRequest("3", n1);
		UpdateNodeRequest n4 = new UpdateNodeRequest("4", n2);
		UpdateNodeRequest n5 = new UpdateNodeRequest("5", n2);
		
		assertNull(n1.getStatus());
		
		n1.requestCompleted();
		assertEquals(ProcessingStatus.INPROGRESS, n1.getStatus());
		assertEquals(4, n1.getChildrenPending());
		assertEquals(0, n1.getChildrenProcessed());
		
		n2.requestCompleted();
		assertEquals(ProcessingStatus.INPROGRESS, n1.getStatus());
		assertEquals(1, n1.getChildrenProcessed());
		assertEquals(ProcessingStatus.INPROGRESS, n2.getStatus());
		assertEquals(0, n2.getChildrenProcessed());
		
		n3.requestCompleted();
		assertEquals(ProcessingStatus.INPROGRESS, n1.getStatus());
		assertEquals(2, n1.getChildrenProcessed());
		assertEquals(ProcessingStatus.INPROGRESS, n2.getStatus());
		assertEquals(0, n2.getChildrenProcessed());
		assertEquals(ProcessingStatus.FINISHED, n3.getStatus());
		
		n4.requestCompleted();
		assertEquals(ProcessingStatus.INPROGRESS, n1.getStatus());
		assertEquals(3, n1.getChildrenProcessed());
		assertEquals(1, n2.getChildrenProcessed());
		assertEquals(0, n3.getChildrenProcessed());
		assertEquals(ProcessingStatus.INPROGRESS, n2.getStatus());
		assertEquals(ProcessingStatus.FINISHED, n4.getStatus());
		
		n5.requestCompleted();
		assertEquals(ProcessingStatus.FINISHED, root.getStatus());
		assertEquals(ProcessingStatus.FINISHED, n1.getStatus());
		assertEquals(ProcessingStatus.FINISHED, n2.getStatus());
		assertEquals(ProcessingStatus.FINISHED, n5.getStatus());
		assertEquals(4, n1.getChildrenProcessed());
	}
	
	@Test
	public void duplicateRequestCompleted() {
		UpdateNodeRequest root = new UpdateNodeRequest("root", null, ProcessingStatus.FINISHED);
		UpdateNodeRequest n1 = new UpdateNodeRequest("1", root);
		UpdateNodeRequest n2 = new UpdateNodeRequest("2", n1);
		UpdateNodeRequest n3 = new UpdateNodeRequest("3", n1);
		
		n1.requestCompleted();
		n2.requestCompleted();
		
		assertEquals(1, n1.getChildrenProcessed());
		assertEquals(ProcessingStatus.INPROGRESS, n1.getStatus());
		assertEquals(ProcessingStatus.FINISHED, n2.getStatus());
		
		n2.requestCompleted();
		assertEquals(1, n1.getChildrenProcessed());
		assertEquals(ProcessingStatus.INPROGRESS, n1.getStatus());
		assertEquals(ProcessingStatus.FINISHED, n2.getStatus());
		
		n2.requestCompleted();
		assertEquals(1, n1.getChildrenProcessed());
		
		n3.requestCompleted();
		assertEquals(2, n1.getChildrenProcessed());
		assertEquals(ProcessingStatus.FINISHED, n1.getStatus());
		assertEquals(ProcessingStatus.FINISHED, n2.getStatus());
		assertEquals(ProcessingStatus.FINISHED, n3.getStatus());
	}
	
	@Test
	public void countChildrenTest() {
		UpdateNodeRequest root = new UpdateNodeRequest("root", null);
		int nestDepth = 5;
		
		UpdateNodeRequest startParent = root;
		for (int j = 0; j < nestDepth; j++) {
			UpdateNodeRequest parent = startParent;
			for (int i = 0; i < nestDepth; i++) {
				UpdateNodeRequest node = new UpdateNodeRequest(j + "" + i, parent);
				parent = node;
			}
			// Start next path from first child
			startParent = startParent.getChildren().get(0);
		}
		
		assertEquals(nestDepth * nestDepth, root.countChildren());
		assertEquals(1, root.countChildren(1));
		assertEquals(3, root.countChildren(2));
		assertEquals(6, root.countChildren(3));
		assertEquals(10, root.countChildren(4));
		assertEquals(15, root.countChildren(5));
		assertEquals(19, root.countChildren(6));
		assertEquals(22, root.countChildren(7));
		assertEquals(24, root.countChildren(8));
		assertEquals(25, root.countChildren(9));
		assertEquals(25, root.countChildren(10));
		
		UpdateNodeRequest thirdChild = root.getChildren().get(0).getChildren().get(0).getChildren().get(0);
		assertEquals((nestDepth - 3) + 2 * nestDepth, thirdChild.countChildren());
		assertEquals(2, thirdChild.countChildren(1));
		assertEquals(5, thirdChild.countChildren(2));
	}
	
	@Test
	public void countChildrenByStatusTest() {
		Map<ProcessingStatus, Integer> randomCounts = new HashMap<ProcessingStatus, Integer>();
		for (ProcessingStatus status: ProcessingStatus.values()) {
			randomCounts.put(status, 0);
		}
		UpdateNodeRequest root = new UpdateNodeRequest("root", null);
		int depth = 5;
		int totalChildren = 0;
		Random random = new Random();
		
		List<UpdateNodeRequest> previousTier = new ArrayList<UpdateNodeRequest>();
		previousTier.add(root);
		
		for (int i = 0; i < depth; i++) {
			int numChildren = 3 + random.nextInt(5);
			List<UpdateNodeRequest> nextTier = new ArrayList<UpdateNodeRequest>(numChildren);
			for (int j = 0; j < numChildren; j++){
				int parentIndex = 0;
				if (previousTier.size() > 1)
					parentIndex = random.nextInt(previousTier.size() - 1);
				ProcessingStatus status = ProcessingStatus.values()[random.nextInt(ProcessingStatus.values().length)];
				UpdateNodeRequest newNode = new UpdateNodeRequest(i + "" + j, previousTier.get(parentIndex), status);
				randomCounts.put(status, randomCounts.get(status) + 1);
				nextTier.add(newNode);
				totalChildren++;
			}
			previousTier = nextTier;
		}
		
		assertEquals(totalChildren, root.countChildren());
		
		Map<ProcessingStatus, Integer> recursiveCounts = root.countChildrenByStatus();
		
		for (ProcessingStatus status: ProcessingStatus.values()) {
			assertEquals(randomCounts.get(status), recursiveCounts.get(status));
		}
	}
}
