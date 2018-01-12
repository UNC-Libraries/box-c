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
import static org.junit.Assert.assertNotNull;

import java.util.NoSuchElementException;

import org.junit.Before;
import org.junit.Test;

import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.search.solr.model.HierarchicalBrowseRequest;
import edu.unc.lib.dl.search.solr.model.HierarchicalBrowseResultResponse;
import edu.unc.lib.dl.search.solr.model.HierarchicalBrowseResultResponse.ResultNode;
import edu.unc.lib.dl.search.solr.model.SearchState;

/**
 * @author bbpennel
 * @since Jan 15, 2015
 */
public class StructureQueryTest extends AbstractSolrQueryLayerTest {
    @Before
    public void init() throws Exception {
        server.add(populate());
        server.commit();
    }

    @Test
    public void getCollectionsTest() throws Exception {
        HierarchicalBrowseRequest browseRequest = new HierarchicalBrowseRequest(1);
        browseRequest.setAccessGroups(new AccessGroupSet("public"));
        browseRequest.setSearchState(new SearchState());
        browseRequest.setRootPid(rootPid.getId());
        HierarchicalBrowseResultResponse resp = queryLayer.getExpandedStructurePath(browseRequest);

        assertEquals("Incorrect number of direct children plus collections object", 3, resp.getResultList().size());

        assertEquals("Root object must be the Collections object", rootPid.getId(),
                resp.getRootNode().getMetadata().getId());
    }

    @Test
    public void getACollectionTest() throws Exception {

        HierarchicalBrowseRequest browseRequest = new HierarchicalBrowseRequest(1);
        browseRequest.setAccessGroups(new AccessGroupSet("public"));
        browseRequest.setSearchState(new SearchState());
        browseRequest.setRootPid(coll1Pid.getId());
        HierarchicalBrowseResultResponse resp = queryLayer.getExpandedStructurePath(browseRequest);

        assertEquals("Incorrect number of direct children plus collections object", 3, resp.getResultList().size());

        ResultNode rootNode = resp.getRootNode();
        assertEquals("Root object must be the Collections object", rootPid.getId(), rootNode.getMetadata().getId());

        ResultNode collectionNode = getChildByPid(resp.getRootNode(), coll1Pid);
        assertEquals("Incorrect collection id", coll1Pid.getId(), collectionNode.getMetadata().getId());
        assertEquals("Collection should only have one container child", 1, collectionNode.getChildren().size());

        ResultNode folderNode = collectionNode.getChildren().get(0);
        assertEquals("Nested folder should have no children", 0, folderNode.getChildren().size());
    }

    @Test
    public void getEmptyStructureTest() throws Exception {

        HierarchicalBrowseRequest browseRequest = new HierarchicalBrowseRequest(1);
        browseRequest.setAccessGroups(new AccessGroupSet("public"));
        browseRequest.setSearchState(new SearchState());
        browseRequest.setRootPid(coll2Pid.getId());
        HierarchicalBrowseResultResponse resp = queryLayer.getExpandedStructurePath(browseRequest);

        assertEquals("Incorrect number of direct children plus collections object", 3, resp.getResultList().size());

        ResultNode rootNode = resp.getRootNode();
        assertEquals("Root object must be the Collections object", rootPid.getId(), rootNode.getMetadata().getId());

        ResultNode collNode = getChildByPid(rootNode, coll2Pid);
        assertNotNull(collNode);
        assertEquals(0, collNode.getChildren().size());
    }

    private ResultNode getChildByPid(ResultNode node, PID pid) {
        try {
            return node.getChildren().stream()
                .filter(c -> c.getMetadata().getPid().equals(pid))
                .findFirst().get();
        } catch (NoSuchElementException e) {
            return null;
        }
    }
}
