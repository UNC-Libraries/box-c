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

import java.util.Date;

import edu.unc.lib.dl.util.IndexingActionType;

public class DeleteChildrenPriorToTimestampRequest extends BlockUntilTargetCompleteRequest {
	private static final long serialVersionUID = 1L;
	protected Date timestampDate;

	public DeleteChildrenPriorToTimestampRequest(String pid, IndexingActionType action, String messageID,
			UpdateNodeRequest parent, UpdateNodeRequest target, long timestamp) {
		super(pid, action, messageID, parent, target);
		this.timestampDate = new Date(timestamp);
	}

	/*public DeleteChildrenPriorToTimestampRequest(String pid, SolrUpdateAction action, SolrUpdateRequest linkedRequest,
			String messageID, UpdateNodeRequest parent, UpdateNodeRequest target, long timestamp) {
		super(pid, action, linkedRequest, messageID, parent, target);
		this.timestampDate = new Date(timestamp);
	}*/

	public String getTimestampString() {
		return org.apache.solr.common.util.DateUtil.getThreadLocalDateFormat().format(timestampDate);
	}

	@Override
	public String toString() {
		return "DeleteChildrenPriorToTimestampRequest [timestampDate=" + timestampDate
				+ ", pid=" + pid + ", action=" + action + ", linkedRequest=" + linkedRequest + "]";
	}

}
