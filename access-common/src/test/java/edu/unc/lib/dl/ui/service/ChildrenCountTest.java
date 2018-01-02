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
package edu.unc.lib.dl.ui.service;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadata;
import edu.unc.lib.dl.search.solr.model.SearchRequest;
import edu.unc.lib.dl.search.solr.model.SearchResultResponse;
import edu.unc.lib.dl.search.solr.model.SearchState;

/**
 * @author bbpennel
 * @since Jan 23, 2015
 */
public class ChildrenCountTest extends AbstractSolrQueryLayerTest {

    @Before
    public void init() throws Exception {
        server.add(populate());
        server.commit();
    }

    @Test
    public void testCountChildren() throws Exception {
        SearchResultResponse resp = getResult(rootPid);

        queryLayer.getChildrenCounts(resp.getResultList(), new AccessGroupSet("public"));

        BriefObjectMetadata collections = getResultByPID(rootPid.getId(), resp.getResultList());
        assertEquals("Incorrect child count on collections object", 6,
                getChildrenCount(collections, "child"));

        BriefObjectMetadata folder = getResultByPID(folderPid.getId(), resp.getResultList());
        assertEquals("Incorrect child count on folder object", 3,
                getChildrenCount(folder, "child"));
    }

    @Test
    public void testCountContainers() throws Exception {
        SearchResultResponse resp = getResult(rootPid);

        queryLayer.getChildrenCounts(resp.getResultList(), new AccessGroupSet("public"), "containers",
                "-resourceType:File", null);

        BriefObjectMetadata collections = getResultByPID(rootPid.getId(), resp.getResultList());
        assertEquals("Incorrect child count on collections object", 4,
                getChildrenCount(collections, "containers"));
    }

    @Test
    public void testCountFromCollection() throws Exception {
        SearchResultResponse resp = getResult(coll1Pid);

        queryLayer.getChildrenCounts(resp.getResultList(), new AccessGroupSet("public"));

        BriefObjectMetadata collections = getResultByPID(coll1Pid.getId(), resp.getResultList());
        assertEquals("Incorrect child count on collections object", 4,
                getChildrenCount(collections, "child"));

        BriefObjectMetadata folder = getResultByPID(folderPid.getId(), resp.getResultList());
        assertEquals("Incorrect child count on folder object", 3,
                getChildrenCount(folder, "child"));
    }

    private BriefObjectMetadata getResultByPID(String pid, List<BriefObjectMetadata> results) {
        for (BriefObjectMetadata meta : results) {
            if (meta.getId().equals(pid)) {
                return meta;
            }
        }
        return null;
    }

    private SearchResultResponse getResult(PID pid) throws Exception {
        SearchRequest request = new SearchRequest();
        AccessGroupSet groups = new AccessGroupSet("public");
        request.setAccessGroups(groups);
        SearchState state = new SearchState();
        state.setRowsPerPage(100000);
        request.setSearchState(state);
        request.setRootPid(pid.getId());
        return queryLayer.getSearchResults(request);
    }



    private int getChildrenCount(BriefObjectMetadata md, String countType) {
        return md.getCountMap().get(countType).intValue();
    }
}
