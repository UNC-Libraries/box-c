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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.solr.common.SolrInputDocument;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.search.solr.model.HierarchicalBrowseRequest;
import edu.unc.lib.dl.search.solr.model.HierarchicalBrowseResultResponse;
import edu.unc.lib.dl.search.solr.model.HierarchicalBrowseResultResponse.ResultNode;
import edu.unc.lib.dl.search.solr.model.SearchState;

/**
 * @author bbpennel
 * @date Jan 15, 2015
 */
public class StructureQueryTest extends AbstractSolrQueryLayerTest {
	private static final Logger log = LoggerFactory.getLogger(StructureQueryTest.class);

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();

		server.add(populate());
		server.commit();
	}

	@Test
	public void getCollectionsTest() throws Exception {

		HierarchicalBrowseRequest browseRequest = new HierarchicalBrowseRequest(1);
		browseRequest.setAccessGroups(new AccessGroupSet("public"));
		browseRequest.setSearchState(new SearchState());
		browseRequest.setRootPid(COLLECTIONS_PID);
		HierarchicalBrowseResultResponse resp = queryLayer.getExpandedStructurePath(browseRequest);

		log.debug("" + resp.getResultList());
		assertEquals("Incorrect number of direct children plus collections object", 3, resp.getResultList().size());

		assertEquals("Root object must be the Collections object", COLLECTIONS_PID,
				resp.getRootNode().getMetadata().getId());
	}

	@Test
	public void getACollectionTest() throws Exception {

		HierarchicalBrowseRequest browseRequest = new HierarchicalBrowseRequest(1);
		browseRequest.setAccessGroups(new AccessGroupSet("public"));
		browseRequest.setSearchState(new SearchState());
		browseRequest.setRootPid("uuid:2");
		HierarchicalBrowseResultResponse resp = queryLayer.getExpandedStructurePath(browseRequest);

		assertEquals("Incorrect number of direct children plus collections object", 3, resp.getResultList().size());

		ResultNode rootNode = resp.getRootNode();
		assertEquals("Root object must be the Collections object", COLLECTIONS_PID, rootNode.getMetadata().getId());

		ResultNode collectionNode = resp.getRootNode().getChildren().get(0);
		assertEquals("Incorrect collection id", "uuid:2", collectionNode.getMetadata().getId());
		assertEquals("Collection should only have one container child", 1, collectionNode.getChildren().size());

		assertEquals("Nested folder should have no children", 0, collectionNode.getChildren().get(0).getChildren().size());
	}

	@Test
	public void getEmptyFolderStructureTest() throws Exception {

		HierarchicalBrowseRequest browseRequest = new HierarchicalBrowseRequest(1);
		browseRequest.setAccessGroups(new AccessGroupSet("public"));
		browseRequest.setSearchState(new SearchState());
		browseRequest.setRootPid("uuid:4");
		HierarchicalBrowseResultResponse resp = queryLayer.getExpandedStructurePath(browseRequest);

		assertEquals("Incorrect number of direct children plus collections object", 3, resp.getResultList().size());

		ResultNode rootNode = resp.getRootNode();
		assertEquals("Root object must be the Collections object", COLLECTIONS_PID, rootNode.getMetadata().getId());

		ResultNode collectionNode = resp.getRootNode().getChildren().get(0);
		assertEquals("Incorrect collection id", "uuid:2", collectionNode.getMetadata().getId());
		assertEquals("Collection should only have one container child", 1, collectionNode.getChildren().size());

		assertEquals("Nested folder should have no children", 0, collectionNode.getChildren().get(0).getChildren().size());
	}

	protected List<SolrInputDocument> populate() {
		List<SolrInputDocument> docs = new ArrayList<SolrInputDocument>();

		SolrInputDocument newDoc = new SolrInputDocument();
		newDoc.addField("title", "Collections");
		newDoc.addField("id", COLLECTIONS_PID);
		newDoc.addField("rollup", COLLECTIONS_PID);
		newDoc.addField("roleGroup", "public");
		newDoc.addField("readGroup", "public");
		newDoc.addField("ancestorIds", "");
		newDoc.addField("resourceType", "Folder");
		docs.add(newDoc);

		newDoc = new SolrInputDocument();
		newDoc.addField("title", "A collection");
		newDoc.addField("id", "uuid:2");
		newDoc.addField("rollup", "uuid:2");
		newDoc.addField("roleGroup", "public admin");
		newDoc.addField("readGroup", "public");
		newDoc.addField("ancestorIds", "/" + COLLECTIONS_PID + "/uuid:2");
		newDoc.addField("ancestorPath", Arrays.asList("1," + COLLECTIONS_PID));
		newDoc.addField("resourceType", "Collection");
		docs.add(newDoc);

		newDoc = new SolrInputDocument();
		newDoc.addField("title", "Subfolder 1");
		newDoc.addField("id", "uuid:4");
		newDoc.addField("rollup", "uuid:4");
		newDoc.addField("roleGroup", "public admin");
		newDoc.addField("readGroup", "public");
		newDoc.addField("ancestorIds", "/" + COLLECTIONS_PID + "/uuid:2/uuid:4");
		newDoc.addField("ancestorPath", Arrays.asList("1," + COLLECTIONS_PID, "2,uuid:2"));
		newDoc.addField("resourceType", "Folder");
		docs.add(newDoc);

		newDoc = new SolrInputDocument();
		newDoc.addField("title", "File");
		newDoc.addField("id", "uuid:6");
		newDoc.addField("rollup", "uuid:6");
		newDoc.addField("roleGroup", "public admin");
		newDoc.addField("readGroup", "public");
		newDoc.addField("ancestorIds", "/" + COLLECTIONS_PID + "/uuid:2");
		newDoc.addField("ancestorPath", Arrays.asList("1," + COLLECTIONS_PID, "2,uuid:2"));
		newDoc.addField("resourceType", "File");
		docs.add(newDoc);

		newDoc = new SolrInputDocument();
		newDoc.addField("title", "Second collection");
		newDoc.addField("id", "uuid:3");
		newDoc.addField("rollup", "uuid:3");
		newDoc.addField("roleGroup", "public admin");
		newDoc.addField("readGroup", "public");
		newDoc.addField("ancestorIds", "/" + COLLECTIONS_PID + "/uuid:3");
		newDoc.addField("ancestorPath", Arrays.asList("1," + COLLECTIONS_PID));
		newDoc.addField("resourceType", "Collection");
		docs.add(newDoc);

		return docs;
	}
}
