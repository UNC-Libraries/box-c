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

import static edu.unc.lib.dl.util.ContentModelHelper.Model.COLLECTION;
import static edu.unc.lib.dl.util.ContentModelHelper.Model.CONTAINER;
import static edu.unc.lib.dl.util.ContentModelHelper.Model.SIMPLE;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.solr.common.SolrInputDocument;
import org.junit.Test;

import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadata;
import edu.unc.lib.dl.search.solr.model.SearchRequest;
import edu.unc.lib.dl.search.solr.model.SearchResultResponse;
import edu.unc.lib.dl.search.solr.model.SearchState;
import edu.unc.lib.dl.search.solr.util.SolrSettings;

/**
 * @author bbpennel
 * @date Jan 23, 2015
 */
public class ChildrenCountTest extends AbstractSolrQueryLayerTest {

    @Test
    public void countChildren() throws Exception {
        int numCollections = 500;
        int numFolders = 4;

        SearchResultResponse resp = getResult(numCollections, numFolders, 0);

        queryLayer.getChildrenCounts(resp.getResultList(), new AccessGroupSet("public"));

        BriefObjectMetadata collections = getResultByPID(COLLECTIONS_PID, resp.getResultList());
        assertEquals("Incorrect child count on collections object", numCollections + numCollections * numFolders,
                collections.getCountMap().get("child").intValue());

        for (int i = 0; i < numCollections; i++) {
            BriefObjectMetadata coll = getResultByPID("uuid:" + i, resp.getResultList());
            assertEquals("Incorrect child count on a collection object", numFolders, coll.getCountMap().get("child")
                    .intValue());
        }
    }

    @Test
    public void countWithFiles() throws Exception {
        int numCollections = 100;
        int numFolders = 4;
        int numFiles = 5;

        SearchResultResponse resp = getResult(numCollections, numFolders, numFiles);

        queryLayer.getChildrenCounts(resp.getResultList(), new AccessGroupSet("public"));

        BriefObjectMetadata collections = getResultByPID(COLLECTIONS_PID, resp.getResultList());
        assertEquals("Incorrect child count on collections object", numCollections + numCollections * numFolders
                + numCollections * numFolders * numFiles,
                collections.getCountMap().get("child").intValue());

        for (int i = 0; i < numCollections; i++) {
            BriefObjectMetadata coll = getResultByPID("uuid:" + i, resp.getResultList());
            assertEquals("Incorrect child count on a collection object", numFolders + numFolders * numFiles,
                    coll.getCountMap().get("child").intValue());
        }
    }

    @Test
    public void countContainers() throws Exception {
        int numCollections = 100;
        int numFolders = 4;
        int numFiles = 5;

        SearchResultResponse resp = getResult(numCollections, numFolders, numFiles);

        queryLayer.getChildrenCounts(resp.getResultList(), new AccessGroupSet("public"), "containers", "contentModel:"
                + SolrSettings.sanitize(CONTAINER.toString()), null);

        BriefObjectMetadata collections = getResultByPID(COLLECTIONS_PID, resp.getResultList());
        assertEquals("Incorrect child count on collections object", numCollections + numCollections * numFolders,
                collections.getCountMap().get("containers").intValue());

        for (int i = 0; i < numCollections; i++) {
            BriefObjectMetadata coll = getResultByPID("uuid:" + i, resp.getResultList());
            assertEquals("Incorrect child count on a collection object", numFolders, coll
                    .getCountMap().get("containers").intValue());
        }
    }

    private BriefObjectMetadata getResultByPID(String pid, List<BriefObjectMetadata> results) {
        for (BriefObjectMetadata meta : results) {
            if (meta.getId().equals(pid)) {
                return meta;
            }
        }
        return null;
    }

    private SearchResultResponse getResult(int numCollections, int numFolders, int numFiles) throws Exception {
        server.add(populate(numCollections, numFolders, numFiles));
        server.commit();

        SearchRequest request = new SearchRequest();
        AccessGroupSet groups = new AccessGroupSet("public");
        request.setAccessGroups(groups);
        SearchState state = new SearchState();
        state.setRowsPerPage(100000);
        request.setSearchState(state);
        request.setRootPid(COLLECTIONS_PID);
        return queryLayer.getSearchResults(request);
    }

    private List<SolrInputDocument> populate(int numCollections, int numFolders, int numFiles) {
        List<SolrInputDocument> docs = new ArrayList<SolrInputDocument>();

        SolrInputDocument newDoc = new SolrInputDocument();
        newDoc.addField("title", "Collections");
        newDoc.addField("id", COLLECTIONS_PID);
        newDoc.addField("rollup", COLLECTIONS_PID);
        newDoc.addField("roleGroup", "public");
        newDoc.addField("readGroup", "public");
        newDoc.addField("ancestorIds", "");
        newDoc.addField("resourceType", "Folder");
        newDoc.addField("contentModel", Arrays.asList(CONTAINER.toString()));
        docs.add(newDoc);

        for (int i = 0; i < numCollections; i++) {
            newDoc = new SolrInputDocument();
            String collUUID = "uuid:" + i;
            String collName = "collection " + i;
            newDoc.addField("title", collName);
            newDoc.addField("id", collUUID);
            newDoc.addField("rollup", collUUID);
            newDoc.addField("roleGroup", "public admin");
            newDoc.addField("readGroup", "public");
            newDoc.addField("ancestorIds", "/" + COLLECTIONS_PID + "/" + collUUID);
            newDoc.addField("ancestorPath", Arrays.asList("1," + COLLECTIONS_PID));
            newDoc.addField("resourceType", "Collection");
            newDoc.addField("contentModel", Arrays.asList(CONTAINER.toString(), COLLECTION.toString()));
            docs.add(newDoc);

            for (int j = 0; j < numFolders; j++) {
                newDoc = new SolrInputDocument();
                String id = i + "-" + j;
                newDoc.addField("title", "folder " + id);
                newDoc.addField("id", "uuid:" + id);
                newDoc.addField("rollup", "uuid:" + id);
                newDoc.addField("roleGroup", "public admin");
                newDoc.addField("readGroup", "public");
                newDoc.addField("ancestorIds", "/" + COLLECTIONS_PID + "/" + collUUID + "/uuid:" + id);
                newDoc.addField("ancestorPath",
 Arrays.asList("1," + COLLECTIONS_PID, "2," + collUUID));
                newDoc.addField("resourceType", "Folder");
                newDoc.addField("contentModel", Arrays.asList(CONTAINER.toString()));
                docs.add(newDoc);

                for (int k = 0; k < numFiles; k++) {
                    newDoc = new SolrInputDocument();
                    String fid = i + "-" + j + "-" + k;
                    newDoc.addField("title", "file " + fid);
                    newDoc.addField("id", "uuid:" + fid);
                    newDoc.addField("rollup", "uuid:" + fid);
                    newDoc.addField("roleGroup", "public admin");
                    newDoc.addField("readGroup", "public");
                    newDoc.addField("ancestorIds", "/" + COLLECTIONS_PID + "/" + collUUID + "/uuid:" + id);
                    newDoc.addField("ancestorPath",
 Arrays.asList("1," + COLLECTIONS_PID, "2," + collUUID));
                    newDoc.addField("resourceType", "File");
                    newDoc.addField("contentModel", Arrays.asList(SIMPLE.toString()));
                    docs.add(newDoc);
                }
            }
        }

        return docs;
    }
}
