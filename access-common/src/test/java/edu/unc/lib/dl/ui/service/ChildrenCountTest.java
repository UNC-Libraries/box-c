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
import java.util.List;
import java.util.UUID;

import org.apache.solr.common.SolrInputDocument;
import org.junit.Before;
import org.junit.Test;

import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadata;
import edu.unc.lib.dl.search.solr.model.SearchRequest;
import edu.unc.lib.dl.search.solr.model.SearchResultResponse;
import edu.unc.lib.dl.search.solr.model.SearchState;

/**
 * @author bbpennel
 * @since Jan 23, 2015
 */
public class ChildrenCountTest extends AbstractSolrQueryLayerTest {

    private PID rootPid;
    private PID collPid;
    private PID folderPid;
    private PID workPid;

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
        assertEquals("Incorrect child count on collections object", 5,
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
        assertEquals("Incorrect child count on collections object", 3,
                getChildrenCount(collections, "containers"));
    }

    @Test
    public void testCountFromCollection() throws Exception {
        SearchResultResponse resp = getResult(collPid);

        queryLayer.getChildrenCounts(resp.getResultList(), new AccessGroupSet("public"));

        BriefObjectMetadata collections = getResultByPID(collPid.getId(), resp.getResultList());
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

    private List<SolrInputDocument> populate() {
        List<SolrInputDocument> docs = new ArrayList<>();

        SolrInputDocument newDoc = new SolrInputDocument();
        rootPid = makePid();
        String rootId = rootPid.getId();
        newDoc.addField("title", "Collections");
        addAccessFields(newDoc, Cdr.ContentRoot.getLocalName(), rootId);
        addAncestors(newDoc, true, rootId);
        docs.add(newDoc);

        newDoc = new SolrInputDocument();
        collPid = makePid();
        String collId = collPid.getId();
        String collName = "collection 1";
        newDoc.addField("title", collName);
        addAccessFields(newDoc, "Collection", collId);
        addAncestors(newDoc, true, rootId, collId);
        docs.add(newDoc);

        newDoc = new SolrInputDocument();
        folderPid = makePid();
        String folderId = folderPid.getId();
        newDoc.addField("title", "folder 1 coll 1");
        addAccessFields(newDoc, "Folder", folderId);
        addAncestors(newDoc, true, rootId, collId, folderId);
        docs.add(newDoc);

        newDoc = new SolrInputDocument();
        workPid = makePid();
        String workId = workPid.getId();
        newDoc.addField("title", "work 1 folder 1 coll 1");
        addAccessFields(newDoc, "Work", workId);
        addAncestors(newDoc, true, rootId, collId, folderId, workId);
        docs.add(newDoc);

        newDoc = new SolrInputDocument();
        PID file1Pid = makePid();
        String file1Id = file1Pid.getId();
        newDoc.addField("title", "file1 work 1 folder 1 coll 1");
        addAccessFields(newDoc, "File", file1Id, workId);
        addAncestors(newDoc, false, rootId, collId, folderId, workId, file1Id);
        docs.add(newDoc);

        newDoc = new SolrInputDocument();
        PID file2Pid = makePid();
        String file2Id = file2Pid.getId();
        newDoc.addField("title", "file2 work 1 folder 1 coll 1");
        addAccessFields(newDoc, "File", file2Id, workId);
        addAncestors(newDoc, false, rootId, collId, folderId, workId, file2Id);
        docs.add(newDoc);

        return docs;
    }

    private void addAncestors(SolrInputDocument doc, boolean isContainer, String... ids) {
        List<String> ancestorPath = new ArrayList<>();
        String ancestorIds = "";
        for (int i = 0; i < ids.length; i++) {
            if (i < ids.length - 1) {
                ancestorPath.add((i + 1) + "," + ids[i]);
            }
            if (i < ids.length - 1 || isContainer) {
                ancestorIds += "/" + ids[i];
            }
        }
        doc.addField("ancestorIds", ancestorIds);
        doc.addField("ancestorPath", ancestorPath);
    }

    private void addAccessFields(SolrInputDocument doc, String type, String id) {
        addAccessFields(doc, type, id, id);
    }

    private void addAccessFields(SolrInputDocument doc, String type, String id, String rollup) {
        doc.addField("id", id);
        doc.addField("rollup", rollup);
        doc.addField("resourceType", type);
        doc.addField("roleGroup", "public admin");
        doc.addField("readGroup", "public");
    }

    private PID makePid() {
        return PIDs.get(UUID.randomUUID().toString());
    }

    private int getChildrenCount(BriefObjectMetadata md, String countType) {
        return md.getCountMap().get(countType).intValue();
    }
}
