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
package edu.unc.lib.dl.services.camel.solrUpdate;

import static edu.unc.lib.dl.acl.util.AccessPrincipalConstants.AUTHENTICATED_PRINC;
import static edu.unc.lib.dl.acl.util.AccessPrincipalConstants.PUBLIC_PRINC;
import static edu.unc.lib.dl.util.IndexingActionType.ADD_SET_TO_PARENT;
import static edu.unc.lib.dl.util.IndexingActionType.DELETE;
import static edu.unc.lib.dl.util.IndexingActionType.DELETE_SOLR_TREE;
import static edu.unc.lib.dl.util.IndexingActionType.RECURSIVE_ADD;
import static edu.unc.lib.dl.util.IndexingActionType.UPDATE_ACCESS;
import static edu.unc.lib.dl.util.IndexingActionType.UPDATE_DESCRIPTION;
import static edu.unc.lib.dl.util.IndexingMessageHelper.makeIndexingOperationBody;
import static java.util.Collections.singleton;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Collection;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.solr.common.SolrInputDocument;
import org.jdom2.Document;
import org.junit.Before;
import org.junit.Test;

import edu.unc.lib.dl.data.ingest.solr.action.IndexingAction;
import edu.unc.lib.dl.fcrepo4.AdminUnit;
import edu.unc.lib.dl.fcrepo4.CollectionObject;
import edu.unc.lib.dl.fcrepo4.RepositoryObject;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadata;
import edu.unc.lib.dl.search.solr.model.SimpleIdRequest;
import edu.unc.lib.dl.search.solr.util.FacetConstants;
import edu.unc.lib.dl.services.camel.solr.AbstractSolrProcessorIT;
import edu.unc.lib.dl.test.TestHelper;
import edu.unc.lib.dl.util.IndexingActionType;
import edu.unc.lib.dl.util.ResourceType;

/**
 *
 * @author bbpennel
 *
 */
public class SolrUpdateProcessorIT extends AbstractSolrProcessorIT {

    private SolrUpdateProcessor processor;

    @Resource(name = "solrIndexingActionMap")
    private Map<IndexingActionType, IndexingAction> solrIndexingActionMap;

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        TestHelper.setContentBase(baseAddress);

        processor = new SolrUpdateProcessor();
        processor.setSolrIndexingActionMap(solrIndexingActionMap);

        when(exchange.getIn()).thenReturn(message);

        generateBaseStructure();
    }

    @Test
    public void testReindexAcls() throws Exception {
        indexObjectsInTripleStore(rootObj, unitObj, collObj);

        indexDummyDocument(unitObj);
        indexDummyDocument(collObj);

        makeIndexingMessage(unitObj, null, UPDATE_ACCESS);

        processor.process(exchange);
        server.commit();

        BriefObjectMetadata unitMd = getSolrMetadata(unitObj);

        assertEquals("Title should not have updated", "dummy title", unitMd.getTitle());

        assertTrue("Read groups did not contain assigned group", unitMd.getReadGroup().contains(PUBLIC_PRINC));
        assertTrue("Admin groups did not contain assigned group", unitMd.getAdminGroup().contains("admin"));

        assertNotNull(unitMd.getDateAdded());

        BriefObjectMetadata collMd = getSolrMetadata(collObj);

        assertEquals("Title should not have updated", "dummy title", collMd.getTitle());

        assertTrue("Read groups did not contain assigned group", collMd.getReadGroup().contains(AUTHENTICATED_PRINC));
        assertTrue("Admin groups did not contain assigned group", collMd.getAdminGroup().contains("admin"));

        assertNotNull(collMd.getDateAdded());
        assertNotNull(collMd.getDateUpdated());
    }

    @Test
    public void testUpdateDescription() throws Exception {
        collObj.setDescription(getClass().getResourceAsStream("/datastreams/simpleMods.xml"));

        indexObjectsInTripleStore(rootObj, unitObj, collObj);

        indexDummyDocument(collObj);

        makeIndexingMessage(collObj, null, UPDATE_DESCRIPTION);

        processor.process(exchange);
        server.commit();

        BriefObjectMetadata collMd = getSolrMetadata(collObj);

        assertEquals("Object title", collMd.getTitle());
        assertEquals("Boxy", collMd.getCreator().get(0));

        assertTrue(collMd.getContentStatus().contains(FacetConstants.CONTENT_DESCRIBED));

        assertNotNull(collMd.getDateAdded());
        assertNotNull(collMd.getDateUpdated());
    }

    @Test
    public void testAddChildren() throws Exception {
        // Add extra collection
        CollectionObject coll2Obj = repositoryObjectFactory.createCollectionObject(null);
        unitObj.addMember(coll2Obj);

        indexObjectsInTripleStore(rootObj, unitObj, collObj, coll2Obj);

        indexDummyDocument(unitObj);
        indexDummyDocument(collObj);
        indexDummyDocument(coll2Obj);

        // Send message indicating the coll2 was added to the unit
        makeIndexingMessage(unitObj, singleton(coll2Obj.getPid()), ADD_SET_TO_PARENT);

        processor.process(exchange);
        server.commit();

        BriefObjectMetadata unitMd = getSolrMetadata(unitObj);
        assertNull("Unit record should not have been updated", unitMd.getDateUpdated());

        BriefObjectMetadata coll1Md = getSolrMetadata(collObj);
        assertNull("First collection should not have been updated", coll1Md.getDateUpdated());

        BriefObjectMetadata coll2Md = getSolrMetadata(coll2Obj);
        assertEquals(coll2Obj.getPid().getId(), coll2Md.getTitle());

        assertEquals(1, coll2Md.getReadGroup().size());
        assertTrue("Only admin group should have read assigned", coll2Md.getReadGroup().contains("admin"));
        assertTrue("Admin groups did not contain assigned group", coll2Md.getAdminGroup().contains("admin"));

        assertNotNull(coll2Md.getDateAdded());
        assertNotNull(coll2Md.getDateUpdated());

        assertEquals(2, coll2Md.getAncestorPath().size());
    }

    @Test
    public void testDelete() throws Exception {
        indexObjectsInTripleStore(rootObj, unitObj, collObj);

        indexDummyDocument(unitObj);
        indexDummyDocument(collObj);

        makeIndexingMessage(collObj, null, DELETE);

        processor.process(exchange);
        server.commit();

        assertNotNull("Unit must still be present", getSolrMetadata(unitObj));
        assertNull("Collection record must be removed", getSolrMetadata(collObj));
    }

    @Test
    public void testDeleteTree() throws Exception {
        indexObjectsInTripleStore(rootObj, unitObj, collObj);

        indexDummyDocument(rootObj);
        indexDummyDocument(unitObj);
        indexDummyDocument(collObj);

        makeIndexingMessage(rootObj, null, RECURSIVE_ADD);
        processor.process(exchange);
        server.commit();

        makeIndexingMessage(unitObj, null, DELETE_SOLR_TREE);
        processor.process(exchange);
        server.commit();

        assertNotNull("Root must still be present", getSolrMetadata(rootObj));
        assertNull("Unit record must be removed", getSolrMetadata(unitObj));
        assertNull("Collection record must be removed", getSolrMetadata(collObj));
    }

    private void makeIndexingMessage(RepositoryObject targetObj, Collection<PID> children, IndexingActionType action) {
        setMessageTarget(targetObj);
        Document messageBody = makeIndexingOperationBody("user", targetObj.getPid(), children, action);
        when(message.getBody()).thenReturn(messageBody);
    }

    private BriefObjectMetadata getSolrMetadata(RepositoryObject obj) {
        SimpleIdRequest idRequest = new SimpleIdRequest(obj.getPid().getId(), accessGroups);
        return solrSearchService.getObjectById(idRequest);
    }

    private void indexDummyDocument(RepositoryObject obj) throws Exception {
        SolrInputDocument doc = new SolrInputDocument();

        doc.setField("id", obj.getPid().getId());
        doc.setField("rollup", obj.getPid().getId());

        doc.setField("title", "dummy title");

        doc.addField("adminGroup", "dummyGroup");
        doc.addField("readGroup", "dummyGroup");
        doc.addField("roleGroup", "dummyGroup");

        String resourceType;
        if (obj instanceof AdminUnit) {
            resourceType = ResourceType.AdminUnit.name();
        } else if (obj instanceof CollectionObject) {
            resourceType = ResourceType.Collection.name();
        } else {
            resourceType = ResourceType.ContentRoot.name();
        }
        doc.setField("resourceType", resourceType);

        server.add(doc);
        server.commit();
    }
}
