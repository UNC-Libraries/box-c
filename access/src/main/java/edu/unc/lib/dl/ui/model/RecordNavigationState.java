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
package edu.unc.lib.dl.ui.model;

import java.util.List;

import edu.unc.lib.dl.ui.exception.NextRecordOutOfBoundsException;
import edu.unc.lib.dl.ui.exception.PreviousRecordOutOfBoundsException;
import edu.unc.lib.dl.search.solr.model.SearchState;

/**
 * Stores navigation state used for moving between items in the full record view.
 * @author bbpennel
 */
public class RecordNavigationState {
	private List<String> recordIdList;
	private String currentRecordId;
	private int currentRecordIndex;
	private long totalResults;
	private SearchState searchState;
	private String searchStateUrl;
	
	public RecordNavigationState(){
		currentRecordIndex = -1;
		totalResults = 0;
		recordIdList = null;
		searchState = null;
		searchStateUrl = null;
		currentRecordId = null;
	}

	public List<String> getRecordIdList() {
		return recordIdList;
	}

	public void setRecordIdList(List<String> recordIdList) {
		this.recordIdList = recordIdList;
	}

	public String getCurrentRecordId() {
		return currentRecordId;
	}

	public void setCurrentRecordId(String currentRecordId) {
		this.currentRecordId = currentRecordId;
	}
	
	public int getCurrentRecordIndex() {
		return currentRecordIndex;
	}

	public void setCurrentRecordIndex(int currentRecordIndex) {
		this.currentRecordIndex = currentRecordIndex;
	}

	public long getTotalResults() {
		return totalResults;
	}

	public void setTotalResults(long totalResults) {
		this.totalResults = totalResults;
	}

	public int indexOf(String id){
		if (this.recordIdList == null)
			return -1;
		return this.recordIdList.indexOf(id);
	}
	
	public String getNextRecordId() throws NextRecordOutOfBoundsException {
		if (currentRecordIndex < 0)
			return null;
		if (currentRecordIndex == this.recordIdList.size() - 1){
			if (currentRecordIndex + searchState.getStartRow() + 1 < this.totalResults){
				throw new NextRecordOutOfBoundsException();
			} else {
				//Hit the end of the list and there aren't any more results, so return null
				return null;
			}
		}
		return recordIdList.get(currentRecordIndex + 1);
	}
	
	public String getPreviousRecordId() throws PreviousRecordOutOfBoundsException {
		if (currentRecordIndex < 0)
			return null;
		if (currentRecordIndex == 0){
			if (searchState.getStartRow() > 0){
				throw new PreviousRecordOutOfBoundsException();
			} else {
				//Hit the end of the list and there aren't any more results, so return null
				return null;
			}
		}
		return recordIdList.get(currentRecordIndex - 1);
	}

	public SearchState getSearchState() {
		return searchState;
	}

	public void setSearchState(SearchState searchState) {
		this.searchState = searchState;
	}

	public String getSearchStateUrl() {
		return searchStateUrl;
	}

	public void setSearchStateUrl(String searchStateUrl) {
		this.searchStateUrl = searchStateUrl;
	}
	
	public void setCurrentRecord(int index){
		this.currentRecordId = this.recordIdList.get(index);
		this.currentRecordIndex = index;
	}
}
