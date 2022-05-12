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
package edu.unc.lib.boxc.services.camel.solrUpdate;

import static edu.unc.lib.boxc.auth.api.AccessPrincipalConstants.AUTHENTICATED_PRINC;
import static edu.unc.lib.boxc.auth.api.AccessPrincipalConstants.PUBLIC_PRINC;
import static edu.unc.lib.boxc.operations.jms.indexing.IndexingActionType.ADD_SET_TO_PARENT;
import static edu.unc.lib.boxc.operations.jms.indexing.IndexingActionType.DELETE;
import static edu.unc.lib.boxc.operations.jms.indexing.IndexingActionType.DELETE_SOLR_TREE;
import static edu.unc.lib.boxc.operations.jms.indexing.IndexingActionType.RECURSIVE_ADD;
import static edu.unc.lib.boxc.operations.jms.indexing.IndexingActionType.UPDATE_ACCESS_TREE;
import static edu.unc.lib.boxc.operations.jms.indexing.IndexingActionType.UPDATE_DESCRIPTION;
import static edu.unc.lib.boxc.operations.jms.indexing.IndexingMessageHelper.makeIndexingOperationBody;
import static java.util.Collections.singleton;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.fcrepo.services.RepositoryObjectLoaderImpl;
import edu.unc.lib.boxc.operations.jms.MessageSender;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingMessageSender;
import edu.unc.lib.boxc.search.solr.facets.FilterableDisplayValueFacet;
import edu.unc.lib.boxc.search.solr.services.TitleRetrievalService;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.NotifyBuilder;
import org.apache.commons.io.FileUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrInputDocument;
import org.jdom2.Document;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;

import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.indexing.solr.action.IndexingAction;
import edu.unc.lib.boxc.indexing.solr.filter.SetCollectionSupplementalInformationFilter;
import edu.unc.lib.boxc.indexing.solr.filter.collection.RLASupplementalFilter;
import edu.unc.lib.boxc.model.api.ResourceType;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.AdminUnit;
import edu.unc.lib.boxc.model.api.objects.CollectionObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObject;
import edu.unc.lib.boxc.model.fcrepo.test.TestHelper;
import edu.unc.lib.boxc.operations.impl.edit.UpdateDescriptionService;
import edu.unc.lib.boxc.operations.impl.edit.UpdateDescriptionService.UpdateDescriptionRequest;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingActionType;
import edu.unc.lib.boxc.search.api.FacetConstants;
import edu.unc.lib.boxc.search.api.models.ContentObjectRecord;
import edu.unc.lib.boxc.search.solr.models.ContentObjectSolrRecord;
import edu.unc.lib.boxc.services.camel.solr.AbstractSolrProcessorIT;
import edu.unc.lib.boxc.services.camel.solrUpdate.SolrUpdateProcessor;

/**
 *
 * @author bbpennel
 *
 */
@ContextHierarchy({
    @ContextConfiguration("/spring-test/jms-context.xml"),
    @ContextConfiguration("/solr-update-processor-it-context.xml")
})
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public class SolrUpdateProcessorIT extends AbstractSolrProcessorIT {

    @Rule
    public final TemporaryFolder tmpFolder = new TemporaryFolder();

    private SolrUpdateProcessor processor;

    @Autowired
    private CamelContext cdrServiceSolrUpdate;
    @Autowired
    private UpdateDescriptionService updateDescriptionService;
    @Mock
    private AgentPrincipals agent;
    @Autowired
    private SetCollectionSupplementalInformationFilter setCollectionSupplementalInformationFilter;
    @Autowired
    private SolrClient solrClient;
    @Autowired
    private RepositoryObjectLoader repositoryObjectLoader;
    @Autowired
    private MessageSender updateWorkSender;
    @Autowired
    private IndexingMessageSender indexingMessageSender;
    @Autowired
    private TitleRetrievalService titleRetrievalService;

    @Resource(name = "solrIndexingActionMap")
    private Map<IndexingActionType, IndexingAction> solrIndexingActionMap;

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        TestHelper.setContentBase(baseAddress);

        processor = new SolrUpdateProcessor();
        processor.setSolrIndexingActionMap(solrIndexingActionMap);
        processor.setRepositoryObjectLoader(repositoryObjectLoader);
        processor.setUpdateWorkSender(updateWorkSender);
        processor.setTitleRetrievalService(titleRetrievalService);
        processor.setIndexingMessageSender(indexingMessageSender);
        processor.setSolrClient(solrClient);

        when(exchange.getIn()).thenReturn(message);

        generateBaseStructure();
    }

    @Test
    public void testReindexAcls() throws Exception {
        indexObjectsInTripleStore();

        repositoryObjectSolrIndexer.index(unitObj.getPid(), collObj.getPid());

        makeIndexingMessage(unitObj, null, UPDATE_ACCESS_TREE);

        NotifyBuilder notify = new NotifyBuilder(cdrServiceSolrUpdate)
                .whenCompleted(2)
                .create();

        processor.process(exchange);

        assertTrue(notify.matches(6l, TimeUnit.SECONDS));

        server.commit();

        ContentObjectRecord unitMd = getSolrMetadata(unitObj);

        assertEquals("Title should not have updated", unitMd.getId(), unitMd.getTitle());

        assertTrue("Read groups did not contain assigned group", unitMd.getReadGroup().contains(PUBLIC_PRINC));
        assertTrue("Admin groups did not contain assigned group", unitMd.getAdminGroup().contains("admin"));

        assertNotNull(unitMd.getDateAdded());

        ContentObjectRecord collMd = getSolrMetadata(collObj);

        assertEquals("Title should not have updated", collMd.getId(), collMd.getTitle());

        assertTrue("Read groups did not contain assigned group", collMd.getReadGroup().contains(AUTHENTICATED_PRINC));
        assertTrue("Admin groups did not contain assigned group", collMd.getAdminGroup().contains("admin"));

        assertNotNull(collMd.getDateAdded());
        assertNotNull(collMd.getDateUpdated());
    }

    @Test
    public void testUpdateDescription() throws Exception {
        var folderObj = repositoryObjectFactory.createFolderObject(null);
        collObj.addMember(folderObj);
        indexObjectsInTripleStore();
        repositoryObjectSolrIndexer.index(unitObj.getPid(), collObj.getPid(), folderObj.getPid());

        var origCollTitle = getSolrMetadata(collObj).getTitle();
        assertEquals(origCollTitle, collObj.getPid().getId());
        var origFolderParentColl = getSolrMetadata(folderObj).getParentCollection();
        assertEquals(FilterableDisplayValueFacet.buildValue(origCollTitle, origCollTitle), origFolderParentColl);

        InputStream modsStream = streamResource("/datastreams/simpleMods.xml");
        updateDescriptionService.updateDescription(new UpdateDescriptionRequest(agent, collObj, modsStream));
        treeIndexer.indexTree(collObj.getModel());

        NotifyBuilder notify = new NotifyBuilder(cdrServiceSolrUpdate)
                .whenCompleted(2)
                .create();

        makeIndexingMessage(collObj, null, UPDATE_DESCRIPTION);

        processor.process(exchange);

        notify.matches(5l, TimeUnit.SECONDS);

        server.commit();

        ContentObjectRecord collMd = getSolrMetadata(collObj);

        assertEquals("Object title", collMd.getTitle());
        assertEquals("Boxy", collMd.getCreator().get(0));

        assertTrue(collMd.getContentStatus().toString(), collMd.getContentStatus().contains(FacetConstants.CONTENT_DESCRIBED));

        assertNotNull(collMd.getDateAdded());
        assertNotNull(collMd.getDateUpdated());

        // Verify that child object got its parent collection title updated
        var updatedFolderParentColl = getSolrMetadata(folderObj);
        assertEquals(FilterableDisplayValueFacet.buildValue("Object title", collMd.getId()),
                updatedFolderParentColl.getParentCollection());
    }

    @Test
    public void testAddChildren() throws Exception {
        // Add extra collection
        CollectionObject coll2Obj = repositoryObjectFactory.createCollectionObject(null);
        unitObj.addMember(coll2Obj);

        indexObjectsInTripleStore();

        repositoryObjectSolrIndexer.index(unitObj.getPid(), collObj.getPid(), coll2Obj.getPid());

        var origUnitUpdated = getSolrMetadata(unitObj).get_version_();
        var origColl1Updated = getSolrMetadata(collObj).get_version_();
        var origColl2Updated = getSolrMetadata(coll2Obj).get_version_();

        // Send message indicating the coll2 was added to the unit
        makeIndexingMessage(unitObj, singleton(coll2Obj.getPid()), ADD_SET_TO_PARENT);

        NotifyBuilder notify = new NotifyBuilder(cdrServiceSolrUpdate)
                .whenCompleted(2)
                .create();

        processor.process(exchange);

        notify.matches(3l, TimeUnit.SECONDS);

        server.commit();

        ContentObjectRecord unitMd = getSolrMetadata(unitObj);
        assertEquals("Unit record should not have been updated", origUnitUpdated, unitMd.get_version_());

        ContentObjectRecord coll1Md = getSolrMetadata(collObj);
        assertEquals("First collection should not have been updated", origColl1Updated, coll1Md.get_version_());

        ContentObjectRecord coll2Md = getSolrMetadata(coll2Obj);
        assertEquals(coll2Obj.getPid().getId(), coll2Md.getTitle());

        assertEquals(1, coll2Md.getReadGroup().size());
        assertTrue("Only admin group should have read assigned", coll2Md.getReadGroup().contains("admin"));
        assertTrue("Admin groups did not contain assigned group", coll2Md.getAdminGroup().contains("admin"));

        assertNotNull(coll2Md.getDateAdded());
        assertNotEquals(origColl2Updated, coll2Md.get_version_());

        assertEquals(2, coll2Md.getAncestorPath().size());
    }

    @Test
    public void testDelete() throws Exception {
        indexObjectsInTripleStore();

        repositoryObjectSolrIndexer.index(unitObj.getPid(), collObj.getPid());

        makeIndexingMessage(collObj, null, DELETE);

        processor.process(exchange);
        server.commit();

        assertNotNull("Unit must still be present", getSolrMetadata(unitObj));
        assertNull("Collection record must be removed", getSolrMetadata(collObj));
    }

    @Test
    public void testDeleteTree() throws Exception {
        indexObjectsInTripleStore();

        repositoryObjectSolrIndexer.index(rootObj.getPid(), unitObj.getPid(), collObj.getPid());

        // Wait for indexing to complete
        NotifyBuilder notify = new NotifyBuilder(cdrServiceSolrUpdate)
                .whenCompleted(2)
                .create();

        makeIndexingMessage(rootObj, null, RECURSIVE_ADD);
        processor.process(exchange);

        notify.matches(5l, TimeUnit.SECONDS);

        server.commit();

        makeIndexingMessage(unitObj, null, DELETE_SOLR_TREE);
        processor.process(exchange);

        server.commit();

        assertNotNull("Root must still be present", getSolrMetadata(rootObj));
        assertNull("Unit record must be removed", getSolrMetadata(unitObj));
        assertNull("Collection record must be removed", getSolrMetadata(collObj));
    }

    @Test
    public void testUpdateDescriptionWithCollectionFilter() throws Exception {
        collectionFilterTest(IndexingActionType.UPDATE_DESCRIPTION);
    }

    @Test
    public void testAddWithCollectionFilter() throws Exception {
        collectionFilterTest(IndexingActionType.ADD);
    }

    public void collectionFilterTest(IndexingActionType indexingType) throws Exception {
        // Configure collection filter to apply to the item being updated
        tmpFolder.create();
        File file = tmpFolder.newFile("collConfig.properties");
        FileUtils.writeStringToFile(file,
                collObj.getPid().getId() + "=" + RLASupplementalFilter.class.getName(),
                StandardCharsets.UTF_8);
        setCollectionSupplementalInformationFilter.setCollectionFilters(file.getAbsolutePath());

        InputStream modsStream = streamResource("/datastreams/modsWithRla.xml");
        updateDescriptionService.updateDescription(new UpdateDescriptionRequest(agent, collObj, modsStream));

        indexObjectsInTripleStore();

        repositoryObjectSolrIndexer.index(unitObj.getPid(), collObj.getPid());

        makeIndexingMessage(collObj, null, indexingType);

        processor.process(exchange);
        server.commit();

        ContentObjectRecord collMd = getSolrMetadata(collObj);

        assertEquals("RLA Item", collMd.getTitle());
        assertTrue(collMd.getContentStatus().contains(FacetConstants.CONTENT_DESCRIBED));

        assertEquals("DBoxc.jpg", collMd.getIdentifierSort());
        Map<String, Object> dynamics = collMd.getDynamicFields();
        assertEquals(2, dynamics.size());
        assertEquals("BoXC 5", dynamics.get(RLASupplementalFilter.SITE_CODE_FIELD));
        assertEquals("12345", dynamics.get(RLASupplementalFilter.CATALOG_FIELD));
    }

    private void makeIndexingMessage(RepositoryObject targetObj, Collection<PID> children, IndexingActionType action) {
        setMessageTarget(targetObj);
        Document messageBody = makeIndexingOperationBody("user", targetObj.getPid(), children, action);
        when(message.getBody()).thenReturn(messageBody);
    }

    private ContentObjectRecord getSolrMetadata(RepositoryObject obj) throws Exception {
        SolrQuery solrQuery = new SolrQuery();
        StringBuilder query = new StringBuilder();
        query.append("id:").append(obj.getPid().getId());

        solrQuery.setQuery(query.toString());
        solrQuery.setRows(1);

        QueryResponse resp = solrClient.query(solrQuery);

        List<ContentObjectSolrRecord> results = resp.getBeans(ContentObjectSolrRecord.class);
        if (results != null && results.size() > 0) {
            return results.get(0);
        }
        return null;
    }
}
