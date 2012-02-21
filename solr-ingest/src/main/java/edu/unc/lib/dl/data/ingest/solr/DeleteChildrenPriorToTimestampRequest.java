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

public class DeleteChildrenPriorToTimestampRequest extends CountDownUpdateRequest {
	protected Date timestampDate;

	public DeleteChildrenPriorToTimestampRequest(String pid, SolrUpdateAction action, long timestamp) {
		super(pid, action);
		this.timestampDate = new Date(timestamp);
	}

	public DeleteChildrenPriorToTimestampRequest(String pid, SolrUpdateAction action, SolrUpdateRequest linkedRequest, long timestamp) {
		super(pid, action, linkedRequest);
		this.timestampDate = new Date(timestamp);
	}

	public String getTimestampString() {
		return org.apache.solr.common.util.DateUtil.getThreadLocalDateFormat().format(timestampDate);
	}

	@Override
	public String toString() {
		return "DeleteChildrenPriorToTimestampRequest [timestampDate=" + timestampDate + ", blockCount=" + blockCount + ", pid="
				+ pid + ", action=" + action + ", linkedRequest=" + linkedRequest + "]";
	}
	
}
