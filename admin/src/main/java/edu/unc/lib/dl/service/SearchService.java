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
package edu.unc.lib.dl.service;

import java.util.List;

import edu.unc.lib.dl.schema.BasicQueryRequest;
import edu.unc.lib.dl.schema.BasicQueryResponseList;
import edu.unc.lib.dl.schema.GetChildrenRequest;
import edu.unc.lib.dl.schema.GetChildrenResponse;
import edu.unc.lib.dl.schema.OverviewDataRequest;
import edu.unc.lib.dl.schema.OverviewDataResponse;
import edu.unc.lib.dl.schema.PathInfoDao;

public interface SearchService {
	public GetChildrenResponse getChildren(GetChildrenRequest request);
	
	public List<PathInfoDao> GetPathInfoDaoChildren(GetChildrenRequest request);

	public List<PathInfoDao> getChildrenFromSolr(String pid, String accessGroupsString, String baseUrl);
	
	public BasicQueryResponseList getCollections();

	public BasicQueryResponseList basicQuery(BasicQueryRequest basicQueryRequest);

	public BasicQueryResponseList getImageViewUrl(String url);

	public BasicQueryResponseList getChildren(String url); // replaced by getChildren(GetChildrenRequest)
	
	public String getTitleByUri(String url);
	
	public void reindexEverything();

	public void addToSearch(List<String> pids);
	
	public OverviewDataResponse getOverviewData(OverviewDataRequest request);
}
