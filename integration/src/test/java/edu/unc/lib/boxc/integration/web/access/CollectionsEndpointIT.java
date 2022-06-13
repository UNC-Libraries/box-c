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
package edu.unc.lib.boxc.integration.web.access;

import com.fasterxml.jackson.databind.JsonNode;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore;
import edu.unc.lib.boxc.integration.factories.AdminUnitFactory;
import edu.unc.lib.boxc.integration.factories.CollectionFactory;
import edu.unc.lib.boxc.integration.factories.WorkFactory;
import edu.unc.lib.boxc.model.fcrepo.services.RepositoryInitializer;
import edu.unc.lib.boxc.model.fcrepo.test.TestHelper;
import edu.unc.lib.boxc.search.api.models.ContentObjectRecord;
import edu.unc.lib.boxc.search.api.requests.SimpleIdRequest;
import edu.unc.lib.boxc.search.solr.services.SolrSearchService;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.solr.client.solrj.SolrClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author bbpennel
 */
@ContextHierarchy({
        @ContextConfiguration("/spring-test/test-fedora-container.xml"),
        @ContextConfiguration("/spring-test/cdr-client-container.xml"),
        @ContextConfiguration("/spring-test/acl-service-context.xml"),
        @ContextConfiguration("/spring-test/solr-standalone-context.xml"),
        @ContextConfiguration("/spring-test/solr-indexing-context.xml"),
        @ContextConfiguration("/spring-test/object-factory-context.xml")
})
@RunWith(SpringJUnit4ClassRunner.class)
public class CollectionsEndpointIT {
    @Autowired
    private AdminUnitFactory adminUnitFactory;
    @Autowired
    private WorkFactory workFactory;
    @Autowired
    private CollectionFactory collectionFactory;

    @Autowired
    protected String baseAddress;

    @Autowired
    protected RepositoryInitializer repoInitializer;

    @Autowired
    protected SolrSearchService solrSearchService;

    @Autowired
    protected SolrClient solrClient;

    protected final static String USERNAME = "test_user";
    protected final static AccessGroupSet GROUPS = new AccessGroupSetImpl("adminGroup");

    private ContentObjectRecord adminUnitRecord;

    @Before
    public void setup() throws Exception {
        TestHelper.setContentBase(baseAddress);
        GroupsThreadStore.storeUsername(USERNAME);
        GroupsThreadStore.storeGroups(GROUPS);
        repoInitializer.initializeRepository();
        // reset solr before every test
        solrClient.deleteByQuery("*:*");

//        var options = Map.of("title", "Best title");
//        var adminUnit = adminUnitFactory.createAdminUnit(options);
//        var collection = collectionFactory.createCollection(adminUnit, options);
//        var work = workFactory.createWork(collection, options);
//
//        adminUnitRecord = solrSearchService.getObjectById(new SimpleIdRequest(adminUnit.getPid(), GROUPS));
    }

    /**
     * Temporary test to demonstrate that factory object creation works
     * @throws Exception
     */
//    @Test
//    public void testFactoryConstruction() throws Exception {
//        assertNotNull(adminUnitRecord);
//        assertEquals("Best title", adminUnitRecord.getTitle());
//
//        var httpClient = HttpClients.createDefault();
//        var getMethod = new HttpGet("http://localhost:48080/access/collectionsJson");
//        try (var resp = httpClient.execute(getMethod)) {
//            assertEquals(200, resp.getStatusLine().getStatusCode());
//        }
//    }

    @Test
    public void testCollectionsJsonOnlyReturnsAdminUnits() throws Exception {
        var options = Map.of("title", "Object" + System.nanoTime());
        var adminUnit1 = adminUnitFactory.createAdminUnit(Map.of("title", "Object1"));
        var adminUnit2 = adminUnitFactory.createAdminUnit(Map.of("title", "Object2"));
        collectionFactory.createCollection(adminUnit1, options);

        var httpClient = HttpClients.createDefault();
        var getMethod = new HttpGet("http://localhost:48080/access/collectionsJson");
        try (var resp = httpClient.execute(getMethod)) {
            ObjectMapper mapper = new ObjectMapper();
            var respJson = mapper.readTree(resp.getEntity().getContent());
            var metadata = IteratorUtils.toList(respJson.get("metadata").elements());

            assertEquals(200, resp.getStatusLine().getStatusCode());
            assertValuePresent(metadata, 0, "AdminUnit");
            assertValuePresent(metadata, 1, "AdminUnit");
        }
    }

    private void assertValuePresent(List<JsonNode> json, int index, String value) {
        var result = json.get(index);
        assertEquals(value, result.get("type").asText());
    }
}
