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
package edu.unc.lib.dl.data.ingest.solr.action;

import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.junit.Before;
import org.junit.Test;

import edu.unc.lib.dl.data.ingest.solr.SolrUpdateRequest;

public class ClearIndexActionTest extends BaseEmbeddedSolrTest {

    @Before
    public void setup() throws Exception {

        server.add(populate());
        server.commit();

    }

    @Test
    public void testDeleteAll() throws Exception {

        SolrDocumentList docListBefore = getDocumentList();
        assertEquals("Index should contain contents before delete", 4, docListBefore.size());

        ClearIndexAction action = new ClearIndexAction();
        action.setSolrUpdateDriver(driver);

        SolrUpdateRequest request = mock(SolrUpdateRequest.class);
        action.performAction(request);

        SolrDocumentList docListAfter = getDocumentList();

        assertEquals("Index must be empty after delete all", 0, docListAfter.size());
    }

    protected List<SolrInputDocument> populate() {
        List<SolrInputDocument> docs = new ArrayList<SolrInputDocument>();

        SolrInputDocument newDoc = new SolrInputDocument();
        newDoc.addField("title", "Collections");
        newDoc.addField("id", "uuid:1");
        newDoc.addField("rollup", "uuid:1");
        newDoc.addField("roleGroup", "public admin");
        newDoc.addField("readGroup", "public");
        newDoc.addField("adminGroup", "admin");
        newDoc.addField("ancestorIds", "/uuid:1");
        newDoc.addField("resourceType", "Folder");
        docs.add(newDoc);

        newDoc = new SolrInputDocument();
        newDoc.addField("title", "A collection");
        newDoc.addField("id", "uuid:2");
        newDoc.addField("rollup", "uuid:2");
        newDoc.addField("roleGroup", "public admin");
        newDoc.addField("readGroup", "public");
        newDoc.addField("adminGroup", "admin");
        newDoc.addField("ancestorIds", "/uuid:1/uuid:2");
        newDoc.addField("ancestorPath", Arrays.asList("1,uuid:1"));
        newDoc.addField("resourceType", "Collection");
        docs.add(newDoc);

        newDoc = new SolrInputDocument();
        newDoc.addField("title", "File");
        newDoc.addField("id", "uuid:6");
        newDoc.addField("rollup", "uuid:6");
        newDoc.addField("roleGroup", "public admin");
        newDoc.addField("readGroup", "public");
        newDoc.addField("adminGroup", "admin");
        newDoc.addField("ancestorIds", "/uuid:1/uuid:2");
        newDoc.addField("ancestorPath", Arrays.asList("1,uuid:1", "2,uuid:2"));
        newDoc.addField("resourceType", "File");
        docs.add(newDoc);

        newDoc = new SolrInputDocument();
        newDoc.addField("title", "Second collection");
        newDoc.addField("id", "uuid:3");
        newDoc.addField("rollup", "uuid:3");
        newDoc.addField("roleGroup", "public admin");
        newDoc.addField("readGroup", "public");
        newDoc.addField("adminGroup", "admin");
        newDoc.addField("ancestorIds", "/uuid:1/uuid:3");
        newDoc.addField("ancestorPath", Arrays.asList("1,uuid:1"));
        newDoc.addField("resourceType", "Collection");
        docs.add(newDoc);

        return docs;
    }
}
