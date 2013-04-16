/**
 * Copyright 2008 The University of North Carolina at Chapel Hill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.unc.lib.dl.data.ingest.solr;

import edu.unc.lib.dl.util.IndexingActionType;

/**
 * Message which blocks until its parent message has finished
 * @author bbpennel
 *
 */
public class BlockUntilTargetCompleteRequest extends SolrUpdateRequest {
	private static final long serialVersionUID = 1L;
	private UpdateNodeRequest targetRequest;
	
	public BlockUntilTargetCompleteRequest(String pid, IndexingActionType action, String messageID,
			UpdateNodeRequest parent, UpdateNodeRequest target) {
		super(pid, action, messageID, parent);
		this.targetRequest = target;
	}

	@Override
	public boolean isBlocked() {
		return !this.targetRequest.getStatus().equals(ProcessingStatus.FINISHED)
				&& !this.targetRequest.getStatus().equals(ProcessingStatus.FAILED);
	}
}
