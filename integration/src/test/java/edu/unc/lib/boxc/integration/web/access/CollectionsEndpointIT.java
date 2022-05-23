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

import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore;
import edu.unc.lib.boxc.integration.factories.AdminUnitFactory;
import edu.unc.lib.boxc.model.fcrepo.services.RepositoryInitializer;
import edu.unc.lib.boxc.model.fcrepo.test.TestHelper;
import edu.unc.lib.boxc.search.api.requests.SimpleIdRequest;
import edu.unc.lib.boxc.search.solr.services.SolrSearchService;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

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

    protected final static String USERNAME = "test_user";
    protected final static AccessGroupSet GROUPS = new AccessGroupSetImpl("adminGroup");

    @Autowired
    protected String baseAddress;

    @Autowired
    protected RepositoryInitializer repoInitializer;

    @Autowired
    protected SolrSearchService solrSearchService;

    @Before
    public void setup() {
        TestHelper.setContentBase(baseAddress);

        GroupsThreadStore.storeUsername(USERNAME);
        GroupsThreadStore.storeGroups(GROUPS);

        repoInitializer.initializeRepository();
    }

    /**
     * Temporary test to demonstrate that factory object creation works
     * @throws Exception
     */
    @Test
    public void testAdminUnitConstruction() throws Exception {
        var adminUnit = adminUnitFactory.createAdminUnit(Map.of("title", "title1"));

        var adminUnitRecord = solrSearchService.getObjectById(new SimpleIdRequest(adminUnit.getPid(), GROUPS));
        assertNotNull(adminUnitRecord);
        assertEquals("title1", adminUnitRecord.getTitle());

        var httpClient = HttpClients.createDefault();
        var getMethod = new HttpGet("http://localhost:48080/access/collectionsJson");
        try (var resp = httpClient.execute(getMethod)) {
            assertEquals(200, resp.getStatusLine().getStatusCode());
        }
    }
}
